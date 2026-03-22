package br.com.abreujp.hexreader.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class OfflineDocsRepository(
    context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val appContext = context.applicationContext
    private val storageDir = File(appContext.filesDir, "offline_docs")
    private val metadataFile = File(storageDir, "downloaded_packages.json")

    suspend fun listDownloadedPackages(): List<DownloadedPackage> = withContext(Dispatchers.IO) {
        readMetadata()
    }

    suspend fun downloadPackage(pkg: HexPackageSummary): DownloadedPackage = withContext(Dispatchers.IO) {
        require(pkg.docsUrl.isNotBlank()) { "Package has no docs URL" }

        storageDir.mkdirs()

        val packageDir = File(storageDir, sanitizePackageName(pkg.name)).apply { mkdirs() }
        val indexFile = File(packageDir, "index.html")

        val request = Request.Builder()
            .url(pkg.docsUrl)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to download docs with status ${response.code}")
            }

            val html = response.body?.string().orEmpty()
            if (html.isBlank()) {
                throw IllegalStateException("Downloaded docs are empty")
            }

            indexFile.writeText(html)
        }

        val downloadedPackage = DownloadedPackage(
            name = pkg.name,
            description = pkg.description,
            latestVersion = pkg.latestVersion,
            docsUrl = pkg.docsUrl,
            localIndexPath = indexFile.absolutePath,
            downloadedAt = System.currentTimeMillis()
        )

        upsertMetadata(downloadedPackage)
        downloadedPackage
    }

    private fun readMetadata(): List<DownloadedPackage> {
        if (!metadataFile.exists()) return emptyList()

        val content = metadataFile.readText()
        if (content.isBlank()) return emptyList()

        val array = JSONArray(content)

        return List(array.length()) { index ->
            array.getJSONObject(index).toDownloadedPackage()
        }.sortedByDescending { it.downloadedAt }
    }

    private fun upsertMetadata(downloadedPackage: DownloadedPackage) {
        val updatedPackages = readMetadata()
            .filterNot { it.name == downloadedPackage.name }
            .toMutableList()
            .apply { add(0, downloadedPackage) }

        val array = JSONArray()
        updatedPackages.forEach { pkg -> array.put(pkg.toJson()) }

        metadataFile.writeText(array.toString())
    }

    private fun sanitizePackageName(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9_.-]"), "_")
    }
}

private fun DownloadedPackage.toJson(): JSONObject {
    return JSONObject().apply {
        put("name", name)
        put("description", description)
        put("latestVersion", latestVersion)
        put("docsUrl", docsUrl)
        put("localIndexPath", localIndexPath)
        put("downloadedAt", downloadedAt)
    }
}

private fun JSONObject.toDownloadedPackage(): DownloadedPackage {
    return DownloadedPackage(
        name = optString("name"),
        description = optString("description"),
        latestVersion = optString("latestVersion"),
        docsUrl = optString("docsUrl"),
        localIndexPath = optString("localIndexPath"),
        downloadedAt = optLong("downloadedAt")
    )
}
