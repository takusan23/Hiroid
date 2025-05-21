package io.github.takusan23.hiroid.tool

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import io.github.takusan23.hiroid.VoskAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** 端末内の音声を MediaProjection を使って録音する */
object InternalAudioTool {

    /** 端末内の音声を録音する */
    @SuppressLint("MissingPermission")
    fun recordInternalAudio(
        context: Context,
        resultCode: Int,
        resultData: Intent
    ) = callbackFlow {
        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val bufferSize = AudioRecord.getMinBufferSize(
            VoskAndroid.SAMPLING_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // MediaProjection
        // メインスレッドで registerCallback() する
        val mediaProjection = withContext(Dispatchers.Main) {
            mediaProjectionManager.getMediaProjection(resultCode, resultData).apply {
                // 画面録画中のコールバック
                registerCallback(object : MediaProjection.Callback() {
                    // MediaProjection 終了時
                    override fun onStop() {
                        super.onStop()
                        cancel()
                    }
                }, null)
            }
        }
        // 内部音声取るのに使う
        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection).apply {
            addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            addMatchingUsage(AudioAttributes.USAGE_GAME)
            addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
        }.build()
        val audioFormat = AudioFormat.Builder().apply {
            setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            setSampleRate(VoskAndroid.SAMPLING_RATE)
            setChannelMask(AudioFormat.CHANNEL_IN_MONO)
        }.build()
        val audioRecord = AudioRecord.Builder().apply {
            setAudioPlaybackCaptureConfig(playbackConfig)
            setAudioFormat(audioFormat)
            setBufferSizeInBytes(bufferSize)
        }.build()

        // 開始
        try {
            audioRecord.startRecording()
            while (true) {
                yield()
                val pcmAudio = ByteArray(bufferSize)
                audioRecord.read(pcmAudio, 0, pcmAudio.size)
                trySend(pcmAudio)
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
            mediaProjection.stop()
        }
    }
}