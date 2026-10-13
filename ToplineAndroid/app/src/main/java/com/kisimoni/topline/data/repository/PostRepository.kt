package com.kisimoni.topline.data.repository

import android.content.Context
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.kisimoni.topline.data.api.ApiService
import com.kisimoni.topline.data.api.CommentListResponse
import com.kisimoni.topline.data.api.CommentLikeStateResponse
import com.kisimoni.topline.data.api.ActivityNotification
import com.kisimoni.topline.data.api.NotificationsResponse
import com.kisimoni.topline.data.api.CreateCommentResponse
import com.kisimoni.topline.data.api.CreatePostRequest
import com.kisimoni.topline.data.api.CreatePostResponse
import com.kisimoni.topline.data.api.AuthorProfileResponse
import com.kisimoni.topline.data.api.DeleteMessageResponse
import com.kisimoni.topline.data.api.FollowStateResponse
import com.kisimoni.topline.data.api.FriendsListsResponse
import com.kisimoni.topline.data.api.LikedPostsResponse
import com.kisimoni.topline.data.api.SavedPostIdsResponse
import com.kisimoni.topline.data.api.SavedPostsResponse
import com.kisimoni.topline.data.api.SavedPostStateResponse
import com.kisimoni.topline.data.api.SavedCollectionsResponse
import com.kisimoni.topline.data.api.SavedCollectionPostsResponse
import com.kisimoni.topline.data.api.CreateSavedCollectionRequest
import com.kisimoni.topline.data.api.CreateSavedCollectionResponse
import com.kisimoni.topline.data.api.UpdateSavedCollectionPostRequest
import com.kisimoni.topline.data.api.UpdateSavedCollectionPostResponse
import com.kisimoni.topline.data.api.DeleteSavedCollectionResponse
import com.kisimoni.topline.data.api.LikeStateResponse
import com.kisimoni.topline.data.api.InboxResponse
import com.kisimoni.topline.data.api.MessageThreadResponse
import com.kisimoni.topline.data.api.PeopleResponse
import com.kisimoni.topline.data.api.PostListResponse
import com.kisimoni.topline.data.api.PostAnalyticsResponse
import com.kisimoni.topline.data.api.SendMessageRequest
import com.kisimoni.topline.data.api.UpdateMessageRequest
import com.kisimoni.topline.data.api.UpdateMessageResponse
import com.kisimoni.topline.data.api.ProfileCountsResponse
import com.kisimoni.topline.data.local.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class PostRepository(
    private val userPreferences: UserPreferences? = null,
    private val apiService: ApiService = ApiService.create(userPreferences),
    private val context: Context? = null,
) {
    suspend fun getPosts(token: String?): PostListResponse = withContext(Dispatchers.IO) {
        apiService.getPosts(token?.let { "Bearer $it" })
    }

    suspend fun getFollowingPosts(token: String): PostListResponse = withContext(Dispatchers.IO) {
        apiService.getFollowingPosts("Bearer $token")
    }

    suspend fun getNotifications(): NotificationsResponse = withContext(Dispatchers.IO) {
        apiService.getNotifications()
    }

    suspend fun getUnreadNotificationCount(): Int = withContext(Dispatchers.IO) {
        apiService.getUnreadNotificationCount().unreadCount
    }

    suspend fun markNotificationRead(notificationId: String) = withContext(Dispatchers.IO) {
        apiService.markNotificationRead(notificationId)
    }

    suspend fun getAuthorPosts(authorId: String): AuthorProfileResponse = withContext(Dispatchers.IO) {
        apiService.getAuthorPosts(authorId)
    }

    suspend fun getPeople(): PeopleResponse = withContext(Dispatchers.IO) {
        apiService.getPeople()
    }

    suspend fun getMyFriends(): FriendsListsResponse = withContext(Dispatchers.IO) {
        val followingResponse = apiService.getMyFollowing()
        val followersResponse = apiService.getMyFollowers()
        FriendsListsResponse(
            followers = followersResponse.followers,
            following = followingResponse.following,
        )
    }

    suspend fun getMyLikedPosts(): LikedPostsResponse = withContext(Dispatchers.IO) {
        apiService.getMyLikedPosts()
    }

    suspend fun getMySavedPostIds(): SavedPostIdsResponse = withContext(Dispatchers.IO) {
        apiService.getMySavedPostIds()
    }

    suspend fun getSavedPosts(): SavedPostsResponse = withContext(Dispatchers.IO) {
        apiService.getSavedPosts()
    }

    suspend fun getSavedCollections(postId: String? = null): SavedCollectionsResponse = withContext(Dispatchers.IO) {
        apiService.getSavedCollections(postId)
    }

    suspend fun getSavedCollectionPosts(collectionId: String): SavedCollectionPostsResponse = withContext(Dispatchers.IO) {
        apiService.getSavedCollectionPosts(collectionId)
    }

    suspend fun createSavedCollection(name: String): CreateSavedCollectionResponse = withContext(Dispatchers.IO) {
        apiService.createSavedCollection(CreateSavedCollectionRequest(name))
    }

    suspend fun updateSavedCollectionPost(
        collectionId: String,
        postId: String,
        included: Boolean,
    ): UpdateSavedCollectionPostResponse = withContext(Dispatchers.IO) {
        apiService.updateSavedCollectionPost(
            collectionId,
            postId,
            UpdateSavedCollectionPostRequest(included),
        )
    }

    suspend fun deleteSavedCollection(collectionId: String): DeleteSavedCollectionResponse = withContext(Dispatchers.IO) {
        apiService.deleteSavedCollection(collectionId)
    }

    suspend fun toggleSavedPost(postId: String): SavedPostStateResponse = withContext(Dispatchers.IO) {
        apiService.toggleSavedPost(postId)
    }

    suspend fun getMyPosts(token: String): PostListResponse = withContext(Dispatchers.IO) {
        apiService.getMyPosts("Bearer $token")
    }

    suspend fun createPost(
        token: String,
        text: String,
        audience: String = "public",
        allowComments: Boolean = true,
    ): CreatePostResponse = withContext(Dispatchers.IO) {
        apiService.createPost(token, CreatePostRequest(text, audience, allowComments))
    }

    suspend fun getMyProfileCounts(token: String): ProfileCountsResponse = withContext(Dispatchers.IO) {
        apiService.getMyProfileCounts(token)
    }

    suspend fun getMyAnalytics(token: String): PostAnalyticsResponse = withContext(Dispatchers.IO) {
        apiService.getMyAnalytics(token)
    }

    suspend fun getInbox(): InboxResponse = withContext(Dispatchers.IO) {
        apiService.getInbox()
    }

    suspend fun getUnreadMessageCount(): Int = withContext(Dispatchers.IO) {
        apiService.getUnreadMessageCount().unreadCount
    }

    suspend fun updatePresence() = withContext(Dispatchers.IO) {
        apiService.updatePresence()
    }

    suspend fun getConversation(userId: String): MessageThreadResponse = withContext(Dispatchers.IO) {
        apiService.markConversationRead(userId)
        apiService.getConversation(userId)
    }

    suspend fun sendMessage(userId: String, text: String): MessageThreadResponse = withContext(Dispatchers.IO) {
        apiService.sendMessage(userId, SendMessageRequest(text))
    }

    suspend fun sendMessageWithImage(userId: String, text: String, uri: Uri): MessageThreadResponse = withContext(Dispatchers.IO) {
        val appContext = context ?: throw IllegalStateException("Context is required to send images.")
        val mimeType = appContext.contentResolver.getType(uri)
            ?: throw IllegalArgumentException("The selected image type could not be identified.")
        require(mimeType.startsWith("image/")) { "Choose an image to send." }
        val tempFile = copyUriToTempFile(appContext, uri, mimeType)
        try {
            val part = MultipartBody.Part.createFormData(
                name = "file",
                filename = tempFile.name,
                body = tempFile.asRequestBody(mimeType.toMediaType()),
            )
            apiService.sendMessageWithImage(
                userId,
                text.toRequestBody("text/plain".toMediaType()),
                part,
            )
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                Log.w("PostRepository", "Unable to remove temporary message image: ${tempFile.name}")
            }
        }
    }

    suspend fun updateMessage(messageId: String, text: String): UpdateMessageResponse = withContext(Dispatchers.IO) {
        apiService.updateMessage(messageId, UpdateMessageRequest(text))
    }

    suspend fun deleteMessage(messageId: String): DeleteMessageResponse = withContext(Dispatchers.IO) {
        apiService.deleteMessage(messageId)
    }

    suspend fun getComments(postId: String): CommentListResponse = withContext(Dispatchers.IO) {
        apiService.getComments(postId)
    }

    suspend fun createComment(
        token: String,
        postId: String,
        text: String,
        parentCommentId: String? = null,
        imageUri: Uri? = null,
    ): CreateCommentResponse = withContext(Dispatchers.IO) {
        val textPart = text.toRequestBody("text/plain".toMediaType())
        val parentPart = parentCommentId.orEmpty().toRequestBody("text/plain".toMediaType())
        if (imageUri == null) {
            apiService.createCommentWithMedia(token, postId, textPart, parentPart, null)
        } else {
            val appContext = context ?: throw IllegalStateException("Context is required to upload comment images.")
            val mimeType = appContext.contentResolver.getType(imageUri)
                ?: throw IllegalArgumentException("The selected image type could not be identified.")
            require(mimeType.startsWith("image/")) { "Choose an image to add to your comment." }
            val tempFile = copyUriToTempFile(appContext, imageUri, mimeType)
            try {
                require(tempFile.length() <= MAX_COMMENT_IMAGE_BYTES) {
                    "Comment images must be 10 MB or smaller."
                }
                val imagePart = MultipartBody.Part.createFormData(
                    name = "image",
                    filename = tempFile.name,
                    body = tempFile.asRequestBody(mimeType.toMediaType()),
                )
                apiService.createCommentWithMedia(token, postId, textPart, parentPart, imagePart)
            } finally {
                if (tempFile.exists() && !tempFile.delete()) {
                    Log.w("PostRepository", "Unable to remove temporary comment image: ${tempFile.name}")
                }
            }
        }
    }

    suspend fun toggleCommentLike(token: String, commentId: String): CommentLikeStateResponse = withContext(Dispatchers.IO) {
        apiService.toggleCommentLike(token, commentId)
    }

    suspend fun deleteComment(
        token: String,
        commentId: String,
    ): com.kisimoni.topline.data.api.DeleteCommentResponse = withContext(Dispatchers.IO) {
        apiService.deleteComment(token, commentId)
    }

    suspend fun getLikeState(postId: String, token: String? = null): LikeStateResponse = withContext(Dispatchers.IO) {
        apiService.getLikeState(token, postId)
    }

    suspend fun toggleLike(token: String, postId: String): LikeStateResponse = withContext(Dispatchers.IO) {
        apiService.toggleLike(token, postId)
    }

    suspend fun getFollowState(userId: String, token: String? = null): FollowStateResponse = withContext(Dispatchers.IO) {
        apiService.getFollowState(token?.let { "Bearer $it" }, userId)
    }

    suspend fun toggleFollow(token: String, userId: String): FollowStateResponse = withContext(Dispatchers.IO) {
        apiService.toggleFollow(token, userId)
    }

    suspend fun createPostWithMedia(
        token: String,
        text: String,
        uri: Uri?,
        coverUri: Uri? = null,
        audience: String = "public",
        allowComments: Boolean = true,
        edit: PostMediaEdit? = null,
    ): CreatePostResponse = withContext(Dispatchers.IO) {
        if (uri == null) {
            return@withContext createPost(token, text, audience, allowComments)
        }

        val appContext = context ?: throw IllegalStateException("Context is required to upload media files.")
        val editPlan = edit?.takeIf { it.videoClips.isNotEmpty() }
        val mimeType = if (editPlan != null) {
            "video/mp4"
        } else {
            appContext.contentResolver.getType(uri)
                ?: throw IllegalArgumentException("The selected file type could not be identified. Choose an image or video.")
        }
        require(mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
            "Choose a supported image or video file."
        }

        val tempFile = if (editPlan != null) {
            VideoEditor.render(appContext, editPlan)
        } else {
            copyUriToTempFile(appContext, uri, mimeType)
        }
        var coverFile: File? = null
        var coverMimeType = "image/jpeg"
        try {
            if (coverUri != null) {
                coverMimeType = appContext.contentResolver.getType(coverUri)
                    ?: throw IllegalArgumentException("The selected cover image type could not be identified.")
                require(coverMimeType.startsWith("image/")) {
                    "Choose an image for the video cover."
                }
                coverFile = copyUriToTempFile(appContext, coverUri, coverMimeType)
            }
            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = tempFile.name,
                body = tempFile.asRequestBody(mimeType.toMediaType()),
            )
            val textBody = text.toRequestBody("text/plain".toMediaType())
            val visibilityBody = audience.toRequestBody("text/plain".toMediaType())
            val allowCommentsBody = allowComments.toString().toRequestBody("text/plain".toMediaType())
            val coverPart = coverFile?.let {
                MultipartBody.Part.createFormData(
                    name = "cover",
                    filename = it.name,
                        body = it.asRequestBody(coverMimeType.toMediaType()),
                )
            }
            apiService.createPostWithMedia(
                token,
                textBody,
                visibilityBody,
                allowCommentsBody,
                filePart,
                coverPart,
            )
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                Log.w("PostRepository", "Unable to remove temporary media file: ${tempFile.name}")
            }
            if (coverFile != null && coverFile.exists() && !coverFile.delete()) {
                Log.w("PostRepository", "Unable to remove temporary cover file: ${coverFile.name}")
            }
        }
    }

    suspend fun downloadVideoWithWatermark(
        videoUrl: String,
        creatorUsername: String,
    ): Uri = withContext(Dispatchers.IO) {
        val appContext = context ?: throw IllegalStateException("Context is required to download videos.")
        val parsedUrl = videoUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("This video does not have a valid download URL.")
        require(parsedUrl.scheme == "https" || parsedUrl.scheme == "http") {
            "Only HTTP and HTTPS video URLs can be downloaded."
        }

        val sourceFile = File.createTempFile("topline_download_source_", ".video", appContext.cacheDir)
        var watermarkedFile: File? = null
        try {
            val request = okhttp3.Request.Builder().url(parsedUrl).get().build()
            val response = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .callTimeout(10, TimeUnit.MINUTES)
                .build()
                .newCall(request)
                .execute()
            response.use {
                if (!it.isSuccessful) {
                    throw IOException("Unable to download video (HTTP ${it.code}).")
                }
                val body = it.body ?: throw IOException("The video download returned no content.")
                if (body.contentLength() > MAX_UPLOAD_BYTES) {
                    throw IllegalArgumentException("Videos must be 500 MB or smaller.")
                }
                body.byteStream().use { input ->
                    sourceFile.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var totalBytes = 0L
                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead < 0) break
                            totalBytes += bytesRead
                            require(totalBytes <= MAX_UPLOAD_BYTES) {
                                "Videos must be 500 MB or smaller."
                            }
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
            }

            val renderedFile = VideoEditor.render(
                appContext,
                PostMediaEdit(videoClips = listOf(VideoClipEdit(Uri.fromFile(sourceFile)))),
                creatorUsername,
            )
            watermarkedFile = renderedFile
            publishVideoToGallery(appContext, renderedFile)
        } finally {
            if (sourceFile.exists() && !sourceFile.delete()) {
                Log.w("PostRepository", "Unable to remove downloaded source video: ${sourceFile.name}")
            }
            watermarkedFile?.let {
                if (it.exists() && !it.delete()) {
                    Log.w("PostRepository", "Unable to remove rendered video: ${it.name}")
                }
            }
        }
    }

    private fun publishVideoToGallery(context: Context, videoFile: File): Uri {
        val displayName = "Topline_${System.currentTimeMillis()}.mp4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Topline")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Unable to create the downloaded video in your gallery.")
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    videoFile.inputStream().use { input -> input.copyTo(output) }
                } ?: throw IOException("Unable to write the downloaded video to your gallery.")
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                return uri
            } catch (error: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw error
            }
        }

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Storage permission is required to save this video.")
        }
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Topline",
        )
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create the Topline folder in your Movies directory.")
        }
        val destination = File(directory, displayName)
        try {
            videoFile.copyTo(destination, overwrite = false)
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
        MediaScannerConnection.scanFile(context, arrayOf(destination.absolutePath), arrayOf("video/mp4"), null)
        return Uri.fromFile(destination)
    }

    private fun copyUriToTempFile(context: Context, uri: Uri, mimeType: String): File {
        val extension = when {
            mimeType == "image/jpeg" -> ".jpg"
            mimeType == "video/quicktime" -> ".mov"
            mimeType == "video/3gpp" -> ".3gp"
            mimeType.startsWith("image/") || mimeType.startsWith("video/") -> ".${mimeType.substringAfter("/")}"
            else -> throw IllegalArgumentException("Choose a supported image or video file.")
        }

        val tempFile = File.createTempFile("topline_upload_", extension, context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        totalBytes += bytesRead
                        require(totalBytes <= MAX_UPLOAD_BYTES) {
                            "Media files must be 500 MB or smaller."
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                }
            } ?: throw IllegalStateException("Unable to read selected media file.")
            return tempFile
        } catch (error: Exception) {
            if (tempFile.exists() && !tempFile.delete()) {
                Log.w("PostRepository", "Unable to remove incomplete media file: ${tempFile.name}")
            }
            throw error
        }
    }

    private companion object {
        const val MAX_UPLOAD_BYTES = 500L * 1024 * 1024
        const val MAX_COMMENT_IMAGE_BYTES = 10L * 1024 * 1024
    }
}
