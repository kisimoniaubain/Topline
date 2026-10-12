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
import androidx.compose.ui.graphics.drawscope.Fill
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
fun MessengerIcon() {
    val tint = LocalContentColor.current
    val cutout = MaterialTheme.colorScheme.surface
    Canvas(Modifier.size(24.dp)) {
        val bubble = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.06f)
            cubicTo(size.width * 0.22f, size.height * 0.06f, size.width * 0.06f, size.height * 0.25f, size.width * 0.06f, size.height * 0.51f)
            cubicTo(size.width * 0.06f, size.height * 0.77f, size.width * 0.23f, size.height * 0.94f, size.width * 0.49f, size.height * 0.94f)
            cubicTo(size.width * 0.75f, size.height * 0.94f, size.width * 0.94f, size.height * 0.75f, size.width * 0.94f, size.height * 0.50f)
            cubicTo(size.width * 0.94f, size.height * 0.24f, size.width * 0.76f, size.height * 0.06f, size.width * 0.50f, size.height * 0.06f)
            close()
        }
        drawPath(bubble, tint)
        val bolt = Path().apply {
            moveTo(size.width * 0.54f, size.height * 0.20f)
            lineTo(size.width * 0.31f, size.height * 0.54f)
            lineTo(size.width * 0.47f, size.height * 0.54f)
            lineTo(size.width * 0.42f, size.height * 0.80f)
            lineTo(size.width * 0.70f, size.height * 0.43f)
            lineTo(size.width * 0.53f, size.height * 0.43f)
            close()
        }
        drawPath(bolt, cutout, style = Fill)
    }
}
