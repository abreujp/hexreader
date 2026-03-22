package br.com.abreujp.hexreader.app

sealed interface HexReaderScreen {
    data object Home : HexReaderScreen
    data object PackageDetails : HexReaderScreen
    data object OfflineReader : HexReaderScreen
}
