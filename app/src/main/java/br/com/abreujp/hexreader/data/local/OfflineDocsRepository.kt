package br.com.abreujp.hexreader.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import br.com.abreujp.hexreader.core.model.DownloadedPackage
import br.com.abreujp.hexreader.core.model.HexPackageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.util.ArrayDeque

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

    suspend fun deletePackage(packageName: String) = withContext(Dispatchers.IO) {
        val currentPackages = readMetadata()
        val packageToDelete = currentPackages.firstOrNull { it.name == packageName } ?: return@withContext

        File(packageToDelete.localIndexPath).parentFile?.deleteRecursively()

        val remainingPackages = currentPackages.filterNot { it.name == packageName }
        val array = JSONArray()
        remainingPackages.forEach { pkg -> array.put(pkg.toJson()) }
        metadataFile.writeText(array.toString())
    }

    suspend fun downloadPackage(pkg: HexPackageSummary): DownloadedPackage = withContext(Dispatchers.IO) {
        require(pkg.docsUrl.isNotBlank()) { "Package has no docs URL" }

        storageDir.mkdirs()

        val packageDir = File(storageDir, sanitizePackageName(pkg.name)).apply {
            deleteRecursively()
            mkdirs()
        }

        val entryPath = try {
            mirrorDocs(pkg.docsUrl.ensureTrailingSlash(), packageDir)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to mirror docs for ${pkg.name} from ${pkg.docsUrl}", exception)
            throw IllegalStateException(
                exception.message ?: "Failed to download package docs",
                exception
            )
        }
        val entryFile = File(packageDir, entryPath)

        val downloadedPackage = DownloadedPackage(
            name = pkg.name,
            description = pkg.description,
            latestVersion = pkg.latestVersion,
            docsUrl = pkg.docsUrl,
            localIndexPath = entryFile.absolutePath,
            downloadedAt = System.currentTimeMillis()
        )

        upsertMetadata(downloadedPackage)
        downloadedPackage
    }

    private fun mirrorDocs(baseUrl: String, packageDir: File): String {
        val baseHttpUrl = baseUrl.toHttpUrl()
        val initialEntry = determineEntryPath(baseHttpUrl)
        val queue = ArrayDeque<String>()
        val visited = mutableSetOf<String>()

        queue.add(initialEntry)
        if (initialEntry != "index.html") {
            queue.add("index.html")
        }

        while (queue.isNotEmpty()) {
            val relativePath = queue.removeFirst()
            if (!visited.add(relativePath)) continue

            val resource = fetchResource(baseHttpUrl, relativePath) ?: continue
            val destinationFile = File(packageDir, relativePath).normalize()
            destinationFile.parentFile?.mkdirs()
            destinationFile.writeBytes(resource.body)

            when (resource.kind) {
                ResourceKind.Html -> {
                    val html = resource.body.toString(Charsets.UTF_8)
                    extractHtmlPaths(html, relativePath, baseHttpUrl).forEach { nextPath ->
                        if (nextPath !in visited) queue.add(nextPath)
                    }
                }

                ResourceKind.Css -> {
                    val css = resource.body.toString(Charsets.UTF_8)
                    extractCssPaths(css, relativePath, baseHttpUrl).forEach { nextPath ->
                        if (nextPath !in visited) queue.add(nextPath)
                    }
                }

                ResourceKind.Binary -> Unit
            }
        }

        return initialEntry
    }

    private fun determineEntryPath(baseHttpUrl: okhttp3.HttpUrl): String {
        fetchResource(baseHttpUrl, "api-reference.html")?.let {
            return "api-reference.html"
        }

        val rootResource = fetchResource(baseHttpUrl, "index.html")
            ?: throw IllegalStateException("Failed to download docs entry page: index.html")

        val rootHtml = rootResource.body.toString(Charsets.UTF_8)
        val redirectTarget = extractMetaRefreshTarget(rootHtml)

        return redirectTarget
            ?.let { resolvePath(it, "index.html", baseHttpUrl).firstOrNull() }
            ?: "index.html"
    }

    private fun fetchResource(baseHttpUrl: okhttp3.HttpUrl, relativePath: String): DownloadedResource? {
        val url = baseHttpUrl.resolve(relativePath) ?: return null
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Skipping resource ${url} with HTTP ${response.code}")
                return null
            }

            val body = response.body?.bytes() ?: return null
            if (body.isEmpty()) return null

            return DownloadedResource(
                body = body,
                kind = resourceKind(response.header("Content-Type").orEmpty(), relativePath)
            )
        }
    }

    private fun extractHtmlPaths(html: String, currentPath: String, baseHttpUrl: okhttp3.HttpUrl): List<String> {
        val attributeValues = trackedAttributes.flatMap { attribute ->
            Regex("""$attribute\s*=\s*[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE)
                .findAll(html)
                .map { match -> match.groupValues[1] }
                .toList()
        }

        val metaRefresh = extractMetaRefreshTarget(html)?.let(::listOf).orEmpty()

        return (attributeValues + metaRefresh)
            .flatMap { value -> resolvePath(value, currentPath, baseHttpUrl) }
            .distinct()
    }

    private fun extractCssPaths(css: String, currentPath: String, baseHttpUrl: okhttp3.HttpUrl): List<String> {
        return Regex("""url\(\s*['\"]?([^'\")\s]+)['\"]?\s*\)""")
            .findAll(css)
            .map { it.groupValues[1] }
            .flatMap { value -> resolvePath(value, currentPath, baseHttpUrl) }
            .distinct()
            .toList()
    }

    private fun resolvePath(value: String, currentPath: String, baseHttpUrl: okhttp3.HttpUrl): List<String> {
        if (ignoredReference(value)) return emptyList()

        val currentUrl = baseHttpUrl.resolve(currentPath) ?: return emptyList()
        val resolvedPath = try {
            val sanitizedValue = sanitizeReference(value)
            URI(currentUrl.toString()).resolve(sanitizedValue).path
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "Skipping invalid reference '$value' from '$currentPath'", exception)
            null
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to resolve reference '$value' from '$currentPath'", exception)
            null
        } ?: return emptyList()

        val basePath = baseHttpUrl.encodedPath

        if (!resolvedPath.startsWith(basePath) || ignoredExtension(resolvedPath)) {
            return emptyList()
        }

        val localPath = toLocalPath(resolvedPath, basePath)
        return if (localPath.isBlank()) emptyList() else listOf(localPath)
    }

    private fun toLocalPath(resolvedPath: String, basePath: String): String {
        val trimmedBase = basePath.trimStart('/').trimEnd('/')
        val normalizedPath = resolvedPath.trimStart('/').normalizeTrailingSlash()

        return when {
            normalizedPath == trimmedBase -> "index.html"
            normalizedPath.startsWith("$trimmedBase/") -> normalizedPath.removePrefix("$trimmedBase/")
            else -> normalizedPath
        }
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

    private fun ignoredReference(value: String): Boolean {
        return value.isBlank() ||
            value.startsWith("#") ||
            value.startsWith("mailto:") ||
            value.startsWith("javascript:") ||
            value.startsWith("data:")
    }

    private fun sanitizeReference(value: String): String {
        val trimmedValue = value.trim()
        return Uri.encode(trimmedValue, "/:@?&=#.-_~!$'()*+,;%")
    }

    private fun ignoredExtension(path: String): Boolean {
        val lower = path.lowercase()
        return ignoredExtensions.any { lower.endsWith(it) }
    }

    private fun resourceKind(contentType: String, path: String): ResourceKind {
        val lowerContentType = contentType.lowercase()
        val lowerPath = path.lowercase()

        return when {
            lowerContentType.contains("text/css") || lowerPath.endsWith(".css") -> ResourceKind.Css
            lowerContentType.contains("text/html") || lowerPath.endsWith(".html") || lowerPath == "index.html" -> ResourceKind.Html
            else -> ResourceKind.Binary
        }
    }

    private data class DownloadedResource(
        val body: ByteArray,
        val kind: ResourceKind
    )

    private enum class ResourceKind {
        Html,
        Css,
        Binary
    }

    private companion object {
        const val TAG = "OfflineDocsRepository"
        val trackedAttributes = listOf("href", "src", "action")
        val ignoredExtensions = listOf(".epub", ".pdf", ".zip", ".tar", ".gz")
    }
}

private fun String.ensureTrailingSlash(): String {
    return if (endsWith('/')) this else "$this/"
}

private fun String.normalizeTrailingSlash(): String {
    return if (endsWith('/')) "${this}index.html" else this
}

private fun extractMetaRefreshTarget(html: String): String? {
    val regex = Regex(
        """<meta[^>]+http-equiv=[\"']refresh[\"'][^>]+content=[\"'][^\"']*url=([^\"'>]+)[\"']""",
        RegexOption.IGNORE_CASE
    )

    return regex.find(html)?.groupValues?.getOrNull(1)?.trim()
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
