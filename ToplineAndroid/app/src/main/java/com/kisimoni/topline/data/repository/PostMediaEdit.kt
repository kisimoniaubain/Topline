package com.kisimoni.topline.data.repository

import android.net.Uri

data class VideoClipEdit(
    val uri: Uri,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = 0,
)

data class PostMediaEdit(
    val videoClips: List<VideoClipEdit> = emptyList(),
    val soundtrackUri: Uri? = null,
    val filterName: String = "none",
)
