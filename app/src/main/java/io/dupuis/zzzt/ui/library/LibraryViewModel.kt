package io.dupuis.zzzt.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.dupuis.zzzt.data.repository.Clip
import io.dupuis.zzzt.data.repository.ClipRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val repo: ClipRepository) : ViewModel() {
    val clips: StateFlow<List<Clip>> = repo.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun delete(clip: Clip) {
        viewModelScope.launch { repo.delete(clip) }
    }

    companion object {
        fun factory(repo: ClipRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LibraryViewModel(repo) as T
            }
    }
}
