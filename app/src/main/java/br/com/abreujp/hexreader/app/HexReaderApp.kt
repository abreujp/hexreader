package br.com.abreujp.hexreader.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.abreujp.hexreader.feature.home.HomeScreen
import br.com.abreujp.hexreader.feature.home.HomeViewModel
import br.com.abreujp.hexreader.feature.offline_reader.OfflineReaderScreen
import br.com.abreujp.hexreader.feature.package_details.PackageDetailsScreen

@Composable
fun HexReaderApp() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(application))
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.screen) {
        HexReaderScreen.Home -> {
            HomeScreen(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSearch = viewModel::onSearch,
                onPackageSelected = viewModel::openPackageDetails,
                onOpenDownloadedPackage = viewModel::openOfflineReader,
                onDeleteDownloadedPackage = viewModel::deleteDownloadedPackage,
                onDownloadElixirDocs = viewModel::downloadElixirDocs
            )
        }

        HexReaderScreen.PackageDetails -> {
            uiState.selectedPackage?.let { selectedPackage ->
                PackageDetailsScreen(
                    selectedPackage = selectedPackage,
                    downloadState = uiState.downloadState,
                    isAlreadyDownloaded = viewModel.isSelectedPackageDownloaded(),
                    onBack = viewModel::navigateBack,
                    onOpenOffline = viewModel::openOfflineReaderForSelectedPackage,
                    onDownload = viewModel::downloadSelectedPackage,
                    onDelete = viewModel::deleteSelectedPackage
                )
            }
        }

        HexReaderScreen.OfflineReader -> {
            uiState.openedDownloadedPackage?.let { downloadedPackage ->
                OfflineReaderScreen(
                    downloadedPackage = downloadedPackage,
                    onBack = viewModel::navigateBack
                )
            }
        }
    }
}
