package io.dupuis.zzzt.ui.add

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.dupuis.zzzt.ZzztApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClipScreen(onBack: () -> Unit, onFetched: (String) -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as ZzztApp).container
    val viewModel: AddClipViewModel = viewModel(
        factory = AddClipViewModel.factory(context, container)
    )
    val uiState by viewModel.uiState.collectAsState()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.onFilePicked(uri)
    }

    val currentState = uiState
    if (currentState is AddClipUiState.Success) {
        LaunchedEffect(currentState.clipId) {
            onFetched(currentState.clipId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add clip") },
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                is AddClipUiState.Idle -> {
                    Button(onClick = { launcher.launch("*/*") }) {
                        Text("Pick file")
                    }
                }
                is AddClipUiState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Importing\u2026")
                    }
                }
                is AddClipUiState.Success -> Unit
                is AddClipUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.resetState() }) {
                            Text("Try again")
                        }
                    }
                }
            }
        }
    }
}
