package io.github.takusan23.hiroid.ui.screen

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "設定") })
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {

        }
    }
}