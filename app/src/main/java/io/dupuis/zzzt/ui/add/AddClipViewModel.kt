package io.dupuis.zzzt.ui.add

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.dupuis.zzzt.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface AddClipUiState {
    data object Idle : AddClipUiState
    data object Loading : AddClipUiState
    data class Success(val clipId: String) : AddClipUiState
    data class Error(val message: String) : AddClipUiState
}

class AddClipViewModel(
    private val context: Context,
    private val container: AppContainer,
) : ViewModel() {

    private val _uiState: MutableStateFlow<AddClipUiState> = MutableStateFlow(AddClipUiState.Idle)
    val uiState: StateFlow<AddClipUiState> = _uiState.asStateFlow()

    fun onFilePicked(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AddClipUiState.Loading
            try {
                val mime = context.contentResolver.getType(uri) ?: ""
                if (!mime.startsWith("audio/") && !mime.startsWith("video/")) {
                    _uiState.value = AddClipUiState.Error("Unsupported file type: $mime")
                    return@launch
                }

                val displayName = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null, null, null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: uri.lastPathSegment ?: "clip"

                val ext = displayName.substringAfterLast('.', "").ifBlank {
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
                }

                val clipId = UUID.randomUUID().toString()
                val target = File(container.clipsDir, "$clipId.$ext")

                context.contentResolver.openInputStream(uri)!!.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }

                val retriever = MediaMetadataRetriever()
                val durationMs: Long
                val title: String
                var thumbPath = ""
                try {
                    retriever.setDataSource(context, uri)
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?: displayName.substringBeforeLast('.').ifBlank { "Untitled" }

                    val thumbFile = File(container.thumbsDir, "$clipId.jpg")
                    val embedded = retriever.embeddedPicture
                    if (embedded != null) {
                        thumbFile.outputStream().use { it.write(embedded) }
                        thumbPath = thumbFile.absolutePath
                    } else if (mime.startsWith("video/")) {
                        val bmp = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (bmp != null) {
                            thumbFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                            bmp.recycle()
                            thumbPath = thumbFile.absolutePath
                        }
                    }
                } finally {
                    retriever.release()
                }

                container.pendingClips[clipId] = AppContainer.PendingClip(
                    sourceUrl = uri.toString(),
                    title = title,
                    audioPath = target.absolutePath,
                    thumbnailPath = thumbPath,
                    durationMs = durationMs,
                )
                _uiState.value = AddClipUiState.Success(clipId)
            } catch (t: Throwable) {
                _uiState.value = AddClipUiState.Error(t.message ?: "Failed to import file")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddClipUiState.Idle
    }

    companion object {
        fun factory(context: Context, container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AddClipViewModel(context.applicationContext, container) as T
            }
    }
}
