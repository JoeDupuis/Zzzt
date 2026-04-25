package io.dupuis.zzzt.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.dupuis.zzzt.ZzztApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fired by `AlarmManager.setAlarmClock`. Starts the foreground [AlarmRingService], which
 * posts a high-importance full-screen-intent notification that the platform uses to
 * launch [AlarmRingActivity] over the lock screen. This is the pattern AOSP DeskClock
 * uses (packages/apps/DeskClock). Launching the Activity directly from a receiver is
 * blocked by BAL on Android 14+ even during the alarm's allowlist window.
 *
 * Re-arm/disable is done here so the DB stays correct even if the user dismisses the
 * ring immediately or the service is killed before running its own coroutine.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_ALARM_FIRE) return
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return

        val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
            action = AlarmRingService.ACTION_START
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(
                AlarmRingService.EXTRA_SILENT,
                intent.getBooleanExtra(AlarmRingService.EXTRA_SILENT, false),
            )
        }
        context.startForegroundService(serviceIntent)

        val container = (context.applicationContext as ZzztApp).container
        container.appScope.launch(Dispatchers.IO) {
            val alarm = container.alarmRepository.getById(alarmId) ?: return@launch
            if (alarm.isOneShot) {
                container.alarmRepository.setEnabled(alarmId, false)
                container.alarmScheduler.cancel(alarmId)
            } else {
                container.alarmRepository.setSkipNext(alarmId, null)
                container.alarmScheduler.schedule(alarm.copy(skipNextAtMs = null))
            }
        }
    }
}
