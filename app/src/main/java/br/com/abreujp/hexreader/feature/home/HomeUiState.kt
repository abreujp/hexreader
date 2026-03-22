package br.com.abreujp.hexreader.feature.home

import br.com.abreujp.hexreader.app.HexReaderScreen
import br.com.abreujp.hexreader.core.model.DownloadedPackage
import br.com.abreujp.hexreader.core.model.HexPackageSummary

data class HomeUiState(
    val screen: HexReaderScreen = HexReaderScreen.Home,
    val searchQuery: String = "",
    val submittedQuery: String? = null,
    val searchState: SearchUiState = SearchUiState.Idle,
    val downloadedPackages: List<DownloadedPackage> = emptyList(),
    val selectedPackage: HexPackageSummary? = null,
    val openedDownloadedPackage: DownloadedPackage? = null,
    val downloadState: DownloadUiState = DownloadUiState.Idle
)

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val packages: List<HexPackageSummary>) : SearchUiState
    data class Empty(val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface DownloadUiState {
    data object Idle : DownloadUiState
    data object Loading : DownloadUiState
    data class Success(val message: String) : DownloadUiState
    data class Error(val message: String) : DownloadUiState
}
