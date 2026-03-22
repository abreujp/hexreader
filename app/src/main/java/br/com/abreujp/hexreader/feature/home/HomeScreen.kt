package br.com.abreujp.hexreader.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.abreujp.hexreader.R
import br.com.abreujp.hexreader.core.model.DownloadedPackage
import br.com.abreujp.hexreader.core.model.HexPackageSummary
import br.com.abreujp.hexreader.core.ui.PackageBadge
import br.com.abreujp.hexreader.core.ui.SearchFeedbackCard
import br.com.abreujp.hexreader.core.ui.SectionTitle

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPackageSelected: (HexPackageSummary) -> Unit,
    onOpenDownloadedPackage: (DownloadedPackage) -> Unit,
    onDeleteDownloadedPackage: (DownloadedPackage) -> Unit
) {
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Top
        ) {
            HeaderSection()
            Spacer(modifier = Modifier.height(28.dp))
            SearchSection(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearch = onSearch
            )
            Spacer(modifier = Modifier.height(20.dp))
            SearchResultsSection(
                submittedQuery = uiState.submittedQuery,
                searchState = uiState.searchState,
                onPackageSelected = onPackageSelected
            )
            Spacer(modifier = Modifier.height(28.dp))
            DownloadedDocumentationSection(
                downloadedPackages = uiState.downloadedPackages,
                onOpenDownloadedPackage = onOpenDownloadedPackage,
                onDeleteDownloadedPackage = onDeleteDownloadedPackage
            )
        }
    }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
        SearchUiState.Idle -> SearchFeedbackCard(
            title = stringResource(R.string.search_results_idle_title),
            description = stringResource(R.string.search_results_idle_description)
        )

        SearchUiState.Loading -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

        is SearchUiState.Error -> SearchFeedbackCard(
            title = stringResource(R.string.search_results_error_title),
            description = searchState.message
        )

        is SearchUiState.Empty -> SearchFeedbackCard(
            title = stringResource(R.string.search_results_empty_title, searchState.query),
            description = stringResource(R.string.search_results_empty_description)
        )

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
                    SearchResultCard(item = item, onClick = { onPackageSelected(item) })
                }
            }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                DownloadedPackageCard(
                    item = item,
                    onOpen = { onOpenDownloadedPackage(item) },
                    onDelete = { onDeleteDownloadedPackage(item) }
                )
            }
        }
    }
}

@Composable
private fun DownloadedPackageCard(
    item: DownloadedPackage,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(text = stringResource(R.string.downloaded_docs_open_offline))
                }

                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(text = stringResource(R.string.package_details_delete_button))
                }
            }
        }
    }
}
