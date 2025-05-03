package controller.handler

import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import org.openqa.selenium.NoAlertPresentException
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

/**
 * Класс-обёртка для работы с системными алертами (native alerts) на устройствах Android и iOS.
 *
 * Предоставляет методы для:
 * - Проверки наличия алерта;
 * - Принятия алерта (accept);
 * - Отклонения алерта (dismiss);
 * - Получения текста алерта.
 *
 * Использует WebDriverWait с заданным таймаутом ожидания появления алерта.
 *
 * @property driver AppiumDriver, управляющий мобильным устройством.
 * @property timeoutExpectation Время ожидания алерта в секундах.
 * @property pollingInterval Частота опроса элемента в миллисекундах.
 */
class AlertHandler(
    private val driver: AppiumDriver<MobileElement>,
    private val timeoutExpectation: Long,
    private val pollingInterval: Long,
) {

    /**
     * Проверяет наличие отображаемого системного алерта.
     *
     * @return true, если алерт найден за указанный [timeout]; false — если нет или возникла ошибка.
     */
    fun isAlertPresent(): Boolean {
        return try {
            WebDriverWait(driver, timeoutExpectation, pollingInterval)
                .until(ExpectedConditions.alertIsPresent())
            true
        } catch (e: NoAlertPresentException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Принимает (нажимает Accept) текущий отображаемый системный алерт.
     *
     * Используется, например, для разрешения доступа к геолокации, камере и т. п.
     *
     * @throws NoAlertPresentException если алерт не отображается в течение [timeout] секунд.
     */
    fun accept(): Unit {
        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        val alert = wait.until(ExpectedConditions.alertIsPresent())
        alert.accept()
    }

    /**
     * Отклоняет (нажимает Cancel) текущий отображаемый системный алерт.
     *
     * Может использоваться, если необходимо отказаться от разрешений.
     *
     * @throws NoAlertPresentException если алерт не отображается в течение [timeout] секунд.
     */
    fun dismiss(): Unit {
        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        val alert = wait.until(ExpectedConditions.alertIsPresent())
        alert.dismiss()
    }

    /**
     * Возвращает текст текущего отображаемого системного алерта.
     *
     * @return Текст, отображаемый в теле алерта (например, "Разрешить приложению отслеживать Ваши действия")
     * @throws NoAlertPresentException если алерт не отображается в течение [timeout] секунд.
     */
    fun getText(): String {
        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        return wait.until(ExpectedConditions.alertIsPresent()).text
    }
}