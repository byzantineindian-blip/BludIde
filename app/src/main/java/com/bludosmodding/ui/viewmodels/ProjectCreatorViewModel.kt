package com.bludosmodding.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bludosmodding.generator.ModProjectGenerator
import com.bludosmodding.generator.ProjectConfig
import com.bludosmodding.network.FabricApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ProjectCreatorState(
    val modName: String = "",
    val packageName: String = "com.example.mod",
    val modId: String = "example_mod",
    val modVersion: String = "1.0.0",
    val modOwner: String = "",
    val modLicense: String = "MIT",
    val selectedLoader: String = "Fabric",
    val selectedMcVersion: String = "",
    val selectedLoaderVersion: String = "",
    val mcVersions: List<String> = emptyList(),
    val loaderVersions: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val creationResult: Result<File>? = null
)

class ProjectCreatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectCreatorState())
    val uiState = _uiState.asStateFlow()

    private val fabricApi = FabricApi.create()

    init {
        fetchVersions()
    }

    fun fetchVersions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                if (_uiState.value.selectedLoader == "Fabric") {
                    val games = fabricApi.getGameVersions()
                    val loaders = fabricApi.getLoaderVersions()
                    
                    val mcVersions = games.filter { it.stable }.map { it.version }
                    val loaderVersions = loaders.filter { it.stable }.map { it.version }

                    _uiState.value = _uiState.value.copy(
                        mcVersions = mcVersions,
                        loaderVersions = loaderVersions,
                        selectedMcVersion = mcVersions.firstOrNull() ?: "",
                        selectedLoaderVersion = loaderVersions.firstOrNull() ?: "",
                        isLoading = false
                    )
                } else {
                    // Forge fallback/mock
                    val mcVersions = listOf("1.21.1", "1.20.1", "1.19.2", "1.18.2")
                    val forgeVersions = listOf("52.0.0", "47.3.0", "43.3.0", "40.2.0")
                    _uiState.value = _uiState.value.copy(
                        mcVersions = mcVersions,
                        loaderVersions = forgeVersions,
                        selectedMcVersion = mcVersions.first(),
                        selectedLoaderVersion = forgeVersions.first(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onModNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(modName = name)
    }

    fun onPackageNameChanged(pkg: String) {
        _uiState.value = _uiState.value.copy(packageName = pkg)
    }

    fun onModIdChanged(id: String) {
        _uiState.value = _uiState.value.copy(modId = id)
    }

    fun onModVersionChanged(ver: String) {
        _uiState.value = _uiState.value.copy(modVersion = ver)
    }

    fun onModOwnerChanged(owner: String) {
        _uiState.value = _uiState.value.copy(modOwner = owner)
    }

    fun onModLicenseChanged(license: String) {
        _uiState.value = _uiState.value.copy(modLicense = license)
    }

    fun onLoaderChanged(loader: String) {
        _uiState.value = _uiState.value.copy(selectedLoader = loader)
        fetchVersions()
    }

    fun onMcVersionChanged(version: String) {
        _uiState.value = _uiState.value.copy(selectedMcVersion = version)
    }

    fun onLoaderVersionChanged(version: String) {
        _uiState.value = _uiState.value.copy(selectedLoaderVersion = version)
    }

    fun createProject() {
        val currentState = _uiState.value
        val config = ProjectConfig(
            name = currentState.modName,
            packageName = currentState.packageName,
            modId = currentState.modId,
            version = currentState.modVersion,
            owner = currentState.modOwner,
            license = currentState.modLicense,
            loader = currentState.selectedLoader,
            minecraftVersion = currentState.selectedMcVersion,
            loaderVersion = currentState.selectedLoaderVersion
        )
        
        viewModelScope.launch {
            val result = ModProjectGenerator.generate(config)
            _uiState.value = _uiState.value.copy(creationResult = result)
        }
    }
}
