package io.dupuis.zzzt.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.dupuis.zzzt.ZzztApp
import io.dupuis.zzzt.data.repository.Alarm
import io.dupuis.zzzt.ui.common.DaysStrip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAlarmsScreen(
    onBack: () -> Unit,
    onNavigateEdit: (alarmId: String?) -> Unit,
) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as ZzztApp).container }
    val viewModel: ManageAlarmsViewModel = viewModel(
        factory = ManageAlarmsViewModel.factory(
            alarmRepository = container.alarmRepository,
            alarmScheduler = container.alarmScheduler,
        ),
    )
    val alarms by viewModel.alarms.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage alarms") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateEdit(null) },
                text = { Text("New alarm") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            HelperLegend()
            Spacer(Modifier.height(16.dp))
            if (alarms.isEmpty()) {
                EmptyState()
            } else {
                AlarmList(
                    alarms = alarms,
                    onToggleEnabled = viewModel::setEnabled,
                    onToggleStar = viewModel::setStarred,
                    onEdit = onNavigateEdit,
                )
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun HelperLegend() {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Starred alarms show on your Bedtime screen.",
                fontSize = 13.sp,
                color = colors.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "No alarms yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap New alarm to create your first one.",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlarmList(
    alarms: List<Alarm>,
    onToggleEnabled: (String, Boolean) -> Unit,
    onToggleStar: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            alarms.forEachIndexed { i, a ->
                AlarmRow(
                    alarm = a,
                    onToggleEnabled = { onToggleEnabled(a.id, it) },
                    onToggleStar = { onToggleStar(a.id, !a.starred) },
                    onEdit = { onEdit(a.id) },
                )
                if (i < alarms.lastIndex) {
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

@Composable
private fun AlarmRow(
    alarm: Alarm,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleStar: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
    ) {
        IconButton(onClick = onToggleStar, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = if (alarm.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (alarm.starred) "Unstar" else "Star",
                tint = if (alarm.starred) colors.primary else colors.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = format12h(alarm.hour, alarm.minute),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = ampm(alarm.hour),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                text = alarm.label.ifBlank { "(no label)" },
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DaysStrip(mask = alarm.daysMask)
        }
        Switch(checked = alarm.enabled, onCheckedChange = onToggleEnabled)
    }
}

private fun format12h(hour: Int, minute: Int): String {
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d".format(h12, minute)
}

private fun ampm(hour: Int): String = if (hour < 12) "AM" else "PM"
