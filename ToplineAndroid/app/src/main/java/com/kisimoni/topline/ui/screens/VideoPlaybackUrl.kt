package com.kisimoni.topline.ui.screens

internal fun cloudinaryPlaybackUrl(videoUrl: String): String {
    if (!videoUrl.contains("res.cloudinary.com/")) return videoUrl

    val normalizedUrl = if (
        videoUrl.contains("/image/upload/") &&
        videoUrl.substringBefore('?').substringBefore('#').substringAfterLast('.')
            .lowercase() in setOf("3gp", "m3u8", "m4v", "mkv", "mov", "mp4", "webm")
    ) {
        videoUrl.replaceFirst("/image/upload/", "/video/upload/")
    } else {
        videoUrl
    }
    val uploadMarker = "/video/upload/"
    val markerIndex = normalizedUrl.indexOf(uploadMarker)
    if (markerIndex < 0) return normalizedUrl

    val transformationStart = markerIndex + uploadMarker.length
    val pathAfterUpload = normalizedUrl.substring(transformationStart)
    val firstSegment = pathAfterUpload.substringBefore('/')
    if (firstSegment.contains("f_mp4") && firstSegment.contains("vc_h264")) {
        return normalizedUrl
    }

    return normalizedUrl.substring(0, transformationStart) +
        "f_mp4,vc_h264/" + pathAfterUpload
}
