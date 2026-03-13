package com.bludosmodding.generator

import android.os.Environment
import java.io.File

data class ProjectConfig(
    val name: String,
    val packageName: String,
    val modId: String,
    val version: String,
    val owner: String,
    val license: String,
    val loader: String, // "Fabric" or "Forge"
    val minecraftVersion: String,
    val loaderVersion: String
)

object ModProjectGenerator {

    fun generate(config: ProjectConfig): Result<File> {
        return try {
            val modsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Mods"
            )
            val projectDir = File(modsDir, config.name)
            
            if (projectDir.exists()) {
                return Result.failure(Exception("Project directory already exists"))
            }

            projectDir.mkdirs()

            // 1. Create directory structure
            val packagePath = config.packageName.replace(".", "/")
            val javaDir = File(projectDir, "src/main/java/$packagePath")
            val resourcesDir = File(projectDir, "src/main/resources")
            
            javaDir.mkdirs()
            resourcesDir.mkdirs()

            // 2. Write build.gradle
            writeBuildGradle(projectDir, config)

            // 3. Write gradle.properties
            writeGradleProperties(projectDir, config)

            // 4. Write settings.gradle
            writeSettingsGradle(projectDir, config)

            // 5. Write Mod Loader specific files
            if (config.loader == "Fabric") {
                writeFabricModJson(resourcesDir, config)
                writeFabricMainClass(javaDir, config)
            } else {
                writeForgeModsToml(resourcesDir, config)
                writeForgeMainClass(javaDir, config)
            }

            Result.success(projectDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeBuildGradle(dir: File, config: ProjectConfig) {
        val template = if (config.loader == "Fabric") {
            """
            plugins {
                id 'fabric-loom' version '1.7-SNAPSHOT'
                id 'maven-publish'
            }

            version = project.mod_version
            group = project.maven_group

            repositories {
                // Add repositories here
            }

            dependencies {
                minecraft "com.mojang:minecraft:${config.minecraftVersion}"
                mappings "net.fabricmc:yarn:${config.minecraftVersion}+build.1:v2"
                modImplementation "net.fabricmc:fabric-loader:${config.loaderVersion}"
            }

            processResources {
                inputs.property "version", project.version
                filteringCharset "UTF-8"

                filesMatching("fabric.mod.json") {
                    expand "version": project.version
                }
            }

            tasks.withType(JavaCompile).configureEach {
                it.options.release = 17
            }

            java {
                withSourcesJar()
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            """.trimIndent()
        } else {
            """
            buildscript {
                repositories {
                    maven { url = 'https://maven.minecraftforge.net' }
                    mavenCentral()
                }
                dependencies {
                    classpath group: 'net.minecraftforge.gradle', name: 'ForgeGradle', version: '6.0.+', changing: true
                }
            }
            apply plugin: 'net.minecraftforge.gradle'

            group = '${config.packageName}'
            version = '${config.version}'

            minecraft {
                mappings channel: 'official', version: '${config.minecraftVersion}'
            }

            dependencies {
                minecraft 'net.minecraftforge:forge:${config.minecraftVersion}-${config.loaderVersion}'
            }
            """.trimIndent()
        }
        File(dir, "build.gradle").writeText(template)
    }

    private fun writeGradleProperties(dir: File, config: ProjectConfig) {
        val content = """
            org.gradle.jvmargs=-Xmx2G
            org.gradle.parallel=true
            
            mod_version=${config.version}
            maven_group=${config.packageName}
            archives_base_name=${config.modId}
            
            minecraft_version=${config.minecraftVersion}
            loader_version=${config.loaderVersion}
        """.trimIndent()
        File(dir, "gradle.properties").writeText(content)
    }

    private fun writeSettingsGradle(dir: File, config: ProjectConfig) {
        File(dir, "settings.gradle").writeText("rootProject.name = '${config.name}'")
    }

    private fun writeFabricModJson(resourcesDir: File, config: ProjectConfig) {
        val content = """
            {
              "schemaVersion": 1,
              "id": "${config.modId}",
              "version": "${"$"}{version}",
              "name": "${config.name}",
              "description": "A Minecraft mod created with BludIDE",
              "authors": ["${config.owner}"],
              "contact": {},
              "license": "${config.license}",
              "environment": "*",
              "entrypoints": {
                "main": ["${config.packageName}.${config.name}"]
              },
              "depends": {
                "fabricloader": ">=${config.loaderVersion}",
                "minecraft": "~${config.minecraftVersion}"
              }
            }
        """.trimIndent()
        File(resourcesDir, "fabric.mod.json").writeText(content)
    }

    private fun writeForgeModsToml(resourcesDir: File, config: ProjectConfig) {
        val metaInf = File(resourcesDir, "META-INF")
        metaInf.mkdirs()
        val content = """
            modLoader="javafml"
            loaderVersion="[${config.loaderVersion.split(".")[0]},)"
            license="${config.license}"

            [[mods]]
            modId="${config.modId}"
            version="${config.version}"
            displayName="${config.name}"
            authors="${config.owner}"
            description='''
            A Minecraft mod created with BludIDE
            '''
        """.trimIndent()
        File(metaInf, "mods.toml").writeText(content)
    }

    private fun writeFabricMainClass(javaDir: File, config: ProjectConfig) {
        val content = """
            package ${config.packageName};

            import net.fabricmc.api.ModInitializer;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            public class ${config.name} implements ModInitializer {
                public static final String MOD_ID = "${config.modId}";
                public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

                @Override
                public void onInitialize() {
                    LOGGER.info("Hello from ${config.name}!");
                }
            }
        """.trimIndent()
        File(javaDir, "${config.name}.java").writeText(content)
    }

    private fun writeForgeMainClass(javaDir: File, config: ProjectConfig) {
        val content = """
            package ${config.packageName};

            import net.minecraftforge.fml.common.Mod;
            import org.apache.logging.log4j.LogManager;
            import org.apache.logging.log4j.Logger;

            @Mod("${config.modId}")
            public class ${config.name} {
                private static final Logger LOGGER = LogManager.getLogger();

                public ${config.name}() {
                    LOGGER.info("Hello from ${config.name}!");
                }
            }
        """.trimIndent()
        File(javaDir, "${config.name}.java").writeText(content)
    }
}
