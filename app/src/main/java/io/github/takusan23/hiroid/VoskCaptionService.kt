package io.github.takusan23.hiroid

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoskCaptionService : LifecycleService() {

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
                val modelPath = getExternalFilesDir(null)!!.resolve("vosk-model-small-ja-0.22")
                val voskAndroid = VoskAndroid(modelPath.path).apply { prepare() }

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
                                val result = voskAndroid.recognizeFromSpeechPcm(pcm) ?: return@collect
                                println(result)
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
    }
}