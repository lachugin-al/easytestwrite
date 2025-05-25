import org.gradle.api.GradleException

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("org.gradle.test-retry") version "1.6.2"
    `java-library`
    `maven-publish`
}

group = "wba"
version = "0.0.23"

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

    // https://mvnrepository.com/artifact/com.microsoft.playwright/playwright
    implementation("com.microsoft.playwright:playwright:1.47.0")
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
    val browserTypeProp = (project.findProperty("playwright.browser.type") as String?).orEmpty()
    val headlessProp = (project.findProperty("playwright.headless") as String?).orEmpty()
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
        "playwright.browser.type" to browserTypeProp,
        "playwright.headless" to headlessProp,
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

tasks.named("test") {
    dependsOn("checkFfmpeg")
}


// Настройки публикации проекта
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

kotlin {
    jvmToolchain(21)
}
