package io.dupuis.zzzt.ui.bedtime

import io.dupuis.zzzt.data.repository.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Tests for the pure "next alarm on Bedtime banner" computation.
 *
 * Invariants being pinned down:
 *  - Only enabled alarms contribute.
 *  - If nothing is enabled and the system has no alarm, result is null (banner shows "No alarm set").
 *  - If Zzzt's soonest and the system's next differ, we pick the earlier.
 *  - The Zzzt label only shows when the chosen trigger IS a Zzzt alarm's firing time.
 */
class BedtimeNextAlarmTest {

    private val zone = ZoneId.of("UTC")
    private val wednesday10am: ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.of(2023, 4, 19, 10, 0), zone)

    private fun alarm(
        id: String = "a",
        hour: Int = 7,
        minute: Int = 0,
        daysMask: Int = 0b1111111,
        enabled: Boolean = true,
        starred: Boolean = true,
        label: String = "",
    ): Alarm = Alarm(
        id = id,
        hour = hour,
        minute = minute,
        label = label,
        daysMask = daysMask,
        enabled = enabled,
        starred = starred,
        skipNextAtMs = null,
        createdAt = 0L,
    )

    private fun ms(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), zone)
            .toInstant().toEpochMilli()

    @Test
    fun `no alarms and no system alarm returns null`() {
        val r = BedtimeNextAlarm.compute(emptyList(), systemNextMs = null, now = wednesday10am)
        assertNull(r.triggerMs)
        assertNull(r.label)
    }

    @Test
    fun `all alarms disabled and no system alarm returns null`() {
        val alarms = listOf(
            alarm(id = "a", hour = 7, minute = 0, enabled = false, label = "Wake"),
            alarm(id = "b", hour = 8, minute = 0, enabled = false, label = "Soft"),
        )
        val r = BedtimeNextAlarm.compute(alarms, systemNextMs = null, now = wednesday10am)
        assertNull("toggling everything off must null the banner", r.triggerMs)
        assertNull(r.label)
    }

    @Test
    fun `single enabled alarm is returned with its label`() {
        val alarms = listOf(alarm(id = "a", hour = 11, minute = 0, label = "Deadline"))
        val r = BedtimeNextAlarm.compute(alarms, systemNextMs = null, now = wednesday10am)
        assertEquals(ms(2023, 4, 19, 11, 0), r.triggerMs)
        assertEquals("Deadline", r.label)
    }

    @Test
    fun `disabled alarm does not contribute even when starred`() {
        val alarms = listOf(
            alarm(id = "off", hour = 6, minute = 0, enabled = false, label = "Morning"),
        )
        val r = BedtimeNextAlarm.compute(alarms, systemNextMs = null, now = wednesday10am)
        assertNull(r.triggerMs)
        assertNull(r.label)
    }

    @Test
    fun `system-only alarm is returned with null label`() {
        val sys = ms(2023, 4, 19, 13, 0)
        val r = BedtimeNextAlarm.compute(emptyList(), systemNextMs = sys, now = wednesday10am)
        assertEquals(sys, r.triggerMs)
        assertNull("system alarms don't carry Zzzt labels", r.label)
    }

    @Test
    fun `zzzt earlier than system returns zzzt with label`() {
        val alarms = listOf(alarm(id = "a", hour = 11, minute = 0, label = "Wake"))
        val sys = ms(2023, 4, 19, 14, 0)
        val r = BedtimeNextAlarm.compute(alarms, systemNextMs = sys, now = wednesday10am)
        assertEquals(ms(2023, 4, 19, 11, 0), r.triggerMs)
        assertEquals("Wake", r.label)
    }

    @Test
    fun `system earlier than zzzt returns system with null label`() {
        val alarms = listOf(alarm(id = "a", hour = 15, minute = 0, label = "Wake"))
        val sys = ms(2023, 4, 19, 11, 0)
        val r = BedtimeNextAlarm.compute(alarms, systemNextMs = sys, now = wednesday10am)
        assertEquals(sys, r.triggerMs)
        assertNull(
            "when system is chosen, we must not reuse a Zzzt label from a different alarm",
            r.label,
        )
    }

    @Test
    fun `disabled alarm label does not leak when system alarm matches its time`() {
        // Simulates the stale-AlarmManager race: user disabled the Zzzt alarm, its cancel
        // hasn't propagated yet, so systemNextMs still equals the disabled alarm's time.
        // We must NOT label the banner with the disabled alarm's label.
        val disabledAlarmMs = ms(2023, 4, 19, 11, 0)
        val alarms = listOf(
            alarm(id = "a", hour = 11, minute = 0, enabled = false, label = "Wake"),
        )
        val r = BedtimeNextAlarm.compute(alarms, systemNextMs = disabledAlarmMs, now = wednesday10am)
        assertEquals(disabledAlarmMs, r.triggerMs)
        assertNull("disabled Zzzt alarm label must not leak into banner", r.label)
    }

    @Test
    fun `picks earliest across multiple enabled alarms and uses its label`() {
        val alarms = listOf(
            alarm(id = "a", hour = 13, minute = 0, label = "Late"),
            alarm(id = "b", hour = 11, minute = 0, label = "Early"),
            alarm(id = "c", hour = 12, minute = 0, label = "Mid"),
        )
        val r = BedtimeNextAlarm.compute(alarms, systemNextMs = null, now = wednesday10am)
        assertEquals(ms(2023, 4, 19, 11, 0), r.triggerMs)
        assertEquals("Early", r.label)
    }

    @Test
    fun `zzzt and system at identical time prefers zzzt label`() {
        // Zzzt's getNextAlarmClock reflects its own setAlarmClock registrations — so if a
        // Zzzt alarm matches systemNextMs exactly, we want its label shown, not null.
        val at = ms(2023, 4, 19, 11, 0)
        val alarms = listOf(alarm(id = "a", hour = 11, minute = 0, label = "Wake"))
        val r = BedtimeNextAlarm.compute(alarms, systemNextMs = at, now = wednesday10am)
        assertEquals(at, r.triggerMs)
        assertEquals("Wake", r.label)
    }
}
