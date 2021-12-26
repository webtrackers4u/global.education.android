package `in`.co.webtrackers.globaleducation

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import android.net.Uri
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.Util


class VideoPLayerActivity : AppCompatActivity(),Player.EventListener {

    var videoFullScreenPlayer: PlayerView? = null
    var player: SimpleExoPlayer? = null
    var spinnerVideoDetails: ProgressBar? = null
    var wakeLock: PowerManager.WakeLock? =null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        videoFullScreenPlayer = findViewById(R.id.videoFullScreenPlayer)
        spinnerVideoDetails = findViewById(R.id.spinnerVideoDetails)
        //dummy video link - https://www.rmp-streaming.com/media/big-buck-bunny-360p.mp4

        val url = intent.getStringExtra("url")?:""
        setUp(url)
        acquireWakeLock();
    }

    private fun acquireWakeLock(){
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GlobalEducation::VideoPlayer").apply {
                    acquire()
                }
            }
    }
    private fun releaseWakeLock(){
        wakeLock?.release();
    }



    private fun setUp(videoUrl:String) {
        initializePlayer()
        if (videoUrl == null) {
            return
        }
        buildMediaSource(Uri.parse(videoUrl))
    }

    private fun initializePlayer() {
        if (player == null) {
            val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
            val videoTrackSelectionFactory: TrackSelection.Factory =
                AdaptiveTrackSelection.Factory(bandwidthMeter)
            val trackSelector: TrackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
            // 1. Create a default TrackSelector
            var loadControl = DefaultLoadControl()
            // 2. Create the player
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl)
            videoFullScreenPlayer!!.player = player
        }
    }

    private fun buildMediaSource(mUri: Uri) {
        // Measures bandwidth during playback. Can be null if not required.
        val bandwidthMeter = DefaultBandwidthMeter()
        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, getString(R.string.app_name)), bandwidthMeter
        )
        // This is the MediaSource representing the media to be played.
        val videoSource: MediaSource =
            ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(mUri)
        // Prepare the player with the source.
        player!!.prepare(videoSource)
        player!!.playWhenReady = true
        player!!.addListener(this)
    }

    private fun releasePlayer() {
        if (player != null) {
            player!!.release()
            player = null
        }
    }

    private fun pausePlayer() {
        if (player != null) {
            player!!.playWhenReady = false
            player!!.playbackState
        }
    }

    private fun resumePlayer() {
        if (player != null) {
            player!!.playWhenReady = true
            player!!.playbackState
        }
    }

    override fun onPause() {
        super.onPause()
        pausePlayer()
        /* if (mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }*/
    }

    override fun onRestart() {
        super.onRestart()
        resumePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        releaseWakeLock()
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}

    override fun onLoadingChanged(isLoading: Boolean) {}

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> spinnerVideoDetails!!.visibility = View.VISIBLE
            Player.STATE_ENDED -> {
            }
            Player.STATE_IDLE -> {
            }
            Player.STATE_READY -> spinnerVideoDetails!!.visibility = View.GONE
            else -> {
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {}

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

    override fun onPlayerError(error: ExoPlaybackException?) {}

    override fun onPositionDiscontinuity(reason: Int) {}

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}

    override fun onSeekProcessed() {}

}