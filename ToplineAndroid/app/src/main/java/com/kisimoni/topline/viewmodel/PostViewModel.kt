package com.kisimoni.topline.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kisimoni.topline.data.api.CommentItem
import com.kisimoni.topline.data.api.ActivityNotification
import com.kisimoni.topline.data.api.DirectMessage
import com.kisimoni.topline.data.api.InboxContact
import com.kisimoni.topline.data.api.PeoplePerson
import com.kisimoni.topline.data.api.PostAnalyticsResponse
import com.kisimoni.topline.data.api.PostItem
import com.kisimoni.topline.data.api.SavedCollection
import com.kisimoni.topline.data.repository.PostRepository
import com.kisimoni.topline.data.repository.PostMediaEdit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

data class PostUiState(
    val isLoading: Boolean = false,
    val posts: List<PostItem> = emptyList(),
    val ownPosts: List<PostItem> = emptyList(),
    val ownPostsCount: Int = 0,
    val ownPostsError: String? = null,
    val ownFollowersCount: Int = 0,
    val ownFollowingCount: Int = 0,
    val ownProfileCountsError: String? = null,
    val errorMessage: String? = null,
    val followingPosts: List<PostItem> = emptyList(),
    val isFollowingLoading: Boolean = false,
    val followingErrorMessage: String? = null,
    val savedPostIds: Set<String> = emptySet(),
    val savedPosts: List<PostItem> = emptyList(),
    val isSavedPostsLoading: Boolean = false,
    val savedPostsError: String? = null,
    val likedPosts: List<PostItem> = emptyList(),
    val isLikedPostsLoading: Boolean = false,
    val likedPostsError: String? = null,
    val savedCollections: List<SavedCollection> = emptyList(),
    val savedCollectionPosts: List<PostItem> = emptyList(),
    val activeSavedCollection: SavedCollection? = null,
    val isSavedCollectionsLoading: Boolean = false,
    val savedCollectionsError: String? = null,
)

data class CommentsUiState(
    val isLoading: Boolean = false,
    val comments: List<CommentItem> = emptyList(),
    val commentsCount: Int = 0,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
)

data class InboxUiState(
    val isLoading: Boolean = false,
    val following: List<InboxContact> = emptyList(),
    val conversations: List<com.kisimoni.topline.data.api.ConversationItem> = emptyList(),
    val unreadMessageCount: Int = 0,
    val errorMessage: String? = null,
)

data class ConversationUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val messages: List<DirectMessage> = emptyList(),
    val errorMessage: String? = null,
)

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val analytics: PostAnalyticsResponse? = null,
    val errorMessage: String? = null,
)

data class PeopleUiState(
    val isLoading: Boolean = false,
    val people: List<PeoplePerson> = emptyList(),
    val errorMessage: String? = null,
    val isFriendsLoading: Boolean = false,
    val followers: List<InboxContact> = emptyList(),
    val following: List<InboxContact> = emptyList(),
    val friendsErrorMessage: String? = null,
)

data class OtherProfileUiState(
    val isLoading: Boolean = false,
    val user: com.kisimoni.topline.data.api.User? = null,
    val posts: List<PostItem> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val errorMessage: String? = null,
)

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<ActivityNotification> = emptyList(),
    val unreadCount: Int = 0,
    val errorMessage: String? = null,
)

private fun List<PostItem>.withSavesCount(postId: String, count: Int): List<PostItem> =
    map { post -> if (post.id == postId) post.copy(savesCount = count) else post }

private fun List<PostItem>.withSharesCount(postId: String, count: Int): List<PostItem> =
    map { post -> if (post.id == postId) post.copy(sharesCount = maxOf(post.sharesCount, count)) else post }

class PostViewModel(
    private val postRepository: PostRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostUiState())
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _commentsUiState = MutableStateFlow(CommentsUiState())
    val commentsUiState: StateFlow<CommentsUiState> = _commentsUiState.asStateFlow()

    private val _inboxUiState = MutableStateFlow(InboxUiState())
    val inboxUiState: StateFlow<InboxUiState> = _inboxUiState.asStateFlow()

    private val _conversationUiState = MutableStateFlow(ConversationUiState())
    val conversationUiState: StateFlow<ConversationUiState> = _conversationUiState.asStateFlow()

    private val _analyticsUiState = MutableStateFlow(AnalyticsUiState())
    val analyticsUiState: StateFlow<AnalyticsUiState> = _analyticsUiState.asStateFlow()
    private val _peopleUiState = MutableStateFlow(PeopleUiState())
    val peopleUiState: StateFlow<PeopleUiState> = _peopleUiState.asStateFlow()
    private val _otherProfileUiState = MutableStateFlow(OtherProfileUiState())
    val otherProfileUiState: StateFlow<OtherProfileUiState> = _otherProfileUiState.asStateFlow()
    private val _notificationsUiState = MutableStateFlow(NotificationsUiState())
    val notificationsUiState: StateFlow<NotificationsUiState> = _notificationsUiState.asStateFlow()

    fun loadFollowingPosts(token: String?) {
        if (token.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                followingPosts = emptyList(),
                isFollowingLoading = false,
                followingErrorMessage = "Sign in to view videos from accounts you follow.",
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFollowingLoading = true, followingErrorMessage = null)
            try {
                val response = postRepository.getFollowingPosts(token)
                _uiState.value = _uiState.value.copy(
                    followingPosts = response.posts,
                    isFollowingLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFollowingLoading = false,
                    followingErrorMessage = errorMessage(e, "Unable to load videos from accounts you follow."),
                )
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsUiState.value = _notificationsUiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = postRepository.getNotifications()
                _notificationsUiState.value = NotificationsUiState(
                    notifications = response.notifications,
                    unreadCount = response.unreadCount,
                )
            } catch (e: Exception) {
                _notificationsUiState.value = _notificationsUiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage(e, "Unable to load notifications."),
                )
            }
        }
    }

    fun loadUnreadCounts() {
        viewModelScope.launch {
            try {
                val unreadMessageCount = postRepository.getUnreadMessageCount()
                _inboxUiState.value = _inboxUiState.value.copy(unreadMessageCount = unreadMessageCount)
            } catch (error: Exception) {
                Log.w("PostViewModel", "Unable to refresh unread message count.", error)
            }
            try {
                val unreadCount = postRepository.getUnreadNotificationCount()
                _notificationsUiState.value = _notificationsUiState.value.copy(unreadCount = unreadCount)
            } catch (error: Exception) {
                Log.w("PostViewModel", "Unable to refresh unread notification count.", error)
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        if (notificationId.isBlank()) return
        viewModelScope.launch {
            try {
                postRepository.markNotificationRead(notificationId)
                val wasUnread = _notificationsUiState.value.notifications
                    .firstOrNull { it.id == notificationId }
                    ?.isRead == false
                val notifications = _notificationsUiState.value.notifications.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                _notificationsUiState.value = _notificationsUiState.value.copy(
                    notifications = notifications,
                    unreadCount = (_notificationsUiState.value.unreadCount - if (wasUnread) 1 else 0).coerceAtLeast(0),
                )
                loadUnreadCounts()
            } catch (e: Exception) {
                _notificationsUiState.value = _notificationsUiState.value.copy(
                    errorMessage = errorMessage(e, "Unable to update notification."),
                )
            }
        }
    }

    fun loadPeople() {
        viewModelScope.launch {
            _peopleUiState.value = _peopleUiState.value.copy(isLoading = true, errorMessage = null)
            try {
                _peopleUiState.value = PeopleUiState(people = postRepository.getPeople().people)
            } catch (e: Exception) {
                _peopleUiState.value = _peopleUiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage(e, "Unable to load people."),
                )
            }
        }
    }

    fun loadMyFriends() {
        viewModelScope.launch {
            _peopleUiState.value = _peopleUiState.value.copy(
                isFriendsLoading = true,
                friendsErrorMessage = null,
            )
            try {
                val response = postRepository.getMyFriends()
                _peopleUiState.value = _peopleUiState.value.copy(
                    isFriendsLoading = false,
                    followers = response.followers,
                    following = response.following,
                )
            } catch (e: Exception) {
                _peopleUiState.value = _peopleUiState.value.copy(
                    isFriendsLoading = false,
                    friendsErrorMessage = errorMessage(e, "Unable to load your friends."),
                )
            }
        }
    }

    fun updatePeopleFollowState(userId: String, isFollowing: Boolean) {
        _peopleUiState.value = _peopleUiState.value.copy(
            people = _peopleUiState.value.people.map { person ->
                if (person.id == userId) person.copy(isFollowing = isFollowing) else person
            },
        )
    }

    fun loadOtherProfile(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _otherProfileUiState.value = OtherProfileUiState(isLoading = true)
            try {
                val response = postRepository.getAuthorPosts(userId)
                val followState = postRepository.getFollowState(userId)
                _otherProfileUiState.value = OtherProfileUiState(
                    user = response.user,
                    posts = response.posts,
                    followersCount = response.followersCount,
                    followingCount = response.followingCount,
                    isFollowing = followState.isFollowing,
                )
            } catch (e: Exception) {
                _otherProfileUiState.value = OtherProfileUiState(
                    errorMessage = errorMessage(e, "Unable to load this profile."),
                )
            }
        }
    }

    fun loadInbox() {
        viewModelScope.launch {
            _inboxUiState.value = _inboxUiState.value.copy(isLoading = true, errorMessage = null)
            try {
                postRepository.updatePresence()
                val response = postRepository.getInbox()
                _inboxUiState.value = InboxUiState(
                    following = response.following,
                    conversations = response.conversations,
                    unreadMessageCount = response.unreadMessageCount,
                )
            } catch (e: Exception) {
                _inboxUiState.value = _inboxUiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage(e, "Unable to load your inbox."),
                )
            }
        }
    }

    fun loadConversation(userId: String) {
        viewModelScope.launch {
            _conversationUiState.value = ConversationUiState(isLoading = true)
            try {
                val response = postRepository.getConversation(userId)
                _conversationUiState.value = ConversationUiState(messages = response.messages)
                loadUnreadCounts()
            } catch (e: Exception) {
                _conversationUiState.value = ConversationUiState(
                    errorMessage = errorMessage(e, "Unable to load this conversation."),
                )
            }
        }
    }

    fun sendMessage(userId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _conversationUiState.value = _conversationUiState.value.copy(isSending = true, errorMessage = null)
            try {
                val response = postRepository.sendMessage(userId, text)
                _conversationUiState.value = _conversationUiState.value.copy(
                    messages = response.messages,
                    isSending = false,
                )
                loadInbox()
            } catch (e: Exception) {
                _conversationUiState.value = _conversationUiState.value.copy(
                    isSending = false,
                    errorMessage = errorMessage(e, "Unable to send your message."),
                )
            }
        }
    }

    fun sendMessage(userId: String, text: String, imageUri: Uri?, onSuccess: (() -> Unit)? = null) {
        if (text.isBlank() && imageUri == null) return
        viewModelScope.launch {
            _conversationUiState.value = _conversationUiState.value.copy(isSending = true, errorMessage = null)
            try {
                val response = if (imageUri == null) {
                    postRepository.sendMessage(userId, text)
                } else {
                    postRepository.sendMessageWithImage(userId, text, imageUri)
                }
                _conversationUiState.value = _conversationUiState.value.copy(
                    messages = response.messages,
                    isSending = false,
                )
                onSuccess?.invoke()
                loadInbox()
            } catch (e: Exception) {
                _conversationUiState.value = _conversationUiState.value.copy(
                    isSending = false,
                    errorMessage = errorMessage(e, "Unable to send your message."),
                )
            }
        }
    }

    fun sendSharedPost(userId: String, text: String, onResult: (Boolean, String?) -> Unit) {
        if (userId.isBlank() || text.isBlank()) {
            onResult(false, "Unable to send this post.")
            return
        }
        viewModelScope.launch {
            try {
                postRepository.sendMessage(userId, text)
                onResult(true, null)
                loadInbox()
            } catch (e: Exception) {
                onResult(false, errorMessage(e, "Unable to send this post."))
            }
        }
    }

    fun updateMessage(messageId: String, text: String) {
        viewModelScope.launch {
            try {
                val updated = postRepository.updateMessage(messageId, text).message
                    ?: throw IllegalStateException("The server did not return the updated message.")
                _conversationUiState.value = _conversationUiState.value.copy(
                    messages = _conversationUiState.value.messages.map {
                        if (it.id == messageId) updated else it
                    },
                    errorMessage = null,
                )
            } catch (e: Exception) {
                _conversationUiState.value = _conversationUiState.value.copy(
                    errorMessage = errorMessage(e, "Unable to edit this message."),
                )
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val response = postRepository.deleteMessage(messageId)
                if (response.success) {
                    _conversationUiState.value = _conversationUiState.value.copy(
                        messages = _conversationUiState.value.messages.filterNot { it.id == messageId },
                        errorMessage = null,
                    )
                    loadInbox()
                }
            } catch (e: Exception) {
                _conversationUiState.value = _conversationUiState.value.copy(
                    errorMessage = errorMessage(e, "Unable to delete this message."),
                )
            }
        }
    }

    fun loadMyAnalytics(token: String) {
        viewModelScope.launch {
            _analyticsUiState.value = AnalyticsUiState(isLoading = true)
            try {
                _analyticsUiState.value = AnalyticsUiState(
                    analytics = postRepository.getMyAnalytics(token),
                )
            } catch (e: Exception) {
                _analyticsUiState.value = AnalyticsUiState(
                    errorMessage = errorMessage(e, "Unable to load your analytics."),
                )
            }
        }
    }

    fun loadPosts(token: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = postRepository.getPosts(token)
                _uiState.value = _uiState.value.copy(
                    posts = response.posts,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage(e, "Unable to load feed."),
                )
            }
        }
    }

    fun loadSavedPostIds() {
        viewModelScope.launch {
            try {
                val response = postRepository.getMySavedPostIds()
                _uiState.value = _uiState.value.copy(savedPostIds = response.savedPostIds.toSet())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorMessage(e, "Unable to load saved videos."),
                )
            }
        }
    }

    fun loadSavedPosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavedPostsLoading = true, savedPostsError = null)
            try {
                val posts = postRepository.getSavedPosts().posts
                _uiState.value = _uiState.value.copy(
                    savedPosts = posts,
                    savedPostIds = posts.map { it.id }.toSet(),
                    isSavedPostsLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSavedPostsLoading = false,
                    savedPostsError = errorMessage(e, "Unable to load saved videos."),
                )
            }
        }
    }

    fun loadLikedPosts(token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLikedPostsLoading = true, likedPostsError = null)
            try {
                val likedIds = postRepository.getMyLikedPosts().likedPostIds.toSet()
                val candidates = buildList {
                    addAll(postRepository.getPosts(token).posts)
                    addAll(postRepository.getMyPosts(token).posts)
                    addAll(postRepository.getFollowingPosts(token).posts)
                }.distinctBy { it.id }
                _uiState.value = _uiState.value.copy(
                    likedPosts = candidates.filter { it.id in likedIds },
                    isLikedPostsLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLikedPostsLoading = false,
                    likedPostsError = errorMessage(e, "Unable to load liked videos."),
                )
            }
        }
    }

    fun loadSavedCollections(postId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavedCollectionsLoading = true,
                savedCollectionsError = null,
            )
            try {
                val collections = postRepository.getSavedCollections(postId).collections
                _uiState.value = _uiState.value.copy(
                    savedCollections = collections,
                    isSavedCollectionsLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSavedCollectionsLoading = false,
                    savedCollectionsError = errorMessage(e, "Unable to load collections."),
                )
            }
        }
    }

    fun loadSavedCollectionPosts(collectionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavedPostsLoading = true,
                savedPostsError = null,
            )
            try {
                val response = postRepository.getSavedCollectionPosts(collectionId)
                _uiState.value = _uiState.value.copy(
                    savedCollectionPosts = response.posts,
                    activeSavedCollection = response.collection,
                    isSavedPostsLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSavedPostsLoading = false,
                    savedPostsError = errorMessage(e, "Unable to load this collection."),
                )
            }
        }
    }

    fun createSavedCollection(name: String, postId: String? = null, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val collection = postRepository.createSavedCollection(name).collection
                if (!postId.isNullOrBlank()) {
                    postRepository.updateSavedCollectionPost(collection.id, postId, true)
                }
                loadSavedCollections(postId)
                onComplete?.invoke()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    savedCollectionsError = errorMessage(e, "Unable to create collection."),
                )
            }
        }
    }

    fun setSavedCollectionMembership(
        collectionId: String,
        postId: String,
        included: Boolean,
    ) {
        viewModelScope.launch {
            try {
                postRepository.updateSavedCollectionPost(collectionId, postId, included)
                loadSavedCollections(postId)
                if (included) {
                    _uiState.value = _uiState.value.copy(savedPostIds = _uiState.value.savedPostIds + postId)
                }
                loadSavedPostIds()
                loadSavedPosts()
                if (_uiState.value.activeSavedCollection?.id == collectionId) {
                    loadSavedCollectionPosts(collectionId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    savedCollectionsError = errorMessage(e, "Unable to update collection."),
                )
            }
        }
    }

    fun deleteSavedCollection(collectionId: String) {
        viewModelScope.launch {
            try {
                postRepository.deleteSavedCollection(collectionId)
                _uiState.value = _uiState.value.copy(
                    savedCollections = _uiState.value.savedCollections.filterNot { it.id == collectionId },
                    activeSavedCollection = if (_uiState.value.activeSavedCollection?.id == collectionId) null
                    else _uiState.value.activeSavedCollection,
                    savedCollectionPosts = if (_uiState.value.activeSavedCollection?.id == collectionId) emptyList()
                    else _uiState.value.savedCollectionPosts,
                )
                loadSavedCollections()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    savedCollectionsError = errorMessage(e, "Unable to delete collection."),
                )
            }
        }
    }

    fun toggleSavedPost(postId: String, onSuccess: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = postRepository.toggleSavedPost(postId)
                _uiState.value = _uiState.value.copy(
                    savedPostIds = if (response.isSaved) {
                        _uiState.value.savedPostIds + postId
                    } else {
                        _uiState.value.savedPostIds - postId
                    },
                    savedPosts = if (response.isSaved) {
                        _uiState.value.savedPosts.withSavesCount(postId, response.savesCount)
                    } else {
                        _uiState.value.savedPosts
                            .filterNot { it.id == postId }
                            .withSavesCount(postId, response.savesCount)
                    },
                    posts = _uiState.value.posts.withSavesCount(postId, response.savesCount),
                    followingPosts = _uiState.value.followingPosts.withSavesCount(postId, response.savesCount),
                    ownPosts = _uiState.value.ownPosts.withSavesCount(postId, response.savesCount),
                    likedPosts = _uiState.value.likedPosts.withSavesCount(postId, response.savesCount),
                    savedCollectionPosts = _uiState.value.savedCollectionPosts
                        .withSavesCount(postId, response.savesCount),
                )
                onSuccess?.invoke(response.isSaved)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorMessage(e, "Unable to save this video."),
                )
                onSuccess?.invoke(_uiState.value.savedPostIds.contains(postId))
            }
        }
    }

    fun recordPostShare(postId: String, onFailure: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = postRepository.recordPostShare(postId)
                val updatedState = _uiState.value
                _uiState.value = updatedState.copy(
                    posts = updatedState.posts.withSharesCount(postId, response.sharesCount),
                    followingPosts = updatedState.followingPosts.withSharesCount(postId, response.sharesCount),
                    ownPosts = updatedState.ownPosts.withSharesCount(postId, response.sharesCount),
                    savedPosts = updatedState.savedPosts.withSharesCount(postId, response.sharesCount),
                    likedPosts = updatedState.likedPosts.withSharesCount(postId, response.sharesCount),
                    savedCollectionPosts = updatedState.savedCollectionPosts
                        .withSharesCount(postId, response.sharesCount),
                )
            } catch (e: Exception) {
                val message = errorMessage(e, "Unable to count this share.")
                _uiState.value = _uiState.value.copy(errorMessage = message)
                onFailure?.invoke(message)
            }
        }
    }

    fun loadMyPosts(token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ownPostsError = null)
            try {
                val response = postRepository.getMyPosts(token)
                _uiState.value = _uiState.value.copy(
                    ownPosts = response.posts,
                    ownPostsCount = response.postCount.takeIf { it > 0 } ?: response.posts.size,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ownPostsError = errorMessage(e, "Unable to load your posts."),
                )
            }
        }
    }

    fun loadMyProfileCounts(token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ownProfileCountsError = null)
            try {
                val response = postRepository.getMyProfileCounts(token)
                _uiState.value = _uiState.value.copy(
                    ownPostsCount = response.postsCount,
                    ownFollowersCount = response.followersCount,
                    ownFollowingCount = response.followingCount,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ownProfileCountsError = errorMessage(e, "Unable to load profile statistics."),
                )
            }
        }
    }

    fun createPost(
        token: String,
        text: String,
        audience: String = "public",
        allowComments: Boolean = true,
        onSuccess: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = postRepository.createPost(token, text, audience, allowComments)
                if (response.success) {
                    val createdPost = response.post
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    if (createdPost != null) {
                        _uiState.value = _uiState.value.copy(posts = listOf(createdPost) + _uiState.value.posts)
                    }
                    loadPosts(token)
                    loadMyPosts(token)
                    loadMyProfileCounts(token)
                    onSuccess?.invoke()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = response.message.ifBlank { "Unable to share post." },
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage(e, "Unable to share post."),
                )
            }
        }
    }

    fun createPostWithMedia(
        token: String,
        text: String,
        uri: Uri?,
        coverUri: Uri? = null,
        audience: String = "public",
        allowComments: Boolean = true,
        edit: PostMediaEdit? = null,
        onSuccess: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = postRepository.createPostWithMedia(
                    token,
                    text,
                    uri,
                    coverUri,
                    audience,
                    allowComments,
                    edit,
                )
                if (response.success) {
                    val createdPost = response.post
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    if (createdPost != null) {
                        _uiState.value = _uiState.value.copy(posts = listOf(createdPost) + _uiState.value.posts)
                    }
                    loadPosts(token)
                    loadMyPosts(token)
                    loadMyProfileCounts(token)
                    onSuccess?.invoke()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = response.message.ifBlank { "Unable to share post." },
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage(e, "Unable to share post."),
                )
            }
        }

    }

    fun downloadVideoWithWatermark(
        videoUrl: String,
        creatorUsername: String,
        onProgress: (String, Float?) -> Unit = { _, _ -> },
        onResult: (Result<Uri>) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val result = postRepository.downloadVideoWithWatermark(videoUrl, creatorUsername) { stage, progress ->
                    withContext(Dispatchers.Main.immediate) {
                        onProgress(stage, progress)
                    }
                }
                onResult(Result.success(result))
            } catch (error: Exception) {
                onResult(Result.failure(error))
            }
        }
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            _commentsUiState.value = _commentsUiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = postRepository.getComments(postId)
                _commentsUiState.value = _commentsUiState.value.copy(
                    comments = response.comments,
                    commentsCount = response.commentsCount,
                    isLoading = false,
                )
                _uiState.value = _uiState.value.copy(
                    posts = _uiState.value.posts.map { post ->
                        if (post.id == postId) post.copy(commentsCount = response.commentsCount) else post
                    },
                    followingPosts = _uiState.value.followingPosts.map { post ->
                        if (post.id == postId) post.copy(commentsCount = response.commentsCount) else post
                    },
                    ownPosts = _uiState.value.ownPosts.map { post ->
                        if (post.id == postId) post.copy(commentsCount = response.commentsCount) else post
                    },
                )
            } catch (e: Exception) {
                _commentsUiState.value = _commentsUiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unable to load comments.",
                )
            }
        }
    }

    fun addComment(
        postId: String,
        token: String,
        text: String,
        imageUri: Uri? = null,
        parentCommentId: String? = null,
        onSuccess: ((Int) -> Unit)? = null,
    ) {
        if (text.isBlank() && imageUri == null) return

        viewModelScope.launch {
            _commentsUiState.value = _commentsUiState.value.copy(isSubmitting = true, errorMessage = null)
            try {
                val response = postRepository.createComment(token, postId, text, parentCommentId, imageUri)
                if (response.success) {
                    val updatedComments = listOfNotNull(response.comment) + _commentsUiState.value.comments
                    _commentsUiState.value = _commentsUiState.value.copy(
                        comments = updatedComments,
                        commentsCount = response.commentsCount,
                        isSubmitting = false,
                    )

                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) post.copy(commentsCount = response.commentsCount) else post
                    }
                    _uiState.value = _uiState.value.copy(
                        posts = updatedPosts,
                        followingPosts = _uiState.value.followingPosts.map { post ->
                            if (post.id == postId) post.copy(commentsCount = response.commentsCount) else post
                        },
                        ownPosts = _uiState.value.ownPosts.map { post ->
                            if (post.id == postId) post.copy(commentsCount = response.commentsCount) else post
                        },
                    )
                    onSuccess?.invoke(response.commentsCount)
                } else {
                    _commentsUiState.value = _commentsUiState.value.copy(
                        isSubmitting = false,
                        errorMessage = response.message.ifBlank { "Unable to add comment." },
                    )
                }
            } catch (e: Exception) {
                _commentsUiState.value = _commentsUiState.value.copy(
                    isSubmitting = false,
                    errorMessage = e.localizedMessage ?: "Unable to add comment.",
                )
            }
        }
    }

    fun toggleCommentLike(commentId: String, token: String) {
        if (commentId.isBlank()) return
        viewModelScope.launch {
            try {
                val response = postRepository.toggleCommentLike(token, commentId)
                _commentsUiState.value = _commentsUiState.value.copy(
                    comments = _commentsUiState.value.comments.map { comment ->
                        if (comment.id == commentId) {
                            comment.copy(
                                likesCount = response.likesCount,
                                likedByCurrentUser = response.likedByCurrentUser,
                            )
                        } else {
                            comment
                        }
                    },
                    errorMessage = null,
                )
            } catch (e: Exception) {
                _commentsUiState.value = _commentsUiState.value.copy(
                    errorMessage = errorMessage(e, "Unable to update comment like."),
                )
            }
        }
    }

    fun deleteComment(postId: String, commentId: String, token: String) {
        viewModelScope.launch {
            try {
                val response = postRepository.deleteComment(token, commentId)
                if (!response.success) {
                    throw IllegalStateException(response.message.ifBlank { "Unable to delete comment." })
                }
                val count = response.commentsCount
                val deletedComment = _commentsUiState.value.comments.firstOrNull { it.id == commentId }
                _commentsUiState.value = _commentsUiState.value.copy(
                    comments = _commentsUiState.value.comments.filterNot {
                        it.id == commentId || (deletedComment?.parentCommentId.isNullOrBlank() && it.parentCommentId == commentId)
                    },
                    commentsCount = count,
                    errorMessage = null,
                )
                _uiState.value = _uiState.value.copy(
                    posts = _uiState.value.posts.map { post ->
                        if (post.id == postId) post.copy(commentsCount = count) else post
                    },
                    followingPosts = _uiState.value.followingPosts.map { post ->
                        if (post.id == postId) post.copy(commentsCount = count) else post
                    },
                    ownPosts = _uiState.value.ownPosts.map { post ->
                        if (post.id == postId) post.copy(commentsCount = count) else post
                    },
                )
            } catch (e: Exception) {
                _commentsUiState.value = _commentsUiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to delete comment.",
                )
            }
        }
    }

    fun toggleLike(
        token: String,
        postId: String,
        onSuccess: ((Boolean, Int) -> Unit)? = null,
        onFailure: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            try {
                val response = postRepository.toggleLike(token, postId)
                val updatedPosts = _uiState.value.posts.map { post ->
                    if (post.id == postId) {
                        val nextLiked = response.likedByCurrentUser
                        val nextCount = if (nextLiked) response.likesCount else response.likesCount.coerceAtLeast(0)
                        post.copy(likedByCurrentUser = nextLiked, likesCount = nextCount)
                    } else {
                        post
                    }
                }
                _uiState.value = _uiState.value.copy(posts = updatedPosts)
                _uiState.value = _uiState.value.copy(
                    followingPosts = _uiState.value.followingPosts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                likedByCurrentUser = response.likedByCurrentUser,
                                likesCount = response.likesCount,
                            )
                        } else post
                    },
                    ownPosts = _uiState.value.ownPosts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                likedByCurrentUser = response.likedByCurrentUser,
                                likesCount = response.likesCount,
                            )
                        } else post
                    },
                )
                onSuccess?.invoke(response.likedByCurrentUser, response.likesCount)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to update like.",
                )
                onFailure?.invoke()
            }
        }
    }

    fun toggleFollow(token: String, userId: String, onSuccess: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = postRepository.toggleFollow(token, userId)
                onSuccess?.invoke(response.isFollowing)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to update follow status.",
                )
            }
        }
    }

    private fun errorMessage(error: Exception, fallback: String): String {
        val httpError = error as? HttpException
            ?: return error.localizedMessage ?: fallback
        val body = httpError.response()?.errorBody()
            ?: return fallback

        return try {
            val serverMessage = JSONObject(body.string()).optString("message").trim()
            if (serverMessage.isNotBlank() && !serverMessage.matches(Regex("(?i)^HTTP\\s+\\d{3}$"))) {
                serverMessage
            } else {
                fallback
            }
        } catch (parseError: IOException) {
            Log.w("PostViewModel", "Unable to read API error response.", parseError)
            fallback
        } catch (parseError: JSONException) {
            Log.w("PostViewModel", "Unable to parse API error response.", parseError)
            fallback
        }
    }
}

class PostViewModelFactory(
    private val postRepository: PostRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostViewModel(postRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
