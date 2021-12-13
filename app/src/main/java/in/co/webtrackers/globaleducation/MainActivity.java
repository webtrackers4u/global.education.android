package in.co.webtrackers.globaleducation;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import android.view.animation.AccelerateInterpolator;



import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private WebView __main_webview;
    private FrameLayout __main_preloader;
    private FrameLayout __main_splash;
    private FrameLayout __main_internet_error;
    private FrameLayout __main_inactive_container;
    private FirebaseRemoteConfig __firebase_remote_config;
    private final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 0;
    private final String FCM_TAG = "FCM TAG";
    private int __BACK_LEVEL = 0;
    private int __MENU_OPEN = 0;
    private String[] _PERMISSION_NEED = {};
    private String _FCM_TOKEN = "";
    private boolean first_open = true;
    //File_Chooser Params
    private static final int FILE_CHOOSER_RESULT_CODE   = 2888;
    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        //


        ///Remote config

        int cache_expiry = 3600;
        __firebase_remote_config = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(cache_expiry)
                .build();
        __firebase_remote_config.setConfigSettingsAsync(configSettings);

        __firebase_remote_config.setDefaultsAsync(R.xml.remote_config_defaults);




        __main_webview = findViewById(R.id.main_webview);
        __main_preloader = findViewById(R.id.main_preloader);
        __main_splash = findViewById(R.id.main_splash);
        __main_internet_error = findViewById(R.id.main_internet_error);
        __main_inactive_container = findViewById(R.id.main_inactive_container);

        try
        {
            // get input stream
            InputStream ims = getAssets().open("logo.png");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            ((ImageView)__main_splash.findViewById(R.id.splash_logo)).setImageDrawable(d);
            ims .close();
        }
        catch(IOException ex)
        {
            return;
        }


        //Blurry.with(this).radius(25).sampling(2).onto(__main_preloader);




        __main_webview.setWebChromeClient(new CustomChromeClient(this));
        __main_webview.setWebViewClient(new CustomWebViewClient(this));
        __main_webview.clearCache(true);
        __main_webview.clearHistory();
        __main_webview.getSettings().setJavaScriptEnabled(true);
        __main_webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        __main_webview.getSettings().setDomStorageEnabled(true);
        __main_webview.addJavascriptInterface(new CustomJavascriptInterface(), "Android");
        __main_webview.getSettings().setAllowFileAccess(true);
        __main_webview.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        //
        ((LinearLayout) findViewById(R.id.internet_retry_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                load_url();
                Toast.makeText(getApplicationContext(), "Retrying...", Toast.LENGTH_SHORT).show();
            }
        });


        //
        _PERMISSION_NEED = new String[]{};
        //checkPermission();
        init_app();
        init_fcm();
        fetch_remote_config();
        load_url();
    }

    private void init_fcm() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(FCM_TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();
                        _FCM_TOKEN = token;

                        // Log and toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(FCM_TAG, msg);
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void init_app(){
        String app_status = __firebase_remote_config.getString("app_status");
        if(!app_status.equals("active")){
            __main_inactive_container.setVisibility(View.VISIBLE);
        }
    }
    private void load_url(){
        __main_webview.loadUrl(__firebase_remote_config.getString("app_url"));
    }



    private void checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(_PERMISSION_NEED,
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        for (int res : grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                checkPermission();
            }
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        private Context __context;
        public CustomWebViewClient(Context ctx){
            __context = ctx;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            __main_internet_error.setVisibility(View.GONE);
            __main_preloader.animate().alpha(1f).setDuration(100).setInterpolator(new AccelerateInterpolator()).start();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            __MENU_OPEN=0;
            __main_splash.setVisibility(View.GONE);
            __main_preloader.animate().alpha(0f).setDuration(100).setInterpolator(new AccelerateInterpolator()).start();



            if (first_open) {
                __main_webview.evaluateJavascript("(function() { window.saveFCMToken('" + _FCM_TOKEN + "'); })();", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {

                    }
                });
                first_open=false;
            }

        }
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error){
            //__main_internet_error.setVisibility(View.VISIBLE);
            //Your code to do
            //Toast.makeText(getApplicationContext(), "Your Internet Connection May not be active Or " + error.getDescription(), Toast.LENGTH_LONG).show();
        }

    }
    private class CustomChromeClient extends WebChromeClient {
        private Context __context;
        public CustomChromeClient(Context ctx){
            __context = ctx;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

        }
        /**
         * Override the default window.alert display interface, avoiding the title as ": from file:////"
         */
        public boolean onJsAlert(WebView view, String url, String message,
                                 JsResult result) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

            builder.setTitle("Alert")
                    .setMessage(message)
                    .setPositiveButton("OK", null);

            // No need to bind key events
            // Shield keys with keycode equal to 84
            builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface dialog, int keyCode,KeyEvent event) {
                    Log.v("onJsAlert", "keyCode==" + keyCode + "event="+ event);
                    return true;
                }
            });
            // Disable response to the event of pressing the back button
            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.show();
            result.confirm();
            // Because there is no binding event, you need to force confirm, otherwise the page will be black and the content will not be displayed.
            return true;
            // return super.onJsAlert(view, url, message, result);
        }

        public boolean onJsConfirm(WebView view, String url, String message,
                                   final JsResult result) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle("Dialog")
                    .setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int which) {
                            result.confirm();
                        }
                    })
                    .setNeutralButton("cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            result.cancel();
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    result.cancel();
                }
            });

            // Shield keycode equal to 84 and other keys, to avoid the dialog box after the button message and the page can no longer pop up the dialog box
            builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode,KeyEvent event) {
                    Log.v("onJsConfirm", "keyCode==" + keyCode + "event="+ event);
                    return true;
                }
            });
            // Disable response to the event of pressing the back button
            // builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
            // return super.onJsConfirm(view, url, message, result);
        }

        /**
         * Override the default window.prompt display interface, avoiding the title as ": from file:////"
         * window.prompt ('Please enter your domain address', '618119.com');
         */
        public boolean onJsPrompt(WebView view, String url, String message,
                                  String defaultValue, final JsPromptResult result) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

            builder.setTitle("Dialog").setMessage(message);

            final EditText et = new EditText(view.getContext());
            et.setSingleLine();
            et.setText(defaultValue);
            builder.setView(et)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            result.confirm(et.getText().toString());
                        }

                    })
                    .setNeutralButton("cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            result.cancel();
                        }
                    });

            // Shield keycode equal to 84 and other keys, to avoid the dialog box after the button message and the page can no longer pop up the dialog box
            builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface dialog, int keyCode,KeyEvent event) {
                    Log.v("onJsPrompt", "keyCode==" + keyCode + "event="+ event);
                    return true;
                }
            });

            // Disable response to the event of pressing the back button
            // builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
            // return super.onJsPrompt(view, url, message, defaultValue,
            // result);
        }



        //For Android API < 11 (3.0 OS)
        public void openFileChooser(ValueCallback<Uri> valueCallback) {
            uploadMessage = valueCallback;
            openImageChooserActivity();
        }

        //For Android API >= 11 (3.0 OS)
        public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
            uploadMessage = valueCallback;
            openImageChooserActivity();
        }

        //For Android API >= 21 (5.0 OS)
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            uploadMessageAboveL = filePathCallback;
            openImageChooserActivity();
            return true;
        }
    }

    public class CustomJavascriptInterface{
        public CustomJavascriptInterface(){

        }
        @JavascriptInterface
        public void Toast(final String message){
            showToast(message);
        }

        @JavascriptInterface
        public void playVideo(final String url){
            openVideoPlayer(url);
        }


    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void openVideoPlayer(String url){
        Intent intent = new Intent(this,VideoPLayerActivity.class);
        intent.putExtra("url",url);
        startActivity(intent);
    }


    private void fetch_remote_config() {


        __firebase_remote_config.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d("FIREBASE MESSAGE", "Config params updated: " + updated);
                            //Toast.makeText(MainActivity.this, "Fetch and activate succeeded", Toast.LENGTH_SHORT).show();

                        } else {
                            //Toast.makeText(MainActivity.this, "Fetch failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        //__firebase_remote_config.fetch(0);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    /*
                    //Toast.makeText(this, String.valueOf(__BACK_LEVEL), Toast.LENGTH_SHORT).show();
                    if(__MENU_OPEN==1){
                        __main_webview.evaluateJavascript("(function() { window.setMenuActive(0); })();", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {

                            }
                        });
                    }else if (__BACK_LEVEL==1){
                        load_url();
                    } else if (__BACK_LEVEL==2 && __main_webview.canGoBack()){
                        __main_webview.goBack();
                    } else {
                        new AlertDialog.Builder(this)
                                //.setTitle("Close the application")
                                .setMessage("Do you want to exit app?")
                                //.setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        finish();
                                    }})
                                .setNegativeButton(android.R.string.no, null).show();
                        //finish();
                    }
                     */



                    if (__main_webview.canGoBack()) {
                        __main_webview.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessage && null == uploadMessageAboveL) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (uploadMessage != null) {
                uploadMessage.onReceiveValue(result);
                uploadMessage = null;
            }
        }
    }
    private void openImageChooserActivity() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILE_CHOOSER_RESULT_CODE);
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("type")) {
                String type = extras.getString("type");
                if (type.equals("test type")) {
                    Toast.makeText(this, extras.getString("message"), Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

}
