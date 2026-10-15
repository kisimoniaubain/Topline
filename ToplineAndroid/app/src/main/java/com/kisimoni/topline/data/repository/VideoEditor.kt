package com.kisimoni.topline.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.text.TextUtils
import android.text.TextPaint
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
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
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
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
        appendEndCard: Boolean = false,
    ): File {
        require(edit.videoClips.isNotEmpty()) { "Choose at least one video clip." }
        val output = File.createTempFile("topline_edited_", ".mp4", context.cacheDir)
        var endCardAssets: EndCardAssets? = null
        try {
            if (appendEndCard) {
                require(!watermarkUsername.isNullOrBlank()) {
                    "A creator name is required to add the downloaded-video end card."
                }
                endCardAssets = withContext(Dispatchers.IO) {
                    createEndCardAssets(context, edit, watermarkUsername)
                }
            }
            export(context, edit, watermarkUsername, endCardAssets, output)
            endCardAssets?.let { verifyEndCardExport(output, it.sourceDurationMs) }
            return output
        } catch (error: Exception) {
            if (output.exists() && !output.delete()) {
                throw IllegalStateException("Unable to clean up the failed video export.", error)
            }
            throw error
        } finally {
            endCardAssets?.delete()
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun export(
        context: Context,
        edit: PostMediaEdit,
        watermarkUsername: String?,
        endCardAssets: EndCardAssets?,
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
        }.toMutableList()
        endCardAssets?.let { assets ->
            val imageItem = MediaItem.Builder()
                .setUri(Uri.fromFile(assets.imageFile))
                .setImageDurationMs(END_CARD_DURATION_MS)
                .build()
            clips += EditedMediaItem.Builder(imageItem)
                .setEffects(Effects(emptyList(), emptyList()))
                .setRemoveAudio(true)
                .build()
        }
        val sequences = mutableListOf(EditedMediaItemSequence(clips))
        edit.soundtrackUri?.let { soundtrack ->
            val audioItem = EditedMediaItem.Builder(MediaItem.fromUri(soundtrack))
                .setRemoveVideo(true)
                .build()
            sequences += EditedMediaItemSequence(listOf(audioItem), true)
        }
        endCardAssets?.let { assets ->
            val chimeItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(assets.chimeFile)))
                .setRemoveVideo(true)
                .build()
            sequences += EditedMediaItemSequence(listOf(chimeItem))
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

    private fun createEndCardAssets(
        context: Context,
        edit: PostMediaEdit,
        creatorUsername: String,
    ): EndCardAssets {
        require(edit.videoClips.size == 1) {
            "The downloaded-video end card currently supports one video clip."
        }
        require(edit.soundtrackUri == null) {
            "The downloaded-video end card cannot be added when a separate soundtrack is present."
        }

        val clip = edit.videoClips.single()
        val retriever = MediaMetadataRetriever()
        val videoDurationMs: Long
        val videoWidth: Int
        val videoHeight: Int
        try {
            retriever.setDataSource(context, clip.uri)
            videoDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: throw IllegalStateException("Unable to read the downloaded video's duration.")
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
                ?: throw IllegalStateException("Unable to read the downloaded video's width.")
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
                ?: throw IllegalStateException("Unable to read the downloaded video's height.")
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                videoWidth = rawHeight
                videoHeight = rawWidth
            } else {
                videoWidth = rawWidth
                videoHeight = rawHeight
            }
        } finally {
            retriever.release()
        }
        require(videoDurationMs > 0) { "The downloaded video has no readable duration." }

        val scale = min(1f, MAX_END_CARD_DIMENSION.toFloat() / max(videoWidth, videoHeight))
        val width = max(2, (videoWidth * scale).roundToInt()).let { if (it % 2 == 0) it else it - 1 }
        val height = max(2, (videoHeight * scale).roundToInt()).let { if (it % 2 == 0) it else it - 1 }
        val imageFile = File.createTempFile("topline_end_card_", ".png", context.cacheDir)
        val chimeFile = File.createTempFile("topline_end_chime_", ".m4a", context.cacheDir)
        try {
            createEndCardBitmap(context, width, height, creatorUsername).useBitmap { bitmap ->
                imageFile.outputStream().buffered().use { stream ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        "Unable to encode the downloaded-video end card."
                    }
                }
            }
            encodeEndCardChime(chimeFile, videoDurationMs)
            return EndCardAssets(imageFile, chimeFile, videoDurationMs)
        } catch (error: Exception) {
            imageFile.delete()
            chimeFile.delete()
            throw error
        }
    }

    private fun createEndCardBitmap(
        context: Context,
        width: Int,
        height: Int,
        creatorUsername: String,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val icon = decodeWhiteLogo(context, R.drawable.icon1, 8)
        val wordmark = decodeWhiteLogo(context, R.drawable.logo1, 4)
        try {
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val lockupWidth = width * END_CARD_LOCKUP_WIDTH_FRACTION
            val iconWidth = width * END_CARD_ICON_WIDTH_FRACTION
            val iconHeight = iconWidth * icon.height / icon.width
            val wordmarkHeight = lockupWidth * wordmark.height / wordmark.width
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = width * END_CARD_HANDLE_SIZE_FRACTION
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val handle = creatorUsername.trim().removePrefix("@").ifBlank { "Topline" }
            val handleText = TextUtils.ellipsize(
                "@$handle",
                textPaint,
                width * END_CARD_HANDLE_MAX_WIDTH_FRACTION,
                TextUtils.TruncateAt.END,
            ).toString()
            val handleHeight = textPaint.descent() - textPaint.ascent()
            val gap = width * END_CARD_LOCKUP_GAP_FRACTION
            val totalHeight = iconHeight + gap + wordmarkHeight + gap + handleHeight
            var top = (height - totalHeight) / 2f

            val iconLeft = (width - iconWidth) / 2f
            canvas.drawBitmap(icon, null, RectF(iconLeft, top, iconLeft + iconWidth, top + iconHeight), iconPaint)
            top += iconHeight + gap

            val wordmarkLeft = (width - lockupWidth) / 2f
            canvas.drawBitmap(
                wordmark,
                null,
                RectF(wordmarkLeft, top, wordmarkLeft + lockupWidth, top + wordmarkHeight),
                iconPaint,
            )
            top += wordmarkHeight + gap - textPaint.ascent()
            canvas.drawText(handleText, (width - textPaint.measureText(handleText)) / 2f, top, textPaint)
        } catch (error: Exception) {
            bitmap.recycle()
            throw error
        } finally {
            icon.recycle()
            wordmark.recycle()
        }
        return bitmap
    }

    private fun encodeEndCardChime(output: File, videoDurationMs: Long) {
        val sampleRate = CHIME_SAMPLE_RATE
        val totalSamples = ((videoDurationMs + END_CARD_DURATION_MS) * sampleRate / 1_000L)
        val chimeStartSample = ((videoDurationMs + CHIME_START_OFFSET_MS) * sampleRate / 1_000L)
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var codecStarted = false
        var muxerStarted = false
        var audioTrack = -1
        var samplePosition = 0L
        var inputFinished = false
        var outputFinished = false
        val bufferInfo = MediaCodec.BufferInfo()
        try {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, CHIME_BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, CHIME_INPUT_BUFFER_BYTES)
            }
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            codecStarted = true

            while (!outputFinished) {
                if (!inputFinished) {
                    val inputIndex = codec.dequeueInputBuffer(CHIME_CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                            ?: throw IllegalStateException("AAC encoder returned no input buffer.")
                        inputBuffer.clear()
                        val samplesToWrite = min(
                            inputBuffer.remaining() / Short.SIZE_BYTES,
                            (totalSamples - samplePosition).toInt().coerceAtLeast(0),
                        )
                        repeat(samplesToWrite) {
                            inputBuffer.putShort(chimeSample(samplePosition, chimeStartSample, sampleRate))
                            samplePosition++
                        }
                        val isLastBuffer = samplePosition >= totalSamples
                        val presentationTimeUs = (samplePosition - samplesToWrite) * 1_000_000L / sampleRate
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            samplesToWrite * Short.SIZE_BYTES,
                            presentationTimeUs,
                            if (isLastBuffer) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0,
                        )
                        inputFinished = isLastBuffer
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, CHIME_CODEC_TIMEOUT_US)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        check(audioTrack == -1) { "AAC encoder changed output format more than once." }
                        audioTrack = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                            ?: throw IllegalStateException("AAC encoder returned no output buffer.")
                        if (
                            bufferInfo.size > 0 &&
                            bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                        ) {
                            check(muxerStarted && audioTrack >= 0) {
                                "AAC encoder produced audio before its output format was ready."
                            }
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(audioTrack, outputBuffer, bufferInfo)
                        }
                        outputFinished = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        } finally {
            if (codecStarted) {
                codec.stop()
            }
            codec.release()
            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
        }
    }

    private fun verifyEndCardExport(output: File, sourceDurationMs: Long) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(output.absolutePath)
            val videoTrack = (0 until extractor.trackCount)
                .firstOrNull { index ->
                    extractor.getTrackFormat(index)
                        .getString(MediaFormat.KEY_MIME)
                        ?.startsWith("video/") == true
                }
                ?: throw IllegalStateException("The downloaded export contains no video track.")
            val videoFormat = extractor.getTrackFormat(videoTrack)
            check(videoFormat.containsKey(MediaFormat.KEY_DURATION)) {
                "Unable to verify the downloaded video's final duration."
            }
            val videoDurationMs = videoFormat.getLong(MediaFormat.KEY_DURATION) / 1_000L
            val expectedDurationMs = sourceDurationMs + END_CARD_DURATION_MS
            check(videoDurationMs >= expectedDurationMs - END_CARD_DURATION_TOLERANCE_MS) {
                "The Topline end card was not appended to the downloaded video. Please try again."
            }
        } finally {
            extractor.release()
        }
    }

    private fun chimeSample(samplePosition: Long, chimeStartSample: Long, sampleRate: Int): Short {
        val elapsedMs = (samplePosition - chimeStartSample) * 1_000L / sampleRate
        if (elapsedMs !in 0 until CHIME_DURATION_MS) return 0
        val frequency = when {
            elapsedMs < CHIME_FIRST_NOTE_MS -> CHIME_FIRST_FREQUENCY_HZ
            elapsedMs < CHIME_NOTE_GAP_END_MS -> return 0
            else -> CHIME_SECOND_FREQUENCY_HZ
        }
        val noteStartMs = if (elapsedMs < CHIME_FIRST_NOTE_MS) 0 else CHIME_NOTE_GAP_END_MS
        val noteDurationMs = if (elapsedMs < CHIME_FIRST_NOTE_MS) {
            CHIME_FIRST_NOTE_MS
        } else {
            CHIME_DURATION_MS - CHIME_NOTE_GAP_END_MS
        }
        val noteElapsedMs = elapsedMs - noteStartMs
        val attack = (noteElapsedMs / CHIME_ATTACK_MS.toFloat()).coerceIn(0f, 1f)
        val release = ((noteDurationMs - noteElapsedMs) / CHIME_RELEASE_MS.toFloat()).coerceIn(0f, 1f)
        val envelope = min(attack, release)
        val seconds = (samplePosition - chimeStartSample).toDouble() / sampleRate
        val value = kotlin.math.sin(2.0 * PI * frequency * seconds) * CHIME_AMPLITUDE * envelope
        return (value * Short.MAX_VALUE).toInt().toShort()
    }

    private data class EndCardAssets(
        val imageFile: File,
        val chimeFile: File,
        val sourceDurationMs: Long,
    ) {
        fun delete() {
            listOf(imageFile, chimeFile).forEach { file ->
                if (file.exists() && !file.delete()) {
                    android.util.Log.w("VideoEditor", "Unable to remove temporary end-card asset: ${file.name}")
                }
            }
        }
    }

    private inline fun <T> Bitmap.useBitmap(block: (Bitmap) -> T): T =
        try {
            block(this)
        } finally {
            recycle()
        }

    private fun createWatermarkOverlay(context: Context, creatorUsername: String): BitmapOverlay {
        val bitmap = Bitmap.createBitmap(WATERMARK_WIDTH, WATERMARK_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val icon = decodeWhiteLogo(context, R.drawable.icon1, 8)
        val wordmark = decodeWhiteLogo(context, R.drawable.logo1, 4)
        try {
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val iconSize = 100f
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
            val wordmarkWidth = 150f
            val wordmarkHeight = wordmarkWidth * wordmark.height / wordmark.width
            val wordmarkTop = iconSize + 26f
            canvas.drawBitmap(
                wordmark,
                null,
                RectF(
                    (WATERMARK_WIDTH - wordmarkWidth) / 2f,
                    wordmarkTop,
                    (WATERMARK_WIDTH + wordmarkWidth) / 2f,
                    wordmarkTop + wordmarkHeight,
                ),
                iconPaint,
            )

            val handle = creatorUsername.trim().removePrefix("@").ifBlank { "Topline" }
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val watermarkText = TextUtils.ellipsize(
                "@$handle",
                textPaint,
                WATERMARK_WIDTH - 20f,
                TextUtils.TruncateAt.END,
            ).toString()
            val handleBaseline = wordmarkTop + wordmarkHeight + 36f
            canvas.drawText(
                watermarkText,
                (WATERMARK_WIDTH - textPaint.measureText(watermarkText)) / 2f,
                handleBaseline,
                textPaint,
            )
        } finally {
            icon.recycle()
            wordmark.recycle()
        }

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
    private const val WATERMARK_HEIGHT = 260
    private const val WATERMARK_SCALE = 0.5f
    private const val WATERMARK_ALPHA = 1f
    private const val WATERMARK_MOVE_INTERVAL_US = 3_000_000L
    private val WATERMARK_POSITIONS = listOf(
        0.88f to 0.88f,
        -0.88f to 0.88f,
        -0.88f to -0.88f,
        0.88f to -0.88f,
    )
    private const val END_CARD_DURATION_MS = 2_000L
    private const val END_CARD_DURATION_TOLERANCE_MS = 500L
    private const val MAX_END_CARD_DIMENSION = 1_920
    private const val END_CARD_LOCKUP_WIDTH_FRACTION = 0.42f
    private const val END_CARD_ICON_WIDTH_FRACTION = 0.12f
    private const val END_CARD_HANDLE_SIZE_FRACTION = 0.055f
    private const val END_CARD_HANDLE_MAX_WIDTH_FRACTION = 0.82f
    private const val END_CARD_LOCKUP_GAP_FRACTION = 0.035f
    private const val CHIME_SAMPLE_RATE = 44_100
    private const val CHIME_BIT_RATE = 96_000
    private const val CHIME_INPUT_BUFFER_BYTES = 16_384
    private const val CHIME_CODEC_TIMEOUT_US = 10_000L
    private const val CHIME_START_OFFSET_MS = 120L
    private const val CHIME_DURATION_MS = 520L
    private const val CHIME_FIRST_NOTE_MS = 170L
    private const val CHIME_NOTE_GAP_END_MS = 215L
    private const val CHIME_ATTACK_MS = 18L
    private const val CHIME_RELEASE_MS = 90L
    private const val CHIME_FIRST_FREQUENCY_HZ = 659
    private const val CHIME_SECOND_FREQUENCY_HZ = 880
    private const val CHIME_AMPLITUDE = 0.24
}
