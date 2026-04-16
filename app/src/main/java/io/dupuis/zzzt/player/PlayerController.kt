package io.dupuis.zzzt.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import io.dupuis.zzzt.data.repository.Clip
import java.io.File

class PlayerController(context: Context) {
    private val appContext = context.applicationContext
    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)),
    ).buildAsync()
    private var controller: MediaController? = null
    private var preparedClipId: String? = null

    init {
        controllerFuture.addListener({
            controller = controllerFuture.get()
        }, MoreExecutors.directExecutor())
    }

    private fun withController(action: (MediaController) -> Unit) {
        val mc = controller
        if (mc != null) {
            action(mc)
        } else {
            controllerFuture.addListener({
                action(controllerFuture.get())
            }, MoreExecutors.directExecutor())
        }
    }

    fun addListener(listener: Player.Listener) = withController { it.addListener(listener) }

    fun removeListener(listener: Player.Listener) = withController { it.removeListener(listener) }

    fun prepareClip(clip: Clip) = withController { mc ->
        doSetup(mc, clip)
        preparedClipId = clip.id
    }

    fun playClip(clip: Clip) = withController { mc ->
        if (preparedClipId != clip.id) {
            doSetup(mc, clip)
        }
        mc.play()
        preparedClipId = null
    }

    private fun doSetup(mc: MediaController, clip: Clip) {
        mc.setMediaItem(
            MediaItem.Builder()
                .setUri(Uri.fromFile(File(clip.audioPath)))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build(),
                )
                .build(),
        )
        mc.repeatMode = Player.REPEAT_MODE_ONE
        mc.prepare()
    }

    fun pause() {
        controller?.pause()
    }

    fun resume() {
        controller?.play()
    }

    fun release() {
        controller = null
        MediaController.releaseFuture(controllerFuture)
    }
}
