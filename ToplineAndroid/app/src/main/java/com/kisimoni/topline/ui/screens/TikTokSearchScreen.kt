package com.kisimoni.topline.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TikTokSearchDialog(
    query: String,
    submitted: Boolean,
    recentQueries: List<String>,
    suggestions: List<String>,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    results: @Composable (Modifier) -> Unit,
) {
    val focusRequester = FocusRequester()
    val keyboardController = LocalSoftwareKeyboardController.current
    val colorScheme = MaterialTheme.colorScheme

    BackHandler(onBack = onBack)
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        Column(
            Modifier.fillMaxSize().background(colorScheme.background),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceVariant,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(placeholder, color = colorScheme.onSurfaceVariant)
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colorScheme.onSurface),
                                cursorBrush = SolidColor(colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    keyboardController?.hide()
                                    onSubmit(query)
                                }),
                            )
                        }
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        onSubmit(query)
                    },
                    enabled = query.isNotBlank(),
                ) {
                    Text("Search", fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider()

            if (submitted && query.isNotBlank()) {
                results(Modifier.weight(1f).fillMaxWidth())
            } else if (query.isBlank()) {
                if (recentQueries.isNotEmpty()) {
                    Text(
                        "Recent searches",
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    recentQueries.forEach { recent ->
                        SearchSuggestionRow(
                            text = recent,
                            recent = true,
                            onClick = {
                                onQueryChange(recent)
                                onSubmit(recent)
                                keyboardController?.hide()
                            },
                        )
                    }
                }
                if (suggestions.isNotEmpty()) {
                    Text(
                        "Suggested searches",
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    suggestions.take(8).forEach { suggestion ->
                        SearchSuggestionRow(
                            text = suggestion,
                            onClick = {
                                onQueryChange(suggestion)
                                onSubmit(suggestion)
                                keyboardController?.hide()
                            },
                        )
                    }
                }
            } else {
                val matchingSuggestions = suggestions
                    .filter { it.contains(query.trim(), ignoreCase = true) }
                    .distinct()
                    .take(6)
                SearchSuggestionRow(
                    text = "Search for \"$query\"",
                    onClick = {
                        onSubmit(query)
                        keyboardController?.hide()
                    },
                )
                matchingSuggestions.forEach { suggestion ->
                    SearchSuggestionRow(
                        text = suggestion,
                        onClick = {
                            onQueryChange(suggestion)
                            onSubmit(suggestion)
                            keyboardController?.hide()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionRow(
    text: String,
    recent: Boolean = false,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            if (recent) Icons.Default.History else Icons.Default.Search,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(text, modifier = Modifier.weight(1f), color = colorScheme.onSurface)
    }
}
