package io.github.takusan23.hiroid.ui.screen

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.takusan23.hiroid.R
import io.github.takusan23.hiroid.VoskCaptionService
import io.github.takusan23.hiroid.tool.VoskModelTool
import io.github.takusan23.hiroid.ui.component.MenuContainer
import io.github.takusan23.hiroid.ui.component.MenuItem
import io.github.takusan23.hiroid.ui.component.ModelSelectBottomSheet
import kotlinx.coroutines.launch

private val GitHubUrl = "https://github.com/takusan23/Hiroid"
private val PrivacyPolicyUrl = "https://takusan.negitoro.dev/pages/hiroid_privacy_policy/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (Route) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // サービスの実行状態
    val isServiceRunning = remember { mutableStateOf(VoskCaptionService.isServiceRunning(context)) }
    LaunchedEffect(key1 = Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            isServiceRunning.value = VoskCaptionService.isServiceRunning(context)
        }
    }

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

    // モデル追加
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch {
                VoskModelTool.addVoskModel(
                    context = context,
                    inputStream = context.contentResolver.openInputStream(uri)!!
                )
                Toast.makeText(context, context.getString(R.string.home_screen_toast_model_added), Toast.LENGTH_SHORT).show()
            }
        }
    )

    // モデル選択ボトムシート
    val isShowModelBottomSheet = remember { mutableStateOf(false) }
    if (isShowModelBottomSheet.value) {
        ModelSelectBottomSheet(
            onClose = { isShowModelBottomSheet.value = false }
        )
    }

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
                    title = stringResource(R.string.home_screen_language_setting)
                ) {
                    MenuItem(
                        title = stringResource(R.string.home_screen_menu_add_model),
                        description = stringResource(R.string.home_screen_menu_add_model_description),
                        iconResId = R.drawable.language_24px,
                        onClick = { filePicker.launch(arrayOf("application/zip")) }
                    )
                    MenuItem(
                        title = stringResource(R.string.home_screen_menu_change_model),
                        description = stringResource(R.string.home_screen_menu_change_model_description),
                        iconResId = R.drawable.language_24px,
                        onClick = { isShowModelBottomSheet.value = true }
                    )
                }
            }

            item {
                MenuContainer(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.home_screen_other)
                ) {
                    MenuItem(
                        title = stringResource(R.string.home_screen_menu_license),
                        description = stringResource(R.string.home_screen_menu_license_description),
                        iconResId = R.drawable.data_object_24px,
                        onClick = { onNavigate(Route.License) }
                    )
                    MenuItem(
                        title = stringResource(R.string.home_screen_menu_privacy_policy),
                        iconResId = R.drawable.open_in_browser_24px,
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, PrivacyPolicyUrl.toUri())) }
                    )
                    MenuItem(
                        title = stringResource(R.string.home_screen_menu_source_code),
                        description = stringResource(R.string.home_screen_menu_source_code_description),
                        iconResId = R.drawable.open_in_browser_24px,
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, GitHubUrl.toUri())) }
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
                text = if (isRunning) stringResource(R.string.home_screen_running_card_running) else stringResource(R.string.home_screen_running_card_not_running),
                fontSize = 20.sp
            )
            Text(text = stringResource(R.string.home_screen_running_card_description))

            if (isRunning) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClick
                ) {
                    Text(text = stringResource(R.string.home_screen_running_card_button_stop))
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClick
                ) {
                    Text(text = stringResource(R.string.home_screen_running_card_button_start))
                }
            }
        }
    }
}