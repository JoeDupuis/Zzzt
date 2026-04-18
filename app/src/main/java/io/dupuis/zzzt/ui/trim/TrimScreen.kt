package io.dupuis.zzzt.ui.trim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import io.dupuis.zzzt.ZzztApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimScreen(clipId: String, onSaved: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as ZzztApp).container
    val viewModel: TrimViewModel = viewModel(factory = TrimViewModel.factory(clipId, container))
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    val pending = state.pending
    if (state.notFound || pending == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Trim") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(if (state.notFound) "Clip not found" else "Loading…")
            }
        }
        return
    }

    val exoPlayer = remember(pending.audioPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(pending.audioPath))))
            prepare()
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(state.trimStartMs, state.trimEndMs) {
        if (exoPlayer.currentPosition < state.trimStartMs || exoPlayer.currentPosition > state.trimEndMs) {
            exoPlayer.seekTo(state.trimStartMs)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trim") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
            )

            val range = state.trimStartMs.toFloat()..state.trimEndMs.toFloat()
            Box(modifier = Modifier.fillMaxWidth().systemGestureExclusion()) {
                RangeSlider(
                    value = range,
                    onValueChange = { v -> viewModel.onTrimChange(v.start.toLong(), v.endInclusive.toLong()) },
                    valueRange = 0f..pending.durationMs.toFloat(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            Text(
                "${formatMs(state.trimStartMs)} – ${formatMs(state.trimEndMs)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = {
                    exoPlayer.seekTo(state.trimStartMs)
                    exoPlayer.play()
                }) { Text("Preview") }
                OutlinedButton(onClick = { exoPlayer.pause() }) { Text("Pause") }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                enabled = state.name.isNotBlank() && state.trimEndMs > state.trimStartMs,
            ) { Text("Save") }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
