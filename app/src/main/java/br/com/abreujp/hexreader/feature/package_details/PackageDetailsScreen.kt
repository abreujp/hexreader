package br.com.abreujp.hexreader.feature.package_details

import androidx.compose.foundation.background
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
import br.com.abreujp.hexreader.core.model.HexPackageSummary
import br.com.abreujp.hexreader.core.ui.PackageBadge
import br.com.abreujp.hexreader.feature.home.DownloadUiState

@Composable
fun PackageDetailsScreen(
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
                        Button(onClick = onOpenOffline, shape = RoundedCornerShape(18.dp)) {
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
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(14.dp))
    }
}
