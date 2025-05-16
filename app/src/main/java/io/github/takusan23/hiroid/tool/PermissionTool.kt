package io.github.takusan23.hiroid.tool

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionTool {

    fun isAllGranted(context: Context): Boolean {
        return isAudioPermissionGranted(context) && isOverlayWindowPermissionGranted(context)
    }

    fun isAudioPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun isOverlayWindowPermissionGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

}