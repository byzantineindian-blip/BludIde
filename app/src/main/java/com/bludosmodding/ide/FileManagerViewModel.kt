package com.bludosmodding.ide

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed class FileNode(val file: File) {
    class Directory(file: File, val children: List<FileNode>, var isExpanded: Boolean = false) : FileNode(file)
    class FileItem(file: File) : FileNode(file)
}

class FileManagerViewModel : ViewModel() {
    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree = _fileTree.asStateFlow()

    private val _selectedFile = MutableStateFlow<File?>(null)
    val selectedFile = _selectedFile.asStateFlow()

    private val _fileContent = MutableStateFlow("")
    val fileContent = _fileContent.asStateFlow()

    fun loadProject(projectPath: String) {
        val root = File(projectPath)
        if (root.exists() && root.isDirectory) {
            _fileTree.value = listOf(buildTree(root))
        }
    }

    private fun buildTree(file: File): FileNode {
        return if (file.isDirectory) {
            val children = file.listFiles()?.map { buildTree(it) }?.sortedWith(
                compareBy<FileNode> { it is FileNode.FileItem }.thenBy { it.file.name }
            ) ?: emptyList()
            FileNode.Directory(file, children)
        } else {
            FileNode.FileItem(file)
        }
    }

    fun selectFile(file: File) {
        if (file.isFile) {
            _selectedFile.value = file
            _fileContent.value = try {
                file.readText()
            } catch (e: Exception) {
                "Error reading file: ${e.message}"
            }
        }
    }

    fun updateFileContent(content: String) {
        _fileContent.value = content
    }

    fun saveCurrentFile() {
        try {
            _selectedFile.value?.writeText(_fileContent.value)
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun toggleDirectory(directory: FileNode.Directory) {
        directory.isExpanded = !directory.isExpanded
        _fileTree.value = _fileTree.value.toList() // Trigger state update
    }
}
