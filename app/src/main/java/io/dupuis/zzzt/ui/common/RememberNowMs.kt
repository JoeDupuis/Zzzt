package io.dupuis.zzzt.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun rememberNowMs(
    periodMs: Long = 60_000L,
    clock: () -> Long = System::currentTimeMillis,
): Long {
    var now by remember(clock) { mutableLongStateOf(clock()) }
    LaunchedEffect(periodMs, clock) {
        while (true) {
            delay(periodMs - clock() % periodMs)
            now = clock()
        }
    }
    return now
}
