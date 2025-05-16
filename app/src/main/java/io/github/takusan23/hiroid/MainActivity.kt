package io.github.takusan23.hiroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.takusan23.hiroid.ui.screen.MainScreen
import io.github.takusan23.hiroid.ui.theme.HiroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HiroidTheme {
                MainScreen()
            }
        }
    }
}
