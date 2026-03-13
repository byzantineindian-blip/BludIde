package com.bludosmodding.ide

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class ProotExecutor(private val context: Context) {

    fun executeGradle(projectPath: String, task: String): Flow<String> = flow {
        try {
            val toolchainDir = File(context.filesDir, "toolchain")
            val prootBinary = File(toolchainDir, "proot")
            val rootfsDir = File(toolchainDir, "rootfs")

            if (!prootBinary.exists()) {
                emit("Error: proot binary not found at ${prootBinary.absolutePath}")
                return@flow
            }

            if (!prootBinary.canExecute()) {
                prootBinary.setExecutable(true)
            }

            val processBuilder = ProcessBuilder().apply {
                command(
                    prootBinary.absolutePath,
                    "-r", rootfsDir.absolutePath,
                    "-b", "/dev",
                    "-b", "/proc",
                    "-b", "$projectPath:/project",
                    "-w", "/project",
                    "/bin/sh", "gradlew", task
                )
                directory(File(projectPath))
                redirectErrorStream(true)
            }

            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var line: String? = reader.readLine()
            while (line != null) {
                emit(line!!)
                line = reader.readLine()
            }

            val exitCode = process.waitFor()
            emit("Process exited with code $exitCode")

        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}
