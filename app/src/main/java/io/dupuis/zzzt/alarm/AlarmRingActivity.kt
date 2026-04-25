package io.dupuis.zzzt.alarm

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import io.dupuis.zzzt.ZzztApp
import io.dupuis.zzzt.data.repository.Alarm
import io.dupuis.zzzt.ui.theme.ZzztTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * UI-only ring screen. The actual alarm state (ringtone, wake-lock, foreground
 * notification) lives in [AlarmRingService]. This Activity is launched by the
 * service's full-screen-intent notification when the device is locked/asleep, or
 * by the user tapping the heads-up notification when they're using the phone.
 *
 * Tapping Stop tells the service to stop; the service's `onDestroy` releases the
 * ringtone and the OS dismisses the notification. The Activity finishes itself.
 */
class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        // setTurnScreenOn only wakes the screen AT launch; once Android's display-off
        // timer fires the activity goes back to sleep and the keyguard swallows focus.
        // FLAG_KEEP_SCREEN_ON holds the display on for the whole lifetime of this window
        // so the ring UI stays visible until the user taps Stop.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID).orEmpty()

        setContent {
            ZzztTheme {
                AlarmRingContent(
                    alarmId = alarmId,
                    onStop = {
                        AlarmRingService.stop(applicationContext)
                        finish()
                    },
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AlarmRingContent(alarmId: String, onStop: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = remember { (context.applicationContext as ZzztApp).container }
    var alarm by remember { mutableStateOf<Alarm?>(null) }
    LaunchedEffect(alarmId) {
        withContext(Dispatchers.IO) {
            alarm = if (alarmId.isNotEmpty()) container.alarmRepository.getById(alarmId) else null
        }
    }

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { now = LocalTime.now(); delay(1000L) }
    }
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "Alarm",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = now.format(formatter),
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = alarm?.label?.takeIf { it.isNotBlank() } ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onStop,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.size(width = 200.dp, height = 64.dp),
            ) {
                Text("Stop", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
