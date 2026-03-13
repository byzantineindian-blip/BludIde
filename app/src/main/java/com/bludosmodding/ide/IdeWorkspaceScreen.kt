package com.bludosmodding.ide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeWorkspaceScreen(
    projectPath: String,
    onBackPressed: () -> Unit,
    fileManagerViewModel: FileManagerViewModel = viewModel(),
    terminalViewModel: TerminalViewModel = viewModel()
) {
    val fileTree by fileManagerViewModel.fileTree.collectAsState()
    val selectedFile by fileManagerViewModel.selectedFile.collectAsState()
    val fileContent by fileManagerViewModel.fileContent.collectAsState()
    val terminalLines by terminalViewModel.terminalLines.collectAsState()
    
    var isFileTreeVisible by remember { mutableStateOf(true) }
    var isTerminalVisible by remember { mutableStateOf(false) }

    LaunchedEffect(projectPath) {
        fileManagerViewModel.loadProject(projectPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = selectedFile?.name ?: "BludIDE",
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { fileManagerViewModel.saveCurrentFile() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { 
                        isTerminalVisible = true
                        terminalViewModel.runBuild(projectPath, "build") 
                    }) {
                        Icon(Icons.Default.Build, contentDescription = "Build")
                    }
                    IconButton(onClick = { isFileTreeVisible = !isFileTreeVisible }) {
                        Icon(
                            if (isFileTreeVisible) Icons.Default.MenuOpen else Icons.Default.Menu,
                            contentDescription = "Toggle File Tree"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // File Tree Side Pane
                AnimatedVisibility(visible = isFileTreeVisible) {
                    Surface(
                        modifier = Modifier.width(250.dp).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        FileTreeView(
                            fileTree = fileTree,
                            onFileSelected = { fileManagerViewModel.selectFile(it) },
                            onToggleDirectory = { fileManagerViewModel.toggleDirectory(it) }
                        )
                    }
                }

                // Editor Pane
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (selectedFile != null) {
                        CodeEditorView(
                            content = fileContent,
                            onContentChange = { fileManagerViewModel.updateFileContent(it) }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select a file to start editing", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Terminal View
            AnimatedVisibility(visible = isTerminalVisible) {
                TerminalView(
                    lines = terminalLines,
                    onClose = { isTerminalVisible = false },
                    onClear = { terminalViewModel.clearTerminal() },
                    modifier = Modifier.height(250.dp).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TerminalView(
    lines: List<String>,
    onClose: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            scrollState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF1E1E1E)) // Dark terminal background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Terminal", color = Color.White, style = MaterialTheme.typography.labelMedium)
            Row {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            items(lines) { line ->
                Text(
                    text = line,
                    color = if (line.startsWith("Error")) Color.Red else Color.LightGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
