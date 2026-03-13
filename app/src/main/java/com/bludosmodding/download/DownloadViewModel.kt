package com.bludosmodding.download

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _downloadStatus = MutableStateFlow(DownloadStatus())
    val downloadStatus = _downloadStatus.asStateFlow()

    private var downloadService: ToolchainDownloadService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ToolchainDownloadService.LocalBinder
            downloadService = binder.getService()
            isBound = true
            
            // Observe the flow from the service
            viewModelScope.launch {
                downloadService?.downloadStatus?.collectLatest { status ->
                    _downloadStatus.value = status
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            downloadService = null
        }
    }

    fun startDownload() {
        val context = getApplication<Application>()
        val intent = Intent(context, ToolchainDownloadService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun bindToService() {
        val context = getApplication<Application>()
        val intent = Intent(context, ToolchainDownloadService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }
}
