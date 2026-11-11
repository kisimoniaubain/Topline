package com.kisimoni.topline.ui.screens

import android.app.DatePickerDialog
import android.app.Activity
import android.graphics.Bitmap
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.Icons
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.kisimoni.topline.ui.BrandLogo
import com.kisimoni.topline.BuildConfig
import com.kisimoni.topline.ui.icons.BootstrapIcons
import com.kisimoni.topline.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.runtime.rememberCoroutineScope

private val LoginBackground = Color(0xFF121315)
private val LoginPanel = Color(0xFF14161A)
private val LoginField = Color(0xFF1B1C1E)
private val LoginOrange = Color(0xFFFF7A00)
private val LoginSecondary = Color(0xFF9EACB8)
private val LoginMuted = Color(0xFFA78B7C)
private const val LOGIN_PREFERENCES = "topline_login_preferences"
private const val REMEMBERED_IDENTIFIER = "remembered_identifier"

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    facebookCallbackManager: CallbackManager,
    onNavigateToHome: () -> Unit,
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context) {
        context.getSharedPreferences(LOGIN_PREFERENCES, Context.MODE_PRIVATE)
    }
    var isLoginMode by remember { mutableStateOf(true) }
    var identifier by remember {
        mutableStateOf(preferences.getString(REMEMBERED_IDENTIFIER, "").orEmpty())
    }
    var password by remember { mutableStateOf("") }
    var resetEmail by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var resetPassword by remember { mutableStateOf("") }
    var showPasswordReset by remember { mutableStateOf(false) }
    var resetCodeSent by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberLogin by remember {
        mutableStateOf(preferences.contains(REMEMBERED_IDENTIFIER))
    }
    var showHelp by remember { mutableStateOf(false) }
    var qrToken by remember { mutableStateOf<String?>(null) }
    var qrPayload by remember { mutableStateOf<String?>(null) }
    var showQrLogin by remember { mutableStateOf(false) }
    var qrMessage by remember { mutableStateOf("") }
    var pendingGoogleEmail by remember { mutableStateOf<String?>(null) }
    var pendingGoogleCredential by remember { mutableStateOf<String?>(null) }
    var googleVerificationCode by remember { mutableStateOf("") }
    var resendCountdown by remember { mutableStateOf(0) }
    var resendTimerKey by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var isGenderMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(qrToken, showQrLogin) {
        val token = qrToken ?: return@LaunchedEffect
        if (!showQrLogin) return@LaunchedEffect
        while (true) {
            delay(2_000)
            when (authViewModel.pollQrLoginOnce(token, rememberLogin)) {
                "approved" -> {
                    showQrLogin = false
                    onNavigateToHome()
                    break
                }
                "denied", "expired", "error" -> {
                    qrMessage = authViewModel.uiState.value.errorMessage
                        ?: "This QR sign-in was not approved."
                    break
                }
            }
        }
    }

    LaunchedEffect(pendingGoogleEmail, resendTimerKey) {
        if (pendingGoogleEmail == null) {
            resendCountdown = 0
            return@LaunchedEffect
        }
        resendCountdown = 60
        while (resendCountdown > 0) {
            delay(1_000)
            resendCountdown -= 1
        }
    }

    val qrBitmap = remember(qrPayload) {
        qrPayload?.let { payload ->
            val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 640, 640)
            Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888).also { bitmap ->
                for (x in 0 until 640) {
                    for (y in 0 until 640) {
                        bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
            }
        }
    }

    if (pendingGoogleEmail != null) {
        GoogleEmailConfirmationScreen(
            email = pendingGoogleEmail.orEmpty(),
            code = googleVerificationCode,
            onCodeChange = { googleVerificationCode = it },
            resendCountdown = resendCountdown,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            infoMessage = uiState.infoMessage,
            onBack = {
                pendingGoogleEmail = null
                pendingGoogleCredential = null
                googleVerificationCode = ""
            },
            onResend = {
                val googleCredential = pendingGoogleCredential
                if (!googleCredential.isNullOrBlank()) {
                    authViewModel.loginWithProvider(
                        provider = "google",
                        credential = googleCredential,
                        rememberMe = rememberLogin,
                        onSuccess = { onNavigateToHome() },
                        onVerificationRequired = {
                            resendTimerKey += 1
                        },
                    )
                }
            },
            onConfirm = {
                authViewModel.confirmGoogleSignup(
                    email = pendingGoogleEmail.orEmpty(),
                    code = googleVerificationCode,
                    rememberMe = rememberLogin,
                    onSuccess = {
                        pendingGoogleEmail = null
                        pendingGoogleCredential = null
                        googleVerificationCode = ""
                        onNavigateToHome()
                    },
                )
            },
            onHelp = { showHelp = true },
        )
    } else {
    Box(
        modifier = Modifier.fillMaxSize().background(LoginBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.size(40.dp))
                BrandLogo(
                    contentDescription = "Topline",
                    modifier = Modifier.size(width = 132.dp, height = 34.dp),
                )
                IconButton(
                    onClick = { showHelp = true },
                    modifier = Modifier.size(40.dp).background(LoginField, CircleShape),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = "Help and support",
                        tint = LoginSecondary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = if (isLoginMode) "Log in to Topline" else "Create your account",
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (isLoginMode) {
                    "Manage your account, check notifications, comment on videos, and more."
                } else {
                    "Join Topline and connect with others."
                },
                modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
                color = LoginSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = LoginPanel),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isLoginMode) {
                        AuthField(
                            label = "Email or Username",
                            value = identifier,
                            onValueChange = { identifier = it },
                            leadingIcon = BootstrapIcons.Person(),
                            keyboardType = KeyboardType.Email,
                        )
                    } else {
                        AuthField("Name", name, { name = it }, leadingIcon = BootstrapIcons.Person())
                        AuthField("Username", username, { username = it })
                        AuthField(
                            "Email",
                            email,
                            { email = it },
                            leadingIcon = BootstrapIcons.Email(),
                            keyboardType = KeyboardType.Email,
                        )
                        AuthField(
                            "Phone (optional)",
                            phone,
                            { phone = it },
                            keyboardType = KeyboardType.Phone,
                        )
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth().clickable {
                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.YEAR, -18)
                                runCatching {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateOfBirth)
                                }.getOrNull()?.let(calendar::setTime)
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        calendar.set(year, month, day)
                                        dateOfBirth = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                            .format(calendar.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH),
                                ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                            },
                            label = { Text("Date of birth") },
                            readOnly = true,
                            singleLine = true,
                            colors = authFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                        )
                        Box {
                            OutlinedTextField(
                                value = gender,
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth().clickable { isGenderMenuExpanded = true },
                                label = { Text("Gender (optional)") },
                                readOnly = true,
                                singleLine = true,
                                colors = authFieldColors(),
                                shape = RoundedCornerShape(14.dp),
                            )
                            DropdownMenu(
                                expanded = isGenderMenuExpanded,
                                onDismissRequest = { isGenderMenuExpanded = false },
                            ) {
                                listOf("Female", "Male", "Non-binary", "Other", "Prefer not to say").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            gender = option
                                            isGenderMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(BootstrapIcons.Lock(), contentDescription = null, tint = LoginSecondary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) BootstrapIcons.EyeSlash() else BootstrapIcons.Eye(),
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                    tint = LoginSecondary,
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = authFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                    )

                    if (isLoginMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = rememberLogin,
                                    onCheckedChange = { rememberLogin = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = LoginOrange,
                                        uncheckedColor = LoginSecondary,
                                    ),
                                )
                                Text("Remember me", color = LoginSecondary, fontSize = 12.sp)
                            }
                            Text(
                                "Forgot password?",
                                modifier = Modifier.clickable {
                                    resetEmail = identifier.takeIf { it.contains("@") }.orEmpty()
                                    resetCodeSent = false
                                    showPasswordReset = true
                                },
                                color = Color(0xFFFFB68B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                        )
                    }
                    if (!uiState.infoMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.infoMessage.orEmpty(),
                            color = Color(0xFF98D6A5),
                            fontSize = 13.sp,
                        )
                    }

                    Button(
                        onClick = {
                            if (isLoginMode) {
                                if (rememberLogin) {
                                    preferences.edit().putString(REMEMBERED_IDENTIFIER, identifier.trim()).apply()
                                } else if (!rememberLogin) {
                                    preferences.edit().remove(REMEMBERED_IDENTIFIER).apply()
                                }
                                authViewModel.login(identifier.trim(), password, rememberLogin) {
                                    onNavigateToHome()
                                }
                            } else {
                                authViewModel.register(
                                    name = name,
                                    username = username,
                                    email = email,
                                    password = password,
                                    dateOfBirth = dateOfBirth,
                                    phone = phone,
                                    gender = gender,
                                ) {
                                    onNavigateToHome()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !uiState.isLoading && if (isLoginMode) {
                            identifier.isNotBlank() && password.isNotBlank()
                        } else {
                            name.isNotBlank() && username.isNotBlank() && email.isNotBlank() &&
                                password.isNotBlank() && dateOfBirth.isNotBlank()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoginOrange,
                            contentColor = Color(0xFF321200),
                        ),
                        shape = RoundedCornerShape(25.dp),
                    ) {
                        Text(
                            text = if (uiState.isLoading) "Please wait…"
                            else if (isLoginMode) "Log In" else "Create Account",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    if (showHelp) {
                        Text(
                            "Choose Forgot password to recover access to your account.",
                            color = LoginSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Spacer(Modifier.weight(1f).height(1.dp).background(Color(0xFF343537)))
                Text("OR CONTINUE WITH", color = LoginMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f).height(1.dp).background(Color(0xFF343537)))
            }
            if (isLoginMode) {
                Button(
                    onClick = {
                        if (BuildConfig.TOPLINE_GOOGLE_WEB_CLIENT_ID.isBlank()) {
                            qrMessage = "Set TOPLINE_GOOGLE_WEB_CLIENT_ID in your local Gradle properties."
                            showHelp = true
                        } else {
                            scope.launch {
                                try {
                                    val activity = context as? Activity
                                        ?: throw IllegalStateException("Unable to open Google sign-in.")
                                    val googleOption = GetGoogleIdOption.Builder()
                                        .setServerClientId(BuildConfig.TOPLINE_GOOGLE_WEB_CLIENT_ID)
                                        .setFilterByAuthorizedAccounts(false)
                                        .setAutoSelectEnabled(false)
                                        .build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleOption)
                                        .build()
                                    val result = CredentialManager.create(context).getCredential(activity, request)
                                    val credential = result.credential as? CustomCredential
                                        ?: throw IllegalStateException("Google did not return a valid sign-in credential.")
                                    if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        throw IllegalStateException("Google did not return an ID token.")
                                    }
                                    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                    authViewModel.loginWithProvider(
                                        provider = "google",
                                        credential = idToken,
                                        rememberMe = rememberLogin,
                                        onSuccess = { onNavigateToHome() },
                                        onVerificationRequired = { email ->
                                            pendingGoogleEmail = email
                                            pendingGoogleCredential = idToken
                                            googleVerificationCode = ""
                                            resendTimerKey += 1
                                        },
                                    )
                                } catch (error: Exception) {
                                    qrMessage = error.localizedMessage ?: "Google sign-in failed."
                                    showHelp = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2022)),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text("Continue with Google", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (BuildConfig.TOPLINE_FACEBOOK_APP_ID.isNotBlank()) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        factory = { viewContext ->
                            LoginButton(viewContext).apply {
                                setPermissions("public_profile", "email")
                                registerCallback(facebookCallbackManager, object : FacebookCallback<LoginResult> {
                                    override fun onSuccess(result: LoginResult) {
                                        authViewModel.loginWithProvider(
                                            provider = "facebook",
                                            credential = result.accessToken.token,
                                            rememberMe = rememberLogin,
                                            onSuccess = { onNavigateToHome() },
                                        )
                                    }

                                    override fun onCancel() {
                                        qrMessage = "Facebook sign-in was cancelled."
                                    }

                                    override fun onError(error: FacebookException) {
                                        qrMessage = error.localizedMessage ?: "Facebook sign-in failed."
                                        showHelp = true
                                    }
                                })
                            }
                        },
                    )
                } else {
                    Button(
                        onClick = {
                            qrMessage = "Set TOPLINE_FACEBOOK_APP_ID in your local Gradle properties."
                            showHelp = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2022)),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Text("Continue with Facebook", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        authViewModel.startQrLogin { response ->
                            qrToken = response.token
                            qrPayload = response.qrPayload
                            qrMessage = ""
                            showQrLogin = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2022)),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text("Continue with QR code", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "By continuing, you agree to Topline’s Terms and Privacy Policy.",
                color = LoginSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isLoginMode) "Don't have an account?" else "Already have an account?",
                    color = LoginSecondary,
                    fontSize = 14.sp,
                )
                TextButton(onClick = {
                    isLoginMode = !isLoginMode
                    password = ""
                }) {
                    Text(
                        text = if (isLoginMode) "Sign up" else "Log in",
                        color = LoginOrange,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Help and support") },
            text = { Text(qrMessage.ifBlank { "Choose Forgot password to recover access to your account." }) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text("Done", color = LoginOrange)
                }
            },
        )
    }

    if (showPasswordReset) {
        AlertDialog(
            onDismissRequest = { showPasswordReset = false },
            title = { Text(if (resetCodeSent) "Reset your password" else "Password recovery") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Account email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = authFieldColors(),
                    )
                    if (resetCodeSent) {
                        OutlinedTextField(
                            value = resetCode,
                            onValueChange = { resetCode = it.take(6) },
                            label = { Text("6-digit reset code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = authFieldColors(),
                        )
                        OutlinedTextField(
                            value = resetPassword,
                            onValueChange = { resetPassword = it },
                            label = { Text("New password (8+ characters)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            colors = authFieldColors(),
                        )
                    }
                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Text(uiState.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isLoading,
                    onClick = {
                        if (!resetCodeSent) {
                            authViewModel.requestPasswordReset(resetEmail.trim()) {
                                resetCodeSent = true
                            }
                        } else {
                            authViewModel.confirmPasswordReset(
                                resetEmail.trim(),
                                resetCode.trim(),
                                resetPassword,
                            ) {
                                showPasswordReset = false
                                identifier = resetEmail.trim()
                                password = ""
                                resetCode = ""
                                resetPassword = ""
                            }
                        }
                    },
                ) {
                    Text(if (uiState.isLoading) "Please wait…" else if (resetCodeSent) "Reset password" else "Send code", color = LoginOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordReset = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showQrLogin) {
        AlertDialog(
            onDismissRequest = {
                showQrLogin = false
                qrToken = null
                qrPayload = null
            },
            title = { Text("Scan to log in") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Scan using Topline on a device where you are already logged in.")
                    Spacer(Modifier.height(12.dp))
                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR code for secure Topline sign-in",
                            modifier = Modifier.size(220.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp),
                        )
                    } ?: CircularProgressIndicator(color = LoginOrange)
                    if (qrMessage.isNotBlank()) {
                        Text(qrMessage, modifier = Modifier.padding(top = 8.dp), color = LoginMuted)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showQrLogin = false
                    qrToken = null
                    qrPayload = null
                }) {
                    Text("Cancel")
                }
            },
        )
    }
    }
}

@Composable
private fun GoogleEmailConfirmationScreen(
    email: String,
    code: String,
    onCodeChange: (String) -> Unit,
    resendCountdown: Int,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onConfirm: () -> Unit,
    onHelp: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val maskedEmail = remember(email) {
        val parts = email.split("@", limit = 2)
        if (parts.size == 2) {
            "${parts[0].take(1)}•••@${parts[1]}"
        } else {
            email
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("‹  Back", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "TOPLINE ID",
                modifier = Modifier
                    .background(LoginField, RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                color = LoginSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onHelp) {
                Text("Help", color = LoginSecondary)
            }
        }

        Spacer(Modifier.height(18.dp))
        BrandLogo(
            contentDescription = "Topline",
            modifier = Modifier.size(width = 92.dp, height = 58.dp),
        )
        Spacer(Modifier.height(22.dp))
        Text(
            "Enter 6-digit code",
            color = Color.White,
            fontSize = 27.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Text(
            "Your code was sent to",
            modifier = Modifier.padding(top = 8.dp),
            color = LoginSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            maskedEmail,
            modifier = Modifier.padding(top = 3.dp),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onBack) {
            Text("Use a different Google account", color = Color(0xFFFF7A00), fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clickable {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = code,
                onValueChange = { value -> onCodeChange(value.filter(Char::isDigit).take(6)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = TextStyle(color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        innerTextField()
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            repeat(6) { index ->
                                val digit = code.getOrNull(index)?.toString()
                                Box(
                                    modifier = Modifier
                                        .size(width = 43.dp, height = 56.dp)
                                        .background(
                                            if (index == code.length) Color(0xFF1F2022) else LoginField,
                                            RoundedCornerShape(12.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (digit != null) {
                                        Text(digit, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Box(
                                            Modifier
                                                .size(width = 8.dp, height = 3.dp)
                                                .background(LoginMuted.copy(alpha = 0.6f), RoundedCornerShape(2.dp)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                errorMessage,
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        } else if (!infoMessage.isNullOrBlank()) {
            Text(
                infoMessage,
                modifier = Modifier.padding(top = 6.dp),
                color = Color(0xFF98D6A5),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }

        Button(
            onClick = {
                keyboardController?.hide()
                onConfirm()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp),
            enabled = code.length == 6 && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = LoginOrange,
                contentColor = Color(0xFF321200),
            ),
            shape = RoundedCornerShape(25.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF321200))
            } else {
                Text("Verify email", fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            if (resendCountdown > 0) "Resend code in ${resendCountdown}s" else "Didn't receive the email?",
            color = LoginSecondary,
            fontSize = 12.sp,
        )
        TextButton(
            onClick = onResend,
            enabled = resendCountdown == 0 && !isLoading,
        ) {
            Text(
                "Resend email code",
                color = if (resendCountdown == 0) Color(0xFFFF7A00) else LoginMuted,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            "The code expires in 10 minutes.",
            modifier = Modifier.padding(top = 5.dp),
            color = LoginMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            "🔒  Secure Topline account verification",
            modifier = Modifier.padding(top = 18.dp),
            color = LoginSecondary.copy(alpha = 0.8f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, contentDescription = null, tint = LoginSecondary) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = authFieldColors(),
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = LoginField,
    unfocusedContainerColor = LoginField,
    focusedBorderColor = LoginOrange,
    unfocusedBorderColor = Color.Transparent,
    focusedLabelColor = Color(0xFFFFB68B),
    unfocusedLabelColor = LoginMuted,
    cursorColor = LoginOrange,
)
