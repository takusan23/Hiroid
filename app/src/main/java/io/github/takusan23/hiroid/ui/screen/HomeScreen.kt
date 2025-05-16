package io.github.takusan23.hiroid.ui.screen

import android.content.Context
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.hiroid.R
import io.github.takusan23.hiroid.VoskCaptionService
import io.github.takusan23.hiroid.ui.component.MenuContainer
import io.github.takusan23.hiroid.ui.component.MenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val isServiceRunning = remember { mutableStateOf(VoskCaptionService.isServiceRunning(context)) }

    // MediaProjection
    val mediaProjectionManager = remember { context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    val mediaProjectionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            VoskCaptionService.startService(
                context = context,
                resultCode = it.resultCode,
                data = it.data ?: return@rememberLauncherForActivityResult
            )
        }
    )
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.app_name)) })
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {

            item {
                RunningCard(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    isRunning = isServiceRunning.value,
                    onClick = {
                        // 起動しているなら終了など
                        if (isServiceRunning.value) {
                            VoskCaptionService.stopService(context)
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                mediaProjectionRequest.launch(mediaProjectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()))
                            } else {
                                mediaProjectionRequest.launch(mediaProjectionManager.createScreenCaptureIntent())
                            }
                        }
                        isServiceRunning.value = !isServiceRunning.value
                    }
                )
            }

            item {
                MenuContainer(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    title = "言語の設定"
                ) {
                    MenuItem(
                        title = "モデルを追加する",
                        description = "他のモデルをダウンロードして追加できます",
                        iconResId = R.drawable.language_24px,
                        onClick = { }
                    )
                    MenuItem(
                        title = "モデルを変更する",
                        description = "追加したモデルを切り替えます",
                        iconResId = R.drawable.language_24px,
                        onClick = { }
                    )
                }
            }

            item {
                MenuContainer(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    title = "そのほか"
                ) {
                    MenuItem(
                        title = "ライセンス",
                        description = "thx",
                        iconResId = R.drawable.data_object_24px,
                        onClick = { }
                    )
                    MenuItem(
                        title = "ソースコードを見る",
                        description = "GitHub から見ることが出来ます",
                        iconResId = R.drawable.open_in_browser_24px,
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
private fun RunningCard(
    modifier: Modifier = Modifier,
    isRunning: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = if (isRunning) "実行中です" else "起動していません",
                fontSize = 20.sp
            )
            Text(text = "端末内の音声を取得して、文字起こしをするアプリです。")

            if (isRunning) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClick
                ) {
                    Text(text = "終了")
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClick
                ) {
                    Text(text = "開始する")
                }
            }
        }
    }
}