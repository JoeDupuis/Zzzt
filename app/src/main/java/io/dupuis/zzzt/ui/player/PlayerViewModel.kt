package io.dupuis.zzzt.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import io.dupuis.zzzt.data.repository.Clip
import io.dupuis.zzzt.data.repository.ClipRepository
import io.dupuis.zzzt.player.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    appContext: Context,
    private val repository: ClipRepository,
    private val clipId: String,
) : ViewModel() {

    private val controller = PlayerController(appContext)

    private val _clip = MutableStateFlow<Clip?>(null)
    val clip: StateFlow<Clip?> = _clip.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repository.getById(clipId)
            _clip.value = loaded
            if (loaded != null) {
                controller.connect { mc ->
                    mc.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                        }
                    })
                    controller.playClip(loaded)
                }
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) controller.pause() else controller.resume()
    }

    override fun onCleared() {
        controller.release()
        super.onCleared()
    }

    companion object {
        fun factory(
            appContext: Context,
            repository: ClipRepository,
            clipId: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PlayerViewModel(appContext, repository, clipId) as T
        }
    }
}
