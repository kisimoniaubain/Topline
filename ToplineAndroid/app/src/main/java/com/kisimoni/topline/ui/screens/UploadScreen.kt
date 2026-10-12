package com.kisimoni.topline.ui.screens

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.kisimoni.topline.data.api.User
import com.kisimoni.topline.data.repository.PostMediaEdit
import com.kisimoni.topline.data.repository.VideoClipEdit

private enum class PublishStep { Select, Edit, Details }
private data class ClipDraft(
    val uri: Uri,
    val durationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = durationMs,
)

@Composable
fun UploadScreen(
    user: User?,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSubmit: (String, Uri?, Uri?, String, Boolean, PostMediaEdit?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(
            "topline_post_draft_${user?.id.orEmpty()}",
            android.content.Context.MODE_PRIVATE,
        )
    }
    var text by remember { mutableStateOf(preferences.getString("caption", "").orEmpty()) }
    var videoClips by remember {
        mutableStateOf(loadClipDrafts(context, preferences.getString("videoClips", null)))
    }
    var selectedPhotoUri by remember { mutableStateOf(preferences.getString("photoUri", null)?.let(Uri::parse)) }
    var coverUri by remember { mutableStateOf(preferences.getString("coverUri", null)?.let(Uri::parse)) }
    var soundtrackUri by remember { mutableStateOf(preferences.getString("soundtrackUri", null)?.let(Uri::parse)) }
    var selectedFilter by remember { mutableStateOf(preferences.getString("filterName", "none") ?: "none") }
    var audience by remember { mutableStateOf(preferences.getString("audience", "public") ?: "public") }
    var allowComments by remember { mutableStateOf(preferences.getBoolean("allowComments", true)) }
    var showAudienceOptions by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var step by remember {
        mutableStateOf(if (videoClips.isEmpty() && selectedPhotoUri == null) PublishStep.Select else PublishStep.Edit)
    }

    val pickVideos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { persistReadPermission(context, it) }
            val addedClips = uris.mapNotNull { uri ->
                try {
                    ClipDraft(uri, mediaDurationMs(context, uri))
                } catch (error: Exception) {
                    Toast.makeText(
                        context,
                        error.message ?: "Unable to read the selected video.",
                        Toast.LENGTH_LONG,
                    ).show()
                    null
                }
            }
            val remaining = (8 - videoClips.size).coerceAtLeast(0)
            if (addedClips.size > remaining) {
                Toast.makeText(context, "You can use up to 8 clips per post.", Toast.LENGTH_LONG).show()
            }
            videoClips = videoClips + addedClips.take(remaining)
            selectedPhotoUri = null
            coverUri = null
            step = PublishStep.Edit
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            selectedPhotoUri = uri
            videoClips = emptyList()
            coverUri = null
            step = PublishStep.Edit
        }
    }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            coverUri = uri
        }
    }
    val pickSoundtrack = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            soundtrackUri = uri
        }
    }
    val recordVideo = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            try {
                if (videoClips.size >= 8) {
                    Toast.makeText(context, "You can use up to 8 clips per post.", Toast.LENGTH_LONG).show()
                } else {
                    videoClips = videoClips + ClipDraft(uri, mediaDurationMs(context, uri))
                }
                selectedPhotoUri = null
                coverUri = null
                step = PublishStep.Edit
            } catch (error: Exception) {
                Toast.makeText(context, error.message ?: "Unable to read the recorded video.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val isVideo = videoClips.isNotEmpty()
    val mediaUri = videoClips.firstOrNull()?.uri ?: selectedPhotoUri
    val editPlan = if (isVideo) {
        PostMediaEdit(
            videoClips = videoClips.map { VideoClipEdit(it.uri, it.trimStartMs, it.trimEndMs) },
            soundtrackUri = soundtrackUri,
            filterName = selectedFilter,
        )
    } else {
        null
    }
    val steps = listOf("Select", "Edit", "Details")
    val stepIndex = when (step) {
        PublishStep.Select -> 0
        PublishStep.Edit -> 1
        PublishStep.Details -> 2
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                when (step) {
                    PublishStep.Select -> onBack()
                    PublishStep.Edit -> step = PublishStep.Select
                    PublishStep.Details -> step = PublishStep.Edit
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Create post", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            steps.forEachIndexed { index, title ->
                Text(
                    text = "${index + 1}. $title",
                    modifier = Modifier.weight(1f),
                    color = if (index == stepIndex) Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (index == stepIndex) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        HorizontalDivider()

        when (step) {
            PublishStep.Select -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Choose a video or photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Pick a clip from your device or record one now.",
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            pickVideos.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Choose video")
                    }
                    Button(
                        onClick = {
                            recordVideo.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Record video")
                    }
                    TextButton(
                        onClick = {
                            pickPhoto.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Choose photo")
                    }
                    TextButton(onClick = { step = PublishStep.Edit }) {
                        Text("Continue without media")
                    }
                    if (videoClips.isNotEmpty() || selectedPhotoUri != null) {
                        TextButton(onClick = { step = PublishStep.Edit }) { Text("Continue with saved draft") }
                    }
                }
            }

            PublishStep.Edit -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialAvatar(user?.name.orEmpty().ifBlank { "Topline" }, 44, user?.profilePicture)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Edit your post", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Add a caption and choose a cover.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (isVideo) {
                        SelectedVideoPreview(videoClips.first().uri)
                        Text("${videoClips.size}/8 clips · reorder or trim each clip")
                        videoClips.forEachIndexed { index, clip ->
                            ClipEditor(
                                clip = clip,
                                onChange = { updated ->
                                    videoClips = videoClips.toMutableList().also { it[index] = updated }
                                },
                                onMove = { delta ->
                                    val destination = index + delta
                                    if (destination in videoClips.indices) {
                                        videoClips = videoClips.toMutableList().apply {
                                            add(destination, removeAt(index))
                                        }
                                    }
                                },
                                onRemove = {
                                    videoClips = videoClips.filterIndexed { clipIndex, _ -> clipIndex != index }
                                },
                                canMoveUp = index > 0,
                                canMoveDown = index < videoClips.lastIndex,
                            )
                        }
                        TextButton(
                            onClick = {
                                pickVideos.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                                )
                            },
                            enabled = videoClips.size < 8,
                        ) {
                            Text("Add clips")
                        }
                        Text("Filters", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("none", "mono", "negative").forEach { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = {
                                        Text(
                                            when (filter) {
                                                "mono" -> "Mono"
                                                "negative" -> "Negative"
                                                else -> "Original"
                                            },
                                        )
                                    },
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Soundtrack", fontWeight = FontWeight.SemiBold)
                                Text(
                                    soundtrackUri?.lastPathSegment?.substringAfterLast('/') ?: "Original clip audio",
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            TextButton(onClick = { pickSoundtrack.launch(arrayOf("audio/*")) }) {
                                Text(if (soundtrackUri == null) "Choose" else "Change")
                            }
                            if (soundtrackUri != null) {
                                IconButton(onClick = { soundtrackUri = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove soundtrack")
                                }
                            }
                        }
                    } else if (selectedPhotoUri != null) {
                        AsyncImage(
                            model = selectedPhotoUri,
                            contentDescription = "Selected photo",
                            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text("Choose media before continuing.")
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(5000) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write a caption…") },
                        supportingText = { Text("${text.length}/5000") },
                        minLines = 2,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                    )
                    if (isVideo) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (coverUri != null) {
                                AsyncImage(
                                    model = coverUri,
                                    contentDescription = "Selected cover",
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            TextButton(
                                onClick = {
                                    pickCover.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            ) {
                                Text(if (coverUri == null) "Choose cover image" else "Change cover image")
                            }
                            if (coverUri != null) {
                                IconButton(onClick = { coverUri = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove custom cover")
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { step = PublishStep.Details },
                        enabled = mediaUri != null || text.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                    ) { Text("Next: post settings") }
                }
            }

            PublishStep.Details -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it.take(5000) },
                                modifier = Modifier.weight(1f).height(172.dp),
                                placeholder = { Text("Add description...") },
                                supportingText = { Text("${text.length}/5000") },
                                maxLines = 6,
                            )
                            Box(
                                modifier = Modifier.size(width = 116.dp, height = 172.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF252525)),
                                contentAlignment = Alignment.Center,
                            ) {
                                val coverPreview = coverUri ?: selectedPhotoUri
                                if (coverPreview != null) {
                                    AsyncImage(
                                        model = coverPreview,
                                        contentDescription = "Post cover preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Text(
                                        if (isVideo) "Video\ncover" else "Photo",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                if (isVideo) {
                                    TextButton(
                                        onClick = {
                                            pickCover.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                            )
                                        },
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                            .fillMaxWidth().background(Color.Black.copy(alpha = 0.65f)),
                                    ) { Text("Edit cover", color = Color.White) }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                text = if (text.isBlank()) "#" else "$text #"
                            }) { Text("# Hashtags") }
                            TextButton(onClick = {
                                text = if (text.isBlank()) "@" else "$text @"
                            }) { Text("@ Mention") }
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showAudienceOptions = !showAudienceOptions
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("◎", style = MaterialTheme.typography.titleLarge)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    if (audience == "public") "Everyone can view this post"
                                    else "Followers can view this post",
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    if (audience == "public") "Visible to everyone on Topline"
                                    else "Visible to people who follow you",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text("›", style = MaterialTheme.typography.headlineSmall)
                        }
                        if (showAudienceOptions) {
                            AudienceChoice(
                                title = "Everyone",
                                subtitle = "Anyone on Topline can view it.",
                                selected = audience == "public",
                            ) { audience = "public"; showAudienceOptions = false }
                            AudienceChoice(
                                title = "Followers",
                                subtitle = "Only people who follow you can view it.",
                                selected = audience == "followers",
                            ) { audience = "followers"; showAudienceOptions = false }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showMoreOptions = !showMoreOptions
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("⚙", style = MaterialTheme.typography.titleLarge)
                            Text("More options", modifier = Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
                            Text("›", style = MaterialTheme.typography.headlineSmall)
                        }
                        if (showMoreOptions) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Allow comments", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Viewers can comment on this post.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(checked = allowComments, onCheckedChange = { allowComments = it })
                            }
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("↗", style = MaterialTheme.typography.titleLarge)
                            Text("Share to", modifier = Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
                            Text("Topline", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!errorMessage.isNullOrBlank()) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                        Text(
                            "Your video will upload when you tap Post. Keep Topline open until publishing finishes.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = {
                                preferences.edit()
                                    .putString("caption", text)
                                    .putString("photoUri", selectedPhotoUri?.toString())
                                    .putString(
                                        "videoClips",
                                        videoClips.joinToString("\n") {
                                            "${it.uri};${it.trimStartMs};${it.trimEndMs}"
                                        },
                                    )
                                    .putString("coverUri", coverUri?.toString())
                                    .putString("soundtrackUri", soundtrackUri?.toString())
                                    .putString("filterName", selectedFilter)
                                    .putString("audience", audience)
                                    .putBoolean("allowComments", allowComments)
                                    .apply()
                                Toast.makeText(context, "Draft saved on this device.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(54.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            enabled = !isSubmitting,
                        ) {
                            Text("Drafts", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                onSubmit(text.trim(), mediaUri, coverUri, audience, allowComments, editPlan)
                            },
                            enabled = !isSubmitting && (text.isNotBlank() || mediaUri != null),
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE2C55)),
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.size(8.dp))
                                Text("Uploading…")
                            } else {
                                Text("Post", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipEditor(
    clip: ClipDraft,
    onChange: (ClipDraft) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
) {
    val durationSeconds = (clip.durationMs / 1000f).coerceAtLeast(0.1f)
    val startSeconds = (clip.trimStartMs / 1000f).coerceIn(0f, durationSeconds)
    val endSeconds = (clip.trimEndMs / 1000f).coerceIn(startSeconds + 0.05f, durationSeconds)
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                clip.uri.lastPathSegment?.substringAfterLast('/') ?: "Video clip",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Text("Up") }
            TextButton(onClick = { onMove(1) }, enabled = canMoveDown) { Text("Down") }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove clip")
            }
        }
        if (clip.durationMs > 0) {
            Text(
                "Trim ${"%.1f".format(startSeconds)}s – ${"%.1f".format(endSeconds)}s",
                style = MaterialTheme.typography.bodySmall,
            )
            RangeSlider(
                value = startSeconds..endSeconds,
                onValueChange = { range ->
                    val minimumGap = 0.1f
                    val start = range.start.coerceIn(0f, durationSeconds - minimumGap)
                    val end = range.endInclusive.coerceIn(start + minimumGap, durationSeconds)
                    onChange(clip.copy(
                        trimStartMs = (start * 1000).toLong(),
                        trimEndMs = (end * 1000).toLong(),
                    ))
                },
                valueRange = 0f..durationSeconds,
            )
        } else {
            Text(
                "Could not read clip duration, so trimming is unavailable.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun mediaDurationMs(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?: throw IllegalArgumentException("Unable to read video duration. Choose another video.")
    } finally {
        retriever.release()
    }
}

private fun loadClipDrafts(context: Context, savedClips: String?): List<ClipDraft> {
    val lines = savedClips?.lines()?.filter(String::isNotBlank).orEmpty()
    if (lines.isEmpty()) return emptyList()
    return lines.mapNotNull { line ->
        val parts = line.split(';')
        val uri = parts.firstOrNull()?.let(Uri::parse) ?: return@mapNotNull null
        val duration = mediaDurationMs(context, uri)
        val maxStart = (duration - 1).coerceAtLeast(0)
        val start = parts.getOrNull(1)?.toLongOrNull()?.coerceIn(0, maxStart) ?: 0
        val end = parts.getOrNull(2)?.toLongOrNull()?.coerceIn(start + 1, duration) ?: duration
        ClipDraft(uri, duration, start, end)
    }
}

private fun persistReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        Toast.makeText(
            context,
            "The selected file may not be available if you leave before publishing.",
            Toast.LENGTH_LONG,
        ).show()
    }
}

@Composable
private fun AudienceChoice(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SelectedVideoPreview(uri: Uri) {
    val context = LocalContext.current
    var playbackError by remember(uri) { mutableStateOf(false) }
    val exoPlayer = remember(context, uri) {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    playbackError = true
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) playbackError = false
                }
            })
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    Box(Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black)) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (playbackError) {
            Text(
                "This video cannot be previewed. Try another video.",
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                color = Color.White,
            )
        }
    }
}
