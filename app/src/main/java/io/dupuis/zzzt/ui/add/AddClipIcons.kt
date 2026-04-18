package io.dupuis.zzzt.ui.add

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun strokeIcon(
    name: String,
    build: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply(build).build()

private fun ImageVector.Builder.strokePath(
    pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
        pathBuilder = pathBuilder,
    )
}

internal val FileIcon: ImageVector = strokeIcon("FileIcon") {
    strokePath {
        moveTo(14f, 2f)
        horizontalLineTo(6f)
        arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
        verticalLineToRelative(16f)
        arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
        horizontalLineToRelative(12f)
        arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
        verticalLineTo(8f)
        close()
    }
    strokePath {
        moveTo(14f, 2f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(6f)
    }
}

internal val ChevronRightIcon: ImageVector = strokeIcon("ChevronRightIcon") {
    strokePath {
        moveTo(9f, 6f)
        lineToRelative(6f, 6f)
        lineToRelative(-6f, 6f)
    }
}
