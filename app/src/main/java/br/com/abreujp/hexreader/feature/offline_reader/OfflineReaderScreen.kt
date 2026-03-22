package br.com.abreujp.hexreader.feature.offline_reader

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.abreujp.hexreader.R
import br.com.abreujp.hexreader.core.model.DownloadedPackage
import java.io.File

@Composable
fun OfflineReaderScreen(
    downloadedPackage: DownloadedPackage,
    onBack: () -> Unit
) {
    val missingTitle = stringResource(R.string.offline_reader_missing_title)
    val missingDescription = stringResource(R.string.offline_reader_missing_description)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.offline_reader_back)
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.offline_reader_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = downloadedPackage.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.domStorageEnabled = true
                }
            },
            update = { webView ->
                val file = File(downloadedPackage.localIndexPath)
                if (file.exists()) {
                    val html = file.readText()
                    val baseUrl = file.parentFile?.toURI()?.toString()
                    webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null)
                } else {
                    webView.loadData(
                        "<html><body><h2>$missingTitle</h2><p>$missingDescription</p></body></html>",
                        "text/html",
                        "utf-8"
                    )
                }
            }
        )
    }
}
