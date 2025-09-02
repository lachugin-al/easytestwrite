package controller.mobile.interaction

import controller.mobile.element.PageElement
import controller.mobile.core.AppContext
import io.appium.java_client.MobileElement
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION

interface UiElementFinding : AppContext {

    fun performScroll(
        element: PageElement? = null,
        scrollCount: Int,
        scrollCapacity: Double,
        scrollDirection: ScrollDirection
    )

    /**
     * Найти элемент на экране по его [element]
     * @param element элемент;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     *
     * @return MobileElement
     */
    fun waitForElement(
        element: PageElement?,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): MobileElement {
        // Если timeoutBeforeExpectation > 0, ожидаем, пока страница не будет в стабильном состоянии
        if (timeoutBeforeExpectation > 0) {
            val beforeWait = WebDriverWait(driver, timeoutBeforeExpectation, pollingInterval)
            try {
                // Ожидаем, пока UI приложения станет стабильным, проверяя, перестал ли меняться исходный код страницы
                var previousPageSource = driver.pageSource
                beforeWait.until { d ->
                    try {
                        val currentPageSource = d.pageSource
                        val isStable = currentPageSource == previousPageSource
                        previousPageSource = currentPageSource

                        // Если исходный код страницы не изменился, считаем UI стабильным
                        // Но продолжаем проверку до истечения таймаута, чтобы убедиться, что он действительно стабилен
                        isStable
                    } catch (e: Exception) {
                        // Если возникла ошибка при получении исходного кода страницы, предполагаем, что UI стабилен
                        logger.debug("Ошибка при проверке стабильности страницы: ${e.message}")
                        true
                    }
                }
                logger.debug("UI выглядит стабильным после ожидания")
            } catch (e: Exception) {
                // Если ожидание не удалось, логируем ошибку, но продолжаем поиск элемента
                logger.warn("Ошибка при ожидании стабильности UI: ${e.message}")
            }
        }

        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        val pageElement = element?.get()
        var currentScroll = 0
        while (true) {
            try {
                return wait.until(ExpectedConditions.visibilityOfElementLocated(pageElement as By)) as MobileElement
            } catch (e: Exception) {
                // Если элемент не найден и передано scrollCount > 0
                if (scrollCount > 0 && currentScroll < scrollCount) {
                    performScroll(
                        scrollCount = 1,
                        scrollCapacity = scrollCapacity,
                        scrollDirection = scrollDirection
                    )
                    currentScroll++
                } else {
                    throw NoSuchElementException("Элемент '$pageElement' не найден за '$timeoutExpectation' секунд после '$currentScroll' скроллирований")
                }
            }
        }
    }

    /**
     * Найти все элементы на экране по его [element] вернуть конкретный из списка по его номеру
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
     *
     * @return MobileElement
     */
    fun waitForElements(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): MobileElement {
        // Если timeoutBeforeExpectation > 0, ожидаем, пока страница не будет в стабильном состоянии
        if (timeoutBeforeExpectation > 0) {
            val beforeWait = WebDriverWait(driver, timeoutBeforeExpectation, pollingInterval)
            try {
                // Ожидаем, пока UI приложения станет стабильным, проверяя, перестал ли меняться исходный код страницы
                var previousPageSource = driver.pageSource
                beforeWait.until { d ->
                    try {
                        val currentPageSource = d.pageSource
                        val isStable = currentPageSource == previousPageSource
                        previousPageSource = currentPageSource

                        // Если исходный код страницы не изменился, считаем UI стабильным
                        // Но продолжаем проверку до истечения таймаута, чтобы убедиться, что он действительно стабилен
                        isStable
                    } catch (e: Exception) {
                        // Если возникла ошибка при получении исходного кода страницы, предполагаем, что UI стабилен
                        logger.debug("Ошибка при проверке стабильности страницы: ${e.message}")
                        true
                    }
                }
                logger.debug("UI выглядит стабильным после ожидания")
            } catch (e: Exception) {
                // Если ожидание не удалось, логируем ошибку, но продолжаем поиск элемента
                logger.warn("Ошибка при ожидании стабильности UI: ${e.message}")
            }
        }

        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        var currentScroll = 0

        while (true) {
            // Получаем все локаторы для текущей платформы
            val locators = element?.getAll() ?: listOf(element?.get())
            var lastException: Exception? = null
            val attemptedLocators = mutableListOf<Any>()
            val failedLocators = mutableListOf<Any>()

            // Перебираем все локаторы
            for (locator in locators.filterNotNull()) {
                try {
                    val pageElement = locator
                    attemptedLocators.add(pageElement)
                    val elements =
                        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(pageElement as By)) as List<MobileElement>

                    val safeIndex = elementNumber ?: 1
                    if (safeIndex < 1 || safeIndex > elements.size) {
                        throw IndexOutOfBoundsException("Элемент $elementNumber вне допустимого диапазона")
                    }

                    // Логируем информацию о неудачных попытках, даже если текущий локатор успешен
                    if (failedLocators.isNotEmpty()) {
                        logger.info("Следующие локаторы ${failedLocators.joinToString(", ")} из списка ${attemptedLocators} были не найдены.")
                    }

                    return elements[safeIndex - 1]
                } catch (e: Exception) {
                    // Сохраняем последнее исключение
                    lastException = e
                    // Добавляем локатор в список неудачных
                    failedLocators.add(locator)
                    // Продолжаем перебор локаторов
                    continue
                }
            }

            // Если ни один локатор не сработал и передано scrollCount > 0
            if (scrollCount > 0 && currentScroll < scrollCount) {
                performScroll(
                    scrollCount = 1,
                    scrollCapacity = scrollCapacity,
                    scrollDirection = scrollDirection
                )
                currentScroll++
            } else {
                // Используем информацию о последнем исключении и используемых локаторах для более детального сообщения об ошибке
                val locatorsInfo = if (failedLocators.isNotEmpty()) {
                    "Следующие локаторы ${failedLocators.joinToString(", ")} из списка ${attemptedLocators} были не найдены."
                } else if (attemptedLocators.isNotEmpty()) {
                    "Попытались найти следующие элементы: ${attemptedLocators.joinToString(", ")}"
                } else {
                    "Не было найдено ни одного элемента"
                }

                val errorMessage = if (lastException != null) {
                    "Элементы не найдены за '$timeoutExpectation' секунд после '$currentScroll' скроллирований. $locatorsInfo. Причина: ${lastException.message}"
                } else {
                    "Элементы не найдены за '$timeoutExpectation' секунд после '$currentScroll' скроллирований. $locatorsInfo"
                }
                throw NoSuchElementException(errorMessage)
            }
        }
    }

    /**
     * Найти элемент на экране по его [element] либо вернуть null
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
     *
     * @return MobileElement or null
     */
    fun waitForElementOrNull(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): MobileElement? {
        return try {
            waitForElements(
                element = element,
                elementNumber = elementNumber,
                timeoutBeforeExpectation = timeoutBeforeExpectation,
                timeoutExpectation = timeoutExpectation,
                pollingInterval = pollingInterval,
                scrollCount = scrollCount,
                scrollCapacity = scrollCapacity,
                scrollDirection = scrollDirection
            )
        } catch (e: NoSuchElementException) {
            null
        }
    }

}