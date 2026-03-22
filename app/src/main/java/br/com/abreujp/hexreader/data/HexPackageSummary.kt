package br.com.abreujp.hexreader.data

data class HexPackageSummary(
    val name: String,
    val description: String,
    val latestVersion: String,
    val weeklyDownloads: Int,
    val hasDocs: Boolean,
    val docsUrl: String,
    val packageUrl: String
)
