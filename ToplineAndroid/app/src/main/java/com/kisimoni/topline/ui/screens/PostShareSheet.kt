package com.kisimoni.topline.ui.screens

import com.kisimoni.topline.ui.icons.BootstrapIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kisimoni.topline.data.api.InboxContact
import com.kisimoni.topline.data.api.PostItem

private val ShareOrange = Color(0xFFF57F17)
private val ShareSheetBackground = Color(0xFF19191B)
private val ShareSheetSurface = Color(0xFF29292C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostShareSheet(
    post: PostItem,
    contacts: List<InboxContact>,
    isSaved: Boolean,
    isDownloading: Boolean,
    isDownloadBusy: Boolean,
    downloadStatus: String?,
    downloadProgress: Float?,
    isLoadingContacts: Boolean,
    onCopyLink: () -> Unit,
    onShareToApps: () -> Unit,
    onDownload: () -> Unit,
    onSave: () -> Unit,
    onManageCollections: () -> Unit,
    onSendToContact: (InboxContact, String, (Boolean, String?) -> Unit) -> Unit,
    onShareCompleted: () -> Unit,
    onDismiss: () -> Unit,
) {
    var message by remember(post.id) { mutableStateOf("") }
    var contactSearch by remember(post.id) { mutableStateOf("") }
    var sendError by remember(post.id) { mutableStateOf<String?>(null) }
    val sendingContacts = remember(post.id) { mutableStateMapOf<String, Boolean>() }
    val sentContacts = remember(post.id) { mutableStateMapOf<String, Boolean>() }
    val shareUrl = post.playableVideoUrl() ?: post.image ?: post.mediaUrl
    val thumbnail = post.coverImage ?: post.image ?: post.mediaUrl ?: post.video ?: post.videoUrl
    val visibleContacts = contacts.filter {
        contactSearch.isBlank() ||
            it.name.contains(contactSearch, ignoreCase = true) ||
            it.username.contains(contactSearch, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ShareSheetBackground,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.64f),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp, bottom = 8.dp)
                    .width(38.dp).height(4.dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.32f)),
            )
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.86f)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = "Video preview",
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(ShareSheetSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(BootstrapIcons.Share(), contentDescription = null, tint = Color.White)
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Share video", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "@${post.author.username}",
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(BootstrapIcons.Close(), contentDescription = "Close share options", tint = Color.White)
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = contactSearch,
                    onValueChange = { contactSearch = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = {
                        Icon(BootstrapIcons.Search(), contentDescription = null, tint = Color.White.copy(alpha = 0.62f))
                    },
                    placeholder = { Text("Search friends", color = Color.White.copy(alpha = 0.52f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.4f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                        cursorColor = ShareOrange,
                    ),
                )

                Text(
                    "Send to",
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isLoadingContacts && contacts.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ShareOrange)
                    }
                } else if (visibleContacts.isEmpty()) {
                    Text(
                        if (contacts.isEmpty()) "Follow people to share videos with them on Topline."
                        else "No friends match that search.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(visibleContacts, key = { it.id }) { contact ->
                            val isSending = sendingContacts[contact.id] == true
                            val wasSent = sentContacts[contact.id] == true
                            Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                AccountAvatar(contact.name, contact.profilePicture, 54)
                                Text(
                                    contact.name,
                                    modifier = Modifier.padding(top = 5.dp),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                )
                                TextButton(
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
                                                onShareCompleted()
                                            } else {
                                                sendError = error ?: "Unable to send this post."
                                            }
                                        }
                                    },
                                    enabled = !isSending && !wasSent,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                ) {
                                    if (isSending) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = ShareOrange,
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Text(
                                            if (wasSent) "Sent" else "Send",
                                            color = if (wasSent) Color.White.copy(alpha = 0.5f) else ShareOrange,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!sendError.isNullOrBlank()) {
                    Text(
                        sendError.orEmpty(),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (contacts.isNotEmpty()) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it.take(300) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("Add a message (optional)", color = Color.White.copy(alpha = 0.52f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                            cursorColor = ShareOrange,
                        ),
                    )
                }

                Text(
                    "Share to",
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        ShareAction(
                            icon = BootstrapIcons.Share(),
                            label = "More apps",
                            onClick = onShareToApps,
                            enabled = !shareUrl.isNullOrBlank(),
                        )
                    }
                    item {
                        ShareAction(
                            icon = BootstrapIcons.Copy(),
                            label = "Copy link",
                            onClick = onCopyLink,
                            enabled = !shareUrl.isNullOrBlank(),
                        )
                    }
                }

                Text(
                    "More actions",
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        ShareAction(
                            icon = BootstrapIcons.Download(),
                            label = if (isDownloading) "Saving…" else "Download",
                            onClick = onDownload,
                            enabled = !isDownloadBusy && post.playableVideoUrl() != null,
                            isLoading = isDownloading,
                        )
                    }
                    item {
                        ShareAction(
                            icon = BootstrapIcons.BookmarkFill(),
                            label = if (isSaved) "Saved" else "Save video",
                            onClick = onSave,
                            tint = if (isSaved) ShareOrange else Color.White,
                        )
                    }
                    item {
                        ShareAction(
                            icon = BootstrapIcons.BookmarkFill(),
                            label = "Collections",
                            onClick = onManageCollections,
                        )
                    }
                }

                if (isDownloading && !downloadStatus.isNullOrBlank()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ShareOrange,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            downloadProgress?.let { "$downloadStatus · ${(it * 100).toInt()}%" }
                                ?: downloadStatus,
                            color = Color.White.copy(alpha = 0.62f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShareSheetSurface,
                    contentColor = Color.White,
                ),
            ) {
                Text("Cancel", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ShareAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = Color.White,
    isLoading: Boolean = false,
) {
    Column(
        Modifier.width(76.dp).clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(ShareSheetSurface),
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
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}
