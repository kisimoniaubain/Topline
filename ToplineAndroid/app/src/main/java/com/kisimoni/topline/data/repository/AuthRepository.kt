package com.kisimoni.topline.data.repository

import android.content.Context
import android.net.Uri
import com.kisimoni.topline.data.api.ApiService
import com.kisimoni.topline.data.api.AuthRequest
import com.kisimoni.topline.data.api.AuthResponse
import com.kisimoni.topline.data.api.SimpleResponse
import com.kisimoni.topline.data.api.RegisterRequest
import com.kisimoni.topline.data.api.PushDeviceRequest
import com.kisimoni.topline.data.api.UpdateProfileRequest
import com.kisimoni.topline.data.api.UserResponse
import com.kisimoni.topline.data.local.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AuthRepository(
    private val userPreferences: UserPreferences? = null,
    private val apiService: ApiService = ApiService.create(userPreferences),
    private val context: Context? = null,
) {
    suspend fun login(identifier: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        apiService.login(AuthRequest(identifier, password))
    }

    suspend fun register(request: RegisterRequest): AuthResponse = withContext(Dispatchers.IO) {
        apiService.register(request)
    }

    suspend fun getCurrentUser(token: String): UserResponse = withContext(Dispatchers.IO) {
        apiService.getCurrentUser("Bearer $token")
    }

    suspend fun updateCurrentUser(token: String, request: UpdateProfileRequest): UserResponse = withContext(Dispatchers.IO) {
        apiService.updateCurrentUser("Bearer $token", request)
    }

    suspend fun updateCurrentUserAvatar(token: String, uri: Uri): UserResponse = withContext(Dispatchers.IO) {
        val appContext = context ?: throw IllegalStateException("Context is required to upload a profile photo.")
        val mimeType = appContext.contentResolver.getType(uri) ?: "image/jpeg"
        require(mimeType.startsWith("image/")) { "Choose an image file for your profile photo." }
        val extension = mimeType.substringAfter("/", "jpeg").replace("jpeg", "jpg")
        val tempFile = File.createTempFile("topline_avatar_", ".$extension", appContext.cacheDir)
        try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("Unable to read the selected profile photo.")

            val part = MultipartBody.Part.createFormData(
                name = "avatar",
                filename = tempFile.name,
                body = tempFile.asRequestBody(mimeType.toMediaType()),
            )
            apiService.updateCurrentUserAvatar("Bearer $token", part)
        } finally {
            tempFile.delete()
        }

    }

    suspend fun unregisterPushDevice(pushToken: String): SimpleResponse = withContext(Dispatchers.IO) {
        apiService.unregisterPushDevice(PushDeviceRequest(pushToken))
    }

    suspend fun deleteCurrentUser(token: String): SimpleResponse = withContext(Dispatchers.IO) {
        apiService.deleteCurrentUser("Bearer $token")
    }
}
