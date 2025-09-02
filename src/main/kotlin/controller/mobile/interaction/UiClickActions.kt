package controller.mobile.interaction

import controller.mobile.element.PageElement
import dsl.testing.StepContext
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION

interface UiClickActions : UiElementFinding {

    /**
     * Найти элемент на экране по его [element] и кликнуть по нему
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     */
    fun StepContext.click(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).click()
    }

    /**
     * Найти элемент на экране, содержащий указанный текст, и кликнуть по нему.
     *
     * Метод построит локатор по стратегии `PageElement.Contains` на всех платформах (Android, iOS, Web)
     * и выполнит поиск элемента с возможностью скроллирования. Если элемент найден, выполняется клик.
     *
     * Метод полезен в ситуациях, когда необходимо кликнуть по элементу, содержащему определённую подстроку
     * (например, часть названия товара, кнопку с динамическим текстом и т.д.), без необходимости заранее
     * знать точное имя или локатор элемента.
     *
     * @param text текст, который точно соответствует в целевом элементе;
     * @param containsText текст, который должен содержаться в целевом элементе;
     * @param elementNumber номер найденного элемента, начиная с 1. Если null, будет использован первый найденный;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation максимальное время ожидания появления элемента, в секундах;
     * @param pollingInterval интервал между попытками поиска элемента, в миллисекундах;
     * @param scrollCount количество попыток скроллирования при неудачном поиске;
     * @param scrollCapacity доля экрана, на которую производится один скролл [0.0 - 1.0];
     * @param scrollDirection направление скроллирования (вниз, вверх и т.п.);
     *
     * @throws java.util.NoSuchElementException если элемент, содержащий указанный текст, не найден после всех попыток.
     */
    fun StepContext.click(
        text: String? = null,
        containsText: String? = null,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        require(!(text == null && containsText == null)) {
            "Необходимо указать либо 'text', либо 'contains'"
        }
        require(!(text != null && containsText != null)) {
            "Нельзя одновременно использовать 'text' и 'contains'"
        }
        val element = when {
            text != null -> PageElement(
                android = PageElement.ExactMatch(text),
                ios = PageElement.ExactMatch(text)
            )

            containsText != null -> PageElement(
                android = PageElement.Contains(containsText),
                ios = PageElement.Contains(containsText),
            )

            else -> error("Указан и не text и не containsText")
        }

        waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).click()
    }
}