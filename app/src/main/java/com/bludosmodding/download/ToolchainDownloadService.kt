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
        startForegroundService()
        performSetup()
        return START_NOT_STICKY
    }

    private fun addLog(message: String) {
        val currentLogs = _downloadStatus.value.terminalLogs
        _downloadStatus.value = _downloadStatus.value.copy(
            terminalLogs = currentLogs + "[${System.currentTimeMillis()}] $message"
        )
    }

    private fun performSetup() {
        serviceScope.launch {
            try {
                _downloadStatus.value = _downloadStatus.value.copy(isDownloading = true)
                addLog("Starting BludIDE Toolchain Setup...")
                
                // Step 1: Initialize Directories
                val toolchainDir = File(filesDir, "toolchain")
                if (!toolchainDir.exists()) {
                    addLog("Creating toolchain directory...")
                    toolchainDir.mkdirs()
                }

                // Step 2: Download Toolchain (Using OkHttp for progress tracking)
                // Note: In a real scenario, this URL would be a real toolchain mirror.
                val url = "https://example.com/toolchain.zip" 
                addLog("Downloading toolchain components from $url...")
                
                downloadFile(url, File(toolchainDir, "toolchain.zip"))

                // Step 3: Extract and Update via Terminal (Shell)
                addLog("Extracting toolchain...")
                executeShellCommand("unzip -o ${toolchainDir.absolutePath}/toolchain.zip -d ${toolchainDir.absolutePath}")

                addLog("Updating toolchain environment...")
                executeShellCommand("chmod +x ${toolchainDir.absolutePath}/bin/*")
                
                addLog("Environment setup complete.")
                
                _downloadStatus.value = _downloadStatus.value.copy(
                    isDownloading = false,
                    isCompleted = true,
                    progress = 1f
                )
                
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()

            } catch (e: Exception) {
                addLog("FATAL ERROR: ${e.message}")
                _downloadStatus.value = _downloadStatus.value.copy(
                    isDownloading = false,
                    error = e.message
                )
                stopSelf()
            }
        }
    }

    private suspend fun downloadFile(url: String, outputFile: File) = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorMsg = "Server returned code ${response.code}. Make sure your internet is stable."
            addLog(errorMsg)
            throw Exception(errorMsg)
        }

        val body = response.body ?: throw Exception("Response body is null")
        val totalBytes = body.contentLength()
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(outputFile)
        val buffer = ByteArray(16384)
        var bytesRead: Int
        var downloadedBytes: Long = 0
        var lastUpdateTime = System.currentTimeMillis()

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            downloadedBytes += bytesRead

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime >= 300) {
                val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                val speed = calculateSpeed(downloadedBytes, currentTime - lastUpdateTime) // Simplistic speed
                
                _downloadStatus.value = _downloadStatus.value.copy(
                    progress = progress,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    speed = speed
                )

                notificationManager.notify(
                    NOTIFICATION_ID,
                    createNotification("Downloading toolchain: ${(progress * 100).toInt()}%", (progress * 100).toInt())
                )
                lastUpdateTime = currentTime
            }
        }
        outputStream.close()
        inputStream.close()
    }

    private fun executeShellCommand(command: String) {
        addLog("> $command")
        try {
            val process = Runtime.getRuntime().exec(command)
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { addLog(it) }
            }
            process.waitFor()
        } catch (e: Exception) {
            addLog("Shell Error: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Toolchain Setup",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = createNotification("Initializing setup...", 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(content: String, progress: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BludIDE Setup")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun calculateSpeed(totalDownloaded: Long, timeDiff: Long): String {
        // Dummy implementation for speed display
        return "Streaming..."
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
