package io.dupuis.zzzt.alarm

import io.dupuis.zzzt.data.repository.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Days are Mon-first: bit 0 = Mon, 1 = Tue, 2 = Wed, 3 = Thu, 4 = Fri, 5 = Sat, 6 = Sun.
 * daysMask == 0 means "one-shot".
 */
class AlarmTimeCalcTest {

    private val zone = ZoneId.of("UTC")

    // Wednesday, April 19, 2023, 10:00 UTC — used as "now" in most cases.
    private val wednesday10am: ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.of(2023, 4, 19, 10, 0), zone)

    private fun alarm(
        hour: Int = 7,
        minute: Int = 0,
        daysMask: Int = 0,
        enabled: Boolean = true,
        starred: Boolean = false,
        skipNextAtMs: Long? = null,
        id: String = "test",
    ): Alarm = Alarm(
        id = id,
        hour = hour,
        minute = minute,
        label = "",
        daysMask = daysMask,
        enabled = enabled,
        starred = starred,
        skipNextAtMs = skipNextAtMs,
        createdAt = 0L,
    )

    private fun expectedMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), zone)
            .toInstant().toEpochMilli()

    @Test
    fun `disabled alarm returns null`() {
        val a = alarm(hour = 7, minute = 0, daysMask = 0b0011111, enabled = false)
        assertNull(AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    // ──────────────────────────────────────────────────────────────
    // One-shot (daysMask == 0)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `one-shot alarm later today fires today`() {
        val a = alarm(hour = 22, minute = 30, daysMask = 0)
        val expected = expectedMs(2023, 4, 19, 22, 30)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `one-shot alarm earlier today fires tomorrow`() {
        val a = alarm(hour = 6, minute = 0, daysMask = 0)
        val expected = expectedMs(2023, 4, 20, 6, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `one-shot alarm at exactly now fires tomorrow`() {
        // Alarm at 10:00 when now is 10:00 — today's candidate is not "after" now.
        val a = alarm(hour = 10, minute = 0, daysMask = 0)
        val expected = expectedMs(2023, 4, 20, 10, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `one-shot midnight alarm fires at next midnight`() {
        val a = alarm(hour = 0, minute = 0, daysMask = 0)
        // now = Wed 10:00; today's 00:00 has passed; next midnight = Thu 00:00.
        val expected = expectedMs(2023, 4, 20, 0, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    // ──────────────────────────────────────────────────────────────
    // Recurring — weekday masks
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `weekday alarm 7am fires same day when still in future`() {
        // now = Wed 10:00; Wed is bit 2 set; alarm at 11:00 is still ahead today.
        val monToFri = 0b0011111
        val a = alarm(hour = 11, minute = 0, daysMask = monToFri)
        val expected = expectedMs(2023, 4, 19, 11, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `weekday alarm already past today moves to next matching day`() {
        // Wed 10:00; alarm at 07:00 Mon-Fri today already fired; next: Thu 07:00.
        val monToFri = 0b0011111
        val a = alarm(hour = 7, minute = 0, daysMask = monToFri)
        val expected = expectedMs(2023, 4, 20, 7, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `weekday alarm on Friday evening rolls to Monday`() {
        // Friday April 21 2023, 20:00; next weekday morning = Monday Apr 24 07:00.
        val monToFri = 0b0011111
        val fridayEvening = ZonedDateTime.of(LocalDateTime.of(2023, 4, 21, 20, 0), zone)
        val a = alarm(hour = 7, minute = 0, daysMask = monToFri)
        val expected = expectedMs(2023, 4, 24, 7, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, fridayEvening))
    }

    @Test
    fun `weekend-only alarm on Wednesday fires Saturday`() {
        val satSun = 0b1100000
        val a = alarm(hour = 9, minute = 0, daysMask = satSun)
        val expected = expectedMs(2023, 4, 22, 9, 0)  // Saturday
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `single-day alarm on own day but in future fires today`() {
        // Wed only (bit 2); Wed 10:00; alarm at 11:30 -> today 11:30.
        val wedOnly = 1 shl 2
        val a = alarm(hour = 11, minute = 30, daysMask = wedOnly)
        val expected = expectedMs(2023, 4, 19, 11, 30)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `single-day alarm on own day but past fires next week`() {
        // Wed only; alarm at 06:00; now Wed 10:00 -> next Wed Apr 26 06:00.
        val wedOnly = 1 shl 2
        val a = alarm(hour = 6, minute = 0, daysMask = wedOnly)
        val expected = expectedMs(2023, 4, 26, 6, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `daily alarm always picks tomorrow once today has passed`() {
        val daily = 0b1111111
        val a = alarm(hour = 8, minute = 0, daysMask = daily)
        // now = Wed 10:00 so today 08:00 already gone -> Thu 08:00.
        val expected = expectedMs(2023, 4, 20, 8, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `day mapping Monday-through-Sunday is correct`() {
        // Anchor on a known Monday: Apr 17 2023.
        val monday5am = ZonedDateTime.of(LocalDateTime.of(2023, 4, 17, 5, 0), zone)
        // Alarm only on Sunday (bit 6) at 08:00 should fire next Sunday Apr 23 08:00.
        val sundayOnly = 1 shl 6
        val a = alarm(hour = 8, minute = 0, daysMask = sundayOnly)
        val expected = expectedMs(2023, 4, 23, 8, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, monday5am))
    }

    // ──────────────────────────────────────────────────────────────
    // Skip-next
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `skipNext equal to current next shifts to following occurrence`() {
        // Mon-Fri, 07:00; now Wed 10:00; default next = Thu 07:00.
        // If we mark Thu 07:00 as skipped, next should be Fri 07:00.
        val monToFri = 0b0011111
        val thuSevenAm = expectedMs(2023, 4, 20, 7, 0)
        val a = alarm(hour = 7, minute = 0, daysMask = monToFri, skipNextAtMs = thuSevenAm)
        val expected = expectedMs(2023, 4, 21, 7, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `skipNext not matching next firing is ignored`() {
        // Next = Thu 07:00. If skip is set to some earlier obsolete time, ignore it.
        val monToFri = 0b0011111
        val obsoleteSkip = expectedMs(2023, 4, 18, 7, 0) // Tuesday, in the past
        val a = alarm(hour = 7, minute = 0, daysMask = monToFri, skipNextAtMs = obsoleteSkip)
        val expected = expectedMs(2023, 4, 20, 7, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `skipNext on one-shot pushes firing another day forward`() {
        // One-shot at 22:30 today; skip today -> should fire tomorrow at 22:30.
        val todayAt = expectedMs(2023, 4, 19, 22, 30)
        val a = alarm(hour = 22, minute = 30, daysMask = 0, skipNextAtMs = todayAt)
        val expected = expectedMs(2023, 4, 20, 22, 30)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    @Test
    fun `skipNext on Friday-only alarm moves to next Friday`() {
        val fridayOnly = 1 shl 4
        // now Wed 10:00; next firing = this Friday Apr 21 07:00.
        val thisFriday = expectedMs(2023, 4, 21, 7, 0)
        val a = alarm(hour = 7, minute = 0, daysMask = fridayOnly, skipNextAtMs = thisFriday)
        val expected = expectedMs(2023, 4, 28, 7, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, wednesday10am))
    }

    // ──────────────────────────────────────────────────────────────
    // Month/year boundary edge cases
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `one-shot rolls across month boundary`() {
        val apr30Late = ZonedDateTime.of(LocalDateTime.of(2023, 4, 30, 23, 0), zone)
        val a = alarm(hour = 1, minute = 0, daysMask = 0)  // 01:00 already passed by "now" 23:00
        val expected = expectedMs(2023, 5, 1, 1, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, apr30Late))
    }

    @Test
    fun `recurring alarm rolls across year boundary`() {
        // Sat Dec 30 2023 23:00; Mon-Fri alarm at 07:00; next = Mon Jan 1 2024 07:00.
        val dec30Late = ZonedDateTime.of(LocalDateTime.of(2023, 12, 30, 23, 0), zone)
        val monToFri = 0b0011111
        val a = alarm(hour = 7, minute = 0, daysMask = monToFri)
        val expected = expectedMs(2024, 1, 1, 7, 0)
        assertEquals(expected, AlarmTimeCalc.nextFiringMs(a, dec30Late))
    }

    // ──────────────────────────────────────────────────────────────
    // General sanity
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `result is always strictly after now for future-scheduled alarms`() {
        val a = alarm(hour = 10, minute = 0, daysMask = 0b1111111)
        val nowMs = wednesday10am.toInstant().toEpochMilli()
        val next = AlarmTimeCalc.nextFiringMs(a, wednesday10am)
        assertNotNull(next)
        assertTrue("next ($next) should be > now ($nowMs)", next!! > nowMs)
    }

    @Test
    fun `result is never the skipped epoch`() {
        val monToFri = 0b0011111
        val firstNext = AlarmTimeCalc.nextFiringMs(
            alarm(hour = 7, minute = 0, daysMask = monToFri),
            wednesday10am,
        )!!
        val withSkip = AlarmTimeCalc.nextFiringMs(
            alarm(hour = 7, minute = 0, daysMask = monToFri, skipNextAtMs = firstNext),
            wednesday10am,
        )
        assertNotNull(withSkip)
        assertNotEquals(firstNext, withSkip)
    }
}
