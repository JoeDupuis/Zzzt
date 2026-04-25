package io.dupuis.zzzt.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.dupuis.zzzt.ZzztApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return
        val container = (context.applicationContext as ZzztApp).container
        container.alarmScheduler.reconcileAll()
    }
}
