package controller.mobile.interaction

import controller.mobile.element.PageElement
import dsl.testing.StepContext
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION

interface UiTypingActions : UiElementFinding {

    /**
     * Найти элемент на экране по его [element] и ввести [text]
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param text текст;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     */
    fun StepContext.typeText(
        element: PageElement?,
        elementNumber: Int? = null,
        text: String,
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
        ).sendKeys(text)
    }
}