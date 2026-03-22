package br.com.abreujp.hexreader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class HexPackagesRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun searchPackages(query: String): List<HexPackageSummary> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        val url = "https://hex.pm/api/packages".toHttpUrl().newBuilder()
            .addQueryParameter("search", normalizedQuery)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Hex.pm search failed with ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val searchResults = parsePackages(body).rankForQuery(normalizedQuery)
            val exactPackage = fetchExactPackage(normalizedQuery)

            mergeExactPackage(exactPackage, searchResults)
        }
    }

    private fun fetchExactPackage(packageName: String): HexPackageSummary? {
        if (packageName.isBlank()) return null

        val url = "https://hex.pm/api/packages/${packageName.lowercase()}"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            return parsePackage(body)
        }
    }

    private fun parsePackages(body: String): List<HexPackageSummary> {
        if (body.isBlank()) return emptyList()

        val array = JSONArray(body)

        return List(array.length()) { index -> parsePackage(array.getJSONObject(index).toString()) }
    }

    private fun parsePackage(body: String): HexPackageSummary {
        val item = JSONObject(body)
        val meta = item.optJSONObject("meta")
        val downloads = item.optJSONObject("downloads")
        val releases = item.optJSONArray("releases") ?: JSONArray()

        return HexPackageSummary(
            name = item.optString("name"),
            description = meta?.optString("description").orEmpty(),
            latestVersion = item.optString("latest_stable_version").ifBlank {
                item.optString("latest_version")
            },
            weeklyDownloads = downloads?.optInt("week") ?: 0,
            hasDocs = releases.anyReleaseWithDocs(),
            docsUrl = item.optString("docs_html_url"),
            packageUrl = item.optString("html_url")
        )
    }
}

private fun mergeExactPackage(
    exactPackage: HexPackageSummary?,
    searchResults: List<HexPackageSummary>
): List<HexPackageSummary> {
    if (exactPackage == null) return searchResults

    return listOf(exactPackage) + searchResults.filterNot { it.name == exactPackage.name }
}

private fun List<HexPackageSummary>.rankForQuery(query: String): List<HexPackageSummary> {
    if (query.isBlank()) return this

    val normalizedQuery = query.lowercase()
    val exactMatches = filter { it.name.lowercase() == normalizedQuery }
    val nameMatches = filter { it.name.lowercase().contains(normalizedQuery) }

    val candidates = if (nameMatches.isNotEmpty()) nameMatches else this

    val sortedCandidates = candidates
        .distinctBy { it.name }
        .sortedWith(
            compareByDescending<HexPackageSummary> { packageMatchScore(it.name, normalizedQuery) }
                .thenByDescending { it.weeklyDownloads }
                .thenByDescending { it.hasDocs }
                .thenBy { it.name }
        )

    if (exactMatches.isEmpty()) return sortedCandidates

    val exactNames = exactMatches.map { it.name }.toSet()
    return exactMatches.distinctBy { it.name } + sortedCandidates.filterNot { it.name in exactNames }
}

private fun packageMatchScore(name: String, query: String): Int {
    val normalizedName = name.lowercase()

    return when {
        normalizedName == query -> 500
        normalizedName.startsWith("${query}_") || normalizedName.startsWith("${query}-") || normalizedName.startsWith("${query}.") -> 350
        normalizedName.startsWith(query) -> 300
        normalizedName.split("_", "-", ".").any { it == query } -> 250
        normalizedName.contains(query) -> 200
        else -> 0
    }
}

private fun JSONArray.anyReleaseWithDocs(): Boolean {
    for (index in 0 until length()) {
        if (optJSONObject(index)?.optBoolean("has_docs") == true) {
            return true
        }
    }

    return false
}
