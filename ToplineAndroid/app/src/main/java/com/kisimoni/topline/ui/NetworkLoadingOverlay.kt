package com.kisimoni.topline.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun NetworkLoadingOverlay() {
    val transition = rememberInfiniteTransition(label = "network-loader")
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background.copy(alpha = 0.24f))
            .semantics {
                contentDescription = "Loading"
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
            contentAlignment = Alignment.Center,
    ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
            LoadingDot(transition, colorScheme.primary, 0)
            LoadingDot(transition, colorScheme.secondary, 140)
            LoadingDot(transition, colorScheme.primary, 280)
        }
    }
}

@Composable
private fun LoadingDot(
    transition: androidx.compose.animation.core.InfiniteTransition,
    color: Color,
    delayMillis: Int,
) {
    val pulse by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 420,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "network-loader-dot-pulse",
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .offset(y = ((1f - pulse) * 4f).dp)
            .size(9.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
                alpha = pulse
            }
            .clip(CircleShape)
            .background(color),
    )
}
