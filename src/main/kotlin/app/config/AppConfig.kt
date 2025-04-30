package app.config

import app.model.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL
import java.util.Properties

/**
 * Конфигурационный класс для тестового фреймворка.
 *
 * Загружает настройки из системных свойств JVM (–D), если они не передано то из файла `config.properties` и предоставляет типизированный доступ
 * к параметрам запуска тестов, включая:
 * - Платформу тестирования (Android, iOS, Web)
 * - Настройки подключения к Appium-серверу
 * - Данные об устройствах и приложениях
 * - Параметры запуска Playwright для веб-тестов
 *
 * Конфигурация загружается при первом обращении к объекту [AppConfig].
 *
 * @throws IllegalArgumentException если файл `config.properties` не найден
 */
object AppConfig {

    private val logger: Logger = LoggerFactory.getLogger(AppConfig::class.java)

    private val properties = Properties().apply {
        try {
            val resourceStream = Thread.currentThread()
                .contextClassLoader
                .getResourceAsStream("config.properties")
                ?: throw IllegalArgumentException(
                    "Файл config.properties не найден в директории test/resources"
                )
            load(resourceStream)
            logger.info("Конфигурация успешно загружена из config.properties")
        } catch (e: Exception) {
            logger.error("Ошибка загрузки config.properties: ${e.message}", e)
            throw e
        }
    }

    // Helper: системное свойство или из файла или default
    private fun prop(name: String, default: String): String =
        System.getProperty(name)
            ?: properties.getProperty(name, default)

    // URL Appium
    private val appiumUrl: URL = URI(prop("appium.url", "http://localhost:4723/")).toURL()

    // Платформа
    private val platform: Platform = runCatching {
        Platform.valueOf(prop("platform", "ANDROID"))
    }.getOrElse { Platform.ANDROID }

    // Версии ОС
    private val androidVersion: String = prop("android.version", "15")
    private val iosVersion: String = prop("ios.version", "18.4")

    // Имена устройств
    private val androidDeviceName: String = prop("android.device.name", "Pixel_XL_API_35")
    private val iosDeviceName: String = prop("ios.device.name", "iPhone 16 Plus")

    // Пути к приложениям
    private val androidAppName: String = prop("android.app.name", "android.apk")
    private val iosAppName: String = prop("ios.app.name", "ios.app")

    // Активити и пакет Android
    private val appActivity: String = prop(
        "app.activity",
        "ru.wildberries.view.main.MainActivity"
    )
    private val appPackage: String = prop(
        "app.package",
        "com.wildberries.ru.dev"
    )

    // Playwright
    private val browserType: String = prop("playwright.browser.type", "chromium")
    private val headless: Boolean = prop("playwright.headless", "true").toBoolean()

    // API

    /**
     * @return true, если текущая платформа — Android.
     */
    fun isAndroid(): Boolean = platform == Platform.ANDROID

    /**
     * @return true, если текущая платформа — iOS.
     */
    fun isiOS(): Boolean = platform == Platform.IOS

    /**
     * @return Текущая целевая платформа тестов.
     */
    fun getPlatform(): Platform = platform

    /**
     * @return URL Appium-сервера.
     */
    fun getAppiumUrl(): URL = appiumUrl

    /**
     * @return Версия Android для тестового окружения.
     */
    fun getAndroidVersion(): String = androidVersion

    /**
     * @return Версия iOS для тестового окружения.
     */
    fun getIosVersion(): String = iosVersion

    /**
     * @return Имя устройства Android, на котором будут выполняться тесты.
     */
    fun getAndroidDeviceName(): String = androidDeviceName

    /**
     * @return Имя устройства iOS, на котором будут выполняться тесты.
     */
    fun getIosDeviceName(): String = iosDeviceName

    /**
     * @return Имя активности Android-приложения для запуска.
     */
    fun getAppActivity(): String = appActivity

    /**
     * @return Имя пакета Android-приложения.
     */
    fun getAppPackage(): String = appPackage

    /**
     * @return Имя APK или .app файла в зависимости от выбранной платформы.
     * Для Web-платформы возвращается пустая строка.
     */
    fun getAppName(): String {
        return when (platform) {
            Platform.ANDROID -> androidAppName
            Platform.IOS -> iosAppName
            Platform.WEB -> ""
        }
    }

    /**
     * @return Тип браузера для запуска Playwright (chromium, firefox, webkit).
     */
    fun getBrowserType(): String = browserType

    /**
     * @return true, если Playwright запускается в headless-режиме.
     */
    fun isHeadless(): Boolean = headless
}