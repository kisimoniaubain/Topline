package com.kisimoni.topline.viewmodel

import android.util.Log
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kisimoni.topline.data.api.User
import com.kisimoni.topline.data.api.UpdateProfileRequest
import com.kisimoni.topline.data.local.UserPreferences
import com.kisimoni.topline.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val accountDeletionError: String? = null,
    val errorMessage: String? = null,
    val currentUser: User? = null,
)

class AuthViewModel(
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val token = userPreferences.authTokenFlow.firstOrNull()
            _uiState.value = _uiState.value.copy(isAuthenticated = !token.isNullOrBlank())
            if (!token.isNullOrBlank()) {
                try {
                    val user = authRepository.getCurrentUser(token).user
                    _uiState.value = _uiState.value.copy(currentUser = user)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.localizedMessage ?: "Unable to load your profile.",
                    )
                }
            }
        }
    }

    fun login(identifier: String, password: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = authRepository.login(identifier, password)
                if (response.success && !response.token.isNullOrBlank()) {
                    userPreferences.saveAuthToken(response.token)
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        isLoading = false,
                        currentUser = response.user,
                        errorMessage = null,
                    )
                    onSuccess?.invoke()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = response.message.ifBlank { "Login failed." },
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unable to log in.",
                )
            }
        }
    }

    fun register(
        name: String,
        username: String,
        email: String,
        password: String,
        dateOfBirth: String,
        phone: String = "",
        gender: String = "",
        onSuccess: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val request = com.kisimoni.topline.data.api.RegisterRequest(
                    name = name,
                    username = username,
                    email = email,
                    phone = phone,
                    password = password,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                )

                val response = authRepository.register(request)
                if (response.success && !response.token.isNullOrBlank()) {
                    userPreferences.saveAuthToken(response.token)
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        isLoading = false,
                        currentUser = response.user,
                        errorMessage = null,
                    )
                    onSuccess?.invoke()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = response.message.ifBlank { "Account creation failed." },
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unable to create account.",
                )
            }
        }
    }

    fun updateProfile(
        name: String,
        username: String,
        email: String,
        phone: String,
        bio: String,
        location: String,
        onSuccess: (() -> Unit)? = null,
    ) {
        val token = userPreferences.getToken()
        if (token.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sign in again to update your profile.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val user = authRepository.updateCurrentUser(
                    token,
                    UpdateProfileRequest(name, username, email, phone, bio, location),
                ).user ?: throw IllegalStateException("The server did not return your updated profile.")
                _uiState.value = _uiState.value.copy(currentUser = user, isLoading = false)
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unable to update your profile.",
                )
            }
        }
    }

    fun updateAvatar(uri: Uri, onSuccess: (() -> Unit)? = null) {
        val token = userPreferences.getToken()
        if (token.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sign in again to update your profile photo.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val user = authRepository.updateCurrentUserAvatar(token, uri).user
                    ?: throw IllegalStateException("The server did not return your updated profile.")
                _uiState.value = _uiState.value.copy(currentUser = user, isLoading = false)
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unable to update your profile photo.",
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            unregisterPushDevice()
            userPreferences.clearSession()
            _uiState.value = AuthUiState(isAuthenticated = false)
        }
    }

    fun deleteAccount() {
        val token = tokenOrNull()
        if (token.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(accountDeletionError = "Sign in again before deleting your account.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingAccount = true, accountDeletionError = null)
            try {
                val response = authRepository.deleteCurrentUser(token)
                if (!response.success) {
                    throw IllegalStateException(response.message.ifBlank { "Unable to delete your account." })
                }
                unregisterPushDevice()
                userPreferences.clearSession()
                _uiState.value = AuthUiState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeletingAccount = false,
                    accountDeletionError = e.localizedMessage ?: "Unable to delete your account.",
                )
            }
        }
    }

    private suspend fun unregisterPushDevice() {
        val pushToken = userPreferences.getPushToken() ?: return
        try {
            authRepository.unregisterPushDevice(pushToken)
        } catch (error: Exception) {
            Log.w("AuthViewModel", "Unable to unregister this device for push notifications.", error)
        } finally {
            userPreferences.clearPushToken()
        }
    }

    fun tokenOrNull(): String? = runBlocking {
        userPreferences.authTokenFlow.firstOrNull()
    }
}

class AuthViewModelFactory(
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(userPreferences, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
