package app.model

/**
 * Перечисление, описывающее поддерживаемые платформы для запуска тестов.
 *
 * Используется для определения типа окружения при инициализации драйверов
 * и выбора соответствующей логики работы тестового фреймворка.
 *
 * @see app.config.AppConfig
 * @see app.driver.AndroidDriver
 * @see app.driver.IosDriver
 * @see app.driver.WebDriver
 */
enum class Platform {
    /** Платформа Android (тестирование через Appium). */
    ANDROID,

    /** Платформа iOS (тестирование через Appium). */
    IOS,

    /** Веб-платформа (тестирование через Playwright). */
    WEB
}