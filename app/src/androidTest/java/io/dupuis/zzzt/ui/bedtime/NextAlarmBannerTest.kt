package io.dupuis.zzzt.ui.bedtime

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.dupuis.zzzt.ui.common.rememberNowMs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NextAlarmBannerTest {

    @get:Rule
    val rule = createComposeRule()

    private var fakeNowMs = 0L
    private val clock: () -> Long = { fakeNowMs }

    @Test
    fun countdownTracksTimePassing() {
        val triggerMs = 9 * 3_600_000L
        rule.mainClock.autoAdvance = false
        rule.setContent {
            NextAlarmBanner(
                triggerMs = triggerMs,
                label = null,
                nowMs = rememberNowMs(clock = clock),
                onClick = {},
            )
        }
        rule.onNodeWithText("in 9h 0m", substring = true).assertExists()

        fakeNowMs = 2 * 3_600_000L
        rule.mainClock.advanceTimeBy(2 * 3_600_000L)
        rule.onNodeWithText("in 7h 0m", substring = true).assertExists()
    }
}
