# BludIDE

**BludIDE** is a powerful, mobile-first Integrated Development Environment (IDE) specifically designed for creating, editing, and compiling Minecraft Java Edition mods (Forge and Fabric) directly on Android.

## 🚀 Features

- **Toolchain Downloader**: Automated, high-speed background download of JDK, Gradle, and a proot Linux environment (>1GB).
- **Project Creator**: Dynamic project generation for Fabric and Forge using official metadata APIs.
- **Advanced Code Editor**: Syntax highlighting for Java and Kotlin with a fluid, Material You (M3) interface.
- **Integrated Terminal**: Powered by `proot`, allowing real-time Gradle builds (`./gradlew build`) within a sandboxed Linux environment.
- **Material You Design**: Full support for dynamic theming and edge-to-edge UI on Android 10 to Android 15+.

## 🛠 Tech Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM + StateFlow/Flow
- **Networking**: Retrofit & OkHttp
- **Background Tasks**: WorkManager & Foreground Services
- **Environment**: Proot-based Linux sandbox for Gradle execution

## 📂 Project Structure

- `app/src/main/java/com/bludosmodding/download`: Service and logic for toolchain setup.
- `app/src/main/java/com/bludosmodding/generator`: Logic for generating mod project templates.
- `app/src/main/java/com/bludosmodding/ide`: The workspace UI, File Tree, Editor, and Terminal engine.
- `app/src/main/java/com/bludosmodding/network`: API clients for Fabric/Forge metadata.

## 🏗 Build Instructions

1. Clone the repository.
2. Open in Android Studio (Ladybug or newer).
3. Ensure you have JDK 17+ configured.
4. Sync Gradle and run the `:app:assembleDebug` task.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
