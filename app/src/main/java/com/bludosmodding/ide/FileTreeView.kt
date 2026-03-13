package com.bludosmodding.ide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun FileTreeView(
    fileTree: List<FileNode>,
    onFileSelected: (File) -> Unit,
    onToggleDirectory: (FileNode.Directory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxHeight()) {
        items(fileTree) { node ->
            FileNodeItem(node, 0, onFileSelected, onToggleDirectory)
        }
    }
}

@Composable
fun FileNodeItem(
    node: FileNode,
    depth: Int,
    onFileSelected: (File) -> Unit,
    onToggleDirectory: (FileNode.Directory) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node is FileNode.Directory) {
                        onToggleDirectory(node)
                    } else {
                        onFileSelected(node.file)
                    }
                }
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .padding(start = (depth * 16).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon: ImageVector = when (node) {
                is FileNode.Directory -> if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight
                is FileNode.FileItem -> Icons.Default.Description
            }
            
            if (node is FileNode.Directory) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.width(24.dp))
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = node.file.name,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (node is FileNode.Directory && node.isExpanded) {
            node.children.forEach { child ->
                FileNodeItem(child, depth + 1, onFileSelected, onToggleDirectory)
            }
        }
    }
}
