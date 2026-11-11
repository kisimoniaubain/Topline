package com.kisimoni.topline.data.repository

import android.content.Context
import android.net.Uri
import com.kisimoni.topline.data.api.ApiService
import com.kisimoni.topline.data.api.bearerAuthorization
import com.kisimoni.topline.data.api.AuthRequest
import com.kisimoni.topline.data.api.AuthResponse
import com.kisimoni.topline.data.api.SimpleResponse
import com.kisimoni.topline.data.api.RegisterRequest
import com.kisimoni.topline.data.api.PushDeviceRequest
import com.kisimoni.topline.data.api.UpdateProfileRequest
import com.kisimoni.topline.data.api.UpdatePrivacyRequest
import com.kisimoni.topline.data.api.UserResponse
import com.kisimoni.topline.data.api.PasswordResetRequest
import com.kisimoni.topline.data.api.PasswordResetConfirmRequest
import com.kisimoni.topline.data.api.ProviderAuthRequest
import com.kisimoni.topline.data.api.GoogleSignupVerificationRequest
import com.kisimoni.topline.data.api.QrAuthRequest
import com.kisimoni.topline.data.api.QrAuthStartResponse
import com.kisimoni.topline.data.api.QrAuthPollResponse
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

    suspend fun requestPasswordReset(email: String): SimpleResponse = withContext(Dispatchers.IO) {
        apiService.requestPasswordReset(PasswordResetRequest(email))
    }

    suspend fun confirmPasswordReset(email: String, code: String, password: String): SimpleResponse =
        withContext(Dispatchers.IO) {
            apiService.confirmPasswordReset(PasswordResetConfirmRequest(email, code, password))
        }

    suspend fun loginWithProvider(provider: String, credential: String): AuthResponse = withContext(Dispatchers.IO) {
        when (provider) {
            "google" -> apiService.loginWithGoogle(ProviderAuthRequest(credential))
            "facebook" -> apiService.loginWithFacebook(ProviderAuthRequest(credential))
            else -> throw IllegalArgumentException("Unsupported sign-in provider.")
        }
    }

    suspend fun confirmGoogleSignup(email: String, code: String): AuthResponse = withContext(Dispatchers.IO) {
        apiService.confirmGoogleSignup(GoogleSignupVerificationRequest(email, code))
    }

    suspend fun startQrLogin(): QrAuthStartResponse = withContext(Dispatchers.IO) {
        apiService.startQrLogin()
    }

    suspend fun pollQrLogin(token: String): QrAuthPollResponse = withContext(Dispatchers.IO) {
        apiService.pollQrLogin(QrAuthRequest(token))
    }

    suspend fun approveQrLogin(token: String, qrToken: String): SimpleResponse = withContext(Dispatchers.IO) {
        apiService.approveQrLogin(bearerAuthorization(token), QrAuthRequest(qrToken))
    }

    suspend fun register(request: RegisterRequest): AuthResponse = withContext(Dispatchers.IO) {
        apiService.register(request)
    }

    suspend fun getCurrentUser(token: String): UserResponse = withContext(Dispatchers.IO) {
        apiService.getCurrentUser(bearerAuthorization(token))
    }

    suspend fun updateCurrentUser(token: String, request: UpdateProfileRequest): UserResponse = withContext(Dispatchers.IO) {
        apiService.updateCurrentUser(bearerAuthorization(token), request)
    }

    suspend fun updatePrivacy(token: String, privateAccount: Boolean): UserResponse = withContext(Dispatchers.IO) {
        apiService.updatePrivacy(bearerAuthorization(token), UpdatePrivacyRequest(privateAccount))
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
            apiService.updateCurrentUserAvatar(bearerAuthorization(token), part)
        } finally {
            tempFile.delete()
        }

    }

    suspend fun unregisterPushDevice(pushToken: String): SimpleResponse = withContext(Dispatchers.IO) {
        apiService.unregisterPushDevice(PushDeviceRequest(pushToken))
    }

    suspend fun deleteCurrentUser(token: String): SimpleResponse = withContext(Dispatchers.IO) {
        apiService.deleteCurrentUser(bearerAuthorization(token))
    }
}
