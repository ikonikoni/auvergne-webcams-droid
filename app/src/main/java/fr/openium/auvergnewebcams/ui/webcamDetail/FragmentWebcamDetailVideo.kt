package fr.openium.auvergnewebcams.ui.webcamDetail

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import fr.openium.auvergnewebcams.R
import fr.openium.auvergnewebcams.base.AbstractFragmentWebcam
import fr.openium.auvergnewebcams.ext.hasNetwork
import fr.openium.auvergnewebcams.utils.LoadWebCamUtils
import fr.openium.kotlintools.ext.*
import fr.openium.rxtools.ext.fromIOToMain
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.exo_player_view.*
import kotlinx.android.synthetic.main.footer_webcam_detail.*
import kotlinx.android.synthetic.main.fragment_webcam_video.*
import timber.log.Timber

/**
 * Created by Openium on 19/02/2019.
 */
class FragmentWebcamDetailVideo : AbstractFragmentWebcam() {

    override val layoutId: Int = R.layout.fragment_webcam_video

    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaDataSourceFactory: DataSource.Factory

    // --- Life cycle
    // ---------------------------------------------------

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            playerViewWebcamVideo.setBackgroundColor(requireContext().getColorCompat(R.color.grey_medium))
            if (webcam.viewsurfHD.isNullOrEmpty()) {
                textViewWebcamDetailLowQualityOnly.show()
            }
        } else {
            playerViewWebcamVideo.setBackgroundColor(requireContext().getColorCompat(R.color.black))
            textViewWebcamDetailLowQualityOnly.gone()
        }
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer()
            playerViewWebcamVideo?.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || !::player.isInitialized) {
            initPlayer()
            playerViewWebcamVideo?.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    // --- Methods
    // ---------------------------------------------------

    private fun initPlayer() {
        player = SimpleExoPlayer.Builder(requireContext()).build()
        mediaDataSourceFactory = DefaultDataSourceFactory(requireContext(), Util.getUserAgent(requireContext(), "mpAW"))

        player.addListener(object : Player.EventListener {

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playerViewWebcamVideo.showWithAnimationCompat()
                }

                wasLastTimeLoadingSuccessfull = true
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                if (error.sourceException is HttpDataSource.InvalidResponseCodeException) {
                    wasLastTimeLoadingSuccessfull = false
                    updateDisplay()
                }

                if (error.sourceException is HttpDataSource.HttpDataSourceException) {
                    exo_buffering.showWithAnimationCompat()
                }

                if (error.sourceException is FileDataSource.FileDataSourceException) {
                    wasLastTimeLoadingSuccessfull = false
                    updateDisplay()
                }
            }
        })

        playerViewWebcamVideo.player = player
        playerViewWebcamVideo.requestFocus()
    }

    private fun releasePlayer() {
        if (Util.SDK_INT > 23) {
            playerViewWebcamVideo?.onPause()
            player.release()
        }
    }

    override fun showDetailContent() {
        linearLayoutWebcamVideoDetailContent.showWithAnimationCompat()
    }

    override fun hideDetailContent() {
        linearLayoutWebcamVideoDetailContent.goneWithAnimationCompat()
    }

    override fun setWebcam() {
        val url = webcam.getUrlForWebcam(prefUtils.isWebcamsHighQuality, true)
        val mediaSource = ProgressiveMediaSource.Factory(mediaDataSourceFactory).createMediaSource(Uri.parse(url))
        player.prepare(mediaSource)
        player.playWhenReady = true

        playerViewWebcamVideo.showController()
    }

    override fun resetWebcam() {
        updateDisplay()
        player.retry()
    }

    override fun shareWebCam() {
        val url = webcam.getUrlForWebcam(canBeHD = true, canBeVideo = true)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, String.format("%s \n%s", webcam.title, url))
            putExtra(Intent.EXTRA_SUBJECT, webcam.title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, getString(R.string.generic_chooser))

        chooser.resolveActivity(requireActivity().packageManager)?.also {
            startActivity(chooser)
        } ?: snackbar(R.string.generic_no_application_for_action, Snackbar.LENGTH_SHORT)
    }

    override fun saveWebcam() {
        val urlSrc = webcam.getUrlForWebcam(canBeHD = true, canBeVideo = true)
        val fileName = String.format("%s_%s.mp4", webcam.title ?: "", System.currentTimeMillis().toString())

        startService(urlSrc, false, fileName)
    }

    override fun refreshWebcam() {
        if (requireContext().hasNetwork) {
            Observable.zip(
                Observable.fromCallable { LoadWebCamUtils.getMediaViewSurf(webcam.viewsurfLD) },
                Observable.fromCallable { LoadWebCamUtils.getMediaViewSurf(webcam.viewsurfHD) },
                BiFunction { t1: String, t2: String ->
                    t1 to t2
                }).observeOn(Schedulers.io())
                .map { pair ->
                    webcam.mediaViewSurfLD = pair.first
                    webcam.mediaViewSurfHD = pair.second

                    viewModelWebcamDetail.updateWebcam(webcam)

                    webcam.mediaViewSurfLD to webcam.mediaViewSurfHD
                }
                .fromIOToMain()
                .subscribe({
                    player.stop()

                    setWebcam()
                    resetWebcam()
                }, { Timber.e(it) }).addTo(disposables)
        } else {
            snackbar(R.string.generic_network_error, Snackbar.LENGTH_SHORT)
        }
    }
}