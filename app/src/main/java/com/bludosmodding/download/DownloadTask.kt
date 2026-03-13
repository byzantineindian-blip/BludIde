package com.bludosmodding.download

data class DownloadTask(
    val name: String,
    val url: String,
    val targetFileName: String,
    val extract: Boolean = true
)
