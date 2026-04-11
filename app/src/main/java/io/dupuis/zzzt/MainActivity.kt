package io.dupuis.zzzt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.dupuis.zzzt.ui.nav.ZzztNavHost
import io.dupuis.zzzt.ui.theme.ZzztTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZzztTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ZzztNavHost()
                }
            }
        }
    }
}
