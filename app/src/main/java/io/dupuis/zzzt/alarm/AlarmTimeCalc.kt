package io.dupuis.zzzt.alarm

import io.dupuis.zzzt.data.repository.Alarm
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AlarmTimeCalc {

    fun nextFiringMs(alarm: Alarm, now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): Long? {
        if (!alarm.enabled) return null
        val target = LocalTime.of(alarm.hour, alarm.minute)
        val candidates = mutableListOf<ZonedDateTime>()
        if (alarm.isOneShot) {
            val today = now.toLocalDate()
            val todayAt = ZonedDateTime.of(today, target, now.zone)
            candidates += if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
        } else {
            for (offset in 0..7) {
                val date = now.toLocalDate().plusDays(offset.toLong())
                if (!maskIncludes(alarm.daysMask, date)) continue
                val at = ZonedDateTime.of(date, target, now.zone)
                if (at.isAfter(now)) candidates += at
            }
        }
        var chosen = candidates.minOrNull()?.toInstant()?.toEpochMilli() ?: return null
        val skip = alarm.skipNextAtMs
        if (skip != null && chosen == skip) {
            chosen = computeAfter(alarm, chosen, now.zone)
        }
        return chosen
    }

    private fun computeAfter(alarm: Alarm, skipAtMs: Long, zone: ZoneId): Long {
        val from = java.time.Instant.ofEpochMilli(skipAtMs).atZone(zone).plusMinutes(1)
        val target = LocalTime.of(alarm.hour, alarm.minute)
        if (alarm.isOneShot) {
            return ZonedDateTime.of(from.toLocalDate().plusDays(1), target, zone).toInstant().toEpochMilli()
        }
        for (offset in 0..7) {
            val date = from.toLocalDate().plusDays(offset.toLong())
            if (!maskIncludes(alarm.daysMask, date)) continue
            val at = ZonedDateTime.of(date, target, zone)
            if (at.isAfter(from)) return at.toInstant().toEpochMilli()
        }
        return ZonedDateTime.of(from.toLocalDate().plusDays(1), target, zone).toInstant().toEpochMilli()
    }

    private fun maskIncludes(mask: Int, date: LocalDate): Boolean {
        val bit = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }
        return (mask shr bit) and 1 == 1
    }
}
