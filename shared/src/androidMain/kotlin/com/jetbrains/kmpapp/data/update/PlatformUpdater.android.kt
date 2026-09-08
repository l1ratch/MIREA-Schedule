package com.jetbrains.kmpapp.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.jetbrains.kmpapp.data.storage.AndroidContextProvider
import java.io.File

actual fun startPlatformUpdate(browserUrl: String, apkUrl: String?) {
    val context = AndroidContextProvider.context ?: return
    val downloadUrl = apkUrl ?: browserUrl
    if (apkUrl == null) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }

    val request = DownloadManager.Request(Uri.parse(downloadUrl))
        .setTitle("Обновление MIREA Schedule")
        .setDescription("Скачивание новой версии")
        .setMimeType("application/vnd.android.package-archive")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "mirea-schedule-update.apk")

    val downloadId = (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
    context.registerReceiver(
        UpdateDownloadReceiver(downloadId),
        android.content.IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
        Context.RECEIVER_NOT_EXPORTED
    )
}

private class UpdateDownloadReceiver(private val expectedId: Long) : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != expectedId) return
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = manager.getUriForDownloadedFile(expectedId) ?: return
        val installUri = manager.query(DownloadManager.Query().setFilterById(expectedId)).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val path = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME))
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
            } else {
                uri
            }
        }
        val installIntent = Intent(Intent.ACTION_VIEW, installUri)
            .setDataAndType(installUri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(installIntent)
        runCatching { context.unregisterReceiver(this) }
    }
}
