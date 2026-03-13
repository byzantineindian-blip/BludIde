package com.bludosmodding.download

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bludosmodding.MainActivity
import com.bludosmodding.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ToolchainDownloadService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _downloadStatus = MutableStateFlow(DownloadStatus())
    val downloadStatus = _downloadStatus.asStateFlow()

    private lateinit var notificationManager: NotificationManager
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "toolchain_download_channel"

    inner class LocalBinder : Binder() {
        fun getService(): ToolchainDownloadService = this@ToolchainDownloadService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = "https://example.com/toolchain.zip" // Placeholder as requested
        startForegroundService()
        downloadToolchain(url)
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Toolchain Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of the toolchain download"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = createNotification("Initializing download...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(content: String, progress: Int = 0): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading Toolchain")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }

    private fun downloadToolchain(url: String) {
        serviceScope.launch {
            try {
                _downloadStatus.value = DownloadStatus(isDownloading = true)
                
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) throw Exception("Failed to download file: ${response.code}")

                val body = response.body ?: throw Exception("Response body is null")
                val totalBytes = body.contentLength()
                val toolchainDir = File(filesDir, "toolchain")
                if (!toolchainDir.exists()) toolchainDir.mkdirs()
                val outputFile = File(toolchainDir, "toolchain.zip")

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes: Long = 0
                var lastUpdateTime = System.currentTimeMillis()
                var bytesSinceLastUpdate: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceLastUpdate += bytesRead

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime >= 500) {
                        val progress = (downloadedBytes.toFloat() / totalBytes) * 100
                        val speed = calculateSpeed(bytesSinceLastUpdate, currentTime - lastUpdateTime)
                        
                        _downloadStatus.value = _downloadStatus.value.copy(
                            progress = progress / 100f,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            speed = speed
                        )

                        notificationManager.notify(
                            NOTIFICATION_ID,
                            createNotification("Downloading: ${progress.toInt()}% ($speed)", progress.toInt())
                        )

                        lastUpdateTime = currentTime
                        bytesSinceLastUpdate = 0
                    }
                }

                outputStream.close()
                inputStream.close()

                _downloadStatus.value = _downloadStatus.value.copy(
                    isDownloading = false,
                    isCompleted = true,
                    progress = 1f
                )
                
                notificationManager.notify(
                    NOTIFICATION_ID,
                    createNotification("Download Complete", 100)
                )
                
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()

            } catch (e: Exception) {
                _downloadStatus.value = _downloadStatus.value.copy(
                    isDownloading = false,
                    error = e.message
                )
                notificationManager.notify(
                    NOTIFICATION_ID,
                    createNotification("Download Failed: ${e.message}", 0)
                )
                stopSelf()
            }
        }
    }

    private fun calculateSpeed(bytes: Long, durationMs: Long): String {
        if (durationMs <= 0) return "0 KB/s"
        val speedBps = (bytes.toDouble() / durationMs) * 1000
        return when {
            speedBps >= 1024 * 1024 -> String.format("%.2f MB/s", speedBps / (1024 * 1024))
            speedBps >= 1024 -> String.format("%.2f KB/s", speedBps / 1024)
            else -> String.format("%.0f B/s", speedBps)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
