package br.com.abreujp.hexreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import br.com.abreujp.hexreader.app.HexReaderApp
import br.com.abreujp.hexreader.ui.theme.HexReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HexReaderTheme {
                Scaffold {
                    HexReaderApp()
                }
            }
        }
    }
}
