package com.bludosmodding.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bludosmodding.download.DownloadStatus
import com.bludosmodding.download.DownloadViewModel

@Composable
fun OnboardingScreen(
    onDownloadComplete: () -> Unit,
    viewModel: DownloadViewModel = viewModel()
) {
    val context = LocalContext.current
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    
    var storagePermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                // Simplified for this demo, usually check READ/WRITE_EXTERNAL_STORAGE
                true 
            }
        )
    }

    var notificationsPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                false 
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        notificationsPermissionGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
             storagePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        }
    }

    LaunchedEffect(downloadStatus.isCompleted) {
        if (downloadStatus.isCompleted) {
            onDownloadComplete()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .blur(20.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                text = "BludIDE",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Professional Toolchain Setup",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            val allPermissionsGranted = notificationsPermissionGranted && storagePermissionGranted

            if (!allPermissionsGranted) {
                PermissionRequestContent(
                    storageGranted = storagePermissionGranted,
                    notificationsGranted = notificationsPermissionGranted,
                    onRequestNotifications = {
                        val perms = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                    },
                    onRequestStorage = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:\${context.packageName}")
                            context.startActivity(intent)
                        }
                    },
                    onStartDownload = {
                        viewModel.startDownload()
                    }
                )
            } else {
                DownloadProgressContent(downloadStatus)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TerminalLogView(
                    logs = downloadStatus.terminalLogs,
                    modifier = Modifier.weight(1f)
                )

                if (!downloadStatus.isDownloading && !downloadStatus.isCompleted && downloadStatus.error == null) {
                    Button(
                        onClick = { viewModel.startDownload() },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("Start Toolchain Setup")
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRequestContent(
    storageGranted: Boolean,
    notificationsGranted: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestStorage: () -> Unit,
    onStartDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Setup Requirements",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            PermissionItem("Notifications", notificationsGranted, onRequestNotifications)
            Spacer(modifier = Modifier.height(8.dp))
            PermissionItem("All Files Access", storageGranted, onRequestStorage)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onStartDownload,
                enabled = storageGranted && notificationsGranted,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Toolchain Download")
            }
        }
    }
}

@Composable
fun PermissionItem(name: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontSize = 16.sp)
        if (granted) {
            Text("Granted", color = Color.Green, fontWeight = FontWeight.Bold)
        } else {
            Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("Grant", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DownloadProgressContent(status: DownloadStatus) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { status.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = status.speed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "\${(status.progress * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (status.error != null) {
            Text(
                text = "Error: \${status.error}",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TerminalLogView(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            scrollState.animateScrollToItem(logs.size - 1)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = Color(0xFF1E1E1E) // Dark terminal color
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.padding(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                Text(
                    text = "--- TOOLCHAIN LOGS ---",
                    color = Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(logs) { log ->
                Text(
                    text = log,
                    color = if (log.contains("ERROR") || log.contains("FATAL")) Color.Red else Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
