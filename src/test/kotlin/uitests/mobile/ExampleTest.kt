package uitests.mobile

/**
 * Параметризованный smoke-тест загрузки мобильного приложения.
 *
 * Проверяем:
 * 1) На старте отображается экран выбора региона (элемент с текстом текущего региона).
 * 2) После выбора региона открывается главный экран.
 * 3) Внизу присутствует навигационная панель с кнопками: "Home", "Catalog", "Cart", "Profile".
 *
 * Параметризация:
 * - Используется @ValueSource — тест запускается для каждого региона из списка.
 * - Чтобы добавить регион, просто расширьте массив в @ValueSource.
 *
 * Особенности:
 * - Для iOS/Android предусмотрена необязательная обработка системных алертов/поп-апов.
 * - При падении конкретного параметра механизм retry (настроенный в build.gradle.kts) перезапустит
 *   только упавший прогон с тем же параметром.
 */
import controller.mobile.base.MobileTest
import io.qameta.allure.Description
import io.qameta.allure.Feature
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uipages.mobile.ExampleMobilePage

class ExampleTest : MobileTest() {

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
                { "Подпишитесь на наши обновления" { click(ExampleMobilePage.dismissNotice) } },
                { "Откллоняем alert" { alert().dismiss() } },
            )

            optionalAndroid(
                { "Пропускаем поп-ап обновления" { click(ExampleMobilePage.notNow, timeoutExpectation = 3) } }
            )

            "Проверяем наличие нижней навигационной панели" {
                "Кнопка 'Home' на навигационной панели отображается" {
                    checkVisible(ExampleMobilePage.homeNavBar)
                }
                "Кнопка 'Catalog' на навигационной панели отображается" {
                    checkVisible(ExampleMobilePage.catNavBar)
                }
                "Кнопка 'Cart' на навигационной панели отображается" {
                    checkVisible(ExampleMobilePage.cartNavBar)
                }
                "Кнопка 'Profile' на навигационной панели отображается" {
                    checkVisible(ExampleMobilePage.profileNavBar)
                }
            }
        }
    }
}
