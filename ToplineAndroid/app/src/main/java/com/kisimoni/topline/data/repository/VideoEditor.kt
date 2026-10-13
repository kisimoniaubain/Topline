package com.kisimoni.topline.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color
import android.text.TextUtils
import android.text.TextPaint
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.RgbFilter
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import com.kisimoni.topline.R
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
        watermarkUsername: String? = null,
    ): File {
        require(edit.videoClips.isNotEmpty()) { "Choose at least one video clip." }
        val output = File.createTempFile("topline_edited_", ".mp4", context.cacheDir)
        try {
            export(context, edit, watermarkUsername, output)
            return output
        } catch (error: Exception) {
            if (output.exists() && !output.delete()) {
                throw IllegalStateException("Unable to clean up the failed video export.", error)
            }
            throw error
        }
    }

    private suspend fun export(
        context: Context,
        edit: PostMediaEdit,
        watermarkUsername: String?,
        output: File,
    ) =
        withContext(Dispatchers.Main.immediate) {
        val videoEffects = buildList<Effect> {
            when (edit.filterName) {
                "mono" -> add(RgbFilter.createGrayscaleFilter())
                "negative" -> add(RgbFilter.createInvertedFilter())
            }
            watermarkUsername?.let {
                add(OverlayEffect(ImmutableList.of(createWatermarkOverlay(context, it))))
            }
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

    private fun createWatermarkOverlay(context: Context, creatorUsername: String): BitmapOverlay {
        val bitmap = Bitmap.createBitmap(WATERMARK_WIDTH, WATERMARK_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val icon = decodeWhiteLogo(context, R.drawable.icon1, 8)
        val wordmark = decodeWhiteLogo(context, R.drawable.logo1, 4)
        try {
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val iconSize = 96f
            canvas.drawBitmap(
                icon,
                null,
                RectF(
                    (WATERMARK_WIDTH - iconSize) / 2f,
                    0f,
                    (WATERMARK_WIDTH + iconSize) / 2f,
                    iconSize,
                ),
                iconPaint,
            )
            val wordmarkWidth = 300f
            val wordmarkHeight = wordmarkWidth * wordmark.height / wordmark.width
            canvas.drawBitmap(
                wordmark,
                null,
                RectF(
                    (WATERMARK_WIDTH - wordmarkWidth) / 2f,
                    iconSize + 4f,
                    (WATERMARK_WIDTH + wordmarkWidth) / 2f,
                    iconSize + 4f + wordmarkHeight,
                ),
                iconPaint,
            )
        } finally {
            icon.recycle()
            wordmark.recycle()
        }

        val handle = creatorUsername.trim().removePrefix("@").ifBlank { "Topline" }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val watermarkText = TextUtils.ellipsize("@$handle", textPaint, WATERMARK_WIDTH - 20f, TextUtils.TruncateAt.END)
            .toString()
        canvas.drawText(
            watermarkText,
            (WATERMARK_WIDTH - textPaint.measureText(watermarkText)) / 2f,
            WATERMARK_HEIGHT - WATERMARK_TEXT_BOTTOM_INSET -
                (textPaint.ascent() + textPaint.descent()) / 2f,
            textPaint,
        )

        return object : BitmapOverlay() {
            override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap

            override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
                val position = ((presentationTimeUs / WATERMARK_MOVE_INTERVAL_US) % WATERMARK_POSITIONS.size)
                    .toInt()
                val (x, y) = WATERMARK_POSITIONS[position]
                return OverlaySettings.Builder()
                    .setBackgroundFrameAnchor(x, y)
                    .setOverlayFrameAnchor(x, y)
                    .setScale(WATERMARK_SCALE, WATERMARK_SCALE)
                    .setAlphaScale(WATERMARK_ALPHA)
                    .build()
            }
        }
    }

    private fun decodeWhiteLogo(context: Context, resourceId: Int, sampleSize: Int): Bitmap {
        val source = BitmapFactory.decodeResource(
            context.resources,
            resourceId,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: throw IllegalStateException("Unable to load a Topline watermark logo.")
        try {
            val pixels = IntArray(source.width * source.height)
            source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
            pixels.indices.forEach { index ->
                pixels[index] = (Color.alpha(pixels[index]) shl 24) or 0x00FFFFFF
            }
            return Bitmap.createBitmap(pixels, source.width, source.height, Bitmap.Config.ARGB_8888)
        } finally {
            source.recycle()
        }
    }

    private const val WATERMARK_WIDTH = 350
    private const val WATERMARK_HEIGHT = 250
    private const val WATERMARK_TEXT_BOTTOM_INSET = 24f
    private const val WATERMARK_SCALE = 0.42f
    private const val WATERMARK_ALPHA = 0.9f
    private const val WATERMARK_MOVE_INTERVAL_US = 3_000_000L
    private val WATERMARK_POSITIONS = listOf(
        -0.72f to -0.72f,
        0.72f to -0.72f,
        0.72f to 0.72f,
        -0.72f to 0.72f,
    )
}
