package io.dupuis.zzzt.ui.bedtime

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.dupuis.zzzt.ZzztApp
import io.dupuis.zzzt.data.repository.Alarm
import io.dupuis.zzzt.data.repository.Clip
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BedtimeScreen(
    onNavigateSelectClip: () -> Unit,
    onNavigateSettings: () -> Unit,
) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as ZzztApp).container }
    val viewModel: BedtimeViewModel = viewModel(
        factory = BedtimeViewModel.factory(
            appContext = context.applicationContext,
            alarmRepository = container.alarmRepository,
            clipRepository = container.clipRepository,
            calendarRepository = container.calendarRepository,
            alarmScheduler = container.alarmScheduler,
        ),
    )
    val state by viewModel.state.collectAsState()
    val isPlaying by container.playerController.isPlaying.collectAsState()
    val currentClipId by container.playerController.currentClipId.collectAsState()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshAgenda() }

    LaunchedEffect(Unit) {
        if (!state.calendarPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        } else {
            viewModel.refreshAgenda()
        }
    }

    // Re-read system next-alarm + calendar whenever the screen comes to the foreground,
    // so external edits (another app's alarm, a new calendar event) show up immediately.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.onForeground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bedtime") },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Settings",
                        )
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
            Spacer(Modifier.height(4.dp))
            FullScreenIntentBanner(context = context)
            NextAlarmBanner(
                triggerMs = state.nextAlarmMs,
                label = state.nextAlarmLabel,
                onClick = {
                    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                },
            )
            Spacer(Modifier.height(16.dp))
            AgendaCard(
                dayMs = state.agendaDateMs,
                events = state.agenda,
                hasPermission = state.calendarPermission,
                onGrant = { calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                onOpenFullDay = {
                    val anchor = state.agendaDateMs ?: System.currentTimeMillis()
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/$anchor"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            LastPlayedCard(
                clip = state.lastPlayedClip,
                isPlaying = isPlaying && currentClipId == state.lastPlayedClip?.id,
                onTogglePlayback = {
                    val c = state.lastPlayedClip ?: return@LastPlayedCard
                    val playing = isPlaying && currentClipId == c.id
                    if (playing) container.playerController.pause()
                    else container.playerController.playClip(c)
                },
                onPickClip = onNavigateSelectClip,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FullScreenIntentBanner(context: Context) {
    // Only relevant on Android 14+ (API 34). Pre-UDC the permission is always granted.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return

    // Re-check each recomposition so granting from Settings reflects when we come back.
    val canUse = remember(context) { mutableStateOf(isFsiGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) canUse.value = isFsiGranted(context)
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    if (canUse.value) return

    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Alarms can't wake your screen",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onErrorContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Grant \"Full-screen notifications\" so Zzzt can show the alarm over the lock screen.",
                fontSize = 13.sp,
                color = colors.onErrorContainer,
            )
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("Open settings", color = colors.onErrorContainer, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun isFsiGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.canUseFullScreenIntent()
}

@Composable
private fun NextAlarmBanner(triggerMs: Long?, label: String?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "NEXT ALARM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (triggerMs != null) formatTimeOfDay(triggerMs) else "No alarm set",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onPrimaryContainer,
                    )
                    if (triggerMs != null) {
                        Text(
                            text = " · in ${formatDelta(triggerMs)}",
                            fontSize = 18.sp,
                            color = colors.onPrimaryContainer.copy(alpha = 0.65f),
                        )
                    }
                }
                if (!label.isNullOrBlank()) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaCard(
    dayMs: Long?,
    events: List<io.dupuis.zzzt.data.calendar.AgendaEvent>,
    hasPermission: Boolean,
    onGrant: () -> Unit,
    onOpenFullDay: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Event,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = agendaTitleFor(dayMs),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = dayMs?.let { formatDateShort(it) } ?: "",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(6.dp))
            when {
                !hasPermission -> PermissionPrompt(onGrant)
                events.isEmpty() -> Text(
                    text = "Nothing on the schedule.",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                else -> AgendaList(events)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onOpenFullDay)
                    .padding(vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Open full day",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary,
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Zzzt needs calendar access to show your morning.",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onGrant, contentPadding = PaddingValues(0.dp)) {
            Text("Grant access")
        }
    }
}

@Composable
private fun AgendaList(events: List<io.dupuis.zzzt.data.calendar.AgendaEvent>) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState())
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        0.85f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        events.forEachIndexed { i, event ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = if (event.allDay) "all day" else formatTimeOfDay(event.startMs),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    modifier = Modifier.width(62.dp),
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            event.calendarColor?.let { Color(it) } ?: colors.primary,
                            CircleShape,
                        ),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (i < events.lastIndex) {
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

@Composable
private fun LastPlayedCard(
    clip: Clip?,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onPickClip: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.primary, CircleShape)
                    .clickable(enabled = clip != null, onClick = onTogglePlayback),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickClip),
            ) {
                Text(
                    text = "LAST PLAYED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = clip?.title ?: "No clip yet — tap to add",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                MiniWaveform(enabled = clip != null)
            }
        }
    }
}

@Composable
private fun MiniWaveform(enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    val base = colors.onPrimaryContainer.copy(alpha = if (enabled) 0.7f else 0.35f)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth().height(18.dp),
    ) {
        val n = 34
        for (i in 0 until n) {
            val env = 0.35f + 0.65f * sin(Math.PI * (i.toDouble() / (n - 1))).toFloat()
            val j = sin((i + 1) * 3.1) * 43758.5453
            val r = (j - Math.floor(j)).toFloat()
            val h = max(0.15f, env * (0.55f + 0.45f * r))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(h)
                    .background(base, RoundedCornerShape(1.dp)),
            )
        }
    }
}

@Composable
private fun AlarmsHeader(onManage: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Alarm,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Alarms",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onManage) {
            Text("Manage", fontSize = 13.sp)
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StarredAlarmsList(
    alarms: List<Alarm>,
    onToggle: (String, Boolean) -> Unit,
    onSkipNext: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    if (alarms.isEmpty()) {
        Surface(
            color = colors.surfaceContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "No starred alarms — tap Manage to add some.",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }
    Surface(
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            alarms.forEachIndexed { i, a ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { onSkipNext(a.id) },
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .alpha(if (a.enabled) 1f else 0.55f),
                ) {
                    Text(
                        text = formatClockTime(a.hour, a.minute),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                        modifier = Modifier.width(92.dp),
                    )
                    Text(
                        text = a.label,
                        fontSize = 14.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = a.enabled,
                        onCheckedChange = { onToggle(a.id, it) },
                    )
                }
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

// ──────────────────────────────────────────────────────────────
// Formatters
// ──────────────────────────────────────────────────────────────

private fun formatTimeOfDay(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return z.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}

private fun formatClockTime(hour: Int, minute: Int): String {
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val ampm = if (hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, minute, ampm)
}

private fun formatDelta(targetMs: Long): String {
    val diff = targetMs - System.currentTimeMillis()
    if (diff < 0) return "now"
    val h = diff / 3_600_000
    val m = (diff % 3_600_000) / 60_000
    return when {
        h <= 0 -> "${m}m"
        else -> "${h}h ${m}m"
    }
}

private fun formatDateShort(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return z.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
}

private fun agendaTitleFor(ms: Long?): String {
    if (ms == null) return "Schedule"
    val today = java.time.LocalDate.now()
    val date = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow morning"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE"))
    }
}
