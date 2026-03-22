package br.com.abreujp.hexreader

import android.content.res.Configuration
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.abreujp.hexreader.data.DownloadedPackage
import br.com.abreujp.hexreader.data.HexPackageSummary
import br.com.abreujp.hexreader.data.HexPackagesRepository
import br.com.abreujp.hexreader.data.OfflineDocsRepository
import br.com.abreujp.hexreader.ui.theme.HexReaderTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HexReaderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HexReaderHome(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun HexReaderHome(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { HexPackagesRepository() }
    val offlineDocsRepository = remember(context) { OfflineDocsRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val blankSearchError = stringResource(R.string.search_blank_error)
    val searchUnavailableError = stringResource(R.string.search_results_error_description)
    val downloadUnavailableError = stringResource(R.string.package_details_download_error)
    val downloadSuccessMessage = stringResource(R.string.package_details_download_success)
    val deleteSuccessMessage = stringResource(R.string.package_details_delete_success)
    val deleteErrorMessage = stringResource(R.string.package_details_delete_error)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var submittedQuery by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPackage by remember { mutableStateOf<HexPackageSummary?>(null) }
    var openedDownloadedPackage by remember { mutableStateOf<DownloadedPackage?>(null) }
    var downloadedPackages by remember { mutableStateOf<List<DownloadedPackage>>(emptyList()) }
    var downloadState by remember { mutableStateOf<DownloadUiState>(DownloadUiState.Idle) }
    var searchState by remember {
        mutableStateOf<SearchUiState>(SearchUiState.Idle)
    }

    LaunchedEffect(Unit) {
        downloadedPackages = offlineDocsRepository.listDownloadedPackages()
    }

    if (openedDownloadedPackage != null) {
        OfflineReaderScreen(
            downloadedPackage = openedDownloadedPackage!!,
            onBack = { openedDownloadedPackage = null }
        )

        return
    }

    if (selectedPackage != null) {
        PackageDetailsScreen(
            selectedPackage = selectedPackage!!,
            downloadState = downloadState,
            isAlreadyDownloaded = downloadedPackages.any { it.name == selectedPackage!!.name },
            onBack = {
                selectedPackage = null
                downloadState = DownloadUiState.Idle
            },
            onOpenOffline = {
                openedDownloadedPackage = downloadedPackages.firstOrNull {
                    it.name == selectedPackage!!.name
                }
            },
            onDownload = {
                coroutineScope.launch {
                    downloadState = DownloadUiState.Loading

                    downloadState = try {
                        offlineDocsRepository.downloadPackage(selectedPackage!!)
                        downloadedPackages = offlineDocsRepository.listDownloadedPackages()
                        DownloadUiState.Success(downloadSuccessMessage)
                    } catch (_: Exception) {
                        DownloadUiState.Error(downloadUnavailableError)
                    }
                }
            },
            onDelete = {
                coroutineScope.launch {
                    downloadState = DownloadUiState.Loading

                    downloadState = try {
                        offlineDocsRepository.deletePackage(selectedPackage!!.name)
                        downloadedPackages = offlineDocsRepository.listDownloadedPackages()
                        DownloadUiState.Success(deleteSuccessMessage)
                    } catch (_: Exception) {
                        DownloadUiState.Error(deleteErrorMessage)
                    }
                }
            }
        )

        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Top
        ) {
            HeaderSection()

            Spacer(modifier = Modifier.height(28.dp))

            SearchSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearch = {
                    val normalizedQuery = searchQuery.trim()

                    submittedQuery = normalizedQuery

                    if (normalizedQuery.isBlank()) {
                        searchState = SearchUiState.Error(
                            message = blankSearchError
                        )
                        return@SearchSection
                    }

                    coroutineScope.launch {
                        searchState = SearchUiState.Loading

                        searchState = try {
                            val results = repository.searchPackages(normalizedQuery)

                            if (results.isEmpty()) {
                                SearchUiState.Empty(normalizedQuery)
                            } else {
                                SearchUiState.Success(results)
                            }
                        } catch (_: Exception) {
                            SearchUiState.Error(
                                message = searchUnavailableError
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SearchResultsSection(
                submittedQuery = submittedQuery,
                searchState = searchState,
                onPackageSelected = { selectedPackage = it }
            )

            Spacer(modifier = Modifier.height(28.dp))

            DownloadedDocumentationSection(
                downloadedPackages = downloadedPackages,
                onOpenDownloadedPackage = { openedDownloadedPackage = it },
                onDeleteDownloadedPackage = { pkg ->
                    coroutineScope.launch {
                        offlineDocsRepository.deletePackage(pkg.name)
                        downloadedPackages = offlineDocsRepository.listDownloadedPackages()
                    }
                }
            )
        }
    }
}

private sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val packages: List<HexPackageSummary>) : SearchUiState
    data class Empty(val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

private sealed interface DownloadUiState {
    data object Idle : DownloadUiState
    data object Loading : DownloadUiState
    data class Success(val message: String) : DownloadUiState
    data class Error(val message: String) : DownloadUiState
}

@Composable
private fun HeaderSection() {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.header_badge),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.search_library_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.search_library_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_library_input_label)) },
                placeholder = { Text(stringResource(R.string.search_library_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = onSearch, shape = RoundedCornerShape(18.dp)) {
                Text(text = stringResource(R.string.search_library_button))
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
    submittedQuery: String?,
    searchState: SearchUiState,
    onPackageSelected: (HexPackageSummary) -> Unit
) {
    SectionTitle(text = stringResource(R.string.search_results_title))

    Spacer(modifier = Modifier.height(12.dp))

    when (searchState) {
        SearchUiState.Idle -> {
            SearchFeedbackCard(
                title = stringResource(R.string.search_results_idle_title),
                description = stringResource(R.string.search_results_idle_description)
            )
        }

        SearchUiState.Loading -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp)

                    Column {
                        Text(
                            text = stringResource(R.string.search_results_loading_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(
                                R.string.search_results_loading_description,
                                submittedQuery.orEmpty()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is SearchUiState.Error -> {
            SearchFeedbackCard(
                title = stringResource(R.string.search_results_error_title),
                description = searchState.message
            )
        }

        is SearchUiState.Empty -> {
            SearchFeedbackCard(
                title = stringResource(R.string.search_results_empty_title, searchState.query),
                description = stringResource(R.string.search_results_empty_description)
            )
        }

        is SearchUiState.Success -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(
                        R.string.search_results_count,
                        searchState.packages.size,
                        submittedQuery.orEmpty()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                searchState.packages.take(15).forEach { item ->
                    SearchResultCard(
                        item = item,
                        onClick = { onPackageSelected(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchFeedbackCard(title: String, description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchResultCard(item: HexPackageSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PackageBadge(text = item.latestVersion.ifBlank { stringResource(R.string.search_results_unknown_version) })

                if (item.hasDocs) {
                    PackageBadge(
                        text = stringResource(R.string.search_results_has_docs_badge),
                        emphasized = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.description.ifBlank {
                    stringResource(R.string.search_results_missing_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.search_results_weekly_downloads, item.weeklyDownloads),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.search_results_open_details),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PackageDetailsScreen(
    selectedPackage: HexPackageSummary,
    downloadState: DownloadUiState,
    isAlreadyDownloaded: Boolean,
    onBack: () -> Unit,
    onOpenOffline: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.package_details_delete_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.package_details_delete_dialog_description,
                        selectedPackage.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text(text = stringResource(R.string.package_details_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(text = stringResource(R.string.package_details_delete_cancel))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.package_details_back)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.package_details_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = selectedPackage.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PackageBadge(
                            text = selectedPackage.latestVersion.ifBlank {
                                stringResource(R.string.search_results_unknown_version)
                            }
                        )

                        if (selectedPackage.hasDocs) {
                            PackageBadge(
                                text = stringResource(R.string.search_results_has_docs_badge),
                                emphasized = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = selectedPackage.description.ifBlank {
                            stringResource(R.string.search_results_missing_description)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    DetailRow(
                        label = stringResource(R.string.package_details_weekly_downloads_label),
                        value = selectedPackage.weeklyDownloads.toString()
                    )
                    DetailRow(
                        label = stringResource(R.string.package_details_docs_url_label),
                        value = selectedPackage.docsUrl.ifBlank {
                            stringResource(R.string.package_details_not_available)
                        }
                    )
                    DetailRow(
                        label = stringResource(R.string.package_details_package_url_label),
                        value = selectedPackage.packageUrl.ifBlank {
                            stringResource(R.string.package_details_not_available)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.package_details_download_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isAlreadyDownloaded) {
                            stringResource(R.string.package_details_downloaded_description)
                        } else {
                            stringResource(R.string.package_details_download_description)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (downloadState is DownloadUiState.Success) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = downloadState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (downloadState is DownloadUiState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = downloadState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAlreadyDownloaded) {
                        Button(
                            onClick = onOpenOffline,
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(text = stringResource(R.string.package_details_open_offline_button))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            enabled = downloadState !is DownloadUiState.Loading,
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(text = stringResource(R.string.package_details_delete_button))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = onDownload,
                        enabled = downloadState !is DownloadUiState.Loading,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        if (downloadState is DownloadUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.size(8.dp))
                        }

                        Text(
                            text = if (isAlreadyDownloaded) {
                                stringResource(R.string.package_details_redownload_button)
                            } else {
                                stringResource(R.string.package_details_download_button)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineReaderScreen(
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
                    webView.loadDataWithBaseURL(
                        baseUrl,
                        html,
                        "text/html",
                        "utf-8",
                        null
                    )
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

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun PackageBadge(text: String, emphasized: Boolean = true) {
    val containerColor = if (emphasized) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (emphasized) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .background(color = containerColor, shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DownloadedDocumentationSection(
    downloadedPackages: List<DownloadedPackage>,
    onOpenDownloadedPackage: (DownloadedPackage) -> Unit,
    onDeleteDownloadedPackage: (DownloadedPackage) -> Unit
) {
    SectionTitle(text = stringResource(R.string.downloaded_docs_title))

    Spacer(modifier = Modifier.height(12.dp))

    if (downloadedPackages.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.downloaded_docs_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.empty_downloaded_docs_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.empty_downloaded_docs_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            downloadedPackages.forEach { item ->
                var showDeleteConfirmation by remember(item.name) { mutableStateOf(false) }

                if (showDeleteConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmation = false },
                        title = { Text(stringResource(R.string.package_details_delete_dialog_title)) },
                        text = {
                            Text(
                                stringResource(
                                    R.string.package_details_delete_dialog_description,
                                    item.name
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirmation = false
                                    onDeleteDownloadedPackage(item)
                                }
                            ) {
                                Text(text = stringResource(R.string.package_details_delete_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmation = false }) {
                                Text(text = stringResource(R.string.package_details_delete_cancel))
                            }
                        }
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDownloadedPackage(item) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PackageBadge(
                                text = item.latestVersion.ifBlank {
                                    stringResource(R.string.search_results_unknown_version)
                                }
                            )

                            PackageBadge(
                                text = stringResource(R.string.downloaded_docs_saved_badge),
                                emphasized = false
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.description.ifBlank {
                                stringResource(R.string.search_results_missing_description)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.downloaded_docs_open_offline),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(text = stringResource(R.string.package_details_delete_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Preview(name = "Light", showBackground = true, device = Devices.PIXEL_6)
@Composable
fun HexReaderHomePreview() {
    HexReaderTheme {
        HexReaderHome()
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.PIXEL_6
)
@Composable
fun HexReaderHomeDarkPreview() {
    HexReaderTheme(darkTheme = true) {
        HexReaderHome()
    }
}
