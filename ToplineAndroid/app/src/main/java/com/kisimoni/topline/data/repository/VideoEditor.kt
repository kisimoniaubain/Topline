package com.kisimoni.topline.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.effect.RgbFilter
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object VideoEditor {
    suspend fun render(
        context: Context,
        edit: PostMediaEdit,
    ): File {
        require(edit.videoClips.isNotEmpty()) { "Choose at least one video clip." }
        val output = File.createTempFile("topline_edited_", ".mp4", context.cacheDir)
        try {
            export(context, edit, output)
            return output
        } catch (error: Exception) {
            if (output.exists() && !output.delete()) {
                throw IllegalStateException("Unable to clean up the failed video export.", error)
            }
            throw error
        }
    }

    private suspend fun export(context: Context, edit: PostMediaEdit, output: File) =
        withContext(Dispatchers.Main.immediate) {
        val videoEffects = when (edit.filterName) {
            "mono" -> listOf(RgbFilter.createGrayscaleFilter())
            "negative" -> listOf(RgbFilter.createInvertedFilter())
            else -> emptyList()
        }
        val clips = edit.videoClips.map { clip ->
            val mediaItemBuilder = MediaItem.Builder().setUri(clip.uri)
            if (clip.trimEndMs > clip.trimStartMs && clip.trimEndMs > 0) {
                mediaItemBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build(),
                )
            }
            EditedMediaItem.Builder(mediaItemBuilder.build())
                .setEffects(Effects(emptyList(), videoEffects))
                .setRemoveAudio(edit.soundtrackUri != null)
                .build()
        }
        val sequences = mutableListOf(EditedMediaItemSequence(clips))
        edit.soundtrackUri?.let { soundtrack ->
            val audioItem = EditedMediaItem.Builder(MediaItem.fromUri(soundtrack))
                .setRemoveVideo(true)
                .build()
            sequences += EditedMediaItemSequence(listOf(audioItem), true)
        }
        val composition = Composition.Builder(sequences).build()

        suspendCancellableCoroutine<Unit> { continuation ->
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                })
                .build()
            continuation.invokeOnCancellation {
                Handler(Looper.getMainLooper()).post { transformer.cancel() }
            }
            transformer.start(composition, output.absolutePath)
        }
    }
}
