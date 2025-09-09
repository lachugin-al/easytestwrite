package mobile.ui

import controller.mobile.base.MobileTest
import dsl.testing.Skip
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.slf4j.LoggerFactory

/**
 * Test class for verifying the @Skip annotation behavior.
 */
class SkipAnnotationTest : MobileTest() {
    override val logger = LoggerFactory.getLogger(SkipAnnotationTest::class.java)

    @BeforeEach
    fun beforeEachTest() {
        logger.info("BeforeEach method executed")
    }

    @Test
    @DisplayName("Test without Skip annotation always runs")
    fun testWithoutSkip() {
        context.run {
            "Test without Skip annotation" {
                logger.info("Test without Skip annotation executed")
            }
        }
    }

    @Test
    @Skip()
    @DisplayName("Test with Skip annotation is always skipped")
    fun testWithSkipTrue() {
        context.run {
            "This test should not run" {
                logger.info("If you see this message, the Skip annotation is not working")
            }
        }
    }

    @Test
    @DisplayName("Test without Skip annotation always runs")
    fun testWithSkipFalse() {
        context.run {
            "Test without Skip annotation" {
                logger.info("Test without Skip annotation executed")
            }
        }
    }

    @Test
    @Skip(platform = "android")
    @DisplayName("Test is skipped only on Android")
    fun testSkipOnAndroid() {
        context.run {
            "This test runs only on iOS" {
                logger.info("Test runs on iOS but is skipped on Android")
            }
        }
    }

    @Test
    @Skip(platform = "ios")
    @DisplayName("Test is skipped only on iOS")
    fun testSkipOnIOS() {
        context.run {
            "This test runs only on Android" {
                logger.info("Test runs on Android but is skipped on iOS")
            }
        }
    }
}
