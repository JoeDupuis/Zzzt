package io.dupuis.zzzt.ui.trim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.dupuis.zzzt.data.repository.Clip
import io.dupuis.zzzt.data.repository.ClipRepository
import io.dupuis.zzzt.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrimUiState(
    val pending: AppContainer.PendingClip? = null,
    val name: String = "",
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val saved: Boolean = false,
    val notFound: Boolean = false,
)

class TrimViewModel(
    private val clipId: String,
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(TrimUiState())
    val state: StateFlow<TrimUiState> = _state.asStateFlow()

    init {
        val pending = container.pendingClips[clipId]
        if (pending == null) {
            _state.value = TrimUiState(notFound = true)
        } else {
            _state.value = TrimUiState(
                pending = pending,
                name = pending.title,
                trimStartMs = 0L,
                trimEndMs = pending.durationMs,
            )
        }
    }

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value)
    }

    fun onTrimChange(start: Long, end: Long) {
        _state.value = _state.value.copy(trimStartMs = start, trimEndMs = end)
    }

    fun save() {
        val current = _state.value
        val pending = current.pending ?: return
        viewModelScope.launch {
            val clip = Clip(
                id = clipId,
                title = current.name.ifBlank { pending.title },
                sourceUrl = pending.sourceUrl,
                audioPath = pending.audioPath,
                thumbnailPath = pending.thumbnailPath,
                durationMs = pending.durationMs,
                trimStartMs = current.trimStartMs,
                trimEndMs = current.trimEndMs,
                createdAt = System.currentTimeMillis(),
            )
            container.clipRepository.insert(clip)
            container.pendingClips.remove(clipId)
            _state.value = current.copy(saved = true)
        }
    }

    companion object {
        fun factory(clipId: String, container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TrimViewModel(clipId, container) as T
            }
    }
}
