package controller.mobile.interaction

import com.google.common.collect.ImmutableList
import controller.mobile.element.PageElement
import dsl.testing.StepContext
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.openqa.selenium.support.ui.WebDriverWait
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION
import java.time.Duration

interface UiTapActions : UiElementFinding {

    /**
     * Нажать в области экрана по [x] [y]
     * @param x точка по [x] в области экрана;
     * @param y точка по [y] в области экрана;
     * @param timeoutBeforeExpectation максимальное количество секунд ожидания перед выполнением нажатия.
     * Если указан параметр [waitCondition], в течение этого времени будет ожидаться выполнение заданного условия.
     * Если [waitCondition] не задан, будет ожидаться стабилизация UI (отсутствие изменений в исходном коде страницы) в течение указанного времени.
     * Если значение равно 0, ожидание перед нажатием не производится.
     */
    fun StepContext.tapArea(
        x: Int,
        y: Int,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        waitCondition: (() -> Boolean)? = null
    ) {
        if (waitCondition != null) {
            WebDriverWait(driver, timeoutBeforeExpectation)
                .until { waitCondition.invoke() }
        } else if (timeoutBeforeExpectation > 0) {
            val beforeWait = WebDriverWait(driver, timeoutBeforeExpectation)

            // ожидание стабилизации страницы
            var previousPageSource = driver.pageSource
            beforeWait.until {
                val currentPageSource = driver.pageSource
                val isStable = currentPageSource == previousPageSource
                previousPageSource = currentPageSource
                isStable
            }
        }

        val pointerInput = PointerInput(PointerInput.Kind.TOUCH, "finger1")
        val sequence = Sequence(pointerInput, 0)
        val origin = PointerInput.Origin.viewport()

        sequence.addAction(pointerInput.createPointerMove(Duration.ZERO, origin, x, y))
        sequence.addAction(pointerInput.createPointerDown(0))
        sequence.addAction(pointerInput.createPointerUp(0))

        driver.perform(ImmutableList.of(sequence))
    }

    /**
     * Нажать в области [element] по его [x] [y]
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param x точка по [x] в области элемента;
     * @param y точка по [y] в области элемента;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     */
    fun StepContext.tapElementArea(
        element: PageElement?,
        elementNumber: Int? = null,
        x: Int,
        y: Int,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        waitForElements(
            element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).let { foundElement ->
            val elementLocation = foundElement.location
            val elementPointX = elementLocation.x + x
            val elementPointY = elementLocation.y + y

            tapArea(elementPointX, elementPointY)
        }
    }
}