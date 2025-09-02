package controller.mobile.interaction

import controller.mobile.element.PageElement
import dsl.testing.ExpectationContext
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION

interface UiVisibilityChecks : UiElementFinding {

    /**
     * Проверить виден ли [element] на экране
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
    fun ExpectationContext.checkVisible(
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
        ).isDisplayed
    }

    /**
     * Проверить, виден ли на экране элемент, содержащий указанный текст.
     *
     * Метод построит локатор по стратегии `PageElement.Contains` на всех платформах (Android, iOS, Web)
     * и выполнит поиск элемента с возможностью скроллирования. Если элемент найден, будет проверена его видимость.
     *
     * Метод полезен для DSL, когда известна только часть текста (например, кнопки, лейбла и т.п.),
     * а точный локатор не задан или изменяется динамически.
     *
     * @param text текст, который точно соответствует в целевом элементе;
     * @param containsText текст, который должен содержаться в элементе;
     * @param elementNumber порядковый номер совпавшего элемента (начиная с 1);
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation максимальное время ожидания элемента;
     * @param pollingInterval интервал между попытками;
     * @param scrollCount количество скроллов при отсутствии элемента;
     * @param scrollCapacity доля экрана, на которую происходит один скролл [0.0 - 1.0];
     * @param scrollDirection направление скроллирования;
     *
     * @throws java.util.NoSuchElementException если элемент не найден или не отображается.
     */
    fun ExpectationContext.checkVisible(
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
        ).isDisplayed
    }
}