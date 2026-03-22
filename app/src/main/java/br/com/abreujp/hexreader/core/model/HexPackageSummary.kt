package br.com.abreujp.hexreader.core.model

data class HexPackageSummary(
    val name: String,
    val description: String,
    val latestVersion: String,
    val weeklyDownloads: Int,
    val hasDocs: Boolean,
    val docsUrl: String,
    val packageUrl: String
)
