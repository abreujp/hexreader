package br.com.abreujp.hexreader.feature.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.abreujp.hexreader.R
import br.com.abreujp.hexreader.app.HexReaderScreen
import br.com.abreujp.hexreader.core.model.DownloadedPackage
import br.com.abreujp.hexreader.core.model.HexPackageSummary
import br.com.abreujp.hexreader.data.local.OfflineDocsRepository
import br.com.abreujp.hexreader.data.remote.HexPackagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val remoteRepository: HexPackagesRepository,
    private val offlineRepository: OfflineDocsRepository
) : ViewModel() {
    private val app = application

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshDownloadedPackages()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearch() {
        val normalizedQuery = _uiState.value.searchQuery.trim()
        _uiState.update { it.copy(submittedQuery = normalizedQuery) }

        if (normalizedQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    searchState = SearchUiState.Error(message = app.getString(R.string.search_blank_error))
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(searchState = SearchUiState.Loading) }

            val nextState = try {
                val results = remoteRepository.searchPackages(normalizedQuery)
                if (results.isEmpty()) {
                    SearchUiState.Empty(normalizedQuery)
                } else {
                    SearchUiState.Success(results)
                }
            } catch (_: Exception) {
                SearchUiState.Error(app.getString(R.string.search_results_error_description))
            }

            _uiState.update { it.copy(searchState = nextState) }
        }
    }

    fun openPackageDetails(pkg: HexPackageSummary) {
        _uiState.update {
            it.copy(
                screen = HexReaderScreen.PackageDetails,
                selectedPackage = pkg,
                downloadState = DownloadUiState.Idle
            )
        }
    }

    fun openOfflineReader(pkg: DownloadedPackage) {
        _uiState.update {
            it.copy(
                screen = HexReaderScreen.OfflineReader,
                openedDownloadedPackage = pkg
            )
        }
    }

    fun openOfflineReaderForSelectedPackage() {
        val selectedName = _uiState.value.selectedPackage?.name ?: return
        val downloadedPackage = _uiState.value.downloadedPackages.firstOrNull { it.name == selectedName } ?: return
        openOfflineReader(downloadedPackage)
    }

    fun navigateBack() {
        when (_uiState.value.screen) {
            HexReaderScreen.Home -> Unit
            HexReaderScreen.PackageDetails -> {
                _uiState.update {
                    it.copy(
                        screen = HexReaderScreen.Home,
                        selectedPackage = null,
                        downloadState = DownloadUiState.Idle
                    )
                }
            }
            HexReaderScreen.OfflineReader -> {
                _uiState.update {
                    val nextScreen = if (it.selectedPackage != null) {
                        HexReaderScreen.PackageDetails
                    } else {
                        HexReaderScreen.Home
                    }

                    it.copy(
                        screen = nextScreen,
                        openedDownloadedPackage = null
                    )
                }
            }
        }
    }

    fun downloadSelectedPackage() {
        val selectedPackage = _uiState.value.selectedPackage ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(downloadState = DownloadUiState.Loading) }

            val nextState = try {
                offlineRepository.downloadPackage(selectedPackage)
                val refreshedPackages = offlineRepository.listDownloadedPackages()
                _uiState.update { current -> current.copy(downloadedPackages = refreshedPackages) }
                DownloadUiState.Success(app.getString(R.string.package_details_download_success))
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to download docs for ${selectedPackage.name}", exception)
                val message = exception.message?.takeIf { it.isNotBlank() }
                    ?: app.getString(R.string.package_details_download_error)
                DownloadUiState.Error(message)
            }

            _uiState.update { it.copy(downloadState = nextState) }
        }
    }

    fun deleteSelectedPackage() {
        val selectedPackage = _uiState.value.selectedPackage ?: return
        deletePackageByName(selectedPackage.name)
    }

    fun deleteDownloadedPackage(pkg: DownloadedPackage) {
        deletePackageByName(pkg.name)
    }

    fun isSelectedPackageDownloaded(): Boolean {
        val selectedName = _uiState.value.selectedPackage?.name ?: return false
        return _uiState.value.downloadedPackages.any { it.name == selectedName }
    }

    private fun refreshDownloadedPackages() {
        viewModelScope.launch {
            val downloadedPackages = offlineRepository.listDownloadedPackages()
            _uiState.update { it.copy(downloadedPackages = downloadedPackages) }
        }
    }

    private fun deletePackageByName(packageName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadState = DownloadUiState.Loading) }

            val nextState = try {
                offlineRepository.deletePackage(packageName)
                val refreshedPackages = offlineRepository.listDownloadedPackages()
                _uiState.update { current -> current.copy(downloadedPackages = refreshedPackages) }
                DownloadUiState.Success(app.getString(R.string.package_details_delete_success))
            } catch (_: Exception) {
                DownloadUiState.Error(app.getString(R.string.package_details_delete_error))
            }

            _uiState.update { current ->
                current.copy(
                    downloadState = nextState,
                    openedDownloadedPackage = if (current.openedDownloadedPackage?.name == packageName) null else current.openedDownloadedPackage,
                    screen = if (current.screen == HexReaderScreen.OfflineReader && current.openedDownloadedPackage?.name == packageName) {
                        HexReaderScreen.Home
                    } else {
                        current.screen
                    }
                )
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"

        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        application = application,
                        remoteRepository = HexPackagesRepository(),
                        offlineRepository = OfflineDocsRepository(application)
                    ) as T
                }
            }
        }
    }
}
