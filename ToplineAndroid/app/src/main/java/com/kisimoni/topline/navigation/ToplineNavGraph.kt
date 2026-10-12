package com.kisimoni.topline.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Alignment
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kisimoni.topline.data.api.User
import com.kisimoni.topline.data.api.NetworkLoadingTracker
import com.kisimoni.topline.ui.screens.AuthScreen
import com.kisimoni.topline.ui.screens.DashboardScreen
import com.kisimoni.topline.ui.screens.EditProfileScreen
import com.kisimoni.topline.ui.screens.HomeScreen
import com.kisimoni.topline.ui.screens.MessagesScreen
import com.kisimoni.topline.ui.screens.NotificationsScreen
import com.kisimoni.topline.ui.screens.PeopleScreen
import com.kisimoni.topline.ui.screens.SettingsScreen
import com.kisimoni.topline.ui.screens.SuggestedPerson
import com.kisimoni.topline.ui.screens.UploadScreen
import com.kisimoni.topline.ui.screens.UserProfileScreen
import com.kisimoni.topline.ui.NetworkLoadingOverlay
import com.kisimoni.topline.viewmodel.AuthViewModel
import com.kisimoni.topline.viewmodel.PostViewModel

@Composable
fun ToplineNavGraph(
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel,
    darkMode: Boolean,
    language: String,
    onDarkModeChanged: (Boolean) -> Unit,
    onLanguageChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    val hasActiveNetworkRequest by NetworkLoadingTracker.isLoading.collectAsState()
    val postState by postViewModel.uiState.collectAsState()
    val inboxState by postViewModel.inboxUiState.collectAsState()
    val conversationState by postViewModel.conversationUiState.collectAsState()
    val analyticsState by postViewModel.analyticsUiState.collectAsState()
    val peopleState by postViewModel.peopleUiState.collectAsState()
    val otherProfileState by postViewModel.otherProfileUiState.collectAsState()
    val notificationsState by postViewModel.notificationsUiState.collectAsState()
    val user = authState.currentUser
    var selectedPerson by remember { mutableStateOf<SuggestedPerson?>(null) }
    var messageContact by remember { mutableStateOf<String?>(null) }
    var messageContactId by remember { mutableStateOf<String?>(null) }
    var playbackProgress by remember { mutableStateOf(0f) }
    var seekSequence by remember { mutableStateOf(0L) }
    var seekRequest by remember { mutableStateOf<Pair<Long, Float>?>(null) }
    var showNetworkLoader by remember { mutableStateOf(false) }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val mainRoutes = setOf(Routes.HOME, Routes.PEOPLE, Routes.UPLOAD, Routes.MESSAGES, Routes.PROFILE)
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(hasActiveNetworkRequest) {
        if (hasActiveNetworkRequest > 0) {
            kotlinx.coroutines.delay(250)
            showNetworkLoader = hasActiveNetworkRequest > 0
        } else {
            showNetworkLoader = false
        }
    }

    LaunchedEffect(authState.isAuthenticated, currentRoute) {
        if (authState.isAuthenticated && currentRoute == Routes.LOGIN) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        } else if (!authState.isAuthenticated && currentRoute != Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    LaunchedEffect(authState.isAuthenticated, currentRoute, messageContactId) {
        if (authState.isAuthenticated && currentRoute == Routes.MESSAGES) {
            if (messageContactId.isNullOrBlank()) {
                postViewModel.loadInbox()
            } else {
                postViewModel.loadConversation(messageContactId.orEmpty())
            }
        }
        if (authState.isAuthenticated && currentRoute == Routes.DASHBOARD) {
            authViewModel.tokenOrNull()?.let(postViewModel::loadMyAnalytics)
        }
    }
    LaunchedEffect(authState.isAuthenticated, currentRoute, selectedPerson?.id) {
        if (authState.isAuthenticated && currentRoute == Routes.PEOPLE) {
            postViewModel.loadPeople()
        }
        if (authState.isAuthenticated && currentRoute == Routes.PROFILE && selectedPerson != null) {
            selectedPerson?.id?.let(postViewModel::loadOtherProfile)
        }
    }
    LaunchedEffect(authState.isAuthenticated, currentRoute, user?.id) {
        if (authState.isAuthenticated) {
            postViewModel.loadUnreadCounts()
        }
        if (authState.isAuthenticated && currentRoute == Routes.NOTIFICATIONS) {
            postViewModel.loadNotifications()
        }
        if (currentRoute != Routes.HOME) playbackProgress = 0f
    }
    LaunchedEffect(authState.isAuthenticated, user?.id) {
        if (!authState.isAuthenticated) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(30_000)
            postViewModel.loadUnreadCounts()
        }
    }

    Scaffold(
        bottomBar = {
            if (authState.isAuthenticated && currentRoute in mainRoutes) {
                Column(modifier = Modifier.fillMaxWidth().background(colorScheme.surface)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (currentRoute == Routes.HOME) 3.dp else 1.dp)
                            .background(colorScheme.outlineVariant)
                            .then(
                                if (currentRoute == Routes.HOME) {
                                    Modifier.pointerInput(currentRoute) {
                                        detectTapGestures { point ->
                                            val fraction = (point.x / size.width).coerceIn(0f, 1f)
                                            seekSequence += 1
                                            seekRequest = seekSequence to fraction
                                        }
                                    }.pointerInput(currentRoute) {
                                        detectDragGestures(
                                            onDragStart = { point ->
                                                val fraction = (point.x / size.width).coerceIn(0f, 1f)
                                                seekSequence += 1
                                                seekRequest = seekSequence to fraction
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                                seekSequence += 1
                                                seekRequest = seekSequence to fraction
                                            },
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        if (currentRoute == Routes.HOME) {
                            Box(
                                Modifier
                                    .fillMaxWidth(playbackProgress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(colorScheme.primary),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    BottomTab(
                        label = "Home",
                        selected = currentRoute == Routes.HOME,
                        onClick = { navController.navigate(Routes.HOME) { launchSingleTop = true } },
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                    }
                    BottomTab(
                        label = "People",
                        selected = currentRoute == Routes.PEOPLE,
                        onClick = { navController.navigate(Routes.PEOPLE) { launchSingleTop = true } },
                    ) {
                        Icon(Icons.Default.People, contentDescription = null)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .semantics { contentDescription = "Create post" }
                            .clickable {
                                navController.navigate(Routes.UPLOAD) { launchSingleTop = true }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                    BottomTab(
                        label = "Inbox",
                        selected = currentRoute == Routes.MESSAGES,
                        unreadCount = inboxState.unreadMessageCount,
                        onClick = {
                            messageContact = null
                            messageContactId = null
                            navController.navigate(Routes.MESSAGES) { launchSingleTop = true }
                        },
                    ) {
                        MessageDotsIcon()
                    }
                    BottomTab(
                        label = "Profile",
                        selected = currentRoute == Routes.PROFILE,
                        onClick = {
                            selectedPerson = null
                            navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                        },
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.LOGIN,
                modifier = Modifier.padding(innerPadding),
            ) {
            composable(Routes.LOGIN) {
                AuthScreen(
                    authViewModel = authViewModel,
                    onNavigateToHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    authViewModel = authViewModel,
                    postViewModel = postViewModel,
                    onOpenAlerts = { navController.navigate(Routes.NOTIFICATIONS) },
                    unreadNotificationCount = notificationsState.unreadCount,
                    onPlaybackProgress = { playbackProgress = it },
                    seekRequest = seekRequest,
                    onSeekRequestHandled = { requestId ->
                        if (seekRequest?.first == requestId) seekRequest = null
                    },
                    onOpenProfile = { author ->
                        selectedPerson = SuggestedPerson(
                            name = author.name,
                            username = author.username,
                            bio = "",
                            location = "",
                            followers = 0,
                            following = 0,
                            id = author.id,
                            profilePicture = author.profilePicture,
                        )
                        navController.navigate(Routes.PROFILE)
                    },
                )
            }

            composable(Routes.PEOPLE) {
                PeopleScreen(
                    people = peopleState.people.map { person ->
                        SuggestedPerson(
                            name = person.name,
                            username = person.username,
                            bio = person.bio.orEmpty(),
                            location = person.location.orEmpty(),
                            followers = 0,
                            following = 0,
                            id = person.id,
                            profilePicture = person.profilePicture,
                            isFollowing = person.isFollowing,
                        )
                    },
                    followers = peopleState.followers,
                    following = peopleState.following,
                    isLoading = peopleState.isLoading,
                    isFriendsLoading = peopleState.isFriendsLoading,
                    errorMessage = peopleState.errorMessage,
                    friendsErrorMessage = peopleState.friendsErrorMessage,
                    onBack = { navController.popBackStack() },
                    onToggleFollow = { person ->
                        val token = authViewModel.tokenOrNull()
                        if (token != null && person.id.isNotBlank()) {
                            postViewModel.toggleFollow(token, person.id) { isFollowing ->
                                postViewModel.updatePeopleFollowState(person.id, isFollowing)
                            }
                        }
                    },
                    onOpenProfile = { person ->
                        selectedPerson = person
                        navController.navigate(Routes.PROFILE)
                    },
                    onOpenFriends = {
                        if (authViewModel.tokenOrNull() != null) postViewModel.loadMyFriends()
                    },
                    onUnfollow = { contact ->
                        val token = authViewModel.tokenOrNull()
                        if (token != null && contact.id.isNotBlank()) {
                            postViewModel.toggleFollow(token, contact.id) {
                                postViewModel.loadMyFriends()
                                postViewModel.loadPeople()
                            }
                        }
                    },
                )
            }

            composable(Routes.UPLOAD) {
                UploadScreen(
                    user = user,
                    isSubmitting = postState.isLoading,
                    errorMessage = postState.errorMessage,
                    onSubmit = { text, uri, coverUri, audience, allowComments, editPlan ->
                        val token = authViewModel.tokenOrNull() ?: return@UploadScreen
                        if (uri != null) {
                            postViewModel.createPostWithMedia(
                                token,
                                text,
                                uri,
                                coverUri,
                                audience,
                                allowComments,
                                editPlan,
                            ) {
                                context.getSharedPreferences(
                                    "topline_post_draft_${user?.id.orEmpty()}",
                                    android.content.Context.MODE_PRIVATE,
                                )
                                    .edit().clear().apply()
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = false }
                                }
                            }
                        } else {
                            postViewModel.createPost(token, text, audience, allowComments) {
                                context.getSharedPreferences(
                                    "topline_post_draft_${user?.id.orEmpty()}",
                                    android.content.Context.MODE_PRIVATE,
                                )
                                    .edit().clear().apply()
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = false }
                                }
                            }

                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.MESSAGES) {
                MessagesScreen(
                    contactName = messageContact,
                    contactId = messageContactId,
                    currentUserId = user?.id.orEmpty(),
                    owner = user,
                    following = inboxState.following,
                    conversations = inboxState.conversations,
                    messages = conversationState.messages,
                    isLoading = if (messageContactId == null) inboxState.isLoading else conversationState.isLoading,
                    isSending = conversationState.isSending,
                    errorMessage = inboxState.errorMessage,
                    conversationError = conversationState.errorMessage,
                    onBack = {
                        if (messageContactId != null) {
                            messageContact = null
                            messageContactId = null
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onFindPeople = { navController.navigate(Routes.PEOPLE) },
                    onRefresh = {
                        if (messageContactId == null) {
                            postViewModel.loadInbox()
                        } else {
                            postViewModel.loadConversation(messageContactId.orEmpty())
                        }
                    },
                    onOpenContact = { contact ->
                        messageContact = contact.name
                        messageContactId = contact.id
                    },
                    onSendMessage = { text, image, onSuccess ->
                        messageContactId?.let { postViewModel.sendMessage(it, text, image, onSuccess) }
                    },
                    onEditMessage = postViewModel::updateMessage,
                    onDeleteMessage = postViewModel::deleteMessage,
                )
            }

            composable(Routes.PROFILE) {
                val isOwnProfile = selectedPerson == null
                LaunchedEffect(isOwnProfile, user?.id) {
                    val token = authViewModel.tokenOrNull()
                    if (isOwnProfile && !token.isNullOrBlank()) {
                        postViewModel.loadMyPosts(token)
                        postViewModel.loadMyProfileCounts(token)
                        postViewModel.loadSavedPosts()
                        postViewModel.loadSavedCollections()
                    }
                }

                val profileUser = if (isOwnProfile) user else otherProfileState.user ?: User(
                    id = selectedPerson?.id.orEmpty(),
                    name = selectedPerson?.name.orEmpty(),
                    username = selectedPerson?.username.orEmpty(),
                    profilePicture = selectedPerson?.profilePicture,
                    bio = selectedPerson?.bio,
                    location = selectedPerson?.location,
                )
                val profilePosts = if (isOwnProfile) postState.ownPosts else otherProfileState.posts
                UserProfileScreen(
                    user = profileUser,
                    posts = profilePosts,
                    savedPosts = postState.savedPosts,
                    savedCollections = postState.savedCollections,
                    savedCollectionPosts = postState.savedCollectionPosts,
                    activeSavedCollection = postState.activeSavedCollection,
                    savedCollectionsError = postState.savedCollectionsError,
                    isSavedCollectionsLoading = postState.isSavedCollectionsLoading,
                    isSavedPostsLoading = postState.isSavedPostsLoading,
                    savedPostsError = postState.savedPostsError,
                    postsCount = postState.ownPostsCount,
                    errorMessage = if (isOwnProfile) postState.ownPostsError else otherProfileState.errorMessage,
                    statsErrorMessage = if (isOwnProfile) postState.ownProfileCountsError else null,
                    isOwnProfile = isOwnProfile,
                    person = selectedPerson,
                    followersCount = if (isOwnProfile) postState.ownFollowersCount else otherProfileState.followersCount,
                    followingCount = if (isOwnProfile) postState.ownFollowingCount else otherProfileState.followingCount,
                    isFollowing = selectedPerson?.isFollowing == true || otherProfileState.isFollowing,
                    onBack = { navController.popBackStack() },
                    onLoadSavedPosts = postViewModel::loadSavedPosts,
                    onLoadSavedCollections = { postViewModel.loadSavedCollections() },
                    onLoadSavedCollectionPosts = postViewModel::loadSavedCollectionPosts,
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onMessage = {
                        messageContact = selectedPerson?.name
                        messageContactId = selectedPerson?.id
                        navController.navigate(Routes.MESSAGES)
                    },
                    onFollow = {
                        val person = selectedPerson
                        val token = authViewModel.tokenOrNull()
                        if (person != null && token != null && person.id.isNotBlank()) {
                            postViewModel.toggleFollow(token, person.id) { isFollowing ->
                                selectedPerson = person.copy(isFollowing = isFollowing)
                                postViewModel.loadOtherProfile(person.id)
                            }
                        }
                    },
                )
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    notifications = notificationsState.notifications,
                    isLoading = notificationsState.isLoading,
                    errorMessage = notificationsState.errorMessage,
                    onBack = { navController.popBackStack() },
                    onRefresh = postViewModel::loadNotifications,
                    onBackToFeed = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    onNotificationClick = { notification ->
                        postViewModel.markNotificationRead(notification.id)
                        val actor = notification.actor
                        when (notification.type) {
                            "message" -> {
                                if (actor != null && actor.id.isNotBlank()) {
                                    messageContact = actor.name
                                    messageContactId = actor.id
                                    navController.navigate(Routes.MESSAGES)
                                }
                            }
                            "follow" -> {
                                if (actor != null && actor.id.isNotBlank()) {
                                    selectedPerson = SuggestedPerson(
                                        name = actor.name,
                                        username = actor.username,
                                        bio = "",
                                        location = "",
                                        followers = 0,
                                        following = 0,
                                        id = actor.id,
                                        profilePicture = actor.profilePicture,
                                    )
                                    navController.navigate(Routes.PROFILE)
                                }
                            }
                            else -> navController.navigate(Routes.HOME)
                        }
                    },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    user = user,
                    darkMode = darkMode,
                    language = language,
                    onBack = { navController.popBackStack() },
                    onDarkModeChanged = onDarkModeChanged,
                    onLanguageChanged = onLanguageChanged,
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onDashboard = { navController.navigate(Routes.DASHBOARD) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    isDeletingAccount = authState.isDeletingAccount,
                    accountDeletionError = authState.accountDeletionError,
                    onDeleteAccount = authViewModel::deleteAccount,
                )
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    user = user,
                    analytics = analyticsState.analytics,
                    isLoading = analyticsState.isLoading,
                    errorMessage = analyticsState.errorMessage,
                    onBack = { navController.popBackStack() },
                    onRefresh = {
                        authViewModel.tokenOrNull()?.let(postViewModel::loadMyAnalytics)
                    },
                )
            }

            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    user = user,
                    isSaving = authState.isLoading,
                    errorMessage = authState.errorMessage,
                    onBack = { navController.popBackStack() },
                    onSave = { name, username, email, phone, bio, location ->
                        authViewModel.updateProfile(name, username, email, phone, bio, location) {
                            navController.popBackStack()
                        }
                    },
                    onChooseAvatar = { uri -> authViewModel.updateAvatar(uri) },
                )
            }
            }
            if (showNetworkLoader) {
                NetworkLoadingOverlay()
            }

        }
    }
}
