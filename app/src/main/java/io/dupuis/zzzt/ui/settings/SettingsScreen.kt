package io.dupuis.zzzt.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.dupuis.zzzt.ZzztApp
import io.dupuis.zzzt.data.calendar.CalendarInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as ZzztApp).container }
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            calendarRepository = container.calendarRepository,
            settingsStore = container.settingsStore,
        ),
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            SectionTitle("Calendars on Bedtime")
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose which calendars contribute events to your Bedtime agenda.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                !state.hasPermission -> PermissionCard()
                state.calendars.isEmpty() -> Text(
                    text = "No visible calendars found on this device.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> CalendarList(
                    calendars = state.calendars,
                    selectedIds = state.selectedIds,
                    onToggle = viewModel::toggleCalendar,
                    onShowAll = viewModel::showAllCalendars,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun PermissionCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Calendar permission was not granted. Grant it from Bedtime to see your calendars here.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
private fun CalendarList(
    calendars: List<CalendarInfo>,
    selectedIds: Set<Long>?,
    onToggle: (Long, Boolean) -> Unit,
    onShowAll: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selectedIds == null) "All calendars (default)" else "${selectedIds.size} selected",
            fontSize = 12.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (selectedIds != null) {
            TextButton(onClick = onShowAll) { Text("Reset to all") }
        }
    }
    Surface(
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            calendars.forEachIndexed { i, cal ->
                val isChecked = selectedIds?.contains(cal.id) ?: true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(cal.id, !isChecked) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                cal.color?.let { Color(it) } ?: colors.primary,
                                CircleShape,
                            ),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cal.displayName,
                            fontSize = 15.sp,
                            color = colors.onSurface,
                        )
                        if (cal.accountName.isNotBlank() && cal.accountName != cal.displayName) {
                            Text(
                                text = cal.accountName,
                                fontSize = 12.sp,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                    Checkbox(checked = isChecked, onCheckedChange = { onToggle(cal.id, it) })
                }
                if (i < calendars.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.outlineVariant.copy(alpha = 0.5f)),
                    )
                }
            }
        }
    }
}
