package uitests.mobile

/**
 * Пример использования параметризованных тестов в мобильном тестировании.
 *
 * В этом классе реализованы два подхода к параметризации тестов:
 * 1. Использование встроенных параметров через @ValueSource - метод exampleTestWithValueSource
 * 2. Использование параметров из CSV файла через @CsvFileSource - метод exampleTestWithCsvSource
 *
 * Для добавления новых поисковых запросов:
 * - В первом случае - добавьте новое значение в массив @ValueSource
 * - Во втором случае - добавьте новую строку в файл src/test/resources/search_terms.csv
 *
 * Оба метода используют общую логику тестирования, вынесенную в приватный метод runexampleTest.
 *
 * При запуске тестов с параметрами, каждый тест будет выполнен для каждого значения параметра.
 * В случае падения теста, механизм перезапуска в build.gradle.kts обеспечит повторный запуск
 * только упавшего теста с тем же параметром.
 */
import controller.mobile.MobileTest
import io.qameta.allure.Description
import io.qameta.allure.Feature
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvFileSource
import uipages.mobile.MobileExamplePage


class ExampleTest : MobileTest() {

    @ParameterizedTest(name = "Example Test с поиском: {0}")
    @ValueSource(strings = ["Стиральный", "Телефон"])
    @DisplayName("Example Test, загрузка приложения (встроенные параметры)")
    @Description(
        """Проверяем что приложение загрузилось, на старте отображается экран с выбором региона,
        | после загрузки приложение присутствует нижняя навигационная панель с кнопками 'Home', 'Catalog', 'Cart', 'Profile'"""
    )
    @Feature("Smoke")
    fun exampleTestWithValueSource(searchTerm: String) {
        runexampleTest(searchTerm)
    }


    @ParameterizedTest(name = "Example Test с поиском из CSV: {0}")
    @CsvFileSource(resources = ["/search_terms.csv"], numLinesToSkip = 1)
    @DisplayName("Example Test, загрузка приложения (параметры из CSV)")
    @Description(
        """Проверяем что приложение загрузилось, на старте отображается экран с выбором региона,
        | после загрузки приложение присутствует нижняя навигационная панель с кнопками 'Home', 'Catalog', 'Cart', 'Profile'"""
    )
    @Feature("Smoke")
    fun exampleTestWithCsvSource(searchTerm: String) {
        runexampleTest(searchTerm)
    }


    @Test
    @DisplayName("Example Test, загрузка приложения (без параметров)")
    @Description(
        """Проверяем что приложение загрузилось, на старте отображается экран с выбором региона,
        | после загрузки приложение присутствует нижняя навигационная панель с кнопками 'Home', 'Catalog', 'Cart', 'Profile'"""
    )
    @Feature("Smoke")
    fun exampleTestWithoutParam() {
        runexampleTest("Стиральный")
    }

    @Tag("Smoke")
    @ParameterizedTest(name = "Example Test с выбором региона: {0}")
    @ValueSource(strings = ["Россия", "Беларусь", "Казахстан", "Киргизия", "Армения", "Узбекистан", "Грузия", "Таджикистан"])
    @DisplayName("Example Test, загрузка приложения")
    @Description(
        """Проверяем что приложение загрузилось, на старте отображается экран с выбором региона,
        | после загрузки приложение присутствует нижняя навигационная панель с кнопками 'Home', 'Catalog', 'Cart', 'Profile'"""
    )
    @Feature("Smoke")
    fun smokeWithParam(regionTerm: String) {
        exampleTest(regionTerm)
    }


    private fun runexampleTest(searchTerm: String) {
        context.run {
            "Загрузка приложения" {
                "Экран с выбором региона отображается" {
                    checkVisible(MobileExamplePage.ruRegionRussiaText)
                }
            }

            "Переходим на главный экран приложения RU регион" {
                click(MobileExamplePage.ruRegionRussiaText)
            }

            "Проверяем наличие нижней навигационной панели" {
                "Кнопка 'Home' на навигационной панели отображается" {
                    checkVisible(MobileExamplePage.homeNavBar)
                }
                "Кнопка 'Catalog' на навигационной панели отображается" {
                    checkVisible(MobileExamplePage.catNavBar)
                }
                "Кнопка 'Cart' на навигационной панели отображается" {
                    checkVisible(MobileExamplePage.cartNavBar)
                }
                "Кнопка 'Profile' на навигационной панели отображается" {
                    checkVisible(MobileExamplePage.profileNavBar)
                }
            }

            "Поскроллим экран вверх и вниз" {
                scrollDown(scrollCount = 5)
                scrollUp(scrollCount = 2)
            }
        }
    }

    private fun exampleTest(regionTerm: String) {
        context.run {
            "Загрузка приложения" {
                "Экран с выбором региона отображается" {
                    checkVisible(text = regionTerm)
                }
            }

            "Переходим на главный экран приложения регион {$regionTerm}" {
                click(regionTerm)
            }

            optionalIos(
                { "Принимаем alert" { alert(timeoutExpectation = 3).accept() } },
                { "Подпишитесь на наши обновления" { click(MobileExamplePage.dismissNotice) } },
                { "Откллоняем alert" { alert().dismiss() } },
            )

            optionalAndroid(
                { "Пропускаем поп-ап обновления" { click(MobileExamplePage.notNow, timeoutExpectation = 3) } }
            )

            "Проверяем наличие нижней навигационной панели" {
                "Кнопка 'Home' на навигационной панели отображается" {
                    checkVisible(MobileExamplePage.homeNavBar)
                }
                "Кнопка 'Catalog' на навигационной панели отображается" {
                    checkVisible(MobileExamplePage.catNavBar)
                }
                "Кнопка 'Cart' на навигационной панели отображается" {
                    checkVisible(MobileExamplePage.cartNavBar)
                }
                "Кнопка 'Profile' на навигационной панели отображается" {
                    checkVisible(MobileExamplePage.profileNavBar)
                }
            }
        }
    }
}
