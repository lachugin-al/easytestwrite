package controller.web

import app.App
import com.microsoft.playwright.ElementHandle
import com.microsoft.playwright.Page
import controller.element.PageElement
import controller.element.ScrollDirection
import dsl.testing.StepContext
import dsl.testing.TestingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION
import java.util.NoSuchElementException

/**
 * Базовый класс для написания веб-тестов.
 *
 * Содержит обертки над основными действиями: прокрутка, поиск элементов,
 * применяя Playwright API через обертки для DSL.
 *
 * Данный класс наследуют все тестовые классы для WEB.
 */
open class WebTest {
    protected var app: App = App().launch()
    protected var context: TestingContext = TestingContext(page)
    protected val page: Page
        get() = app.webDriver ?: throw IllegalStateException("Драйвер не инициализирован")
    private val logger: Logger = LoggerFactory.getLogger(WebTest::class.java)

    /**
     * Прокрутить вниз страницу.
     */
    fun StepContext.scrollDown(
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) {
        performScroll(element=null, scrollCount, scrollCapacity, ScrollDirection.Down)
    }

    /**
     * Прокрутить вверх страницу.
     */
    fun StepContext.scrollUp(
        element: PageElement? = null,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) {
        performScroll(element=null, scrollCount, scrollCapacity, ScrollDirection.Up)
    }

    /**
     * Прокрутить вправо страницу.
     */
    fun StepContext.scrollRight(
        element: PageElement? = null,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) {
        performScroll(element=null, scrollCount, scrollCapacity, ScrollDirection.Right)
    }

    /**
     * Прокрутить влево страницу.
     */
    fun StepContext.scrollLeft(
        element: PageElement? = null,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) {
        performScroll(element=null, scrollCount, scrollCapacity, ScrollDirection.Left)
    }

    /**
     * Прокрутить вниз внутри конкретного элемента.
     */
    fun StepContext.swipeDown(
        element: PageElement? = null,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) {
        performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Down)
    }

    /**
     * Прокрутить вверх внутри конкретного элемента.
     */
    fun StepContext.swipeUp(
        element: PageElement? = null,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) {
        performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Up)
    }

    /**
     * Прокрутить вправо внутри конкретного элемента.
     */
    fun StepContext.swipeRight(
        element: PageElement? = null,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) {
        performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Right)
    }

    /**
     * Прокрутить влево внутри конкретного элемента.
     */
    fun StepContext.swipeLeft(
        element: PageElement? = null,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) {
        performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Left)
    }

    /**
     * Выполнить прокрутку в зависимости от направления (вверх, вниз, влево, вправо) и величины прокрутки.
     * Этот метод использует функцию evaluate() из Playwright для выполнения JavaScript-кода для прокрутки.
     */
    private fun performScroll(
        element: PageElement? = null,
        scrollCount: Int,
        scrollCapacity: Double,
        scrollDirection: ScrollDirection
    ) {
        require(scrollCapacity > 0.0 && scrollCapacity <= 1.0) {
            "scrollCapacity имеет значение $scrollCapacity, но может принимать значения от 0.0 до 1.0"
        }

        if (element != null) {
            val elementSelector = element.get() as String
            repeat(scrollCount) {
                when (scrollDirection) {
                    ScrollDirection.Down -> page.evaluate("document.querySelector('$elementSelector').scrollBy(0, document.querySelector('$elementSelector').scrollHeight * $scrollCapacity);")
                    ScrollDirection.Up -> page.evaluate("document.querySelector('$elementSelector').scrollBy(0, -document.querySelector('$elementSelector').scrollHeight * $scrollCapacity);")
                    ScrollDirection.Right -> page.evaluate("document.querySelector('$elementSelector').scrollBy(document.querySelector('$elementSelector').scrollWidth * $scrollCapacity, 0);")
                    ScrollDirection.Left -> page.evaluate("document.querySelector('$elementSelector').scrollBy(-document.querySelector('$elementSelector').scrollWidth * $scrollCapacity, 0);")
                }
            }
        } else {
            repeat(scrollCount) {
                when (scrollDirection) {
                    ScrollDirection.Down -> page.evaluate("window.scrollBy(0, window.innerHeight * $scrollCapacity);")
                    ScrollDirection.Up -> page.evaluate("window.scrollBy(0, -window.innerHeight * $scrollCapacity);")
                    ScrollDirection.Right -> page.evaluate("window.scrollBy(window.innerWidth * $scrollCapacity, 0);")
                    ScrollDirection.Left -> page.evaluate("window.scrollBy(-window.innerWidth * $scrollCapacity, 0);")
                }
            }
        }
    }

    /**
     * Найти все элементы на экране по его [selector] вернуть конкретный из списка по его номеру
     * @param selector элемент;
     * @param selectorNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
     *
     * @return the specific ElementHandle if found
     */
    private fun waitForElements(
        selector: String,
        selectorNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): ElementHandle? {
        Thread.sleep(timeoutBeforeExpectation * 1_000)

        var currentScroll = 0

        while (true) {
            try {
                // Ожидаем элементы
                val elements = page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout((timeoutExpectation * 1_000).toDouble()))
                val allElements = page.querySelectorAll(selector)

                if (allElements != null && allElements.isNotEmpty()) {
                    val safeIndex = selectorNumber ?: 1
                    if (safeIndex < 1 || safeIndex > allElements.size) {
                        throw IndexOutOfBoundsException("Индекс элемента $selectorNumber вне допустимого диапазона")
                    }
                    return allElements[safeIndex - 1]
                }
            } catch (e: Exception) {
                // Если элементы не найдены и передано scrollCount > 0
                if (scrollCount > 0 && currentScroll < scrollCount) {
                    performScroll(
                        scrollCount = 1,
                        scrollCapacity = scrollCapacity,
                        scrollDirection = scrollDirection
                    )
                    currentScroll++
                } else {
                    throw NoSuchElementException("Элементы '$selector' не найдены за '$timeoutExpectation' секунд после '$currentScroll' скроллирований")
                }
            }
        }
    }
}