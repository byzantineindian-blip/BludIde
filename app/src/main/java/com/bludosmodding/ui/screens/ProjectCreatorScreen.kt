package com.bludosmodding.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bludosmodding.ui.viewmodels.ProjectCreatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCreatorScreen(
    onProjectCreated: (String) -> Unit,
    viewModel: ProjectCreatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.creationResult) {
        uiState.creationResult?.onSuccess {
            onProjectCreated(it.absolutePath)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create New Mod", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.createProject() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Create, contentDescription = null) },
                text = { Text("Create Project") }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Project Details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = uiState.modName,
                    onValueChange = { viewModel.onModNameChanged(it) },
                    label = { Text("Mod Name") },
                    placeholder = { Text("My Awesome Mod") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = uiState.packageName,
                    onValueChange = { viewModel.onPackageNameChanged(it) },
                    label = { Text("Package Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = uiState.modId,
                    onValueChange = { viewModel.onModIdChanged(it) },
                    label = { Text("Mod ID") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.modVersion,
                        onValueChange = { viewModel.onModVersionChanged(it) },
                        label = { Text("Version") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = uiState.modLicense,
                        onValueChange = { viewModel.onModLicenseChanged(it) },
                        label = { Text("License") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                }

                OutlinedTextField(
                    value = uiState.modOwner,
                    onValueChange = { viewModel.onModOwnerChanged(it) },
                    label = { Text("Mod Owner") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Environment", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                LoaderDropdown(
                    selectedLoader = uiState.selectedLoader,
                    onLoaderSelected = { viewModel.onLoaderChanged(it) }
                )

                VersionDropdown(
                    label = "Minecraft Version",
                    selectedVersion = uiState.selectedMcVersion,
                    versions = uiState.mcVersions,
                    onVersionSelected = { viewModel.onMcVersionChanged(it) }
                )

                VersionDropdown(
                    label = "${uiState.selectedLoader} Version",
                    selectedVersion = uiState.selectedLoaderVersion,
                    versions = uiState.loaderVersions,
                    onVersionSelected = { viewModel.onLoaderVersionChanged(it) }
                )
                
                uiState.creationResult?.onFailure {
                    Text(
                        text = "Error: ${it.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoaderDropdown(
    selectedLoader: String,
    onLoaderSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val loaders = listOf("Fabric", "Forge")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLoader,
            onValueChange = {},
            readOnly = true,
            label = { Text("Mod Loader") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            loaders.forEach { loader ->
                DropdownMenuItem(
                    text = { Text(loader) },
                    onClick = {
                        onLoaderSelected(loader)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionDropdown(
    label: String,
    selectedVersion: String,
    versions: List<String>,
    onVersionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedVersion,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            versions.forEach { version ->
                DropdownMenuItem(
                    text = { Text(version) },
                    onClick = {
                        onVersionSelected(version)
                        expanded = false
                    }
                )
            }
        }
    }
}
