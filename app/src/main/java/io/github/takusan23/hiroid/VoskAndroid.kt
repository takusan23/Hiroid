package io.github.takusan23.hiroid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

/** Vosk を使って文字起こしをする */
class VoskAndroid(private val modelPath: String) {

    private var model: Model? = null
    private var recognizer: Recognizer? = null

    /** モデルを読み込む */
    suspend fun prepare() {
        withContext(Dispatchers.IO) {
            model = Model(modelPath)
            recognizer = Recognizer(model, SAMPLING_RATE.toFloat())
        }
    }

    /** 喋り声の音声（PCM）を入力し、文字起こし結果を取得する */
    suspend fun recognizeFromSpeechPcm(pcmByteArray: ByteArray): VoskResult? {
        val recognizer = recognizer ?: return null

        // 文字起こしする
        val isFullyText = withContext(Dispatchers.Default) {
            recognizer.acceptWaveForm(pcmByteArray, pcmByteArray.size)
        }

        // JSON なのでパースする
        val voskResult = if (isFullyText) {
            val jsonObject = JSONObject(recognizer.result)
            VoskResult.Result(text = jsonObject.getString("text"))
        } else {
            val jsonObject = JSONObject(recognizer.partialResult)
            VoskResult.Partial(partial = jsonObject.getString("partial"))
        }

        // 空文字なら return
        return if (voskResult.isBlank) {
            null
        } else {
            voskResult
        }
    }

    /** 破棄する */
    fun destroy() {
        model?.close()
        recognizer?.close()
    }

    /** [recognizeFromSpeechPcm]の返り値 */
    sealed interface VoskResult {
        val isBlank: Boolean
            get() = when (this) {
                is Partial -> partial.isBlank()
                is Result -> text.isBlank()
            }

        /** 確定した文章 */
        @JvmInline
        value class Result(val text: String) : VoskResult

        /** 部分的に確定した文章 */
        @JvmInline
        value class Partial(val partial: String) : VoskResult
    }

    companion object {
        /** Vosk で受け付けるサンプリングレート */
        const val SAMPLING_RATE = 16000
    }
}