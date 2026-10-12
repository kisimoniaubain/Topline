package com.kisimoni.topline

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.kisimoni.topline.data.api.PushDeviceRequest
import com.kisimoni.topline.data.local.UserPreferences
import com.kisimoni.topline.data.api.ApiService
import com.kisimoni.topline.data.repository.AuthRepository
import com.kisimoni.topline.data.repository.PostRepository
import com.kisimoni.topline.ui.theme.ToplineTheme
import com.kisimoni.topline.viewmodel.AuthViewModel
import com.kisimoni.topline.viewmodel.AuthViewModelFactory
import com.kisimoni.topline.viewmodel.PostViewModel
import com.kisimoni.topline.viewmodel.PostViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = UserPreferences(this)
        val useDarkSplash = preferences.isDarkModeEnabled()
        setTheme(if (useDarkSplash) R.style.Theme_Topline_Dark else R.style.Theme_Topline)
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        val presenceApi = ApiService.create(preferences)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferences.authTokenFlow.collectLatest { token ->
                    if (!token.isNullOrBlank()) {
                        while (isActive) {
                            try {
                                presenceApi.updatePresence()
                            } catch (error: IOException) {
                                Log.w("MainActivity", "Unable to update online presence.", error)
                            } catch (error: HttpException) {
                                Log.w("MainActivity", "Online presence request was rejected.", error)
                            }
                            delay(45_000)
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(preferences.authTokenFlow, preferences.pushTokenFlow) { token, pushToken ->
                    token to pushToken
                }.collectLatest { (token, pushToken) ->
                    if (!token.isNullOrBlank()) {
                        if (!pushToken.isNullOrBlank()) {
                            try {
                                val response = presenceApi.registerPushDevice(PushDeviceRequest(pushToken))
                                if (!response.success) {
                                    Log.w("MainActivity", "Server did not register this device for push notifications.")
                                } else if (!preferences.hasShownPushWelcomeNotification()) {
                                    val shown = ToplineNotifications.show(
                                        this@MainActivity,
                                        "Topline notifications are on",
                                        "We'll let you know when someone follows you, likes or comments on your posts, or sends you a message.",
                                    )
                                    if (shown) {
                                        preferences.markPushWelcomeNotificationShown()
                                    }
                                }
                            } catch (error: Exception) {
                                Log.w("MainActivity", "Unable to register this device for push notifications.", error)
                            }
                        } else {
                            try {
                                FirebaseMessaging.getInstance().token
                                    .addOnSuccessListener(preferences::savePushToken)
                                    .addOnFailureListener { error ->
                                        Log.w("MainActivity", "Unable to get this device's push token.", error)
                                    }
                            } catch (error: IllegalStateException) {
                                Log.w("MainActivity", "Firebase is not configured for this Android build.", error)
                            }
                        }
                    }
                }
            }
        }
        setContent {
            var darkMode by remember { mutableStateOf(preferences.isDarkModeEnabled()) }
            var language by remember { mutableStateOf(preferences.getLanguage()) }
            ToplineTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ToplineApp(
                        preferences = preferences,
                        darkMode = darkMode,
                        language = language,
                        onDarkModeChanged = {
                            darkMode = it
                            preferences.saveDarkModeEnabled(it)
                        },
                        onLanguageChanged = {
                            language = it
                            preferences.saveLanguage(it)
                        },
                    )
                }
            }
        }

    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001,
            )
        }
    }
}

@Composable
private fun ToplineApp(
    preferences: UserPreferences,
    darkMode: Boolean,
    language: String,
    onDarkModeChanged: (Boolean) -> Unit,
    onLanguageChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val authRepository = AuthRepository(preferences, context = context)
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(preferences, authRepository))
    val postRepository = PostRepository(preferences, context = context)
    val postViewModel: PostViewModel = viewModel(factory = PostViewModelFactory(postRepository))

    com.kisimoni.topline.navigation.ToplineNavGraph(
        authViewModel = authViewModel,
        postViewModel = postViewModel,
        darkMode = darkMode,
        language = language,
        onDarkModeChanged = onDarkModeChanged,
        onLanguageChanged = onLanguageChanged,
    )
}
