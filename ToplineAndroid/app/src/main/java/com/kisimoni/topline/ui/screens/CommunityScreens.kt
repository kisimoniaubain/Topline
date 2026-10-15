package com.kisimoni.topline.ui.screens

import com.kisimoni.topline.ui.icons.BootstrapIcons

import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.kisimoni.topline.data.api.ConversationItem
import com.kisimoni.topline.data.api.DirectMessage
import com.kisimoni.topline.data.api.InboxContact
import com.kisimoni.topline.data.api.ActivityNotification
import com.kisimoni.topline.data.api.PostAuthor
import com.kisimoni.topline.data.api.PostItem
import com.kisimoni.topline.data.api.SavedCollection
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kisimoni.topline.data.api.User

private val Orange = Color(0xFFF57F17)

data class SuggestedPerson(
    val name: String,
    val username: String,
    val bio: String,
    val location: String,
    val followers: Int,
    val following: Int,
    val id: String = "",
    val profilePicture: String? = null,
    val isFollowing: Boolean = false,
)

@Composable
fun PeopleScreen(
    people: List<SuggestedPerson>,
    followers: List<InboxContact>,
    following: List<InboxContact>,
    isLoading: Boolean,
    isFriendsLoading: Boolean,
    errorMessage: String?,
    friendsErrorMessage: String?,
    onBack: () -> Unit,
    onToggleFollow: (SuggestedPerson) -> Unit,
    onOpenProfile: (SuggestedPerson) -> Unit,
    onOpenFriends: () -> Unit,
    onUnfollow: (InboxContact) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var searchDraft by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var searchSubmitted by remember { mutableStateOf(false) }
    var recentSearches by remember { mutableStateOf(emptyList<String>()) }
    var showFriends by remember { mutableStateOf(false) }
    var showFollowers by remember { mutableStateOf(true) }
    val filteredPeople = people.filter { person ->
        query.isBlank() ||
            person.name.contains(query.trim(), ignoreCase = true) ||
            person.username.contains(query.trim().removePrefix("@"), ignoreCase = true) ||
            person.bio.contains(query.trim(), ignoreCase = true) ||
            person.location.contains(query.trim(), ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp),
    ) {
        if (showFriends) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showFriends = false }) {
                    Icon(BootstrapIcons.ArrowBack(), contentDescription = "Back to people")
                }
                Text("Friends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenFriends) {
                    Icon(BootstrapIcons.Refresh(), contentDescription = "Refresh friends")
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { showFollowers = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showFollowers) Orange else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showFollowers) Color.White else MaterialTheme.colorScheme.onSurface,
                    ),
                ) { Text("Followers (${followers.size})") }
                Button(
                    onClick = { showFollowers = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showFollowers) Orange else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!showFollowers) Color.White else MaterialTheme.colorScheme.onSurface,
                    ),
                ) { Text("Following (${following.size})") }
            }
            val displayedFriends = if (showFollowers) followers else following
            if (!friendsErrorMessage.isNullOrBlank()) {
                Text(friendsErrorMessage, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onOpenFriends) { Text("Try again", color = Orange) }
            } else if (isFriendsLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Orange)
                }
            } else if (displayedFriends.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (showFollowers) "You don't have any followers yet." else "You aren't following anyone yet.",
                        color = Color.Gray,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(displayedFriends, key = { it.id }) { person ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            InitialAvatar(person.name, 48, person.profilePicture)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(person.name, fontWeight = FontWeight.SemiBold)
                                Text("@${person.username}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!showFollowers) {
                                OutlinedButton(onClick = { onUnfollow(person) }) {
                                    Text("Unfollow", color = Orange)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            ScreenHeader(title = "People", onBack = onBack)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Find people and connect",
                    modifier = Modifier.weight(1f),
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = {
                    showFriends = true
                    onOpenFriends()
                }) {
                    Text("Friends", color = Orange)
                }
                IconButton(onClick = {
                    searchDraft = ""
                    searchSubmitted = false
                    showSearch = true
                }) {
                    Icon(
                        BootstrapIcons.Search(),
                        contentDescription = "Search people",
                        tint = Orange,
                    )
                }
            }
            if (showSearch) {
                TikTokSearchDialog(
                    query = searchDraft,
                    submitted = searchSubmitted,
                    recentQueries = recentSearches,
                    suggestions = people.map { "@${it.username}" }.filterNot { it == "@" },
                    placeholder = "Search people",
                    onQueryChange = {
                        searchDraft = it
                        searchSubmitted = false
                    },
                    onSubmit = { submittedQuery ->
                        searchDraft = submittedQuery
                        query = submittedQuery.trim()
                        searchSubmitted = true
                        if (query.isNotBlank()) {
                            recentSearches = (listOf(query) + recentSearches.filterNot {
                                it.equals(query, ignoreCase = true)
                            }).take(8)
                        }
                    },
                    onBack = {
                        showSearch = false
                        query = ""
                    },
                    results = { modifier ->
                        if (filteredPeople.isEmpty()) {
                            Box(modifier, contentAlignment = Alignment.Center) {
                                Text("No people match \"$query\".", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = modifier,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 12.dp,
                                ),
                            ) {
                                items(filteredPeople, key = { it.id }) { person ->
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                showSearch = false
                                                onOpenProfile(person)
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        InitialAvatar(person.name, 48, person.profilePicture)
                                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                            Text(person.name, fontWeight = FontWeight.SemiBold)
                                            Text("@${person.username}", color = Color.Gray)
                                        }
                                        Button(
                                            onClick = { onToggleFollow(person) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (person.isFollowing) Color(0xFFEDEDED) else Orange,
                                                contentColor = if (person.isFollowing) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    Color.White
                                                },
                                            ),
                                        ) {
                                            Text(if (person.isFollowing) "Following" else "Follow")
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
            Text("People to follow", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (!errorMessage.isNullOrBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            } else if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Orange)
                }
            } else if (filteredPeople.isEmpty()) {
                Text(
                    if (query.isNotBlank()) "No people match \"$query\"."
                    else "No new people to follow right now.",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredPeople, key = { it.id }) { person ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenProfile(person) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                InitialAvatar(person.name, 52, person.profilePicture)
                                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(person.name, fontWeight = FontWeight.SemiBold)
                                    Text("@${person.username}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    Text(person.bio, color = Color.DarkGray, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    onClick = { onToggleFollow(person) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (person.isFollowing) Color(0xFFEDEDED) else Orange,
                                        contentColor = if (person.isFollowing) MaterialTheme.colorScheme.onSurface else Color.White,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(if (person.isFollowing) "Unfollow" else "Follow")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessagesScreen(
    contactName: String?,
    contactId: String?,
    currentUserId: String,
    owner: User?,
    following: List<InboxContact>,
    conversations: List<ConversationItem>,
    messages: List<DirectMessage>,
    isLoading: Boolean,
    isSending: Boolean,
    errorMessage: String?,
    conversationError: String?,
    onBack: () -> Unit,
    onFindPeople: () -> Unit,
    onRefresh: () -> Unit,
    onOpenContact: (InboxContact) -> Unit,
    onSendMessage: (String, Uri?, () -> Unit) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onDeleteMessage: (String) -> Unit,
) {
    var inboxSearchQuery by remember { mutableStateOf("") }
    var inboxSearchDraft by remember { mutableStateOf("") }
    var showInboxSearch by remember { mutableStateOf(false) }
    var inboxSearchSubmitted by remember { mutableStateOf(false) }
    var recentInboxSearches by remember { mutableStateOf(emptyList<String>()) }
    val inboxSearchSuggestions = remember(following, conversations) {
        (following.flatMap { listOf(it.name, "@${it.username}") } +
            conversations.flatMap { listOf(it.user.name, "@${it.user.username}", it.lastMessage) })
            .filter(String::isNotBlank)
            .distinct()
            .take(12)
    }
    val filteredFollowing = following.filter { person ->
        inboxSearchQuery.isBlank() ||
            person.name.contains(inboxSearchQuery.trim(), ignoreCase = true) ||
            person.username.contains(inboxSearchQuery.trim().removePrefix("@"), ignoreCase = true)
    }
    val filteredConversations = conversations.filter { conversation ->
        inboxSearchQuery.isBlank() ||
            conversation.user.name.contains(inboxSearchQuery.trim(), ignoreCase = true) ||
            conversation.user.username.contains(inboxSearchQuery.trim().removePrefix("@"), ignoreCase = true) ||
            conversation.lastMessage.contains(inboxSearchQuery.trim(), ignoreCase = true)
    }
    var draft by remember(contactId) { mutableStateOf("") }
    var selectedImage by remember(contactId) { mutableStateOf<Uri?>(null) }
    var messageBeingEdited by remember { mutableStateOf<DirectMessage?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var messageBeingDeleted by remember { mutableStateOf<DirectMessage?>(null) }
    val conversationContact = remember(contactId, following, conversations) {
        following.firstOrNull { it.id == contactId }
            ?: conversations.firstOrNull { it.user.id == contactId }?.user
    }
    val messageListState = rememberLazyListState()
    LaunchedEffect(contactId, messages.size) {
        if (contactId != null && messages.isNotEmpty()) {
            messageListState.animateScrollToItem(messages.lastIndex)
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImage = uri
    }
    if (contactId != null) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        ) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        BootstrapIcons.ArrowBack(),
                        contentDescription = "Back to messages",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                AccountAvatar(contactName.orEmpty(), conversationContact?.profilePicture, 42)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        contactName ?: "Messages",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (conversationContact?.isOnline == true) "Active now" else "Conversation",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        BootstrapIcons.Refresh(),
                        contentDescription = "Refresh conversation",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (!conversationError.isNullOrBlank()) {
                Text(
                    conversationError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            LazyColumn(
                state = messageListState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isLoading && messages.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Orange)
                        }
                    }
                }
                if (!isLoading && messages.isEmpty() && conversationError.isNullOrBlank()) {
                    item {
                        Text(
                            "Say hello to start your conversation.",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                items(messages, key = { it.id }) { message ->
                    val senderId = message.sender.id.trim()
                    val ownerUsername = owner?.username.orEmpty().removePrefix("@").trim()
                    val sentByMe = senderId.isNotEmpty() && senderId.equals(currentUserId.trim(), ignoreCase = true) ||
                        (ownerUsername.isNotEmpty() &&
                            message.sender.username.trim().removePrefix("@").equals(ownerUsername, ignoreCase = true)) ||
                        (owner?.name?.isNotBlank() == true &&
                            message.sender.name.trim().equals(owner.name.trim(), ignoreCase = true))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (sentByMe) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        if (!sentByMe) {
                            AccountAvatar(
                                message.sender.name.ifBlank { contactName.orEmpty() },
                                message.sender.profilePicture ?: conversationContact?.profilePicture,
                                28,
                            )
                            Spacer(Modifier.size(7.dp))
                        }
                        Column(
                            modifier = Modifier.widthIn(max = 280.dp),
                            horizontalAlignment = if (sentByMe) Alignment.End else Alignment.Start,
                        ) {
                            if (!message.image.isNullOrBlank() || message.text.isNotBlank()) {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (sentByMe) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        .padding(10.dp),
                                ) {
                                    if (!message.image.isNullOrBlank()) {
                                        AsyncImage(
                                            model = message.image,
                                            contentDescription = "Message image",
                                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                    if (message.text.isNotBlank()) {
                                        Text(
                                            message.text,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                                            color = if (sentByMe) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    formatMessageTime(message.createdAt),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                if (sentByMe && message.text.isNotBlank()) {
                                    IconButton(onClick = {
                                        messageBeingEdited = message
                                        editDraft = message.text
                                    }, modifier = Modifier.size(34.dp)) {
                                        Icon(
                                            BootstrapIcons.Edit(),
                                            contentDescription = "Edit message",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    IconButton(onClick = { messageBeingDeleted = message }, modifier = Modifier.size(34.dp)) {
                                        Icon(
                                            BootstrapIcons.Delete(),
                                            contentDescription = "Delete message",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (selectedImage != null) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = selectedImage,
                        contentDescription = "Image to send",
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    IconButton(onClick = { selectedImage = null }) {
                        Icon(BootstrapIcons.Close(), contentDescription = "Remove selected image")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { pickImage.launch("image/*") }, enabled = !isSending) {
                    Icon(BootstrapIcons.AddPhoto(), contentDescription = "Attach image", tint = Orange)
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(2000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Write a message") },
                    maxLines = 4,
                    shape = RoundedCornerShape(26.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                IconButton(
                    onClick = {
                        onSendMessage(draft, selectedImage) {
                            draft = ""
                            selectedImage = null
                        }
                    },
                    enabled = (draft.isNotBlank() || selectedImage != null) && !isSending,
                ) {
                    if (isSending) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Orange, strokeWidth = 2.dp)
                    } else {
                        Icon(BootstrapIcons.Send(), contentDescription = "Send message", tint = Orange)
                    }
                }
            }
        }
        messageBeingEdited?.let { message ->
            AlertDialog(
                onDismissRequest = { messageBeingEdited = null },
                title = { Text("Edit message") },
                text = {
                    OutlinedTextField(
                        value = editDraft,
                        onValueChange = { editDraft = it.take(2000) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEditMessage(message.id, editDraft)
                            messageBeingEdited = null
                        },
                        enabled = editDraft.isNotBlank(),
                    ) { Text("Save", color = Orange) }
                },
                dismissButton = { TextButton(onClick = { messageBeingEdited = null }) { Text("Cancel") } },
            )
        }
        messageBeingDeleted?.let { message ->
            AlertDialog(
                onDismissRequest = { messageBeingDeleted = null },
                title = { Text("Delete message?") },
                text = { Text("This will remove your message from the conversation.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteMessage(message.id)
                        messageBeingDeleted = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { messageBeingDeleted = null }) { Text("Cancel") } },
            )
        }
        return
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(BootstrapIcons.ArrowBack(), contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text("Inbox", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your conversations and people", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = {
                inboxSearchDraft = ""
                inboxSearchSubmitted = false
                showInboxSearch = true
            }) {
                Icon(
                    BootstrapIcons.Search(),
                    contentDescription = "Search messages",
                    tint = Orange,
                )
            }
            IconButton(onClick = onRefresh) { Icon(BootstrapIcons.Refresh(), contentDescription = "Refresh inbox") }
        }
        if (showInboxSearch) {
            TikTokSearchDialog(
                query = inboxSearchDraft,
                submitted = inboxSearchSubmitted,
                recentQueries = recentInboxSearches,
                suggestions = inboxSearchSuggestions,
                placeholder = "Search people or messages",
                onQueryChange = {
                    inboxSearchDraft = it
                    inboxSearchSubmitted = false
                },
                onSubmit = { submittedQuery ->
                    inboxSearchDraft = submittedQuery
                    inboxSearchQuery = submittedQuery.trim()
                    inboxSearchSubmitted = true
                    if (inboxSearchQuery.isNotBlank()) {
                        recentInboxSearches = (listOf(inboxSearchQuery) + recentInboxSearches.filterNot {
                            it.equals(inboxSearchQuery, ignoreCase = true)
                        }).take(8)
                    }
                },
                onBack = {
                    showInboxSearch = false
                    inboxSearchQuery = ""
                },
                results = { modifier ->
                    val matchingPeople = filteredFollowing.filter { person ->
                        person.name.contains(inboxSearchQuery, ignoreCase = true) ||
                            person.username.contains(inboxSearchQuery.removePrefix("@"), ignoreCase = true)
                    }
                    if (matchingPeople.isEmpty() && filteredConversations.isEmpty()) {
                        Box(modifier, contentAlignment = Alignment.Center) {
                            Text("No matching people or messages.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = modifier,
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            if (matchingPeople.isNotEmpty()) {
                                item {
                                    Text(
                                        "People",
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                items(matchingPeople, key = { "person-${it.id}" }) { person ->
                                    Row(
                                        Modifier.fillMaxWidth().clickable {
                                            showInboxSearch = false
                                            inboxSearchQuery = ""
                                            onOpenContact(person)
                                        }.padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AccountAvatar(person.name, person.profilePicture, 46)
                                        Column(Modifier.padding(start = 12.dp)) {
                                            Text(person.name, fontWeight = FontWeight.Medium)
                                            Text("@${person.username}", color = Color.Gray)
                                        }
                                    }
                                }
                            }
                            if (filteredConversations.isNotEmpty()) {
                                item {
                                    Text(
                                        "Messages",
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                items(filteredConversations, key = { "conversation-${it.user.id}" }) { conversation ->
                                    Row(
                                        Modifier.fillMaxWidth().clickable {
                                            showInboxSearch = false
                                            inboxSearchQuery = ""
                                            onOpenContact(conversation.user)
                                        }.padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AccountAvatar(conversation.user.name, conversation.user.profilePicture, 46)
                                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                            Text(conversation.user.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                conversation.lastMessage,
                                                color = Color.Gray,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            )
        }
        if (!errorMessage.isNullOrBlank()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                TextButton(onClick = onRefresh) { Text("Retry", color = Orange) }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Following", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(filteredFollowing.size.toString(), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
        }
        LazyRow(
            contentPadding = PaddingValues(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
                item(key = "owner-account") {
                    Column(
                        modifier = Modifier.width(64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box {
                            AccountAvatar(owner?.name.orEmpty().ifBlank { "You" }, owner?.profilePicture, 54)
                            Box(
                                Modifier.align(Alignment.BottomEnd).size(16.dp)
                                    .clip(CircleShape).background(MaterialTheme.colorScheme.background).padding(2.dp)
                                    .clip(CircleShape).background(Color(0xFF31A24C)),
                            )
                        }
                        Text("You", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                        Text("Your profile", color = Color.Gray, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                items(filteredFollowing, key = { it.id }) { person ->
                    Column(
                        modifier = Modifier.width(64.dp).clickable { onOpenContact(person) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box {
                            AccountAvatar(person.name, person.profilePicture, 54)
                            Box(
                                Modifier.align(Alignment.BottomEnd)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(if (person.isOnline) Color(0xFF31A24C) else Color.Gray),
                            )
                        }
                        Text(person.name, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                        Text(
                            if (person.isOnline) "Online" else "Offline",
                            color = if (person.isOnline) Color(0xFF31A24C) else Color.Gray,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Recent chats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(conversations.size.toString(), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
        }
        if (isLoading && conversations.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Orange)
            }
        } else if (filteredConversations.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(BootstrapIcons.Message(), contentDescription = null, tint = Orange, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(10.dp))
                Text(if (inboxSearchQuery.isBlank()) "No conversations yet" else "No matching conversations", fontWeight = FontWeight.Bold)
                Text(
                    if (inboxSearchQuery.isBlank()) "Start a chat with someone you follow."
                    else "Try another name, username, or message.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onFindPeople,
                    modifier = Modifier.padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                ) { Text("Find people") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filteredConversations, key = { it.user.id }) { conversation ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                            onOpenContact(conversation.user)
                        }.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AccountAvatar(conversation.user.name, conversation.user.profilePicture, 52)
                        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                            Text(conversation.user.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                conversation.lastMessage.ifBlank { "Start a conversation" },
                                maxLines = 1,
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(formatMessageTime(conversation.updatedAt), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    val normalized = timestamp.replace('T', ' ')
    return normalized.take(16).removeSuffix("Z")
}

@Composable
fun DashboardScreen(
    user: User?,
    analytics: com.kisimoni.topline.data.api.PostAnalyticsResponse?,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ScreenHeader("Creator dashboard", onBack = onBack, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) { Icon(BootstrapIcons.Refresh(), contentDescription = "Refresh analytics") }
        }
        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRefresh) { Text("Try again", color = Orange) }
        } else if (isLoading && analytics == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Orange)
            }
        } else if (analytics != null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    SettingsCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialAvatar(user?.name.orEmpty(), 58, user?.profilePicture)
                            Column(Modifier.padding(start = 14.dp)) {
                                Text(user?.name.orEmpty().ifBlank { "Your creator account" }, fontWeight = FontWeight.Bold)
                                Text(
                                    user?.username.orEmpty().takeIf(String::isNotBlank)?.let { "@$it" } ?: "Topline creator",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!user?.bio.isNullOrBlank()) {
                                    Text(
                                        user?.bio.orEmpty(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                    Text("Account overview", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AnalyticsStat("Posts", analytics.postsCount, Modifier.weight(1f))
                        AnalyticsStat("Followers", analytics.followersCount, Modifier.weight(1f))
                    }
                    Row(
                        Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AnalyticsStat("Following", analytics.followingCount, Modifier.weight(1f))
                        AnalyticsStat("Likes", analytics.likesCount, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    SettingsCard {
                        Text("Engagement", fontWeight = FontWeight.Bold)
                        Text(
                            "${analytics.likesCount + analytics.commentsCount} total likes and comments across ${analytics.postsCount} posts",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        if (analytics.postsCount > 0) {
                            Text(
                                "Average ${(analytics.likesCount + analytics.commentsCount) / analytics.postsCount} interactions per post",
                                color = Orange,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Text(
                            "Video views and follower history aren’t tracked yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                item {
                    Text("Recent posts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (analytics.recentPosts.isEmpty()) {
                    item { Text("Share a post to start tracking its performance.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(analytics.recentPosts, key = { "recent-${it.id}" }) { post ->
                        SettingsCard {
                            Text(post.text.ifBlank { "Media post" }, maxLines = 2, fontWeight = FontWeight.SemiBold)
                            Text(
                                post.createdAt?.take(10).orEmpty().ifBlank { "Published" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("${post.likes} likes  ·  ${post.comments} comments", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Text("Top performing posts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (analytics.topPosts.isEmpty()) {
                    item { Text("Your top posts will appear here as people engage.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(analytics.topPosts, key = { it.id }) { post ->
                        SettingsCard {
                            Text(post.text.ifBlank { "Media post" }, maxLines = 2, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            val interactionTotal = post.likes + post.comments
                            Text("${post.likes} likes  ·  ${post.comments} comments", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp)
                                    .clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                if (interactionTotal > 0) {
                                    Box(
                                        Modifier.fillMaxWidth(
                                            interactionTotal.toFloat() /
                                                analytics.topPosts.maxOf { it.likes + it.comments }.coerceAtLeast(1),
                                        ).height(8.dp).background(Orange, CircleShape),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStat(label: String, count: Int, modifier: Modifier = Modifier) {
    SettingsCard(modifier) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<ActivityNotification>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onBackToFeed: () -> Unit,
    onRefresh: () -> Unit,
    onNotificationClick: (ActivityNotification) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ScreenHeader(title = "Notifications", onBack = onBack, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) { Icon(BootstrapIcons.Refresh(), contentDescription = "Refresh notifications") }
        }
        if (isLoading && notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Orange)
            }
        } else if (!errorMessage.isNullOrBlank() && notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    TextButton(onClick = onRefresh) { Text("Try again", color = Orange) }
                }
            }
        } else if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(82.dp).clip(CircleShape).background(Color(0xFFFFE7CC)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(BootstrapIcons.BellFill(), contentDescription = null, tint = Orange, modifier = Modifier.size(38.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("You’re all caught up", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Likes, comments, messages, and new followers will show up here.", color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                    Button(onClick = onBackToFeed, colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
                        Text("Back to feed")
                    }
                }
            }
        } else {
            if (!errorMessage.isNullOrBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notifications, key = { it.id }) { notification ->
                    val actor = notification.actor
                    val actorName = actor?.name?.ifBlank { actor.username }?.ifBlank { "Someone" } ?: "Someone"
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNotificationClick(notification) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notification.isRead) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            InitialAvatar(actorName, 48, actor?.profilePicture)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    text = "$actorName ${notification.text}",
                                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                                )
                                Text(notification.createdAt.orEmpty().replace('T', ' ').take(16), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                Text(notification.type.replaceFirstChar { it.uppercase() }, color = Orange, style = MaterialTheme.typography.labelSmall)
                            }
                            if (!notification.isRead) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(Orange))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    user: User?,
    darkMode: Boolean,
    language: String,
    onBack: () -> Unit,
    onDarkModeChanged: (Boolean) -> Unit,
    onLanguageChanged: (String) -> Unit,
    onEditProfile: () -> Unit,
    onDashboard: () -> Unit,
    onLogout: () -> Unit,
    isDeletingAccount: Boolean = false,
    accountDeletionError: String? = null,
    onDeleteAccount: () -> Unit = {},
) {
    var showLanguages by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    val languages = listOf("English", "Kiswahili", "Français", "Nederlands", "العربية", "Español")

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp)) {
        ScreenHeader(title = "Settings", onBack = onBack)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialAvatar(user?.name.orEmpty(), 54, user?.profilePicture)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Account", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
                            Text(user?.name.orEmpty().ifBlank { "Topline User" }, fontWeight = FontWeight.Bold)
                            Text(user?.email.orEmpty(), color = Color.Gray)
                        }
                    }
                }
            }
            item {
                SettingsCard(
                    modifier = Modifier.clickable(onClick = onDashboard),
                ) {
                    Text("Creator dashboard", fontWeight = FontWeight.Bold)
                    Text("Track your posts, likes, comments, and top-performing content.", color = Color.Gray)
                }
            }
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(BootstrapIcons.Sliders(), contentDescription = null, tint = Orange)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Appearance", fontWeight = FontWeight.SemiBold)
                            Text(if (darkMode) "Dark mode" else "Light mode", color = Color.Gray)
                            Text("Applies across the entire app", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = darkMode, onCheckedChange = onDarkModeChanged)
                    }
                }
            }
            item {
                SettingsCard(modifier = Modifier.clickable { showLanguages = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(BootstrapIcons.Language(), contentDescription = null, tint = Orange)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Language", fontWeight = FontWeight.SemiBold)
                            Text(language, color = Color.Gray)
                            Text("Choose your preferred language", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onEditProfile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Edit Profile") }
            }
            if (!accountDeletionError.isNullOrBlank()) {
                item { Text(accountDeletionError, color = MaterialTheme.colorScheme.error) }
            }
            item {
                TextButton(
                    onClick = { confirmDeleteAccount = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDeletingAccount,
                ) {
                    Text(
                        if (isDeletingAccount) "Deleting account..." else "Delete Account",
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            item {
                TextButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Log Out", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showLanguages) {
        AlertDialog(
            onDismissRequest = { showLanguages = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Select language", modifier = Modifier.weight(1f))
                    IconButton(onClick = { showLanguages = false }) {
                        Icon(BootstrapIcons.Close(), contentDescription = "Close")
                    }
                }
            },
            text = {
                Column {
                    languages.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onLanguageChanged(option)
                                showLanguages = false
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = language == option, onClick = {
                                onLanguageChanged(option)
                                showLanguages = false
                            })
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text("Log Out", color = Color(0xFFC62828)) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } },
        )
    }

    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) confirmDeleteAccount = false },
            title = { Text("Delete your account?") },
            text = {
                Text("This permanently deletes your account, posts, comments, likes, follows, messages, and notifications. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteAccount = false
                        onDeleteAccount()
                    },
                    enabled = !isDeletingAccount,
                ) { Text(if (isDeletingAccount) "Deleting..." else "Delete permanently", color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAccount = false }, enabled = !isDeletingAccount) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun EditProfileScreen(
    user: User?,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit,
    onChooseAvatar: (Uri) -> Unit,
) {
    var name by remember(user?.name) { mutableStateOf(user?.name.orEmpty()) }
    var username by remember(user?.username) { mutableStateOf(user?.username.orEmpty()) }
    var email by remember(user?.email) { mutableStateOf(user?.email.orEmpty()) }
    var phone by remember(user?.phone) { mutableStateOf(user?.phone.orEmpty()) }
    var bio by remember(user?.bio) { mutableStateOf(user?.bio.orEmpty()) }
    var location by remember(user?.location) { mutableStateOf(user?.location.orEmpty()) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onChooseAvatar(uri)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp)) {
        ScreenHeader(title = "Edit Profile", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    UserAvatar(name, user?.profilePicture, 88)
                    TextButton(onClick = { avatarPicker.launch("image/*") }) {
                        Icon(BootstrapIcons.Image(), contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Edit profile photo")
                    }
                }
            }
            item { ProfileField("Full Name", name, { name = it }) }
            item { ProfileField("Username", username, { username = it }) }
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bio") },
                    minLines = 3,
                    maxLines = 5,
                )
            }
            item { ProfileField("Location", location, { location = it }) }
            if (!errorMessage.isNullOrBlank()) item {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { onSave(name, username, email, phone, bio, location) },
                enabled = !isSaving && name.isNotBlank() && username.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Orange),
                shape = RoundedCornerShape(14.dp),
            ) { Text(if (isSaving) "Saving..." else "Save Changes") }
        }
    }
}

@Composable
fun UserProfileScreen(
    user: User?,
    posts: List<PostItem>,
    savedPosts: List<PostItem>,
    likedPosts: List<PostItem>,
    isLikedPostsLoading: Boolean,
    likedPostsError: String?,
    savedCollections: List<SavedCollection>,
    savedCollectionPosts: List<PostItem>,
    activeSavedCollection: SavedCollection?,
    savedCollectionsError: String?,
    isSavedCollectionsLoading: Boolean,
    isSavedPostsLoading: Boolean,
    savedPostsError: String?,
    postsCount: Int,
    errorMessage: String?,
    statsErrorMessage: String?,
    isOwnProfile: Boolean,
    person: SuggestedPerson?,
    followersCount: Int,
    followingCount: Int,
    isFollowing: Boolean,
    onBack: () -> Unit,
    onLoadSavedPosts: () -> Unit,
    onLoadLikedPosts: () -> Unit,
    onLoadSavedCollections: () -> Unit,
    onLoadSavedCollectionPosts: (String) -> Unit,
    onSettings: () -> Unit,
    onDashboard: () -> Unit,
    onNotifications: () -> Unit,
    onEditProfile: () -> Unit,
    onMessage: () -> Unit,
    onFollow: () -> Unit,
) {
    var selectedProfileTab by remember(isOwnProfile) { mutableStateOf(ProfileContentTab.POSTS) }
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    var isProfileMenuOpen by remember { mutableStateOf(false) }
    val privatePosts = remember(posts) { posts.filter { !it.visibility.equals("public", ignoreCase = true) } }
    val displayedPosts = when {
        selectedProfileTab == ProfileContentTab.POSTS ->
            if (isOwnProfile) posts.filter { it.visibility.equals("public", ignoreCase = true) } else posts
        selectedProfileTab == ProfileContentTab.PRIVATE -> privatePosts
        selectedProfileTab == ProfileContentTab.REPOSTS -> emptyList()
        selectedCollectionId != null -> savedCollectionPosts
        selectedProfileTab == ProfileContentTab.SAVED -> savedPosts
        else -> likedPosts
    }
    val name = person?.name ?: user?.name.orEmpty().ifBlank { "Topline User" }
    val username = person?.username ?: user?.username.orEmpty()
    val bio = person?.bio ?: user?.bio.orEmpty()
    val location = person?.location ?: user?.location.orEmpty()
    val selectProfileTab: (ProfileContentTab) -> Unit = { tab ->
        selectedProfileTab = tab
        selectedCollectionId = null
        when (tab) {
            ProfileContentTab.SAVED -> {
                onLoadSavedPosts()
                onLoadSavedCollections()
            }
            ProfileContentTab.LIKED -> onLoadLikedPosts()
            else -> Unit
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(BootstrapIcons.ArrowBack(), contentDescription = "Back")
            }
            Text(if (username.isBlank()) "Profile" else "@$username", fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (isOwnProfile) {
                val menuIconTint = MaterialTheme.colorScheme.onSurface
                IconButton(onClick = { isProfileMenuOpen = true }) {
                    Canvas(Modifier.size(24.dp)) {
                        val lineStart = 4.dp.toPx()
                        val lineEnd = size.width - lineStart
                        val strokeWidth = 2.dp.toPx()
                        listOf(6.dp, 12.dp, 18.dp).forEach { y ->
                            drawLine(
                                color = menuIconTint,
                                start = Offset(lineStart, y.toPx()),
                                end = Offset(lineEnd, y.toPx()),
                                strokeWidth = strokeWidth,
                            )
                        }
                    }
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    UserAvatar(name, user?.profilePicture ?: person?.profilePicture, 96)
                    if (isOwnProfile) {
                        TextButton(onClick = onEditProfile) { Text("Edit profile photo", color = Orange) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (username.isNotBlank()) Text("@$username", color = Color.Gray)
                    if (bio.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(bio, textAlign = TextAlign.Center)
                    }
                    if (location.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(location, color = Color.Gray)
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ProfileStat("Posts", if (isOwnProfile) postsCount else posts.size)
                        ProfileStat("Followers", followersCount)
                        ProfileStat("Following", followingCount)
                    }
                    if (!statsErrorMessage.isNullOrBlank()) {
                        Text(
                            statsErrorMessage,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (!isOwnProfile && person != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onFollow, colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
                                Text(if (isFollowing) "Following" else "Follow")
                            }
                            TextButton(onClick = onMessage) { Text("Message", color = Orange) }
                        }
                    }
                    if (isOwnProfile) {
                        ProfileContentTabRow(
                            selectedTab = selectedProfileTab,
                            onSelectTab = selectProfileTab,
                        )
                    }
                    if (isOwnProfile && selectedProfileTab == ProfileContentTab.SAVED) {
                        if (!savedCollectionsError.isNullOrBlank()) {
                            Text(
                                savedCollectionsError,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item {
                                OutlinedButton(
                                    onClick = {
                                        selectedCollectionId = null
                                        onLoadSavedPosts()
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                ) {
                                    Text("All saved")
                                }
                            }
                            items(savedCollections, key = { it.id }) { collection ->
                                OutlinedButton(
                                    onClick = {
                                        selectedCollectionId = collection.id
                                        onLoadSavedCollectionPosts(collection.id)
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                ) {
                                    Text("${collection.name} (${collection.postCount})")
                                }
                            }
                            if (isSavedCollectionsLoading && savedCollections.isEmpty()) {
                                item {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Orange,
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(10.dp))
                }
            }
            if (
                ((selectedProfileTab == ProfileContentTab.SAVED && isSavedPostsLoading) ||
                    (selectedProfileTab == ProfileContentTab.LIKED && isLikedPostsLoading)) &&
                displayedPosts.isEmpty()
            ) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Orange)
                    }
                }
            } else if (
                ((selectedProfileTab == ProfileContentTab.SAVED && !savedPostsError.isNullOrBlank()) ||
                    (selectedProfileTab == ProfileContentTab.LIKED && !likedPostsError.isNullOrBlank())) &&
                displayedPosts.isEmpty()
            ) {
                item {
                    Text(
                        if (selectedProfileTab == ProfileContentTab.SAVED) savedPostsError.orEmpty()
                        else likedPostsError.orEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (
                (selectedProfileTab == ProfileContentTab.POSTS || selectedProfileTab == ProfileContentTab.PRIVATE) &&
                !errorMessage.isNullOrBlank()
            ) {
                item {
                    Text(
                        errorMessage,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (displayedPosts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 42.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            when {
                                selectedProfileTab == ProfileContentTab.REPOSTS ->
                                    "Reposts aren’t available yet"
                                selectedCollectionId != null && activeSavedCollection?.id == selectedCollectionId ->
                                    "This collection is empty"
                                selectedProfileTab == ProfileContentTab.SAVED -> "No saved videos yet"
                                selectedProfileTab == ProfileContentTab.LIKED -> "No liked videos yet"
                                selectedProfileTab == ProfileContentTab.PRIVATE -> "No private videos yet"
                                else -> "No public posts yet"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when {
                                selectedProfileTab == ProfileContentTab.REPOSTS ->
                                    "Reposting videos isn’t supported yet."
                                selectedCollectionId != null && activeSavedCollection?.id == selectedCollectionId ->
                                    "Add saved videos to this collection from the share menu."
                                selectedProfileTab == ProfileContentTab.SAVED ->
                                    "Tap Save on a video and it will appear here."
                                selectedProfileTab == ProfileContentTab.LIKED ->
                                    "Videos you like will appear here."
                                selectedProfileTab == ProfileContentTab.PRIVATE ->
                                    "Videos shared with followers or restricted visibility appear here."
                                else ->
                                    "When this user shares something publicly, it will appear here."
                            },
                            color = Color.Gray,
                        )
                    }
                }
            } else {
                items(displayedPosts.chunked(3)) { rowPosts ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        rowPosts.forEach { post ->
                            val videoUrl = post.playableVideoUrl()
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = videoUrl != null) {
                                        selectedVideoId = post.id
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                val media = if (videoUrl != null) {
                                    post.coverImage ?: cloudinaryVideoThumbnail(videoUrl)
                                } else {
                                    post.displayableMediaUrl()
                                }
                                if (!media.isNullOrBlank()) {
                                    AsyncImage(
                                        model = media,
                                        contentDescription = if (videoUrl == null) "Post photo" else "Video thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else if (videoUrl == null) {
                                    Text(post.text, modifier = Modifier.padding(8.dp), maxLines = 4)
                                }
                                if (videoUrl != null) {
                                    Icon(
                                        BootstrapIcons.Play(),
                                        contentDescription = "Video",
                                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                        repeat(3 - rowPosts.size) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }

    }

    selectedVideoId?.let { videoId ->
        val videoPosts = displayedPosts.filter { it.playableVideoUrl() != null }
        val initialPage = videoPosts.indexOfFirst { it.id == videoId }.coerceAtLeast(0)
        if (videoPosts.isNotEmpty()) {
            ProfileVideoViewer(
                posts = videoPosts,
                initialPage = initialPage,
                onDismiss = { selectedVideoId = null },
            )
        }
    }
    if (isProfileMenuOpen) {
        Dialog(
            onDismissRequest = { isProfileMenuOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { isProfileMenuOpen = false },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.86f)
                        .widthIn(max = 340.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = {}),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Menu",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = { isProfileMenuOpen = false }) {
                            Icon(BootstrapIcons.Close(), contentDescription = "Close menu")
                        }
                    }
                    HorizontalDivider()
                    ProfileMenuItem("Creator dashboard", BootstrapIcons.GridFill()) {
                        isProfileMenuOpen = false
                        onDashboard()
                    }
                    ProfileMenuItem("Notifications", BootstrapIcons.Bell()) {
                        isProfileMenuOpen = false
                        onNotifications()
                    }
                    ProfileMenuItem("Saved videos", BootstrapIcons.Bookmark()) {
                        isProfileMenuOpen = false
                        selectProfileTab(ProfileContentTab.SAVED)
                    }
                    ProfileMenuItem("Liked videos", BootstrapIcons.Heart()) {
                        isProfileMenuOpen = false
                        selectProfileTab(ProfileContentTab.LIKED)
                    }
                    ProfileMenuItem("Edit profile", BootstrapIcons.Edit()) {
                        isProfileMenuOpen = false
                        onEditProfile()
                    }
                    ProfileMenuItem("Settings", BootstrapIcons.Settings()) {
                        isProfileMenuOpen = false
                        onSettings()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private enum class ProfileContentTab(val description: String) {
    POSTS("Public posts"),
    PRIVATE("Private videos"),
    REPOSTS("Reposts"),
    SAVED("Saved videos"),
    LIKED("Liked videos"),
}

@Composable
private fun ProfileContentTabRow(
    selectedTab: ProfileContentTab,
    onSelectTab: (ProfileContentTab) -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ProfileContentTab.values().forEach { tab ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier.weight(1f).height(48.dp).clickable { onSelectTab(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                val tabIcon = when (tab) {
                    ProfileContentTab.POSTS -> BootstrapIcons.GridFill()
                    ProfileContentTab.PRIVATE -> BootstrapIcons.Lock()
                    ProfileContentTab.REPOSTS -> BootstrapIcons.ArrowRepeat()
                    ProfileContentTab.SAVED -> BootstrapIcons.BookmarkFill()
                    ProfileContentTab.LIKED -> BootstrapIcons.HeartFill()
                }
                Icon(
                    imageVector = tabIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) contentColor else contentColor.copy(alpha = 0.62f),
                )
                if (selected) {
                    Spacer(
                        Modifier
                            .fillMaxWidth(0.58f)
                            .height(2.dp)
                            .background(contentColor),
                    )
                } else {
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileVideoViewer(
    posts: List<PostItem>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { posts.size })
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { posts[it].id },
            ) { page ->
                val post = posts[page]
                val videoUrl = post.playableVideoUrl()
                if (videoUrl != null) {
                    ProfileVideoPage(
                        post = post,
                        videoUrl = videoUrl,
                        isActive = pagerState.currentPage == page,
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
            ) {
                Icon(BootstrapIcons.Close(), contentDescription = "Close video", tint = Color.White)
            }
            Text(
                "${pagerState.currentPage + 1} / ${posts.size}",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProfileVideoPage(
    post: PostItem,
    videoUrl: String,
    isActive: Boolean,
) {
    val context = LocalContext.current
    val playbackUrl = remember(videoUrl) { cloudinaryPlaybackUrl(videoUrl) }
    var playbackError by remember(videoUrl) { mutableStateOf(false) }
    var originalUrlFallbackUsed by remember(videoUrl) { mutableStateOf(false) }
    var isPlaying by remember(videoUrl) { mutableStateOf(isActive) }
    var isBuffering by remember(videoUrl) { mutableStateOf(true) }
    val exoPlayer = remember(context, videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    playbackError = true
                    isBuffering = false
                    isPlaying = false
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) playbackError = false
                }
            })
            setMediaItem(MediaItem.fromUri(playbackUrl))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = isActive
        }
    }

    LaunchedEffect(exoPlayer, playbackError) {
        if (!playbackError) return@LaunchedEffect
        if (!originalUrlFallbackUsed && playbackUrl != videoUrl) {
            originalUrlFallbackUsed = true
            playbackError = false
            isBuffering = true
            exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = isActive
        }
    }

    LaunchedEffect(exoPlayer, isActive) {
        exoPlayer.playWhenReady = isActive
        if (!isActive) exoPlayer.pause()
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(exoPlayer, isActive) {
                detectTapGestures(onTap = {
                    if (isActive && !playbackError) {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }
                })
            },
    ) {
        AndroidView(
            factory = { PlayerView(it).apply { player = exoPlayer; useController = false } },
            modifier = Modifier.fillMaxSize(),
        )
        if (isBuffering && !playbackError && isActive) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(42.dp),
                color = Color.White,
                strokeWidth = 3.dp,
            )
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(220.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                    ),
                ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 30.dp),
        ) {
            Text(
                "@${post.author.username}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            if (post.text.isNotBlank()) {
                Text(post.text, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (!isPlaying && !playbackError) {
            Icon(
                BootstrapIcons.Play(),
                contentDescription = "Play video",
                modifier = Modifier.align(Alignment.Center).size(68.dp),
                tint = Color.White.copy(alpha = 0.9f),
            )
        }
        if (playbackError) {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "This video is temporarily unavailable.",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = {
                    playbackError = false
                    isBuffering = true
                    exoPlayer.setMediaItem(
                        MediaItem.fromUri(if (originalUrlFallbackUsed) videoUrl else playbackUrl),
                    )
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = isActive
                }) {
                    Text("Try again", color = Color.White)
                }
            }
        }
    }
}

private fun cloudinaryVideoThumbnail(videoUrl: String): String? {
    if (!videoUrl.contains("res.cloudinary.com/")) return null
    val transformed = videoUrl.replaceFirst("/upload/", "/upload/so_0,f_jpg/")
    return transformed.replace(Regex("\\.(mp4|mov|webm|m4v)(\\?.*)?$", RegexOption.IGNORE_CASE), ".jpg")
}

@Composable
private fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    showBack: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack && onBack != null) {
            IconButton(onClick = onBack) { Icon(BootstrapIcons.ArrowBack(), contentDescription = "Back") }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InitialAvatar(
    name: String,
    size: Int,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size.dp).clip(CircleShape).background(Orange),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$name profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(name.firstOrNull()?.uppercase() ?: "U", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AccountAvatar(name: String, avatarUrl: String?, size: Int) {
    InitialAvatar(name, size, avatarUrl)
}

@Composable
private fun UserAvatar(name: String, avatarUrl: String?, size: Int) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Orange),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$name profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(name.firstOrNull()?.uppercase() ?: "U", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
private fun ProfileStat(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray)
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
