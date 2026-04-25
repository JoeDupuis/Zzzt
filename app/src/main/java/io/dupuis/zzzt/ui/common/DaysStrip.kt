package io.dupuis.zzzt.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Letters = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun DaysStrip(
    mask: Int,
    editable: Boolean = false,
    onToggle: ((bitIndex: Int) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Letters.forEachIndexed { i, letter ->
            val on = (mask shr i) and 1 == 1
            val size = if (editable) 28.dp else 20.dp
            var mod: Modifier = Modifier.size(size)
            mod = if (on) {
                mod.background(colors.primary, CircleShape)
            } else {
                mod
                    .background(androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                    .border(1.dp, colors.outlineVariant, CircleShape)
            }
            if (editable && onToggle != null) {
                mod = mod.clickable { onToggle(i) }
            }
            Box(modifier = mod, contentAlignment = Alignment.Center) {
                Text(
                    text = letter,
                    fontSize = if (editable) 12.sp else 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) colors.onPrimary else colors.onSurfaceVariant,
                )
            }
        }
    }
}
