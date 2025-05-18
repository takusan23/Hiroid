package io.github.takusan23.hiroid.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.github.takusan23.hiroid.R
import io.github.takusan23.hiroid.tool.PermissionTool
import io.github.takusan23.hiroid.tool.VoskModelTool
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val ModelDownloadLink = "https://alphacephei.com/vosk/models"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onComplete: () -> Unit) {
    val isAllPermissionGranted = remember { mutableStateOf(false) }
    val isPreparedModel = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.setup_screen_title)) })
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {

            item {
                PermissionCard(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    onComplete = { isAllPermissionGranted.value = true }
                )
            }

            item {
                VoskModelCard(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    onComplete = { isPreparedModel.value = true }
                )
            }

            // 準備ができた
            item {
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier.align(Alignment.Center),
                        enabled = isAllPermissionGranted.value && isPreparedModel.value,
                        onClick = onComplete
                    ) {
                        Text(text = stringResource(R.string.setup_button_start))
                    }
                }
            }
        }
    }
}

@Composable
private fun VoskModelCard(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isProgress = remember { mutableStateOf(false) }
    val isPreparedModel = remember { mutableStateOf(false) }

    // onComplete を呼ぶ
    LaunchedEffect(key1 = Unit) {
        snapshotFlow { isPreparedModel.value }.first { it }
        onComplete()
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch {
                // モデルをアプリに追加
                isProgress.value = true
                VoskModelTool.addVoskModel(context, context.contentResolver.openInputStream(uri)!!)
                isProgress.value = false

                // 終わればチェック
                isPreparedModel.value = VoskModelTool.getVoskModelList(context).isNotEmpty()
            }
        }
    )

    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Text(
                text = stringResource(R.string.setup_button_vosk_model_card_title),
                fontSize = 20.sp
            )
            Text(
                text = stringResource(R.string.setup_button_vosk_model_card_description)
            )

            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, ModelDownloadLink.toUri())) }) {
                Icon(
                    painter = painterResource(R.drawable.open_in_browser_24px),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.setup_button_vosk_model_button_open_browser))
            }
            Button(onClick = { filePicker.launch(arrayOf("application/zip")) }) {
                Text(text = stringResource(R.string.setup_button_vosk_model_button_select_model))
            }

            if (isProgress.value) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun PermissionCard(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val isRecordGranted = remember { mutableStateOf(PermissionTool.isAudioPermissionGranted(context)) }
    val isOverlayWindowGranted = remember { mutableStateOf(PermissionTool.isOverlayWindowPermissionGranted(context)) }

    // onComplete を呼ぶ
    LaunchedEffect(key1 = Unit) {
        snapshotFlow { isRecordGranted.value }.first { it }
        snapshotFlow { isOverlayWindowGranted.value }.first { it }
        onComplete()
    }

    val permissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isRecordGranted.value = it }
    )
    val overlaySetting = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { isOverlayWindowGranted.value = PermissionTool.isOverlayWindowPermissionGranted(context) }
    )

    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Text(
                text = stringResource(R.string.setup_screen_permission_card_title),
                fontSize = 20.sp
            )
            Text(
                text = stringResource(R.string.setup_button_permission_card_description)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRecordGranted.value) {
                    Icon(
                        painter = painterResource(R.drawable.check_circle_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Button(onClick = { permissionRequest.launch(android.Manifest.permission.RECORD_AUDIO) }) {
                    Text(text = stringResource(R.string.setup_button_permission_button_record))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOverlayWindowGranted.value) {
                    Icon(
                        painter = painterResource(R.drawable.check_circle_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Button(onClick = { overlaySetting.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }) {
                    Text(text = stringResource(R.string.setup_button_permission_button_overlay))
                }
            }
        }
    }
}