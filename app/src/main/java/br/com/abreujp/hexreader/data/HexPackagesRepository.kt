package br.com.abreujp.hexreader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class HexPackagesRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun searchPackages(query: String): List<HexPackageSummary> = withContext(Dispatchers.IO) {
        val url = "https://hex.pm/api/packages".toHttpUrl().newBuilder()
            .addQueryParameter("search", query)
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
            parsePackages(body)
        }
    }

    private fun parsePackages(body: String): List<HexPackageSummary> {
        if (body.isBlank()) return emptyList()

        val array = JSONArray(body)

        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val meta = item.optJSONObject("meta")
            val downloads = item.optJSONObject("downloads")
            val releases = item.optJSONArray("releases") ?: JSONArray()

            HexPackageSummary(
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
}

private fun JSONArray.anyReleaseWithDocs(): Boolean {
    for (index in 0 until length()) {
        if (optJSONObject(index)?.optBoolean("has_docs") == true) {
            return true
        }
    }

    return false
}
