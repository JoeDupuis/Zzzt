package io.dupuis.zzzt.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.dupuis.zzzt.R
import io.dupuis.zzzt.ZzztApp

/**
 * Foreground service that holds the alarm ring state for the duration of a single
 * firing. Posts a high-importance notification with [NotificationCompat.Builder.setFullScreenIntent]
 * which the OS uses to launch [AlarmRingActivity] over the lock screen. This is the
 * pattern used by AOSP DeskClock and it's the one path that reliably bypasses
 * Android 14's BAL restrictions for alarm-style apps.
 */
class AlarmRingService : Service() {

    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRingAndRelease()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> start(
                alarmId = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID).orEmpty(),
                silent = intent?.getBooleanExtra(EXTRA_SILENT, false) == true,
            )
        }
        return START_NOT_STICKY
    }

    private fun start(alarmId: String, silent: Boolean) {
        ensureChannel()

        val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this,
            REQ_FULLSCREEN,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val label = runCatching {
            val container = (applicationContext as ZzztApp).container
            // Read-only; safe off main (getById is synchronous wrapper over Dao on IO).
            kotlinx.coroutines.runBlocking { container.alarmRepository.getById(alarmId)?.label }
        }.getOrNull().orEmpty()

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Alarm")
            .setContentText(label.ifBlank { "Tap to open" })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .build()

        // Android 14+: must pass the foregroundServiceType as declared in the manifest,
        // otherwise the system silently strips FGS status ("FGS stop ... has no types!")
        // and the notification/activity get torn down.
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notif,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        acquireWake()
        if (!silent) startRingtone()
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Zzzt alarm rings"
            setBypassDnd(true)
            enableLights(true)
            enableVibration(true)
            // No sound on the channel itself — we play the ringtone manually so it loops.
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }

    private fun startRingtone() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val r = RingtoneManager.getRingtone(this, uri) ?: return
        // Force USAGE_ALARM so the ringtone plays on STREAM_ALARM (uses alarm volume, exempt
        // from Do Not Disturb by default). Without this, Ringtone defaults to
        // USAGE_NOTIFICATION_RINGTONE → STREAM_RING, which is silenced by DND or low ring volume.
        r.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) r.isLooping = true
        r.play()
        ringtone = r
    }

    private fun acquireWake() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "zzzt:alarm-ring",
        ).apply { acquire(10 * 60 * 1000L) }
    }

    private fun stopRingAndRelease() {
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { wakeLock?.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        stopRingAndRelease()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "io.dupuis.zzzt.ACTION_ALARM_RING_START"
        const val ACTION_STOP = "io.dupuis.zzzt.ACTION_ALARM_RING_STOP"
        const val EXTRA_SILENT = "silent"
        const val CHANNEL_ID = "zzzt-alarms"
        const val NOTIF_ID = 0xA1AE
        private const val REQ_FULLSCREEN = 0xFA11

        fun stop(context: Context) {
            val i = Intent(context, AlarmRingService::class.java).setAction(ACTION_STOP)
            context.startService(i)
        }
    }
}
