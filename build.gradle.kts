import org.gradle.api.GradleException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.tasks.testing.Test
import java.net.URI
import com.github.gradle.node.NodeExtension
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("org.gradle.test-retry") version "1.6.2"
//    id("io.gitlab.arturbosch.detekt") version "1.23.5"
    id("com.github.node-gradle.node") version "7.1.0"
    `java-library`
    `maven-publish`
}

group = "testing-tools"
version = "0.1.4"

node {
    version.set("20.19.0")
    download.set(true)
    // (optional) nodeProjectDir.set(file("appium-runner"))
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.1")
    implementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    implementation("io.appium:java-client:7.6.0")
    implementation("commons-io:commons-io:2.14.0")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("io.qameta.allure:allure-java-commons:2.28.1")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testImplementation("io.qameta.allure:allure-junit5:2.28.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("io.mockk:mockk:1.13.12")

    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
}

tasks.test {
    // Read all possible Gradle -P properties (if provided)
    val platformProp = (project.findProperty("platform") as String?).orEmpty()
    val appiumUrlProp = (project.findProperty("appium.url") as String?).orEmpty()
    val androidVersionProp = (project.findProperty("android.version") as String?).orEmpty()
    val iosVersionProp = (project.findProperty("ios.version") as String?).orEmpty()
    val androidDeviceNameProp = (project.findProperty("android.device.name") as String?).orEmpty()
    val iosDeviceNameProp = (project.findProperty("ios.device.name") as String?).orEmpty()
    val androidAppNameProp = (project.findProperty("android.app.name") as String?).orEmpty()
    val iosAppNameProp = (project.findProperty("ios.app.name") as String?).orEmpty()
    val appActivityProp = (project.findProperty("app.activity") as String?).orEmpty()
    val appPackageProp = (project.findProperty("app.package") as String?).orEmpty()
    val iosAutoAcceptAlertsProp = (project.findProperty("ios.auto_accept_alerts") as String?).orEmpty()
    val iosAutoDismissAlertsProp = (project.findProperty("ios.auto_dismiss_alerts") as String?).orEmpty()
    val tagProp = (project.findProperty("tag") as String?).orEmpty()
    val androidVideoRecordingEnabledProp = (project.findProperty("android.video.recording.enabled") as String?).orEmpty()
    val iosVideoRecordingEnabledProp = (project.findProperty("ios.video.recording.enabled") as String?).orEmpty()
    val videoRecordingSizeProp = (project.findProperty("video.recording.size") as String?).orEmpty()
    val videoRecordingQualityProp = (project.findProperty("video.recording.quality") as String?).orEmpty()
    val videoRecordingBitrateProp = (project.findProperty("video.recording.bitrate") as String?).orEmpty()
    val videoRecordingOutputDirProp = (project.findProperty("video.recording.output.dir") as String?).orEmpty()
    val screenshotOnSuccessProp = (project.findProperty("screenshot.on.success") as String?).orEmpty()
    val screenshotOnFailureProp = (project.findProperty("screenshot.on.failure") as String?).orEmpty()

    // Configure JUnit Platform
    useJUnitPlatform {
        if (tagProp.isNotBlank()) {
            val tags = tagProp.split(",")
            includeTags(*tags.toTypedArray())
        }
        when (platformProp.lowercase()) {
            "ios" -> excludeTags = mutableSetOf("androidOnly")
            "android" -> excludeTags = mutableSetOf("iosOnly")
        }
    }

    retry {
        maxRetries.set(3)
        failOnPassedAfterRetry.set(false)
    }

    // Pass all values to JVM system properties
    listOf(
        "platform" to platformProp,
        "appium.url" to appiumUrlProp,
        "android.version" to androidVersionProp,
        "ios.version" to iosVersionProp,
        "android.device.name" to androidDeviceNameProp,
        "ios.device.name" to iosDeviceNameProp,
        "android.app.name" to androidAppNameProp,
        "ios.app.name" to iosAppNameProp,
        "app.activity" to appActivityProp,
        "app.package" to appPackageProp,
        "ios.auto_accept_alerts" to iosAutoAcceptAlertsProp,
        "ios.auto_dismiss_alerts" to iosAutoDismissAlertsProp,
        "android.video.recording.enabled" to androidVideoRecordingEnabledProp,
        "ios.video.recording.enabled" to iosVideoRecordingEnabledProp,
        "video.recording.size" to videoRecordingSizeProp,
        "video.recording.quality" to videoRecordingQualityProp,
        "video.recording.bitrate" to videoRecordingBitrateProp,
        "video.recording.output.dir" to videoRecordingOutputDirProp,
        "screenshot.on.success" to screenshotOnSuccessProp,
        "screenshot.on.failure" to screenshotOnFailureProp
    ).forEach { (key, value) ->
        if (value.isNotBlank()) systemProperty(key, value)
    }
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.getByName("javadoc"))
}

tasks.register("checkFfmpeg") {
    doLast {
        val osName = System.getProperty("os.name").lowercase()
        val ffmpegCommand = if (osName.contains("win")) "where" else "which"

        val result = exec {
            isIgnoreExitValue = true
            commandLine(ffmpegCommand, "ffmpeg")
        }
        if (result.exitValue != 0) {
            throw GradleException("""
                FFmpeg is not installed or not found in PATH!
                Video recording requires ffmpeg. Please install it:
                - macOS:  brew install ffmpeg
                - Linux:  sudo apt install ffmpeg
                - Windows: choco install ffmpeg  or  winget install ffmpeg
            """.trimIndent())
        } else {
            println("FFmpeg found, video recording will work.")
        }
    }
}

/* -------------------------------  Appium utils  ------------------------------- */

// Base Appium URL with default value
fun appiumBaseUrl(project: Project): String =
    (project.findProperty("appium.url") as String?).orEmpty().ifBlank { "http://localhost:4723/" }

// Check if Appium is alive (GET /status)
fun isAppiumRunning(project: Project): Boolean {
    val baseUrl = appiumBaseUrl(project)
    val statusUrl = URI.create(baseUrl.trimEnd('/') + "/status").toURL()
    return try {
        val conn = statusUrl.openConnection() as HttpURLConnection
        conn.connectTimeout = 1000
        conn.readTimeout = 1000
        conn.requestMethod = "GET"
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        code == 200 && body.isNotBlank()
    } catch (_: Exception) {
        false
    }
}

// Get path to npm from node-gradle (no reflection)
fun resolvedNpmPath(project: org.gradle.api.Project): String {
    val nodeExt = project.extensions.getByType(NodeExtension::class.java)
    val nodeDir = nodeExt.resolvedNodeDir.get().asFile
    val isWin = System.getProperty("os.name").lowercase().contains("win")
    return if (isWin) File(nodeDir, "npm.cmd").absolutePath
    else File(File(nodeDir, "bin"), "npm").absolutePath
}

/* ----------------------------------------------------------------------------- */

tasks.register("ensureAppium") {
    doLast {
        val skip = (project.findProperty("skipAppiumCheck") as String?)?.toBoolean() == true
        if (skip) {
            println("Skipping Appium check (skipAppiumCheck=true)")
            return@doLast
        }
        val baseUrl = appiumBaseUrl(project)
        val statusUrl = URI.create(baseUrl.trimEnd('/') + "/status").toURL()

        var ok = false
        val attempts = 10
        val delayMs = 1000L
        repeat(attempts) { idx ->
            try {
                val conn = statusUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 1500
                conn.readTimeout = 1500
                conn.requestMethod = "GET"
                val code = conn.responseCode
                val stream = (if (code in 200..299) conn.inputStream else conn.errorStream)
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (code == 200 && body.isNotBlank()) {
                    println("Appium status response: HTTP $code")
                    ok = true
                } else {
                    println("Appium not ready yet (attempt ${idx + 1}/$attempts, code=$code)")
                }
                conn.disconnect()
            } catch (e: Exception) {
                println("Appium check failed (attempt ${idx + 1}/$attempts): ${e.message}")
            }
            if (!ok) Thread.sleep(delayMs)
        }
        if (!ok) {
            throw GradleException(
                """
                Appium server is not reachable at $statusUrl.
                Start it via:
                  cd appium-runner && npm run setup && npm start
                Or skip this check with -PskipAppiumCheck=true
                """.trimIndent()
            )
        }
    }
}

// Flags to auto-start/stop Appium
val autoStartAppium: Boolean = (project.findProperty("appium.auto.start") as String?)?.toBoolean() ?: true
val startAppiumLocalProp: Boolean = (project.findProperty("appium.local.start") as String?)?.toBoolean() ?: true
val stopAppiumLocalProp: Boolean = (project.findProperty("appium.local.stop") as String?)?.toBoolean() ?: true

val nodeRunnerDir = file("appium-runner")

tasks.register<NpmTask>("nodeRunnerSetup") {
    workingDir.set(file("appium-runner"))
    args.set(listOf("run", "setup"))
}

// Local Appium process if we started it ourselves
val appiumProcKey = "__appiumProc__"
val appiumLogFileKey = "__appiumLogFile__"
val appiumLogWriterKey = "__appiumLogWriter__"
val appiumLogThreadKey = "__appiumLogThread__"

tasks.register("startAppiumLocal") {
    onlyIf {
        autoStartAppium && startAppiumLocalProp && !isAppiumRunning(project)
    }
    dependsOn("nodeRunnerSetup")
    doFirst {
        val npmPath = resolvedNpmPath(project)
        project.extensions.extraProperties.set("npmBinPath", npmPath)
    }
    doLast {
        val npm = project.extensions.extraProperties.get("npmBinPath") as String
        val baseUrl = appiumBaseUrl(project)
        val statusUrl = URI.create(baseUrl.trimEnd('/') + "/status").toURL()

        fun isUp(): Boolean = isAppiumRunning(project)

        // Prepare log file
        val logsDir = File(buildDir, "appium-logs")
        logsDir.mkdirs()
        val logFile = File(logsDir, "appium-${System.currentTimeMillis()}.log")
        project.extensions.extraProperties.set(appiumLogFileKey, logFile.absolutePath)
        println("Appium logs → ${logFile.absolutePath}")

        println("Starting Appium from node-runner (npm start)…")
        val pb = ProcessBuilder(npm, "start")
        pb.directory(nodeRunnerDir)
        pb.redirectErrorStream(true)

        // Add node/bin to PATH so npm can find node (especially on *nix)
        val env = pb.environment()
        val pathKey = env.keys.firstOrNull { it.equals("Path", ignoreCase = true) } ?: "PATH"
        val nodeBin = File(npm).parentFile.absolutePath
        env[pathKey] = nodeBin + File.pathSeparator + (env[pathKey] ?: "")

        val process = pb.start()
        project.extensions.extraProperties.set(appiumProcKey, process)

        // Log forwarder that mirrors process stdout to file and Gradle console
        val writer = BufferedWriter(FileWriter(logFile, true))
        val t = Thread({
            process.inputStream.bufferedReader().useLines { seq ->
                seq.forEach { line ->
                    try {
                        writer.appendLine(line)
                        writer.flush()
                    } catch (_: Exception) {}
                    println("[appium] $line")
                }
            }
        }, "appium-log-forwarder")
        t.isDaemon = true
        t.start()
        project.extensions.extraProperties.set(appiumLogWriterKey, writer)
        project.extensions.extraProperties.set(appiumLogThreadKey, t)

        // Wait for readiness
        val attempts = 60
        val delayMs = 1000L
        var ok = false
        repeat(attempts) {
            if (isUp()) { ok = true; return@repeat }
            Thread.sleep(delayMs)
        }
        if (!ok) {
            println("Appium failed to start in time, stopping process…")
            try { process.destroy() } catch (_: Exception) {}
            // Close writer
            try { writer.close() } catch (_: Exception) {}
            throw GradleException("Failed to start Appium at $statusUrl within ${(attempts * delayMs) / 1000}s")
        }
        println("Appium started and is reachable at $statusUrl")
    }
}

tasks.register("stopAppiumLocal") {
    // Keep the global flag — but the task becomes a no-op if we didn't start anything
    onlyIf { autoStartAppium && stopAppiumLocalProp }
    doLast {
        val extra = project.extensions.extraProperties

        val hasProc     = extra.has(appiumProcKey)
        val hasWriter   = extra.has(appiumLogWriterKey)
        val hasLogFile  = extra.has(appiumLogFileKey)
        val hasLogThread= extra.has(appiumLogThreadKey)

        // If we didn’t start locally (no process or log resources) — do nothing
        if (!hasProc && !hasWriter && !hasLogFile && !hasLogThread) {
            println("No locally started Appium process to stop.")
            return@doLast
        }

        // Close logger (if present)
        if (hasWriter) {
            (extra.get(appiumLogWriterKey) as? java.io.BufferedWriter)?.let {
                try { it.flush() } catch (_: Exception) {}
                try { it.close() } catch (_: Exception) {}
            }
            try { extra.set(appiumLogWriterKey, null) } catch (_: Exception) {}
        }

        // Clean the mirror thread reference (daemon; no need to stop explicitly)
        if (hasLogThread) {
            try { extra.set(appiumLogThreadKey, null) } catch (_: Exception) {}
        }

        // Stop locally started process (if any)
        if (hasProc) {
            val proc = extra.get(appiumProcKey)
            if (proc is Process && proc.isAlive) {
                println("Stopping local Appium process...")
                proc.destroy()
                try {
                    if (!proc.waitFor(5, TimeUnit.SECONDS)) {
                        println("Appium did not stop gracefully, destroying forcibly...")
                        proc.destroyForcibly()
                    }
                } catch (_: Exception) {}
            }
            try { extra.set(appiumProcKey, null) } catch (_: Exception) {}
        }

        // Print log path (if present) and clear the key
        if (hasLogFile) {
            val lastLog = (extra.get(appiumLogFileKey) as? String).orEmpty()
            if (lastLog.isNotBlank()) println("Last Appium log file: $lastLog")
            try { extra.set(appiumLogFileKey, null) } catch (_: Exception) {}
        }
    }
}

tasks.named("ensureAppium") {
    mustRunAfter("startAppiumLocal")
}

tasks.named<Test>("test") {
    dependsOn("checkFfmpeg")
    if (autoStartAppium && startAppiumLocalProp) {
        dependsOn("startAppiumLocal")
    }
    dependsOn("ensureAppium")
    if (autoStartAppium && stopAppiumLocalProp) {
        finalizedBy("stopAppiumLocal")
    }
    systemProperty("appium.auto.start", autoStartAppium.toString())
    systemProperty("appium.local.start", startAppiumLocalProp.toString())
    systemProperty("appium.local.stop", stopAppiumLocalProp.toString())
}

/* ------------------------------  publishing  ------------------------------ */

// Check whether a publish task is being executed
val isPublishTask = gradle.startParameter.taskNames.any { it.contains("publish") }

// Configure publishing only when a publish task is running,
// or add an empty configuration to avoid errors for other tasks
if (isPublishTask) {
    // These checks are executed only for publishing tasks
    val propUser = findProperty("nexusUser") as String?
    val propPassword = findProperty("nexusPassword") as String?
    val envUser = System.getenv("NEXUS_USER")
    val envPassword = System.getenv("NEXUS_PASSWORD")

    val nexusUser: String = propUser
        ?: envUser
        ?: throw GradleException(
            "nexusUser is not set: " +
                    "specify it in gradle.properties (nexusUser=…) or via -P, " +
                    "or set env NEXUS_USER"
        )

    val nexusPassword: String = propPassword
        ?: envPassword
        ?: throw GradleException(
            "nexusPassword is not set: " +
                    "specify it in gradle.properties (nexusPassword=…) or via -P, " +
                    "or set env NEXUS_PASSWORD"
        )

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])
                groupId = "testing-tools"
                artifactId = "easytestwrite"
                version = project.version.toString()
            }
        }
        repositories {
            maven {
                name = "maven"
                url = uri("https://nexus.someone.repository/repository/maven/")
                credentials {
                    username = nexusUser
                    password = nexusPassword
                }
            }
        }
    }
} else {
    // Empty publishing configuration for non-publishing tasks
    publishing {
        // Empty publications and repositories
    }
}

kotlin {
    jvmToolchain(21)
}

//detekt {
//    autoCorrect = true
//    buildUponDefaultConfig = true
//    allRules = false
//    config.setFrom(files("$projectDir/config/detekt/detekt.yml"))
//    baseline = file("$projectDir/config/detekt/baseline.xml")
//
//    reports {
//        html.required.set(true)
//        xml.required.set(true)
//        txt.required.set(true)
//        sarif.required.set(true)
//        md.required.set(true)
//    }
//}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.5")
}