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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerController(
    context: Context,
    private val onPlaybackStart: (clipId: String) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)),
    ).buildAsync()
    private var controller: MediaController? = null
    private var preparedClipId: String? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentClipId = MutableStateFlow<String?>(null)
    val currentClipId: StateFlow<String?> = _currentClipId.asStateFlow()

    private val internalListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
    }

    init {
        controllerFuture.addListener({
            controller = controllerFuture.get().also { it.addListener(internalListener) }
            _isPlaying.value = controller?.isPlaying == true
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
        _currentClipId.value = clip.id
        onPlaybackStart(clip.id)
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
        controller?.removeListener(internalListener)
        controller = null
        MediaController.releaseFuture(controllerFuture)
    }
}
