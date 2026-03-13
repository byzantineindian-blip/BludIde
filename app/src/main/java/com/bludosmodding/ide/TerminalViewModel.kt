package com.bludosmodding.ide

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val _terminalLines = MutableStateFlow<List<String>>(emptyList())
    val terminalLines = _terminalLines.asStateFlow()

    private val prootExecutor = ProotExecutor(application)

    fun runBuild(projectPath: String, task: String = "build") {
        _terminalLines.value = listOf("Starting Gradle task: $task...")
        viewModelScope.launch {
            prootExecutor.executeGradle(projectPath, task).collect { line ->
                _terminalLines.value = _terminalLines.value + line
            }
        }
    }

    fun clearTerminal() {
        _terminalLines.value = emptyList()
    }
}
