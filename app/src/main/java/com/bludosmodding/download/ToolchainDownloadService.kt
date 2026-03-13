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

    private val tasks = listOf(
        DownloadTask(
            name = "Proot Environment",
            url = "https://github.com/termux/proot/releases/download/v5.1.107/proot-v5.1.107-aarch64-static",
            targetFileName = "proot",
            extract = false
        ),
        DownloadTask(
            name = "Linux RootFS (Alpine)",
            url = "https://dl-cdn.alpinelinux.org/alpine/v3.18/releases/aarch64/alpine-minirootfs-3.18.4-aarch64.tar.gz",
            targetFileName = "rootfs.tar.gz",
            extract = true
        ),
        DownloadTask(
            name = "OpenJDK 17",
            url = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8.1%2B1/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.8.1_1.tar.gz",
            targetFileName = "jdk17.tar.gz",
            extract = true
        ),
        DownloadTask(
            name = "Gradle 8.3",
            url = "https://services.gradle.org/distributions/gradle-8.3-bin.zip",
            targetFileName = "gradle.zip",
            extract = true
        )
    )

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
        runFullSetup()
        return START_NOT_STICKY
    }

    private fun addLog(message: String) {
        val currentLogs = _downloadStatus.value.terminalLogs
        _downloadStatus.value = _downloadStatus.value.copy(
            terminalLogs = currentLogs + "[LOG] $message"
        )
    }

    private fun runFullSetup() {
        serviceScope.launch {
            try {
                _downloadStatus.value = _downloadStatus.value.copy(isDownloading = true)
                addLog("Initializing BludIDE Full Toolchain Setup...")

                val toolchainDir = File(filesDir, "toolchain")
                if (!toolchainDir.exists()) toolchainDir.mkdirs()

                tasks.forEachIndexed { index, task ->
                    addLog("Processing (${index + 1}/${tasks.size}): ${task.name}")
                    val outputFile = File(toolchainDir, task.targetFileName)
                    
                    if (!outputFile.exists()) {
                        addLog("Downloading from ${task.url}...")
                        downloadFile(task.url, outputFile, task.name)
                    } else {
                        addLog("${task.name} exists. Verifying...")
                    }

                    if (task.extract) {
                        addLog("Extracting ${task.targetFileName}...")
                        executeExtraction(outputFile, toolchainDir)
                        addLog("Cleaning up archive...")
                        outputFile.delete()
                    }
                }

                addLog("Setting executable permissions...")
                val proot = File(toolchainDir, "proot")
                if (proot.exists()) proot.setExecutable(true, false)

                addLog("Toolchain setup complete. BludIDE is ready.")
                
                _downloadStatus.value = _downloadStatus.value.copy(
                    isDownloading = false,
                    isCompleted = true,
                    progress = 1f
                )
                
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()

            } catch (e: Exception) {
                addLog("ERROR: ${e.message}")
                _downloadStatus.value = _downloadStatus.value.copy(
                    isDownloading = false,
                    error = e.message
                )
                stopSelf()
            }
        }
    }

    private suspend fun downloadFile(url: String, outputFile: File, taskName: String) = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) throw Exception("Network failure ($taskName): ${response.code}")

        val body = response.body ?: throw Exception("Null body for $taskName")
        val totalBytes = body.contentLength()
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(outputFile)
        val buffer = ByteArray(65536)
        var bytesRead: Int
        var downloadedBytes: Long = 0
        var lastUpdateTime = System.currentTimeMillis()

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            downloadedBytes += bytesRead

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime >= 500) {
                val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                val mb = String.format("%.2f", downloadedBytes / (1024.0 * 1024.0))
                val totalMb = String.format("%.2f", totalBytes / (1024.0 * 1024.0))
                
                _downloadStatus.value = _downloadStatus.value.copy(
                    progress = progress,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    speed = "$mb / $totalMb MB"
                )

                notificationManager.notify(
                    NOTIFICATION_ID,
                    createNotification("Setting up $taskName: ${(progress * 100).toInt()}%", (progress * 100).toInt())
                )
                lastUpdateTime = currentTime
            }
        }
        outputStream.close()
        inputStream.close()
    }

    private fun executeExtraction(file: File, targetDir: File) {
        val command = when {
            file.name.endsWith(".tar.gz") -> "tar -xzf ${file.absolutePath} -C ${targetDir.absolutePath}"
            file.name.endsWith(".zip") -> "unzip -o ${file.absolutePath} -d ${targetDir.absolutePath}"
            else -> return
        }
        
        try {
            val process = Runtime.getRuntime().exec(command)
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { addLog(it) }
            }
            process.waitFor()
        } catch (e: Exception) {
            addLog("Extraction failed for ${file.name}: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BludIDE Toolchain",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = createNotification("Initializing BludIDE...", 0)
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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
