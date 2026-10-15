package com.kisimoni.topline.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("_id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("phone") val phone: String? = null,
    @SerialName("dateOfBirth") val dateOfBirth: String? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("profilePicture") val profilePicture: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("location") val location: String? = null,
)

@Serializable
data class AuthResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("token") val token: String? = null,
    @SerialName("user") val user: User? = null,
)

@Serializable
data class AuthRequest(
    @SerialName("identifier") val identifier: String,
    @SerialName("password") val password: String,
)

@Serializable
data class RegisterRequest(
    @SerialName("name") val name: String,
    @SerialName("username") val username: String,
    @SerialName("email") val email: String,
    @SerialName("phone") val phone: String = "",
    @SerialName("password") val password: String,
    @SerialName("dateOfBirth") val dateOfBirth: String,
    @SerialName("gender") val gender: String = "",
)

@Serializable
data class UpdateProfileRequest(
    @SerialName("name") val name: String,
    @SerialName("username") val username: String,
    @SerialName("email") val email: String,
    @SerialName("phone") val phone: String,
    @SerialName("bio") val bio: String,
    @SerialName("location") val location: String,
)

@Serializable
data class PostAuthor(
    @SerialName("_id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("profilePicture") val profilePicture: String? = null,
)

@Serializable
data class PostItem(
    @SerialName("_id") val id: String = "",
    @SerialName("author") val author: PostAuthor = PostAuthor(),
    @SerialName("text") val text: String = "",
    @SerialName("image") val image: String? = null,
    @SerialName("video") val video: String? = null,
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("video_url") val legacyVideoUrl: String? = null,
    @SerialName("mediaUrl") val mediaUrl: String? = null,
    @SerialName("mediaType") val mediaType: String? = null,
    @SerialName("media_type") val legacyMediaType: String? = null,
    @SerialName("coverImage") val coverImage: String? = null,
    @SerialName("visibility") val visibility: String = "public",
    @SerialName("allowComments") val allowComments: Boolean = true,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("commentsCount") val commentsCount: Int = 0,
    @SerialName("likesCount") val likesCount: Int = 0,
    @SerialName("sharesCount") val sharesCount: Int = 0,
    @SerialName("savesCount") val savesCount: Int = 0,
    @SerialName("likedByCurrentUser") val likedByCurrentUser: Boolean = false,
) {
    fun playableVideoUrl(): String? {
        sequenceOf(video, videoUrl, legacyVideoUrl)
            .firstOrNull { !it.isNullOrBlank() && it.isValidMediaUrl() }
            ?.trim()
            ?.let { return it }

        val declaredMediaType = sequenceOf(mediaType, legacyMediaType)
            .firstOrNull { !it.isNullOrBlank() }
        val isDeclaredVideo = declaredMediaType?.let {
            it.equals("video", ignoreCase = true) ||
                it.startsWith("video/", ignoreCase = true)
        } == true
        return sequenceOf(mediaUrl, image)
            .firstOrNull { url ->
                !url.isNullOrBlank() && url.isValidMediaUrl() &&
                    (isDeclaredVideo || url.looksLikeVideoUrl())
            }
            ?.trim()
    }

    fun displayableMediaUrl(): String? {
        playableVideoUrl()?.let { return it }
        return sequenceOf(image, mediaUrl)
            .firstOrNull { url ->
                !url.isNullOrBlank() && url.isValidMediaUrl() && !url.looksLikeVideoUrl()
            }
            ?.trim()
    }

    private fun String.isValidMediaUrl(): Boolean =
        startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

    private fun String.looksLikeVideoUrl(): Boolean {
        val path = substringBefore('?').substringBefore('#').lowercase()
        return "/video/upload/" in path ||
            path.substringAfterLast('.', "") in setOf("3gp", "m3u8", "m4v", "mkv", "mov", "mp4", "webm")
    }
}

@Serializable
data class PostListResponse(
    @SerialName("posts") val posts: List<PostItem> = emptyList(),
    @SerialName("postCount") val postCount: Int = 0,
)

@Serializable
data class PeoplePerson(
    @SerialName("_id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("profilePicture") val profilePicture: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("isFollowing") val isFollowing: Boolean = false,
)

@Serializable
data class PeopleResponse(
    @SerialName("people") val people: List<PeoplePerson> = emptyList(),
)

@Serializable
data class FriendsListsResponse(
    @SerialName("followers") val followers: List<InboxContact> = emptyList(),
    @SerialName("following") val following: List<InboxContact> = emptyList(),
)

@Serializable
data class AuthorProfileResponse(
    @SerialName("user") val user: User? = null,
    @SerialName("posts") val posts: List<PostItem> = emptyList(),
    @SerialName("followersCount") val followersCount: Int = 0,
    @SerialName("followingCount") val followingCount: Int = 0,
)

@Serializable
data class LikedPostsResponse(
    @SerialName("likedPostIds") val likedPostIds: List<String> = emptyList(),
)

@Serializable
data class SavedPostIdsResponse(
    @SerialName("savedPostIds") val savedPostIds: List<String> = emptyList(),
)

@Serializable
data class SavedPostsResponse(
    @SerialName("posts") val posts: List<PostItem> = emptyList(),
)

@Serializable
data class SavedPostStateResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("isSaved") val isSaved: Boolean = false,
    @SerialName("savesCount") val savesCount: Int = 0,
    @SerialName("message") val message: String = "",
)

@Serializable
data class PostShareResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("sharesCount") val sharesCount: Int = 0,
)

@Serializable
data class SavedCollection(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("postCount") val postCount: Int = 0,
    @SerialName("isIncluded") val isIncluded: Boolean = false,
)

@Serializable
data class SavedCollectionsResponse(
    @SerialName("collections") val collections: List<SavedCollection> = emptyList(),
)

@Serializable
data class SavedCollectionPostsResponse(
    @SerialName("collection") val collection: SavedCollection = SavedCollection(),
    @SerialName("posts") val posts: List<PostItem> = emptyList(),
)

@Serializable
data class CreateSavedCollectionRequest(
    @SerialName("name") val name: String,
)

@Serializable
data class CreateSavedCollectionResponse(
    @SerialName("collection") val collection: SavedCollection = SavedCollection(),
)

@Serializable
data class UpdateSavedCollectionPostRequest(
    @SerialName("included") val included: Boolean,
)

@Serializable
data class UpdateSavedCollectionPostResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("included") val included: Boolean = false,
)

@Serializable
data class DeleteSavedCollectionResponse(
    @SerialName("success") val success: Boolean = false,
)

@Serializable
data class ActivityActor(
    @SerialName("_id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("profilePicture") val profilePicture: String? = null,
)

@Serializable
data class ActivityNotification(
    @SerialName("_id") val id: String = "",
    @SerialName("actor") val actor: ActivityActor? = null,
    @SerialName("type") val type: String = "",
    @SerialName("entityId") val entityId: String = "",
    @SerialName("text") val text: String = "",
    @SerialName("isRead") val isRead: Boolean = false,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class NotificationsResponse(
    @SerialName("notifications") val notifications: List<ActivityNotification> = emptyList(),
    @SerialName("unreadCount") val unreadCount: Int = 0,
)

@Serializable
data class UnreadCountResponse(
    @SerialName("unreadCount") val unreadCount: Int = 0,
)

@Serializable
data class PushDeviceRequest(
    @SerialName("token") val token: String,
)

@Serializable
data class CreatePostRequest(
    @SerialName("text") val text: String = "",
    @SerialName("visibility") val visibility: String = "public",
    @SerialName("allowComments") val allowComments: Boolean = true,
)

@Serializable
data class CreatePostResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("post") val post: PostItem? = null,
)

@Serializable
data class CommentAuthor(
    @SerialName("_id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("profilePicture") val profilePicture: String? = null,
)

@Serializable
data class CommentItem(
    @SerialName("_id") val id: String = "",
    @SerialName("post") val postId: String = "",
    @SerialName("author") val author: CommentAuthor = CommentAuthor(),
    @SerialName("text") val text: String = "",
    @SerialName("image") val image: String? = null,
    @SerialName("parentComment") val parentCommentId: String? = null,
    @SerialName("likesCount") val likesCount: Int = 0,
    @SerialName("likedByCurrentUser") val likedByCurrentUser: Boolean = false,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class CommentListResponse(
    @SerialName("comments") val comments: List<CommentItem> = emptyList(),
    @SerialName("commentsCount") val commentsCount: Int = 0,
)

@Serializable
data class CreateCommentRequest(
    @SerialName("text") val text: String,
)

@Serializable
data class CreateCommentResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("comment") val comment: CommentItem? = null,
    @SerialName("commentsCount") val commentsCount: Int = 0,
    @SerialName("message") val message: String = "",
)

@Serializable
data class CommentLikeStateResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("commentId") val commentId: String = "",
    @SerialName("likesCount") val likesCount: Int = 0,
    @SerialName("likedByCurrentUser") val likedByCurrentUser: Boolean = false,
    @SerialName("message") val message: String = "",
)

@Serializable
data class LikeStateResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("postId") val postId: String = "",
    @SerialName("likedByCurrentUser") val likedByCurrentUser: Boolean = false,
    @SerialName("likesCount") val likesCount: Int = 0,
    @SerialName("message") val message: String = "",
)

@Serializable
data class FollowStateResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("userId") val userId: String = "",
    @SerialName("isFollowing") val isFollowing: Boolean = false,
    @SerialName("followersCount") val followersCount: Int = 0,
    @SerialName("followingCount") val followingCount: Int = 0,
    @SerialName("message") val message: String = "",
)

@Serializable
data class ProfileCountsResponse(
    @SerialName("followersCount") val followersCount: Int = 0,
    @SerialName("followingCount") val followingCount: Int = 0,
    @SerialName("postsCount") val postsCount: Int = 0,
)

@Serializable
data class InboxContact(
    @SerialName("_id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("profilePicture") val profilePicture: String? = null,
    @SerialName("isOnline") val isOnline: Boolean = false,
)

@Serializable
data class ConversationItem(
    @SerialName("user") val user: InboxContact = InboxContact(),
    @SerialName("lastMessage") val lastMessage: String = "",
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("unreadCount") val unreadCount: Int = 0,
)

@Serializable
data class InboxResponse(
    @SerialName("following") val following: List<InboxContact> = emptyList(),
    @SerialName("conversations") val conversations: List<ConversationItem> = emptyList(),
    @SerialName("unreadMessageCount") val unreadMessageCount: Int = 0,
)

@Serializable
data class DirectMessage(
    @SerialName("_id") val id: String = "",
    @SerialName("sender") val sender: InboxContact = InboxContact(),
    @SerialName("receiver") val receiver: InboxContact = InboxContact(),
    @SerialName("text") val text: String = "",
    @SerialName("image") val image: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class MessageThreadResponse(
    @SerialName("messages") val messages: List<DirectMessage> = emptyList(),
)

@Serializable
data class SendMessageRequest(
    @SerialName("text") val text: String,
)

@Serializable
data class UpdateMessageRequest(
    @SerialName("text") val text: String,
)

@Serializable
data class UpdateMessageResponse(
    @SerialName("message") val message: DirectMessage? = null,
)

@Serializable
data class DeleteMessageResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("messageId") val messageId: String = "",
)

@Serializable
data class PostPerformance(
    @SerialName("_id") val id: String = "",
    @SerialName("text") val text: String = "",
    @SerialName("image") val image: String? = null,
    @SerialName("video") val video: String? = null,
    @SerialName("coverImage") val coverImage: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("likes") val likes: Int = 0,
    @SerialName("comments") val comments: Int = 0,
)

@Serializable
data class PostAnalyticsResponse(
    @SerialName("postsCount") val postsCount: Int = 0,
    @SerialName("likesCount") val likesCount: Int = 0,
    @SerialName("commentsCount") val commentsCount: Int = 0,
    @SerialName("followersCount") val followersCount: Int = 0,
    @SerialName("followingCount") val followingCount: Int = 0,
    @SerialName("recentPosts") val recentPosts: List<PostPerformance> = emptyList(),
    @SerialName("topPosts") val topPosts: List<PostPerformance> = emptyList(),
)

@Serializable
data class DeleteCommentResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("commentsCount") val commentsCount: Int = 0,
    @SerialName("message") val message: String = "",
)
