package com.kisimoni.topline.ui.screens

import com.kisimoni.topline.ui.icons.BootstrapIcons

import android.app.DatePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.kisimoni.topline.ui.BrandLogo
import com.kisimoni.topline.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
) {
    val uiState by authViewModel.uiState.collectAsState()
    val errorMessage = uiState.errorMessage
    var isLoginMode by remember { mutableStateOf(true) }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var isGenderMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                BrandLogo(
                    contentDescription = "Topline logo",
                    modifier = Modifier.size(width = 144.dp, height = 72.dp),
                )

                Text(
                    text = "Connect. Share. Stay informed.",
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )

                Text(
                    text = if (isLoginMode) "Welcome back" else "Create your account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!isLoginMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                        leadingIcon = { Icon(BootstrapIcons.Person(), contentDescription = null) },
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        leadingIcon = { Icon(BootstrapIcons.Email(), contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phone (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
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
                                    dateOfBirth = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH),
                            ).apply {
                                datePicker.maxDate = System.currentTimeMillis()
                            }.show()
                        },
                        label = { Text("Date of birth (YYYY-MM-DD)") },
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.YEAR, -18)
                                runCatching {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateOfBirth)
                                }.getOrNull()?.let(calendar::setTime)
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        calendar.set(year, month, day)
                                        dateOfBirth = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH),
                                ).apply {
                                    datePicker.maxDate = System.currentTimeMillis()
                                }.show()
                            }) {
                                Icon(BootstrapIcons.Calendar(), contentDescription = "Choose date of birth")
                            }
                        },
                    )

                    Box {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth().clickable { isGenderMenuExpanded = true },
                            label = { Text("Gender (optional)") },
                            placeholder = { Text("Select gender") },
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { isGenderMenuExpanded = true }) {
                                    Icon(BootstrapIcons.ArrowDropDown(), contentDescription = "Choose gender")
                                }
                            },
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

                if (isLoginMode) {
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email or username") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    leadingIcon = { Icon(BootstrapIcons.Lock(), contentDescription = null) },
                    visualTransformation = if (isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) {
                                    BootstrapIcons.EyeSlash()
                                } else {
                                    BootstrapIcons.Eye()
                                },
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Button(
                    onClick = {
                        if (isLoginMode) {
                            authViewModel.login(identifier, password) {
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
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                    enabled = !uiState.isLoading && if (isLoginMode) {
                        identifier.isNotBlank() && password.isNotBlank()
                    } else {
                        name.isNotBlank() && username.isNotBlank() && email.isNotBlank() &&
                            password.isNotBlank() && dateOfBirth.isNotBlank()
                    },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = if (uiState.isLoading) "Please wait..." else if (isLoginMode) "Sign In" else "Create Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isLoginMode) "Don’t have an account?" else "Already have an account?",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(
                        onClick = { isLoginMode = !isLoginMode },
                    ) {
                        Text(
                            text = if (isLoginMode) "Create Account" else "Sign in",
                            color = Color(0xFFF57F17),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
