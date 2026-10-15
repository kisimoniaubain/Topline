package com.kisimoni.topline.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kisimoni.topline.ui.icons.BootstrapIcons

@Composable
fun RowScope.BottomTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    unreadCount: Int = 0,
    icon: @Composable () -> Unit,
) {
    val tint = if (selected) Color.White else Color(0xFF9E9E9E)
    Column(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) {
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
    Icon(BootstrapIcons.ChatLeftTextFill(), contentDescription = null)
}

@Composable
fun HomeOutlineIcon() {
    Icon(BootstrapIcons.HomeFill(), contentDescription = null)
}

@Composable
fun UserGroupIcon() {
    Icon(BootstrapIcons.PeopleFill(), contentDescription = null)
}

@Composable
fun ProfileOutlineIcon() {
    Icon(BootstrapIcons.PersonFill(), contentDescription = null)
}
