package io.dupuis.zzzt.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.dupuis.zzzt.MainActivity
import io.dupuis.zzzt.data.repository.Alarm
import io.dupuis.zzzt.data.repository.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmScheduler(
    private val context: Context,
    private val repository: AlarmRepository,
    private val appScope: CoroutineScope,
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val triggerMs = AlarmTimeCalc.nextFiringMs(alarm) ?: run {
            cancel(alarm.id)
            return
        }
        val fireIntent = buildFireIntent(alarm.id)
        val showIntent = PendingIntent.getActivity(
            context,
            requestCode(alarm.id, kind = RequestKind.SHOW),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerMs, showIntent),
            fireIntent,
        )
    }

    fun cancel(id: String) {
        alarmManager.cancel(buildFireIntent(id))
    }

    fun reconcileAll() {
        appScope.launch(Dispatchers.IO) {
            repository.allEnabled().forEach { schedule(it) }
        }
    }

    fun skipNext(id: String) {
        appScope.launch(Dispatchers.IO) {
            val alarm = repository.getById(id) ?: return@launch
            val next = AlarmTimeCalc.nextFiringMs(alarm) ?: return@launch
            val updated = repository.setSkipNext(id, next) ?: return@launch
            schedule(updated)
        }
    }

    fun nextFiringAcrossAll(alarms: List<Alarm>): Long? =
        alarms.mapNotNull { AlarmTimeCalc.nextFiringMs(it) }.minOrNull()

    private fun buildFireIntent(id: String): PendingIntent {
        // Fire a broadcast to AlarmReceiver, which starts a foreground service that posts
        // a full-screen-intent notification. This is the pattern AOSP DeskClock uses and
        // the one that reliably bypasses Android 14+ BAL for alarm-style apps.
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_FIRE
            putExtra(EXTRA_ALARM_ID, id)
            `package` = context.packageName
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(id, kind = RequestKind.FIRE),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private enum class RequestKind { FIRE, SHOW }

    private fun requestCode(id: String, kind: RequestKind): Int {
        val base = id.hashCode()
        return if (kind == RequestKind.FIRE) base else base xor 0x5A5A5A5A.toInt()
    }

    companion object {
        const val ACTION_ALARM_FIRE = "io.dupuis.zzzt.ACTION_ALARM_FIRE"
        const val EXTRA_ALARM_ID = "alarmId"
    }
}
