package io.github.takusan23.hiroid.tool

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object VoskModelTool {

    private const val MODEL_FOLDER = "model"

    /**
     * モデル一覧を返す
     *
     * @param context [Context]
     * @return ファイル
     */
    suspend fun getVoskModelList(context: Context): List<File> {
        return withContext(Dispatchers.IO) {
            context.getExternalFilesDir(null)!!
                .resolve(MODEL_FOLDER)
                .listFiles()
                ?.filter {
                    // Vosk であるためには、以下のフォルダが存在しているはず
                    val modelFileList = it.listFiles()?.map { it.name } ?: emptyList()
                    "am" in modelFileList && "conf" in modelFileList && "graph" in modelFileList && "ivector" in modelFileList
                }
        } ?: emptyList()
    }

    /**
     * モデルを追加する
     * InputStream なのは、Storage Access Framework でモデルのファイルを選ぶため
     *
     * @param context [Context]
     * @param inputStream [inputStream]
     */
    suspend fun addVoskModel(context: Context, inputStream: InputStream) {
        withContext(Dispatchers.IO) {
            val outputFolder = context.getExternalFilesDir(null)!!.resolve(MODEL_FOLDER)
            ZipInputStream(inputStream.buffered()).use { zipInputStream ->
                while (true) {
                    yield()
                    // もうない場合は break
                    val zipEntry = zipInputStream.nextEntry ?: break
                    val entryName = zipEntry.name
                    val unzipPath = outputFolder.resolve(entryName)
                    if (zipEntry.isDirectory) {
                        // フォルダの場合
                        unzipPath.mkdirs()
                    } else {
                        // ファイルの場合
                        unzipPath.outputStream().buffered().use { outputStream ->
                            zipInputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        }
    }


    /**
     * モデルを削除する
     *
     * @param context [Context]
     * @param folderName モデルのフォルダ名
     */
    suspend fun removeModel(context: Context, folderName: String) {
        withContext(Dispatchers.IO) {
            context.getExternalFilesDir(null)!!.resolve(MODEL_FOLDER).resolve(folderName).deleteRecursively()
        }
    }

}