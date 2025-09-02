import org.gradle.api.GradleException
import java.net.URL
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.tasks.testing.Test
import java.net.URI

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("org.gradle.test-retry") version "1.6.2"
//    id("io.gitlab.arturbosch.detekt") version "1.23.5"
    `java-library`
    `maven-publish`
    id("com.github.node-gradle.node") version "7.1.0"
}

group = "wba"
version = "0.1.1"

node {
    version.set("20.19.0")
    download.set(true)
    // (опционально) nodeProjectDir.set(file("appium-runner"))
}

repositories {
    maven {
        url = uri("https://wba-nexus.wb.ru/repository/wba-maven/")
    }
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
    // Читаем все возможные Gradle-свойства
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

    // Настраиваем JUnit Platform
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

    // Прокидываем в JVM-системные свойства все значения
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
        "video.recording.output.dir" to videoRecordingOutputDirProp
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
                Для работы записи видео требуется ffmpeg. 
                Установите его:
                - Mac:     brew install ffmpeg
                - Linux:   sudo apt install ffmpeg
                - Windows: choco install ffmpeg или winget install ffmpeg
            """.trimIndent())
        } else {
            println("FFmpeg найден, запись видео будет работать.")
        }
    }
}

tasks.register("ensureAppium") {
    doLast {
        val skip = (project.findProperty("skipAppiumCheck") as String?)?.toBoolean() == true
        if (skip) {
            println("Skipping Appium check (skipAppiumCheck=true)")
            return@doLast
        }
        val baseUrl = (project.findProperty("appium.url") as String?).orEmpty().ifBlank { "http://localhost:4723/" }
        val statusUrlStr = baseUrl.trimEnd('/') + "/status"
        val statusUrl = URL(statusUrlStr)
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

// Auto-start Appium from bundled appium-runner (node_modules) during Gradle tasks
val autoStartAppium: Boolean = (project.findProperty("appium.auto.start") as String?)?.toBoolean() ?: true
val startAppiumLocalProp: Boolean = (project.findProperty("appium.local.start") as String?)?.toBoolean() ?: true
val stopAppiumLocalProp: Boolean = (project.findProperty("appium.local.stop") as String?)?.toBoolean() ?: true

val nodeRunnerDir = file("appium-runner")

tasks.register<NpmTask>("nodeRunnerSetup") {
    workingDir.set(file("appium-runner"))
    args.set(listOf("run", "setup"))
}

// Start Appium server in background using npm start inside appium-runner
val appiumProcKey = "__appiumProc__"

tasks.register("startAppiumLocal") {
    onlyIf { autoStartAppium && startAppiumLocalProp }
    dependsOn("nodeRunnerSetup")
    doFirst {
        val nodeExt = project.extensions.getByType(com.github.gradle.node.NodeExtension::class.java)
        val npmExec = try {
            val execProvider = com.github.gradle.node.NodeExtension::class.java.getMethod("getNpmExecutable").invoke(nodeExt)
            val asFile = execProvider.javaClass.getMethod("getAsFile").invoke(execProvider)
            asFile.javaClass.getMethod("getAbsolutePath").invoke(asFile) as String
        } catch (e: Exception) {
            if (System.getProperty("os.name").lowercase().contains("win")) "npm.cmd" else "npm"
        }
        project.extensions.extraProperties.set("npmBinPath", npmExec)
    }
    doLast {
        val npm = project.extensions.extraProperties.get("npmBinPath") as String
        val baseUrl = (project.findProperty("appium.url") as String?).orEmpty().ifBlank { "http://localhost:4723/" }
        val statusUrl = URI(baseUrl.trimEnd('/') + "/status").toURL()

        fun isUp(): Boolean {
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
            } catch (e: Exception) { false }
        }

        if (isUp()) {
            println("Appium is already running at $statusUrl — will not start a new instance.")
            return@doLast
        }

        println("Starting Appium from appium-runner (npm start)...")
        val pb = ProcessBuilder(npm, "start")
        pb.directory(nodeRunnerDir)
        pb.redirectErrorStream(true)
        val process = pb.start()
        // Save process to project extra to stop later
        project.extensions.extraProperties.set(appiumProcKey, process)

        // Wait until server is up
        val attempts = 60
        val delayMs = 1000L
        var ok = false
        repeat(attempts) {
            if (isUp()) { ok = true; return@repeat }
            Thread.sleep(delayMs)
        }
        if (!ok) {
            println("Appium failed to start in time, stopping process...")
            try { process.destroy() } catch (_: Exception) {}
            throw GradleException("Failed to start Appium at $statusUrl within ${(attempts * delayMs) / 1000}s")
        }
        println("Appium started and is reachable at $statusUrl")
    }
}

tasks.register("stopAppiumLocal") {
    onlyIf { autoStartAppium && stopAppiumLocalProp }
    doLast {
        val extra = project.extensions.extraProperties
        if (extra.has(appiumProcKey)) {
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
            // Clean the flag
            try { extra.set(appiumProcKey, null) } catch (_: Exception) {}
        } else {
            println("No locally started Appium process to stop.")
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
    // Propagate flags to test JVM so AppConfig can read them via System.getProperty (dotted keys)
    systemProperty("appium.auto.start", autoStartAppium.toString())
    systemProperty("appium.local.start", startAppiumLocalProp.toString())
    systemProperty("appium.local.stop", stopAppiumLocalProp.toString())
}


// Настройки публикации проекта
// Проверяем, выполняется ли задача публикации
val isPublishTask = gradle.startParameter.taskNames.any { it.contains("publish") }

// Настраиваем публикацию только если выполняется задача публикации
// или добавляем пустую конфигурацию, чтобы избежать ошибок при других задачах
if (isPublishTask) {
    // Эти проверки выполняются только при задачах публикации
    val propUser = findProperty("wbaNexusUser") as String?
    val propPassword = findProperty("wbaNexusPassword") as String?
    val envUser = System.getenv("WBA_NEXUS_USER")
    val envPassword = System.getenv("WBA_NEXUS_PASSWORD")

    val wbaNexusUser: String = propUser
        ?: envUser
        ?: throw GradleException(
            "Не задан wbaNexusUser: " +
                    "укажите в gradle.properties (wbaNexusUser=…) или через -P, " +
                    "или задайте env WBA_NEXUS_USER"
        )

    val wbaNexusPassword: String = propPassword
        ?: envPassword
        ?: throw GradleException(
            "Не задан wbaNexusPassword: " +
                    "укажите в gradle.properties (wbaNexusPassword=…) или через -P, " +
                    "или задайте env WBA_NEXUS_PASSWORD"
        )

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])
                groupId = "wba"
                artifactId = "easytestwrite"
                version = project.version.toString()
            }
        }
        repositories {
            maven {
                name = "wba-maven"
                url = uri("https://wba-nexus.wb.ru/repository/wba-maven/")
                credentials {
                    username = wbaNexusUser
                    password = wbaNexusPassword
                }
            }
        }
    }
} else {
    // Пустая конфигурация публикации для непубликационных задач
    publishing {
        // Пустые публикации и репозитории
    }
}

kotlin {
    jvmToolchain(21)
}

//detekt {
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

//dependencies {
//    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.5")
//}
