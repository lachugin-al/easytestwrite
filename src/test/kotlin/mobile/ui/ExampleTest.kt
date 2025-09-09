package mobile.ui

/**
 * Parameterized smoke test for mobile app launch.
 *
 * Checks:
 * 1) On startup, the region selection screen is displayed (element with the current region text).
 * 2) After selecting a region, the main screen opens.
 * 3) The bottom navigation bar is present with buttons: "Home", "Catalog", "Cart", "Profile".
 *
 * Parameterization:
 * - Uses @ValueSource — the test runs for each region in the list.
 * - To add a region, simply extend the array in @ValueSource.
 *
 * Features:
 * - Optional handling of system alerts/pop-ups for iOS/Android is provided.
 * - If a specific parameter run fails, the retry mechanism (configured in build.gradle.kts)
 *   will rerun only the failed run with the same parameter.
 */
import controller.mobile.base.MobileTest
import io.qameta.allure.Description
import io.qameta.allure.Feature
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import mobile.pages.ExampleMobilePage

class ExampleTest : MobileTest() {

    @Tag("Smoke")
    @ParameterizedTest(name = "Example Test with region selection: {0}")
    @ValueSource(strings = ["Россия", "Беларусь", "Казахстан", "Киргизия", "Армения", "Узбекистан", "Грузия", "Таджикистан"])
    @DisplayName("Example Test, app launch")
    @Description(
        """Verify that the app has launched, the region selection screen is displayed at startup,
        | and after loading, the bottom navigation bar with buttons 'Home', 'Catalog', 'Cart', 'Profile' is present."""
    )
    @Feature("Smoke")
    fun smokeWithParam(regionTerm: String) {
        exampleTest(regionTerm)
    }

    private fun exampleTest(regionTerm: String) {
        context.run {
            "App launch" {
                "Region selection screen is displayed" {
                    checkVisible(text = regionTerm)
                }
            }

            "Navigate to the main screen for region {$regionTerm}" {
                click(regionTerm)
            }

            optionalIos(
                { "Accept alert" { alert(timeoutExpectation = 3).accept() } },
                { "Subscribe to our updates" { click(ExampleMobilePage.dismissNotice) } },
                { "Dismiss alert" { alert().dismiss() } },
            )

            optionalAndroid(
                { "Skip update pop-up" { click(ExampleMobilePage.notNow, timeoutExpectation = 3) } }
            )

            "Verify bottom navigation bar presence" {
                "'Home' button in the navigation bar is displayed" {
                    checkVisible(ExampleMobilePage.homeNavBar)
                }
                "'Catalog' button in the navigation bar is displayed" {
                    checkVisible(ExampleMobilePage.catNavBar)
                }
                "'Cart' button in the navigation bar is displayed" {
                    checkVisible(ExampleMobilePage.cartNavBar)
                }
                "'Profile' button in the navigation bar is displayed" {
                    checkVisible(ExampleMobilePage.profileNavBar)
                }
            }
        }
    }
}
