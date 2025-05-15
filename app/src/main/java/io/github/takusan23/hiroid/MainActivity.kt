package io.github.takusan23.hiroid

import android.content.Context
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.takusan23.hiroid.ui.theme.HiroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        println(
            getExternalFilesDir(null)!!.resolve("vosk-model-small-ja-0.22").path
        )
        setContent {
            HiroidTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current

    // 権限
    val permissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "権限が付与されました", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // 権限を要求
    LaunchedEffect(key1 = Unit) {
        permissionRequest.launch(android.Manifest.permission.RECORD_AUDIO)
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

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    mediaProjectionRequest.launch(mediaProjectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()))
                } else {
                    mediaProjectionRequest.launch(mediaProjectionManager.createScreenCaptureIntent())
                }
            }) { Text("開始") }
            Button(onClick = {
                VoskCaptionService.stopService(context)
            }) { Text("終了") }
        }
    }
}