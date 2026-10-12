package com.kisimoni.topline.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kisimoni.topline.data.api.SavedCollection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedCollectionsSheet(
    postTitle: String,
    isSaved: Boolean,
    collections: List<SavedCollection>,
    isLoading: Boolean,
    errorMessage: String?,
    onLoad: () -> Unit,
    onToggleSave: () -> Unit,
    onCreate: (String) -> Unit,
    onToggle: (SavedCollection, Boolean) -> Unit,
    onDelete: (SavedCollection) -> Unit,
    onDismiss: () -> Unit,
) {
    var collectionName by remember { mutableStateOf("") }
    var collectionToDelete by remember { mutableStateOf<SavedCollection?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Save to collection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "“$postTitle” stays private in Saved. Add it to any collections below.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = onToggleSave,
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(if (isSaved) "Remove from Saved" else "Save video privately")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = collectionName,
                    onValueChange = { collectionName = it.take(50) },
                    modifier = Modifier.weight(1f),
                    label = { Text("New collection") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Button(
                    onClick = {
                        onCreate(collectionName.trim())
                        collectionName = ""
                    },
                    enabled = collectionName.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("Create")
                }
            }
            if (!errorMessage.isNullOrBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (collections.isEmpty()) {
                Text(
                    "Create a collection to group saved videos.",
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn {
                    items(collections, key = { it.id }) { collection ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = collection.isIncluded,
                                onCheckedChange = { onToggle(collection, it) },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(collection.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "${collection.postCount} videos",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            IconButton(onClick = { collectionToDelete = collection }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete ${collection.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.size(1.dp))
        }
    }

    collectionToDelete?.let { collection ->
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text("Delete collection?") },
            text = { Text("“${collection.name}” will be deleted. Its videos will remain in Saved.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(collection)
                    collectionToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) { Text("Cancel") }
            },
        )
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        onLoad()
    }
}
