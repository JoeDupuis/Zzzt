package io.dupuis.zzzt.alarm

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import io.dupuis.zzzt.MainActivity

/**
 * Debug-only entry point to schedule a `setAlarmClock` for N seconds from now, using
 * the EXACT same broadcast-PI → AlarmReceiver → AlarmRingService pipeline that real
 * alarms use. Invoke from adb:
 *
 *   adb shell am start -n io.dupuis.zzzt/.alarm.DebugAlarmTrigger --ei sec 20
 *
 * The receiver will log-harmlessly miss the "debug-test" alarm in the DB (no rearm),
 * but the ring service / notification / activity path fires identically.
 */
class DebugAlarmTrigger : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sec = intent.getIntExtra("sec", 30)
        // Default silent=true so debug triggers don't blast the emulator audio through the
        // host's speakers. Override with `--ez loud true` when testing the ringtone path.
        val silent = !intent.getBooleanExtra("loud", false)

        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        val triggerMs = System.currentTimeMillis() + sec * 1000L

        val show = PendingIntent.getActivity(
            this,
            REQ_SHOW,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val fire = PendingIntent.getBroadcast(
            this,
            REQ_FIRE,
            Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmScheduler.ACTION_ALARM_FIRE
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, "debug-test")
                putExtra(AlarmRingService.EXTRA_SILENT, silent)
                `package` = packageName
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMs, show), fire)

        val toast = "Zzzt debug alarm in ${sec}s${if (silent) " (silent)" else ""}"
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
        finish()
    }

    private companion object {
        const val REQ_SHOW = 90001
        const val REQ_FIRE = 90002
    }
}
