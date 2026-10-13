package com.kisimoni.topline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kisimoni.topline.data.api.InboxContact
import com.kisimoni.topline.data.api.PostItem

private val ShareOrange = Color(0xFFF57F17)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostShareSheet(
    post: PostItem,
    contacts: List<InboxContact>,
    isSaved: Boolean,
    isDownloading: Boolean,
    isLoadingContacts: Boolean,
    onCopyLink: () -> Unit,
    onShareToApps: () -> Unit,
    onDownload: () -> Unit,
    onSave: () -> Unit,
    onManageCollections: () -> Unit,
    onSendToContact: (InboxContact, String, (Boolean, String?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var message by remember(post.id) { mutableStateOf("") }
    var sendError by remember(post.id) { mutableStateOf<String?>(null) }
    val sendingContacts = remember(post.id) { mutableStateMapOf<String, Boolean>() }
    val sentContacts = remember(post.id) { mutableStateMapOf<String, Boolean>() }
    val shareUrl = post.playableVideoUrl() ?: post.image ?: post.mediaUrl
    val description = post.text.ifBlank { "Video by @${post.author.username}" }
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(bottom = 24.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Share", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Share, contentDescription = "Close share options", tint = colorScheme.onSurfaceVariant)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val thumbnail = post.coverImage ?: post.image ?: post.mediaUrl ?: post.video ?: post.videoUrl
                if (!thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = "Post preview",
                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        Modifier.size(58.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = ShareOrange)
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(description, maxLines = 2, fontWeight = FontWeight.Medium)
                    Text(
                        "@${post.author.username}",
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ShareAction(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy link",
                    onClick = onCopyLink,
                    enabled = !shareUrl.isNullOrBlank(),
                )
                ShareAction(
                    icon = Icons.Default.Share,
                    label = "Other apps",
                    onClick = onShareToApps,
                    enabled = !shareUrl.isNullOrBlank(),
                )
                ShareAction(
                    icon = Icons.Default.Download,
                    label = if (isDownloading) "Saving…" else "Download",
                    onClick = onDownload,
                    enabled = !isDownloading && post.playableVideoUrl() != null,
                    isLoading = isDownloading,
                )
                ShareAction(
                    icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = if (isSaved) "Saved" else "Bookmark",
                    onClick = onSave,
                    tint = if (isSaved) ShareOrange else colorScheme.onSurface,
                )
                ShareAction(
                    icon = Icons.Default.BookmarkBorder,
                    label = "Collections",
                    onClick = onManageCollections,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(
                "Send to",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (isLoadingContacts && contacts.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ShareOrange)
                }
            } else if (contacts.isEmpty()) {
                Text(
                    "Follow people to share videos with them on Topline.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    color = colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        val isSending = sendingContacts[contact.id] == true
                        val wasSent = sentContacts[contact.id] == true
                        Column(
                            Modifier.width(72.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box {
                                AccountAvatar(contact.name, contact.profilePicture, 54)
                                IconButton(
                                    onClick = {
                                        val messageText = buildString {
                                            append("Check out this post on Topline")
                                            if (post.author.username.isNotBlank()) append(" by @${post.author.username}")
                                            if (post.text.isNotBlank()) append("\n${post.text}")
                                            if (!shareUrl.isNullOrBlank()) append("\n$shareUrl")
                                            if (message.isNotBlank()) append("\n\n$message")
                                        }
                                        sendingContacts[contact.id] = true
                                        sendError = null
                                        onSendToContact(contact, messageText) { success, error ->
                                            sendingContacts.remove(contact.id)
                                            if (success) {
                                                sentContacts[contact.id] = true
                                            } else {
                                                sendError = error ?: "Unable to send this post."
                                            }
                                        }
                                    },
                                    enabled = !isSending && !wasSent,
                                    modifier = Modifier.align(Alignment.BottomEnd).size(26.dp)
                                        .clip(CircleShape).background(ShareOrange),
                                ) {
                                    if (isSending) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send to ${contact.name}",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                contact.name,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurface,
                            )
                            if (wasSent) {
                                Text("Sent", color = ShareOrange, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            if (!sendError.isNullOrBlank()) {
                Text(
                    sendError.orEmpty(),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    color = colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (contacts.isNotEmpty()) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(300) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Add a message (optional)") },
                )
            }
        }
    }
}

@Composable
private fun ShareAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    isLoading: Boolean = false,
) {
    Column(
        Modifier.width(64.dp).clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ShareOrange, strokeWidth = 2.dp)
            } else {
                Icon(icon, contentDescription = null, tint = tint)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}
