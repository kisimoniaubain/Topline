package com.kisimoni.topline.ui.screens

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.kisimoni.topline.ui.BrandLogo
import com.kisimoni.topline.data.api.PostItem
import com.kisimoni.topline.data.api.PostAuthor
import com.kisimoni.topline.data.api.CommentAuthor
import com.kisimoni.topline.data.api.CommentItem
import com.kisimoni.topline.viewmodel.AuthViewModel
import com.kisimoni.topline.viewmodel.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel,
    onOpenAlerts: () -> Unit,
    unreadNotificationCount: Int,
    onOpenProfile: (PostAuthor) -> Unit,
    onPlaybackProgress: (Float) -> Unit,
    seekRequest: Pair<Long, Float>?,
    onSeekRequestHandled: (Long) -> Unit,
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by postViewModel.uiState.collectAsState()
    val inboxState by postViewModel.inboxUiState.collectAsState()
    val commentsState by postViewModel.commentsUiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val likedPosts = remember { mutableStateMapOf<String, Boolean>() }
    val likeRequestsInFlight = remember { mutableStateMapOf<String, Boolean>() }
    val sampleSavedPosts = remember { mutableStateMapOf<String, Boolean>() }
    val saveRequestsInFlight = remember { mutableStateMapOf<String, Boolean>() }
    val localComments = remember { mutableStateMapOf<String, List<CommentItem>>() }
    val sampleCommentLikes = remember { mutableStateMapOf<String, Boolean>() }
    val samplePosts = remember {
        listOf(
            PostItem(
                id = "sample-flower",
                author = PostAuthor(name = "Topline", username = "topline"),
                text = "A sample clip for testing video playback.",
                video = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                likesCount = 245,
            ),
            PostItem(
                id = "sample-sintel",
                author = PostAuthor(name = "Sintel", username = "sintel"),
                text = "Animated trailer sample.",
                video = "https://media.w3.org/2010/05/sintel/trailer.mp4",
                likesCount = 128,
            ),
            PostItem(
                id = "sample-bunny",
                author = PostAuthor(name = "Big Buck Bunny", username = "bigbuckbunny"),
                text = "A short animated sample clip.",
                video = "https://media.w3.org/2010/05/bunny/trailer.mp4",
                likesCount = 532,
            ),
        )
    }
    var showFollowing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var searchDraft by remember { mutableStateOf("") }
    var searchSubmitted by remember { mutableStateOf(false) }
    var recentSearches by remember { mutableStateOf(emptyList<String>()) }
    val sourcePosts = if (showFollowing) uiState.followingPosts else uiState.posts + samplePosts
    val searchSuggestions = remember(sourcePosts) {
        sourcePosts.sortedByDescending { it.likesCount }
            .flatMap { listOf(it.author.name, it.text.takeIf(String::isNotBlank).orEmpty()) }
            .filter(String::isNotBlank)
            .distinct()
            .take(8)
    }
    val feedPosts = remember(sourcePosts, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            sourcePosts
        } else {
            sourcePosts.filter { post ->
                post.text.contains(query, ignoreCase = true) ||
                    post.author.name.contains(query, ignoreCase = true) ||
                    post.author.username.contains(query, ignoreCase = true)
            }
        }
    }
    val pagerState = rememberPagerState(pageCount = { feedPosts.size })
    val context = LocalContext.current
    var selectedPost by remember { mutableStateOf<PostItem?>(null) }
    var sharingPost by remember { mutableStateOf<PostItem?>(null) }
    var collectionsPost by remember { mutableStateOf<PostItem?>(null) }
    var commentDraft by remember { mutableStateOf("") }
    var commentImage by remember { mutableStateOf<Uri?>(null) }
    var replyTarget by remember { mutableStateOf<CommentItem?>(null) }
    val pickCommentImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        commentImage = uri
    }
    val feedErrorMessage = if (showFollowing) uiState.followingErrorMessage else uiState.errorMessage
    val commentsErrorMessage = commentsState.errorMessage

    LaunchedEffect(authState.currentUser?.id) {
        postViewModel.loadPosts(authViewModel.tokenOrNull())
        if (authState.currentUser != null) postViewModel.loadSavedPostIds()
    }
    LaunchedEffect(showFollowing, authState.currentUser?.id) {
        pagerState.scrollToPage(0)
        if (showFollowing) postViewModel.loadFollowingPosts(authViewModel.tokenOrNull())
    }
    LaunchedEffect(searchQuery, showFollowing) {
        pagerState.scrollToPage(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
    ) {
        if (!feedErrorMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 68.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.94f)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Videos are temporarily unavailable.",
                        modifier = Modifier.weight(1f),
                        color = colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = {
                        if (showFollowing) {
                            postViewModel.loadFollowingPosts(authViewModel.tokenOrNull())
                        } else {
                            postViewModel.loadPosts(authViewModel.tokenOrNull())
                        }
                    }) {
                        Text("Retry", color = colorScheme.primary)
                    }
                }
            }
        }

        if ((if (showFollowing) uiState.isFollowingLoading else uiState.isLoading) && feedPosts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading feed...", color = colorScheme.onBackground)
            }
        } else if (feedPosts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isNotBlank()) "No videos match \"$searchQuery\"."
                    else if (showFollowing) "No videos from accounts you follow yet."
                    else "No posts yet. Be the first to share.",
                    color = colorScheme.onBackground,
                )
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> feedPosts[page].id },
            ) { page ->
                val post = feedPosts[page]
                val isLiked = likedPosts[post.id] ?: post.likedByCurrentUser
                val likesCount = (
                    post.likesCount +
                        if (isLiked != post.likedByCurrentUser) {
                            if (isLiked) 1 else -1
                        } else {
                            0
                        }
                    ).coerceAtLeast(0)
                val isSaved = if (post.id.startsWith("sample-")) {
                    sampleSavedPosts[post.id] == true
                } else {
                    post.id in uiState.savedPostIds
                }
                fun likePost() {
                    if (post.id.startsWith("sample-")) {
                        likedPosts[post.id] = !isLiked
                    } else {
                        if (likeRequestsInFlight[post.id] == true) return
                        val token = authViewModel.tokenOrNull() ?: return
                        val nextLiked = !isLiked
                        likeRequestsInFlight[post.id] = true
                        likedPosts[post.id] = nextLiked
                        postViewModel.toggleLike(
                            token,
                            post.id,
                            onSuccess = { serverLiked, _ ->
                                likedPosts[post.id] = serverLiked
                                likeRequestsInFlight.remove(post.id)
                            },
                            onFailure = {
                                likedPosts.remove(post.id)
                                likeRequestsInFlight.remove(post.id)
                            },
                        )
                    }
                }

                VideoFeedCard(
                    post = post,
                    isLiked = isLiked,
                    likesCount = likesCount,
                    isSaved = isSaved,
                    isActive = page == pagerState.currentPage && selectedPost == null,
                    onPlaybackProgress = onPlaybackProgress,
                    seekRequest = seekRequest,
                    onSeekRequestHandled = onSeekRequestHandled,
                    onOpenAuthor = {
                        if (post.author.id.isNotBlank()) onOpenProfile(post.author)
                    },
                    onLike = ::likePost,
                    onTapLike = { if (!isLiked) likePost() },
                    onComment = {
                        selectedPost = post
                        commentDraft = ""
                        commentImage = null
                        replyTarget = null
                        if (post.id.startsWith("sample-")) {
                            localComments[post.id] = localComments[post.id].orEmpty()
                        } else {
                            postViewModel.loadComments(post.id)
                        }
                    },
                    onSave = {
                        if (post.id.startsWith("sample-")) {
                            sampleSavedPosts[post.id] = !isSaved
                            return@VideoFeedCard
                        }
                        if (saveRequestsInFlight[post.id] == true) return@VideoFeedCard
                        if (authViewModel.tokenOrNull().isNullOrBlank()) {
                            Toast.makeText(context, "Sign in to save posts.", Toast.LENGTH_SHORT).show()
                            return@VideoFeedCard
                        }
                        if (isSaved) {
                            collectionsPost = post
                            return@VideoFeedCard
                        }
                        saveRequestsInFlight[post.id] = true
                        postViewModel.toggleSavedPost(post.id) { saved ->
                            saveRequestsInFlight.remove(post.id)
                            if (saved != isSaved) {
                                Toast.makeText(
                                    context,
                                    if (saved) "Video added to Saved" else "Video removed from Saved",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            if (saved) collectionsPost = post
                        }
                    },
                    onShare = {
                        sharingPost = post
                        postViewModel.loadInbox()
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.62f), Color.Black.copy(alpha = 0f)),
                    ),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandLogo(
                    contentDescription = "Topline",
                    modifier = Modifier
                        .width(48.dp)
                        .height(28.dp),
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Following",
                        color = if (showFollowing) Color.White else Color.White.copy(alpha = 0.68f),
                        fontWeight = if (showFollowing) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clickable { showFollowing = true }.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                    Text("|", color = Color.White.copy(alpha = 0.55f))
                    Text(
                        "For You",
                        color = if (showFollowing) Color.White.copy(alpha = 0.68f) else Color.White,
                        fontWeight = if (showFollowing) FontWeight.Normal else FontWeight.Bold,
                        modifier = Modifier.clickable { showFollowing = false }.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                }
                IconButton(onClick = {
                    searchDraft = ""
                    searchSubmitted = false
                    showSearch = true
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Search videos", tint = Color.White)
                }
                IconButton(onClick = onOpenAlerts) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Alerts", tint = Color.White)
                        if (unreadNotificationCount > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp)
                                    .size(16.dp).clip(CircleShape).background(Color(0xFFE53935)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSearch) {
        TikTokSearchDialog(
            query = searchDraft,
            submitted = searchSubmitted,
            recentQueries = recentSearches,
            suggestions = searchSuggestions,
            placeholder = "Search videos, captions, or creators",
            onQueryChange = {
                searchDraft = it
                searchSubmitted = false
            },
            onSubmit = { submittedQuery ->
                searchDraft = submittedQuery
                searchQuery = submittedQuery.trim()
                searchSubmitted = true
                if (searchQuery.isNotBlank()) {
                    recentSearches = (listOf(searchQuery) + recentSearches.filterNot {
                        it.equals(searchQuery, ignoreCase = true)
                    }).take(8)
                }
            },
            onBack = {
                showSearch = false
                searchQuery = ""
            },
            results = { modifier ->
                val matchingPosts = sourcePosts.filter { post ->
                    searchQuery.isNotBlank() &&
                        (post.text.contains(searchQuery, ignoreCase = true) ||
                            post.author.name.contains(searchQuery, ignoreCase = true) ||
                            post.author.username.contains(searchQuery, ignoreCase = true))
                }
                if (matchingPosts.isEmpty()) {
                    Box(modifier, contentAlignment = Alignment.Center) {
                        Text("No videos match \"$searchQuery\".", color = colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = modifier,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                    ) {
                        items(matchingPosts, key = { it.id }) { post ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { showSearch = false }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                    Text(post.text.ifBlank { "Video by ${post.author.name}" }, maxLines = 2)
                                    Text(
                                        "@${post.author.username}",
                                        color = colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Text("${post.likesCount} likes", color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
        )
    }

    sharingPost?.let { post ->
        val shareUrl = post.playableVideoUrl() ?: post.image ?: post.mediaUrl
        PostShareSheet(
            post = post,
            contacts = (inboxState.following + inboxState.conversations.map { it.user })
                .distinctBy { it.id }
                .filter { it.id.isNotBlank() },
            isSaved = if (post.id.startsWith("sample-")) {
                sampleSavedPosts[post.id] == true
            } else {
                post.id in uiState.savedPostIds
            },
            isLoadingContacts = inboxState.isLoading,
            onCopyLink = {
                if (shareUrl.isNullOrBlank()) {
                    Toast.makeText(context, "This post has no shareable link.", Toast.LENGTH_SHORT).show()
                } else {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Topline post link", shareUrl))
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                }
            },
            onShareToApps = {
                if (!shareUrl.isNullOrBlank()) {
                    val shareText = buildString {
                        if (post.text.isNotBlank()) append("${post.text}\n")
                        append(shareUrl)
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                }
            },
            onSave = {
                if (post.id.startsWith("sample-")) {
                    sampleSavedPosts[post.id] = sampleSavedPosts[post.id] != true
                } else if (authViewModel.tokenOrNull().isNullOrBlank()) {
                    Toast.makeText(context, "Sign in to save posts.", Toast.LENGTH_SHORT).show()
                } else {
                    val wasSaved = post.id in uiState.savedPostIds
                    postViewModel.toggleSavedPost(post.id) { saved ->
                        if (saved != wasSaved) {
                            Toast.makeText(
                                context,
                                if (saved) "Post saved" else "Post removed from saved",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            },
            onManageCollections = {
                if (post.id.startsWith("sample-")) {
                    Toast.makeText(context, "Sign in to organize saved videos.", Toast.LENGTH_SHORT).show()
                } else if (authViewModel.tokenOrNull().isNullOrBlank()) {
                    Toast.makeText(context, "Sign in to organize saved videos.", Toast.LENGTH_SHORT).show()
                } else if (post.id in uiState.savedPostIds) {
                    collectionsPost = post
                    sharingPost = null
                } else {
                    postViewModel.toggleSavedPost(post.id) { saved ->
                        if (saved) {
                            collectionsPost = post
                            sharingPost = null
                        } else {
                            Toast.makeText(context, "Unable to save this video.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onSendToContact = { contact, text, onResult ->
                postViewModel.sendSharedPost(contact.id, text, onResult)
            },
            onDismiss = { sharingPost = null },
        )
    }

    collectionsPost?.let { post ->
        SavedCollectionsSheet(
            postTitle = post.text.ifBlank { "Video by @${post.author.username}" },
            isSaved = post.id in uiState.savedPostIds,
            collections = uiState.savedCollections,
            isLoading = uiState.isSavedCollectionsLoading,
            errorMessage = uiState.savedCollectionsError,
            onLoad = { postViewModel.loadSavedCollections(post.id) },
            onToggleSave = {
                if (saveRequestsInFlight[post.id] != true) {
                    saveRequestsInFlight[post.id] = true
                    postViewModel.toggleSavedPost(post.id) { saved ->
                        saveRequestsInFlight.remove(post.id)
                        if (saved) postViewModel.loadSavedCollections(post.id)
                        else Toast.makeText(context, "Video removed from Saved.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCreate = { name -> postViewModel.createSavedCollection(name, post.id) },
            onToggle = { collection, included ->
                postViewModel.setSavedCollectionMembership(collection.id, post.id, included)
            },
            onDelete = { collection -> postViewModel.deleteSavedCollection(collection.id) },
            onDismiss = { collectionsPost = null },
        )
    }

    selectedPost?.let { post ->
        val isSamplePost = post.id.startsWith("sample-")
        val visibleComments = if (isSamplePost) localComments[post.id].orEmpty() else commentsState.comments
        val rootComments = visibleComments.filter { it.parentCommentId.isNullOrBlank() }
        val isCommentsLoading = !isSamplePost && commentsState.isLoading
        val visibleCommentsError = if (isSamplePost) null else commentsErrorMessage
        ModalBottomSheet(
            onDismissRequest = {
                selectedPost = null
                commentImage = null
                replyTarget = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Comments (${if (isSamplePost) visibleComments.size else commentsState.commentsCount})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                if (isCommentsLoading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFF57F17))
                    }
                } else if (!visibleCommentsError.isNullOrBlank()) {
                    Text(
                        text = visibleCommentsError,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (visibleComments.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No comments yet. Start the conversation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(rootComments, key = { it.id }) { comment ->
                            val replies = visibleComments.filter { it.parentCommentId == comment.id }
                            CommentRow(
                                comment = comment,
                                isLiked = if (isSamplePost) {
                                    sampleCommentLikes[comment.id] == true
                                } else {
                                    comment.likedByCurrentUser
                                },
                                currentUserId = authState.currentUser?.id.orEmpty(),
                                onLike = {
                                    if (isSamplePost) {
                                        sampleCommentLikes[comment.id] = sampleCommentLikes[comment.id] != true
                                    } else {
                                        authViewModel.tokenOrNull()?.let { token ->
                                            postViewModel.toggleCommentLike(comment.id, token)
                                        }
                                    }
                                },
                                onReply = {
                                    replyTarget = comment
                                },
                                onDelete = {
                                    if (isSamplePost) {
                                        val idsToRemove = setOf(comment.id) + replies.map { it.id }
                                        localComments[post.id] = visibleComments.filterNot { it.id in idsToRemove }
                                    } else {
                                        authViewModel.tokenOrNull()?.let { token ->
                                            postViewModel.deleteComment(post.id, comment.id, token)
                                        }
                                    }
                                },
                            )
                            replies.forEach { reply ->
                                CommentRow(
                                    comment = reply,
                                    isReply = true,
                                    isLiked = if (isSamplePost) {
                                        sampleCommentLikes[reply.id] == true
                                    } else {
                                        reply.likedByCurrentUser
                                    },
                                    currentUserId = authState.currentUser?.id.orEmpty(),
                                    onLike = {
                                        if (isSamplePost) {
                                            sampleCommentLikes[reply.id] = sampleCommentLikes[reply.id] != true
                                        } else {
                                            authViewModel.tokenOrNull()?.let { token ->
                                                postViewModel.toggleCommentLike(reply.id, token)
                                            }
                                        }
                                    },
                                    onReply = {
                                        replyTarget = comment
                                    },
                                    onDelete = {
                                        if (isSamplePost) {
                                            localComments[post.id] = visibleComments.filterNot { it.id == reply.id }
                                        } else {
                                            authViewModel.tokenOrNull()?.let { token ->
                                                postViewModel.deleteComment(post.id, reply.id, token)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                replyTarget?.let { replyingTo ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Replying to @${replyingTo.author.username}",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = { replyTarget = null }) { Text("Cancel", color = Color(0xFFF57F17)) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("❤️", "😂", "😍", "🔥", "👏", "🥰", "😭", "🙏").forEach { emoji ->
                        TextButton(
                            onClick = { commentDraft = (commentDraft + emoji).take(1000) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(emoji, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                commentImage?.let { imageUri ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Image attached to comment",
                            modifier = Modifier.size(68.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        TextButton(onClick = { commentImage = null }) { Text("Remove", color = Color(0xFFF57F17)) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { pickCommentImage.launch("image/*") }) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach image to comment",
                            tint = Color(0xFFF57F17),
                        )
                    }
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it.take(1000) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (replyTarget == null) "Add a comment..." else "Write a reply...") },
                        maxLines = 3,
                    )
                    IconButton(
                        onClick = {
                            val token = authViewModel.tokenOrNull() ?: return@IconButton
                            val target = replyTarget
                            if (isSamplePost) {
                                val currentUser = authState.currentUser
                                val comment = CommentItem(
                                    id = "local-${System.nanoTime()}",
                                    postId = post.id,
                                    author = CommentAuthor(
                                        id = currentUser?.id.orEmpty(),
                                        name = currentUser?.name.orEmpty(),
                                        username = currentUser?.username.orEmpty(),
                                        profilePicture = currentUser?.profilePicture,
                                    ),
                                    text = commentDraft,
                                    image = commentImage?.toString(),
                                    parentCommentId = target?.id,
                                )
                                localComments[post.id] = visibleComments + comment
                                commentDraft = ""
                                commentImage = null
                                replyTarget = null
                            } else {
                                postViewModel.addComment(
                                    postId = post.id,
                                    token = token,
                                    text = commentDraft,
                                    imageUri = commentImage,
                                    parentCommentId = target?.id,
                                ) {
                                    commentDraft = ""
                                    commentImage = null
                                    replyTarget = null
                                    selectedPost = post.copy(commentsCount = it)
                                }
                            }
                        },
                        enabled = !commentsState.isSubmitting && (commentDraft.isNotBlank() || commentImage != null),
                    ) {
                        if (commentsState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFF57F17))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Post comment", tint = Color(0xFFF57F17))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentItem,
    currentUserId: String,
    isLiked: Boolean,
    isReply: Boolean = false,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
) {
    val authorName = comment.author.name.ifBlank { comment.author.username.ifBlank { "Topliner" } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 40.dp else 0.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(if (isReply) 30.dp else 36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF57F17)),
            contentAlignment = Alignment.Center,
        ) {
            if (!comment.author.profilePicture.isNullOrBlank()) {
                AsyncImage(
                    model = comment.author.profilePicture,
                    contentDescription = "$authorName profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(authorName.firstOrNull()?.uppercase() ?: "U", color = Color.White)
            }
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(authorName, fontWeight = FontWeight.SemiBold)
            if (comment.text.isNotBlank()) {
                Text(comment.text, color = MaterialTheme.colorScheme.onSurface)
            }
            if (!comment.image.isNullOrBlank()) {
                AsyncImage(
                    model = comment.image,
                    contentDescription = "Comment image",
                    modifier = Modifier.padding(top = 6.dp)
                        .fillMaxWidth(0.72f)
                        .height(170.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.createdAt.orEmpty().take(10),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
                TextButton(onClick = onReply) {
                    Text("Reply", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (comment.author.id.isNotBlank() && comment.author.id == currentUserId) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onLike, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike comment" else "Like comment",
                    tint = if (isLiked) Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
            if (comment.likesCount > 0) {
                Text(comment.likesCount.toString(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun VideoFeedCard(
    post: PostItem,
    isLiked: Boolean,
    likesCount: Int,
    isSaved: Boolean,
    isActive: Boolean,
    onPlaybackProgress: (Float) -> Unit,
    seekRequest: Pair<Long, Float>?,
    onSeekRequestHandled: (Long) -> Unit,
    onOpenAuthor: () -> Unit,
    onLike: () -> Unit,
    onTapLike: () -> Unit,
    onComment: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val authorName = post.author.name.ifBlank { post.author.username.ifBlank { "Topline User" } }
    val authorHandle = if (post.author.username.isNotBlank()) "@${post.author.username}" else "@topline"
    val videoUrl = post.playableVideoUrl()
    val mediaUrl = post.displayableMediaUrl()

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!mediaUrl.isNullOrBlank()) {
                if (videoUrl != null) {
                    VideoPreview(
                        videoUrl = cloudinaryPlaybackUrl(videoUrl),
                        isActive = isActive,
                        onTapLike = onTapLike,
                        onPlaybackProgress = onPlaybackProgress,
                        seekRequest = seekRequest,
                        onSeekRequestHandled = onSeekRequestHandled,
                    )
                } else {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF111111)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = post.text.ifBlank { "Topline update" },
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.45f to Color.Black.copy(alpha = 0.28f),
                                1.0f to Color.Black.copy(alpha = 0.82f),
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 84.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialAvatar(
                        authorName,
                        48,
                        post.author.profilePicture,
                        Modifier.clickable(onClick = onOpenAuthor),
                    )
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(
                            authorHandle,
                            modifier = Modifier.clickable(onClick = onOpenAuthor),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            authorName,
                            modifier = Modifier.clickable(onClick = onOpenAuthor),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    text = post.text.ifBlank { "Fresh videos, people, and ideas from your network." },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (mediaUrl != null) {
                    Text(
                        text = if (videoUrl == null) "Photo" else "Video",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onLike) {
                        TikTokHeartIcon(isLiked = isLiked)
                    }
                    Text(text = "$likesCount", color = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onComment, enabled = post.allowComments) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = if (post.allowComments) "Comments" else "Comments disabled",
                            tint = if (post.allowComments) Color.White else Color.White.copy(alpha = 0.45f),
                        )
                    }
                    Text(
                        text = if (post.allowComments) "${post.commentsCount}" else "Off",
                        color = Color.White,
                    )
                }

                IconButton(onClick = onShare) {
                    TikTokShareIcon()
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onSave) {
                        TikTokSaveIcon(isSaved = isSaved)
                    }
                }
            }

        }
    }
}

@Composable
private fun TikTokShareIcon() {
    Canvas(Modifier.size(28.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.84f)
            cubicTo(
                size.width * 0.16f, size.height * 0.60f,
                size.width * 0.20f, size.height * 0.43f,
                size.width * 0.38f, size.height * 0.40f,
            )
            cubicTo(
                size.width * 0.55f, size.height * 0.36f,
                size.width * 0.68f, size.height * 0.42f,
                size.width * 0.68f, size.height * 0.52f,
            )
            lineTo(size.width * 0.68f, size.height * 0.30f)
            lineTo(size.width * 0.96f, size.height * 0.52f)
            lineTo(size.width * 0.68f, size.height * 0.75f)
            lineTo(size.width * 0.68f, size.height * 0.60f)
            cubicTo(
                size.width * 0.61f, size.height * 0.49f,
                size.width * 0.52f, size.height * 0.48f,
                size.width * 0.42f, size.height * 0.51f,
            )
            cubicTo(
                size.width * 0.27f, size.height * 0.56f,
                size.width * 0.28f, size.height * 0.69f,
                size.width * 0.28f, size.height * 0.84f,
            )
        }
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(
                width = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun TikTokHeartIcon(isLiked: Boolean) {
    Canvas(Modifier.size(32.dp)) {
        val heart = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.88f)
            cubicTo(size.width * 0.43f, size.height * 0.82f, size.width * 0.08f, size.height * 0.55f, size.width * 0.08f, size.height * 0.32f)
            cubicTo(size.width * 0.08f, size.height * 0.04f, size.width * 0.40f, size.height * 0.02f, size.width * 0.5f, size.height * 0.25f)
            cubicTo(size.width * 0.60f, size.height * 0.02f, size.width * 0.92f, size.height * 0.04f, size.width * 0.92f, size.height * 0.32f)
            cubicTo(size.width * 0.92f, size.height * 0.55f, size.width * 0.57f, size.height * 0.82f, size.width * 0.5f, size.height * 0.88f)
            close()
        }
        drawPath(
            path = heart,
            color = if (isLiked) Color(0xFFFF2D55) else Color.White,
            style = if (isLiked) androidx.compose.ui.graphics.drawscope.Fill else Stroke(
                width = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun TikTokSaveIcon(isSaved: Boolean) {
    Canvas(Modifier.size(30.dp)) {
        val bookmark = Path().apply {
            moveTo(size.width * 0.24f, size.height * 0.12f)
            quadraticTo(size.width * 0.24f, size.height * 0.08f, size.width * 0.30f, size.height * 0.08f)
            lineTo(size.width * 0.70f, size.height * 0.08f)
            quadraticTo(size.width * 0.76f, size.height * 0.08f, size.width * 0.76f, size.height * 0.14f)
            lineTo(size.width * 0.76f, size.height * 0.90f)
            lineTo(size.width * 0.50f, size.height * 0.70f)
            lineTo(size.width * 0.24f, size.height * 0.90f)
            close()
        }
        drawPath(
            path = bookmark,
            color = Color.White,
            style = if (isSaved) androidx.compose.ui.graphics.drawscope.Fill else Stroke(
                width = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun VideoPreview(
    videoUrl: String,
    isActive: Boolean,
    onTapLike: () -> Unit,
    onPlaybackProgress: (Float) -> Unit,
    seekRequest: Pair<Long, Float>?,
    onSeekRequestHandled: (Long) -> Unit,
) {
    val context = LocalContext.current
    var playbackError by remember(videoUrl) { mutableStateOf(false) }
    var isPlaying by remember(videoUrl) { mutableStateOf(isActive) }
    var isBuffering by remember(videoUrl) { mutableStateOf(true) }
    var heartBurstId by remember(videoUrl) { mutableStateOf(0) }
    val exoPlayer = remember(context, videoUrl) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        playbackError = true
                        isBuffering = false
                        isPlaying = false
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_READY) playbackError = false
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })
                setMediaItem(MediaItem.fromUri(videoUrl))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = isActive
            }
    }

    LaunchedEffect(exoPlayer, isActive, seekRequest) {
        exoPlayer.playWhenReady = isActive
        if (!isActive) isPlaying = false
        if (isActive) {
            onPlaybackProgress(0f)
            seekRequest?.let { (requestId, fraction) ->
                val duration = exoPlayer.duration
                if (duration > 0) {
                    exoPlayer.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
                    onSeekRequestHandled(requestId)
                }
            }
        }
    }

    LaunchedEffect(exoPlayer, isActive) {
        if (!isActive) return@LaunchedEffect
        while (true) {
            val duration = exoPlayer.duration
            if (duration > 0) {
                onPlaybackProgress((exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
            }
            kotlinx.coroutines.delay(250)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Box(
        Modifier.fillMaxSize().pointerInput(videoUrl, isActive) {
            detectTapGestures(
                onTap = {
                    if (isActive) {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    }
                },
                onDoubleTap = {
                    if (isActive) {
                        heartBurstId += 1
                        onTapLike()
                    }
                },
            )
        },
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (isBuffering && !playbackError && isActive) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(42.dp),
                color = Color.White,
                strokeWidth = 3.dp,
            )
        }
        if (!isPlaying && !playbackError && isActive && !isBuffering) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play video",
                modifier = Modifier.align(Alignment.Center).size(72.dp),
                tint = Color.White.copy(alpha = 0.9f),
            )
        }
        if (playbackError) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "This video could not be played. Check your connection or try again.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = {
                        playbackError = false
                        exoPlayer.seekTo(0)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = isActive
                    },
                ) {
                    Text("Try again", color = Color.White)
                }
            }
        }
        if (heartBurstId > 0) {
            key(heartBurstId) {
                HeartBurst(heartBurstId) { finishedBurstId ->
                    if (heartBurstId == finishedBurstId) heartBurstId = 0
                }
            }
        }
    }
}

@Composable
private fun HeartBurst(burstId: Int, onFinished: (Int) -> Unit) {
    val heartOffsets = listOf(-112, -76, -38, 0, 42, 82, 116)
    val heartSizes = listOf(34, 46, 40, 58, 38, 48, 32)

    LaunchedEffect(burstId) {
        kotlinx.coroutines.delay(1_050)
        onFinished(burstId)
    }

    Box(Modifier.fillMaxSize()) {
        heartOffsets.forEachIndexed { index, xOffset ->
            val rise = remember(burstId, index) { Animatable(0f) }
            val alpha = remember(burstId, index) { Animatable(1f) }
            LaunchedEffect(burstId, index) {
                val delay = index * 35
                kotlinx.coroutines.coroutineScope {
                    launch {
                        rise.animateTo(
                            targetValue = -205f - (index % 3) * 32f,
                            animationSpec = tween(durationMillis = 820, delayMillis = delay),
                        )
                    }
                    launch {
                        alpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 820, delayMillis = delay + 180),
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = xOffset.dp, y = rise.value.dp)
                    .size(heartSizes[index].dp),
                tint = Color(0xFFFF2D55).copy(alpha = alpha.value),
            )
        }
    }
}
