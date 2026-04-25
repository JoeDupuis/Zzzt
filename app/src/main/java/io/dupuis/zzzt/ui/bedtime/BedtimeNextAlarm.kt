package io.dupuis.zzzt.ui.bedtime

import io.dupuis.zzzt.alarm.AlarmTimeCalc
import io.dupuis.zzzt.data.repository.Alarm
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure computation of the "next alarm" shown on the Bedtime banner.
 *
 * Takes the full Zzzt alarm set (which may include disabled ones) plus the system's
 * `AlarmManager.getNextAlarmClock().triggerTime` and returns whichever fires first.
 *
 * The label only populates when the chosen trigger matches an ENABLED Zzzt alarm's
 * computed next firing — we never leak a disabled alarm's label even if the system
 * still reports its trigger time (e.g. briefly during a toggle-off race).
 */
object BedtimeNextAlarm {

    data class Result(val triggerMs: Long?, val label: String?)

    fun compute(
        alarms: List<Alarm>,
        systemNextMs: Long?,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()),
    ): Result {
        val enabled = alarms.filter { it.enabled }
        val enabledWithNext = enabled.mapNotNull { a ->
            AlarmTimeCalc.nextFiringMs(a, now)?.let { a to it }
        }
        val zzztNext = enabledWithNext.minByOrNull { it.second }?.second

        val candidates = listOfNotNull(systemNextMs, zzztNext)
        val chosen = candidates.minOrNull() ?: return Result(null, null)

        // Label only if an enabled Zzzt alarm's next firing lines up with the chosen trigger.
        val label = enabledWithNext.firstOrNull { it.second == chosen }?.first?.label
        return Result(chosen, label)
    }
}
