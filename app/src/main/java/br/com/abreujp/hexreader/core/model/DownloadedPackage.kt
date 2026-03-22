package br.com.abreujp.hexreader.core.model

data class DownloadedPackage(
    val name: String,
    val description: String,
    val latestVersion: String,
    val docsUrl: String,
    val localIndexPath: String,
    val downloadedAt: Long
)
