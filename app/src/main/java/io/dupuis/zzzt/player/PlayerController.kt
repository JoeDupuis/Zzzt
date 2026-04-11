package io.dupuis.zzzt.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import io.dupuis.zzzt.data.repository.Clip
import java.io.File

class PlayerController(context: Context) {
    private val appContext = context.applicationContext
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    fun connect(onReady: (MediaController) -> Unit) {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val mc = future.get()
            controller = mc
            onReady(mc)
        }, MoreExecutors.directExecutor())
    }

    fun playClip(clip: Clip) {
        val mc = controller ?: return
        val item = MediaItem.Builder()
            .setUri(Uri.fromFile(File(clip.audioPath)))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimStartMs)
                    .setEndPositionMs(clip.trimEndMs)
                    .build(),
            )
            .build()
        mc.setMediaItem(item)
        mc.repeatMode = Player.REPEAT_MODE_ONE
        mc.prepare()
        mc.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun resume() {
        controller?.play()
    }

    fun release() {
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }
}
