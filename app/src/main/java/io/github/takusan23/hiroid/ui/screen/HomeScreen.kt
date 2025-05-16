package io.github.takusan23.hiroid.ui.screen

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.takusan23.hiroid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.app_name)) })
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {

        }
    }
}