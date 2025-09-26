plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("org.gradle.test-retry") version "1.6.2"
    id("org.jetbrains.dokka") version "1.9.20"
//    id("io.gitlab.arturbosch.detekt") version "1.23.5"
    `java-library`
    `maven-publish`
}

group = "testing-tools"
version = "0.1.6"

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
    dependsOn(tasks.named("dokkaHtml"))
    archiveClassifier.set("javadoc")
    // Package Dokka HTML output as the javadoc artifact
    from(layout.buildDirectory.dir("dokka/html"))
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

tasks.register("checkAppium") {
    doLast {
        val osName = System.getProperty("os.name").lowercase()
        val whichCmd = if (osName.contains("win")) "where" else "which"

        // Check that `appium` CLI is available
        val appiumWhich = exec {
            isIgnoreExitValue = true
            commandLine(whichCmd, "appium")
        }
        if (appiumWhich.exitValue != 0) {
            throw GradleException(
                """
                Appium CLI is not installed or not found in PATH!

                Please install Appium first (requires Node.js):
                  npm install -g appium

                Verify with: appium -v
                """.trimIndent()
            )
        } else {
            println("Appium CLI found in PATH.")
        }

        // Check that required drivers are installed: uiautomator2 and xcuitest
        val uiAutoCmd = if (osName.contains("win")) {
            listOf("cmd", "/c", "appium driver list --installed | findstr /i uiautomator2")
        } else {
            listOf("sh", "-c", "appium driver list --installed | grep -i uiautomator2")
        }
        val xcuiCmd = if (osName.contains("win")) {
            listOf("cmd", "/c", "appium driver list --installed | findstr /i xcuitest")
        } else {
            listOf("sh", "-c", "appium driver list --installed | grep -i xcuitest")
        }

        val uiaResult = exec {
            isIgnoreExitValue = true
            commandLine(uiAutoCmd)
        }
        val xcuiResult = exec {
            isIgnoreExitValue = true
            commandLine(xcuiCmd)
        }

        val haveUiAutomator2 = uiaResult.exitValue == 0
        val haveXcuitest = xcuiResult.exitValue == 0

        if (!haveUiAutomator2 || !haveXcuitest) {
            val missing = buildList {
                if (!haveUiAutomator2) add("uiautomator2")
                if (!haveXcuitest) add("xcuitest")
            }.joinToString(", ")
            throw GradleException(
                """
                Required Appium drivers are missing: $missing

                Install them with:
                  appium driver install uiautomator2
                  appium driver install xcuitest

                Then verify with: appium driver list --installed
                """.trimIndent()
            )
        } else {
            println("Appium drivers check passed: uiautomator2 and xcuitest are installed.")
        }
    }
}

// Convenience task to generate API docs
tasks.register("generateApiDocs") {
    group = "documentation"
    description = "Generates API documentation using Dokka (HTML)"
    dependsOn("dokkaHtml")
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

//dependencies {
//    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.5")
//}
