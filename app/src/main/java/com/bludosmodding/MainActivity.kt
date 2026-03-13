package com.bludosmodding

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bludosmodding.ide.IdeWorkspaceScreen
import com.bludosmodding.ui.screens.OnboardingScreen
import com.bludosmodding.ui.screens.ProjectCreatorScreen
import com.bludosmodding.ui.theme.BludIdeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BludIdeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
                    val context = LocalContext.current
                    
                    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                        when (screen) {
                            Screen.Onboarding -> OnboardingScreen(
                                onDownloadComplete = {
                                    currentScreen = Screen.ProjectCreator
                                }
                            )
                            Screen.ProjectCreator -> ProjectCreatorScreen(
                                onProjectCreated = { path ->
                                    Toast.makeText(context, "Project created", Toast.LENGTH_SHORT).show()
                                    currentScreen = Screen.Editor(path)
                                }
                            )
                            is Screen.Editor -> IdeWorkspaceScreen(
                                projectPath = screen.projectPath,
                                onBackPressed = { currentScreen = Screen.ProjectCreator }
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen {
    data object Onboarding : Screen()
    data object ProjectCreator : Screen()
    data class Editor(val projectPath: String) : Screen()
}
