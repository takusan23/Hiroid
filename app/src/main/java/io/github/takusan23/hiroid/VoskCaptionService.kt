package io.github.takusan23.hiroid

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.takusan23.hiroid.tool.DataStoreKey
import io.github.takusan23.hiroid.tool.VoskModelTool
import io.github.takusan23.hiroid.tool.dataStore
import io.github.takusan23.hiroid.ui.theme.HiroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoskCaptionService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val voskResultList = mutableStateOf(emptyList<VoskAndroid.VoskResult>())
    private val composeView by lazy {
        ComposeView(this).apply {
            setContent {
                HiroidTheme {
                    Column(
                        modifier = Modifier
                            .width(400.dp)
                            .height(200.dp)
                            .pointerInput(key1 = Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    params.x += dragAmount.x.toInt()
                                    params.y += dragAmount.y.toInt()
                                    windowManager.updateViewLayout(this@apply, params)
                                }
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {

                        val state = rememberLazyListState()
                        LaunchedEffect(key1 = Unit) {
                            // 一番下にスクロールしている場合は true になる Flow
                            snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index == (voskResultList.value.size - 1) }.collectLatest { isBottom ->
                                if (!isBottom) return@collectLatest
                                // 常に下にスクロールする
                                snapshotFlow { voskResultList.value }.collectLatest {
                                    state.animateScrollToItem(index = it.size)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                modifier = Modifier.size(48.dp),
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null
                            )
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            IconButton(onClick = { stopSelf() }) {
                                Icon(
                                    painter = painterResource(R.drawable.close_24px),
                                    contentDescription = null
                                )
                            }
                        }

                        LazyColumn(
                            state = state,
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            itemsIndexed(voskResultList.value) { index, text ->
                                if (index != 0) {
                                    HorizontalDivider()
                                }
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = when (text) {
                                        is VoskAndroid.VoskResult.Partial -> text.partial
                                        is VoskAndroid.VoskResult.Result -> text.text
                                    },
                                    color = when (text) {
                                        is VoskAndroid.VoskResult.Partial -> Color.Red
                                        is VoskAndroid.VoskResult.Result -> Color.Unspecified
                                    },
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    override fun onCreate() {
        super.onCreate()
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        savedStateRegistryController.performRestore(null)
        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(composeView)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // フォアグラウンドサービス通知を出す
        val notificationManager = NotificationManagerCompat.from(this)
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW).apply {
                setName("文字起こしサービス実行中")
            }.build()
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle("文字起こしサービス")
            setContentText("端末内の音声を収集して、文字起こしをしています。")
            setSmallIcon(R.drawable.ic_launcher_foreground)
        }.build()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        if (intent != null) {
            lifecycleScope.launch {
                // モデルを指定して Vosk
                // 指定があればそれ、ないなら一番最初のモデル
                val modelPath = dataStore.data.first()[DataStoreKey.MODEL_FILE_PATH] ?: VoskModelTool.getVoskModelList(this@VoskCaptionService).first().path
                val voskAndroid = VoskAndroid(modelPath).apply { prepare() }

                try {
                    withContext(Dispatchers.Default) {
                        InternalAudioTool
                            .recordInternalAudio(
                                context = this@VoskCaptionService,
                                resultCode = intent.getIntExtra(INTENT_KEY_MEDIA_PROJECTION_RESULT_CODE, -1),
                                resultData = IntentCompat.getParcelableExtra(intent, INTENT_KEY_MEDIA_PROJECTION_RESULT_DATA, Intent::class.java)!!
                            )
                            .conflate()
                            .collect { pcm ->
                                // 文字起こし
                                val result = voskAndroid.recognizeFromSpeechPcm(pcm) ?: return@collect
                                // 配列に足す
                                // Partial は配列に一個あれば良い
                                voskResultList.value = voskResultList.value.filterIsInstance<VoskAndroid.VoskResult.Result>() + result
                            }
                    }
                } finally {
                    // recordInternalAudio が MediaProjection 終了でキャンセル例外を投げる
                    voskAndroid.destroy()
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    companion object {

        // 通知周り
        private const val NOTIFICATION_ID = 1234
        private const val CHANNEL_ID = "hiroid_running_service"

        // Intent に MediaProjection の結果を入れるのでそのキー
        private const val INTENT_KEY_MEDIA_PROJECTION_RESULT_CODE = "result_code"
        private const val INTENT_KEY_MEDIA_PROJECTION_RESULT_DATA = "result_data"

        /** フォアグラウンドサービスを開始する */
        fun startService(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, VoskCaptionService::class.java).apply {
                // サービスで MediaProjection を開始するのに必要
                putExtra(INTENT_KEY_MEDIA_PROJECTION_RESULT_CODE, resultCode)
                putExtra(INTENT_KEY_MEDIA_PROJECTION_RESULT_DATA, data)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** フォアグラウンドサービスを終了する */
        fun stopService(context: Context) {
            val intent = Intent(context, VoskCaptionService::class.java)
            context.stopService(intent)
        }

        /**
         * サービスが起動中かどうか
         * 非推奨だが代替案がない...
         *
         * @param context [Context]
         * @return 起動中なら true
         */
        fun isServiceRunning(context: Context): Boolean {
            return (context.getSystemService(ACTIVITY_SERVICE) as ActivityManager)
                .getRunningServices(Integer.MAX_VALUE)
                .any { it.service.className == VoskCaptionService::class.java.name }
        }
    }
}