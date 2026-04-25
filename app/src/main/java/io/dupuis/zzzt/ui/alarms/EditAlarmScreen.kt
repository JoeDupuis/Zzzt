package io.dupuis.zzzt.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.dupuis.zzzt.ZzztApp
import io.dupuis.zzzt.ui.common.DaysStrip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmScreen(
    alarmId: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as ZzztApp).container }
    val viewModel: EditAlarmViewModel = viewModel(
        factory = EditAlarmViewModel.factory(
            alarmRepository = container.alarmRepository,
            alarmScheduler = container.alarmScheduler,
        ),
    )
    LaunchedEffect(alarmId) { viewModel.load(alarmId) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onBack()
    }

    val timeState = rememberTimePickerState(
        initialHour = state.hour,
        initialMinute = state.minute,
        is24Hour = false,
    )

    LaunchedEffect(timeState.hour, timeState.minute) {
        if (state.loaded) viewModel.setTime(timeState.hour, timeState.minute)
    }

    LaunchedEffect(state.loaded, state.hour, state.minute) {
        if (state.loaded) {
            if (timeState.hour != state.hour) timeState.hour = state.hour
            if (timeState.minute != state.minute) timeState.minute = state.minute
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == null) "New alarm" else "Edit alarm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (alarmId != null) {
                        IconButton(onClick = viewModel::delete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !state.saving,
                        modifier = Modifier.padding(end = 4.dp),
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TimePicker(state = timeState)
            }
            Spacer(Modifier.height(16.dp))

            SectionHeader("Label")
            OutlinedTextField(
                value = state.label,
                onValueChange = viewModel::setLabel,
                placeholder = { Text("Morning") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            SectionHeader("Repeat")
            DaysStrip(
                mask = state.daysMask,
                editable = true,
                onToggle = viewModel::toggleDay,
            )
            if (state.daysMask == 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "One-time — fires once at the next occurrence.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))

            ToggleRow(
                title = "Show on Bedtime",
                subtitle = "Starred alarms appear on the Bedtime screen.",
                checked = state.starred,
                onCheckedChange = viewModel::setStarred,
            )
            Spacer(Modifier.height(4.dp))
            ToggleRow(
                title = "Enabled",
                subtitle = "Alarms fire only when enabled.",
                checked = state.enabled,
                onCheckedChange = viewModel::setEnabled,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
