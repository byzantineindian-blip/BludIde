package com.bludosmodding.download

data class DownloadStatus(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val speed: String = "0 KB/s",
    val isCompleted: Boolean = false,
    val error: String? = null
)
