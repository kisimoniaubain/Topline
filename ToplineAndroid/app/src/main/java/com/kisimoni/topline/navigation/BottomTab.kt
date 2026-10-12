package com.kisimoni.topline.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RowScope.BottomTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    unreadCount: Int = 0,
    icon: @Composable () -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides tint,
        ) {
            Box(Modifier.size(24.dp)) {
                icon()
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-5).dp)
                            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                            .background(Color(0xFFE53935), CircleShape)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
fun MessageDotsIcon() {
    val tint = LocalContentColor.current
    val cutout = MaterialTheme.colorScheme.surface
    Canvas(Modifier.size(24.dp)) {
        val bubble = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.12f)
            cubicTo(size.width * 0.08f, size.height * 0.12f, size.width * 0.04f, size.height * 0.19f, size.width * 0.04f, size.height * 0.28f)
            lineTo(size.width * 0.04f, size.height * 0.68f)
            cubicTo(size.width * 0.04f, size.height * 0.77f, size.width * 0.10f, size.height * 0.83f, size.width * 0.19f, size.height * 0.83f)
            lineTo(size.width * 0.28f, size.height * 0.83f)
            lineTo(size.width * 0.19f, size.height * 0.98f)
            cubicTo(size.width * 0.17f, size.height * 1.01f, size.width * 0.21f, size.height * 1.02f, size.width * 0.25f, size.height * 0.99f)
            lineTo(size.width * 0.48f, size.height * 0.83f)
            lineTo(size.width * 0.82f, size.height * 0.83f)
            cubicTo(size.width * 0.91f, size.height * 0.83f, size.width * 0.96f, size.height * 0.77f, size.width * 0.96f, size.height * 0.68f)
            lineTo(size.width * 0.96f, size.height * 0.28f)
            cubicTo(size.width * 0.96f, size.height * 0.19f, size.width * 0.90f, size.height * 0.12f, size.width * 0.81f, size.height * 0.12f)
            close()
        }
        drawPath(bubble, tint)
        val dotRadius = size.minDimension * 0.055f
        val dotY = size.height * 0.48f
        listOf(0.34f, 0.50f, 0.66f).forEach { x ->
            drawCircle(
                color = cutout,
                radius = dotRadius,
                center = androidx.compose.ui.geometry.Offset(size.width * x, dotY),
            )
        }
    }
}
