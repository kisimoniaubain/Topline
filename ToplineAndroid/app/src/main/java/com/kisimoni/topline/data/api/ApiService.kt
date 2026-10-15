package com.kisimoni.topline.data.api

import com.kisimoni.topline.config.ApiConfig
import com.kisimoni.topline.data.local.UserPreferences
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.PATCH
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("user/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): UserResponse

    @PUT("user/me")
    suspend fun updateCurrentUser(@Header("Authorization") token: String, @Body request: UpdateProfileRequest): UserResponse

    @Multipart
    @PUT("user/me/avatar")
    suspend fun updateCurrentUserAvatar(
        @Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part,
    ): UserResponse

    @GET("posts")
    suspend fun getPosts(@Header("Authorization") token: String? = null): PostListResponse

    @GET("posts/following")
    suspend fun getFollowingPosts(@Header("Authorization") token: String): PostListResponse

    @GET("posts/mine")
    suspend fun getMyPosts(@Header("Authorization") token: String): PostListResponse

    @GET("posts/author/{authorId}")
    suspend fun getAuthorPosts(@Path("authorId") authorId: String): AuthorProfileResponse

    @GET("posts/mine/analytics")
    suspend fun getMyAnalytics(@Header("Authorization") token: String): PostAnalyticsResponse

    @GET("friends/me/counts")
    suspend fun getMyProfileCounts(@Header("Authorization") token: String): ProfileCountsResponse

    @GET("friends/me/following")
    suspend fun getMyFollowing(): FriendsListsResponse

    @GET("friends/me/followers")
    suspend fun getMyFollowers(): FriendsListsResponse

    @POST("messages/presence")
    suspend fun updatePresence(): SimpleResponse

    @GET("messages/unread-count")
    suspend fun getUnreadMessageCount(): UnreadCountResponse

    @GET("messages/inbox")
    suspend fun getInbox(): InboxResponse

    @GET("messages/{userId}")
    suspend fun getConversation(@Path("userId") userId: String): MessageThreadResponse

    @PATCH("messages/{userId}/read")
    suspend fun markConversationRead(@Path("userId") userId: String): SimpleResponse

    @POST("messages/{userId}")
    suspend fun sendMessage(
        @Path("userId") userId: String,
        @Body request: SendMessageRequest,
    ): MessageThreadResponse

    @Multipart
    @POST("messages/{userId}")
    suspend fun sendMessageWithImage(
        @Path("userId") userId: String,
        @Part("text") text: okhttp3.RequestBody,
        @Part file: MultipartBody.Part,
    ): MessageThreadResponse

    @PATCH("messages/{messageId}")
    suspend fun updateMessage(
        @Path("messageId") messageId: String,
        @Body request: UpdateMessageRequest,
    ): UpdateMessageResponse

    @DELETE("messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): DeleteMessageResponse

    @GET("friends/people")
    suspend fun getPeople(): PeopleResponse

    @GET("notifications")
    suspend fun getNotifications(): NotificationsResponse

    @GET("notifications/unread-count")
    suspend fun getUnreadNotificationCount(): UnreadCountResponse

    @PATCH("notifications/{notificationId}/read")
    suspend fun markNotificationRead(@Path("notificationId") notificationId: String): SimpleResponse

    @POST("notifications/devices")
    suspend fun registerPushDevice(@Body request: PushDeviceRequest): SimpleResponse

    @DELETE("notifications/devices")
    suspend fun unregisterPushDevice(@Body request: PushDeviceRequest): SimpleResponse

    @DELETE("user/me")
    suspend fun deleteCurrentUser(@Header("Authorization") token: String): SimpleResponse

    @POST("posts")
    suspend fun createPost(
        @Header("Authorization") token: String,
        @Body request: CreatePostRequest,
    ): CreatePostResponse

    @Multipart
    @POST("posts")
    suspend fun createPostWithMedia(
        @Header("Authorization") token: String,
        @Part("text") text: okhttp3.RequestBody,
        @Part("visibility") visibility: okhttp3.RequestBody,
        @Part("allowComments") allowComments: okhttp3.RequestBody,
        @Part file: MultipartBody.Part,
        @Part cover: MultipartBody.Part?,
    ): CreatePostResponse

    @GET("comments/{postId}")
    suspend fun getComments(@Path("postId") postId: String): CommentListResponse

    @POST("comments/{postId}")
    suspend fun createComment(
        @Header("Authorization") token: String,
        @Path("postId") postId: String,
        @Body request: CreateCommentRequest,
    ): CreateCommentResponse

    @Multipart
    @POST("comments/{postId}")
    suspend fun createCommentWithMedia(
        @Header("Authorization") token: String,
        @Path("postId") postId: String,
        @Part("text") text: okhttp3.RequestBody,
        @Part("parentCommentId") parentCommentId: okhttp3.RequestBody,
        @Part image: MultipartBody.Part?,
    ): CreateCommentResponse

    @POST("comments/{commentId}/like")
    suspend fun toggleCommentLike(
        @Header("Authorization") token: String,
        @Path("commentId") commentId: String,
    ): CommentLikeStateResponse

    @DELETE("comments/{commentId}")
    suspend fun deleteComment(
        @Header("Authorization") token: String,
        @Path("commentId") commentId: String,
    ): DeleteCommentResponse

    @GET("likes/{postId}")
    suspend fun getLikeState(
        @Header("Authorization") token: String? = null,
        @Path("postId") postId: String,
    ): LikeStateResponse

    @GET("likes/mine")
    suspend fun getMyLikedPosts(): LikedPostsResponse

    @GET("saved/mine")
    suspend fun getMySavedPostIds(): SavedPostIdsResponse

    @GET("saved")
    suspend fun getSavedPosts(): SavedPostsResponse

    @GET("saved/collections")
    suspend fun getSavedCollections(@Query("postId") postId: String? = null): SavedCollectionsResponse

    @GET("saved/collections/{collectionId}")
    suspend fun getSavedCollectionPosts(
        @Path("collectionId") collectionId: String,
    ): SavedCollectionPostsResponse

    @POST("saved/collections")
    suspend fun createSavedCollection(
        @Body request: CreateSavedCollectionRequest,
    ): CreateSavedCollectionResponse

    @PUT("saved/collections/{collectionId}/posts/{postId}")
    suspend fun updateSavedCollectionPost(
        @Path("collectionId") collectionId: String,
        @Path("postId") postId: String,
        @Body request: UpdateSavedCollectionPostRequest,
    ): UpdateSavedCollectionPostResponse

    @DELETE("saved/collections/{collectionId}")
    suspend fun deleteSavedCollection(
        @Path("collectionId") collectionId: String,
    ): DeleteSavedCollectionResponse

    @POST("saved/{postId}")
    suspend fun toggleSavedPost(@Path("postId") postId: String): SavedPostStateResponse

    @POST("posts/{postId}/share")
    suspend fun recordPostShare(@Path("postId") postId: String): PostShareResponse

    @POST("likes/{postId}")
    suspend fun toggleLike(
        @Header("Authorization") token: String,
        @Path("postId") postId: String,
    ): LikeStateResponse

    @GET("friends/{userId}")
    suspend fun getFollowState(
        @Header("Authorization") token: String? = null,
        @Path("userId") userId: String,
    ): FollowStateResponse

    @POST("friends/{userId}")
    suspend fun toggleFollow(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
    ): FollowStateResponse

    companion object {
        fun create(userPreferences: UserPreferences? = null): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(NetworkLoadingInterceptor())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .callTimeout(10, TimeUnit.MINUTES)
                .apply {
                    if (userPreferences != null) {
                        addInterceptor(AuthInterceptor(userPreferences))
                    }
                }
                .build()

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val retrofit = Retrofit.Builder()
                .baseUrl(ApiConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }

    private class AuthInterceptor(
        private val userPreferences: UserPreferences,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val token = runBlocking(Dispatchers.IO) {
                userPreferences.authTokenFlow.firstOrNull()
            }

            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }

            return chain.proceed(request)
        }
    }
}

@kotlinx.serialization.Serializable
data class UserResponse(
    val user: User? = null,
    val success: Boolean = false,
    val message: String = "",
)

@kotlinx.serialization.Serializable
data class SimpleResponse(
    val success: Boolean = false,
    val message: String = "",
)
