package app

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL
import java.util.Properties

/**
 * Конфигурационный класс для тестового фреймворка.
 *
 * Загружает настройки из файла `config.properties` и предоставляет типизированный доступ
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
            val resourceStream =
                Thread.currentThread().contextClassLoader.getResourceAsStream("config.properties")
                    ?: throw IllegalArgumentException("Файл config.properties не найден в директории test/resources")
            load(resourceStream)
            logger.info("Конфигурация успешно загружена из config.properties")
        } catch (e: Exception) {
            logger.error("Ошибка загрузки конфигурации: ${e.message}", e)
            throw e
        }
    }

    private val appiumUrl: URL = URI(properties.getProperty("appium.url", "http://localhost:4723/")).toURL()
    private val platform: Platform = runCatching {
        Platform.valueOf(properties.getProperty("platform")) }.getOrElse { Platform.ANDROID }
    private val androidVersion: String = properties.getProperty("android.version", "14")
    private val iosVersion: String = properties.getProperty("ios.version", "17.5")
    private val androidDeviceName: String = properties.getProperty("android.device.name", "WBA_AVD_API35")
    private val iosDeviceName: String = properties.getProperty("ios.device.name", "iPhone 15 Pro Max")
    private val androidAppName: String = properties.getProperty("android.app.name", "android.apk")
    private val iosAppName: String = properties.getProperty("ios.app.name", "ios.app")
    private val appActivity: String = properties.getProperty("app.activity", "ru.wildberries.view.main.MainActivity")
    private val appPackage: String = properties.getProperty("app.package", "com.wildberries.ru.dev")
    private val browserType: String = properties.getProperty("playwright.browser.type", "chromium")
    private val headless: Boolean = properties.getProperty("playwright.headless", "true").toBoolean()

    /**
     * @return true, если текущая платформа — Android.
     */
    fun isAndroid(): Boolean = platform == Platform.ANDROID

    /**
     * @return true, если текущая платформа — iOS.
     */
    fun isiOS(): Boolean = platform == Platform.IOS

    /**
     * @return Версия Android для тестового окружения.
     */
    fun getAndroidVersion(): String = androidVersion

    /**
     * @return Версия iOS для тестового окружения.
     */
    fun getIosVersion(): String = iosVersion

    /**
     * @return Имя устройства iOS, на котором будут выполняться тесты.
     */
    fun getIosDeviceName(): String = iosDeviceName

    /**
     * @return Имя устройства Android, на котором будут выполняться тесты.
     */
    fun getAndroidDeviceName(): String = androidDeviceName

    /**
     * @return URL Appium-сервера.
     */
    fun getAppiumUrl(): URL = appiumUrl

    /**
     * @return Текущая целевая платформа тестов.
     */
    fun getPlatform(): Platform = platform

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