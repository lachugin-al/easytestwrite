package dsl.testing

import app.AppConfig
import app.Platform
import java.lang.Exception

/**
 * Базовый класс всех контекстов тестирования.
 *
 * Предоставляет вспомогательные методы для выполнения кода в зависимости от платформы,
 * а также для безопасного выполнения опциональных действий без прерывания теста.
 *
 * @see TestingDslMarker
 */
open class BaseContext {

    /**
     * Выполняет переданный блок кода только в случае, если целевая платформа — iOS.
     *
     * Блок [action] будет проигнорирован для всех остальных платформ.
     *
     * @param action Лямбда, содержащая действия для выполнения на iOS.
     */
    fun onlyIos(action: () -> Unit) = action.invokeIfRequiredPlatform(Platform.IOS)

    /**
     * Выполняет переданный блок кода только в случае, если целевая платформа — Android.
     *
     * Блок [action] будет проигнорирован для всех остальных платформ.
     *
     * @param action Лямбда, содержащая действия для выполнения на Android.
     */
    fun onlyAndroid(action: () -> Unit) = action.invokeIfRequiredPlatform(Platform.ANDROID)

    /**
     * Выполняет переданный блок кода в безопасном режиме.
     *
     * Если внутри [action] произойдёт ошибка — тест продолжит выполнение без прерывания.
     * Исключение будет проглочено без дополнительной обработки.
     *
     * Используется для необязательных проверок, скриншотов или вспомогательных операций.
     *
     * @param action Лямбда с необязательной логикой выполнения.
     */
    @Suppress("SwallowedException")
    fun optional(action: () -> Unit) {
        try {
            action.invoke()
        } catch (e: Exception) {
            // Ошибки подавляются, чтобы тест продолжал выполнение
        }
    }

    /**
     * Вспомогательная функция для условного выполнения блока кода в зависимости от платформы.
     *
     * @param platform Платформа, для которой необходимо выполнить код.
     */
    private fun (() -> Unit).invokeIfRequiredPlatform(platform: Platform) {
        if (AppConfig.getPlatform() == platform) invoke()
    }
}
