package app.model

/**
 * Enumeration describing the supported platforms for running tests.
 *
 * Used to determine the target environment when initializing drivers
 * and to select the appropriate logic of the test framework.
 *
 * @see app.driver.AndroidDriver
 * @see app.driver.IosDriver
 */
enum class Platform {
    /** Android platform (testing via Appium). */
    ANDROID,

    /** iOS platform (testing via Appium). */
    IOS
}
