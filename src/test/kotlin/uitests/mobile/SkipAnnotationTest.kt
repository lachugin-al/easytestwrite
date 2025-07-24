package uitests.mobile

import controller.mobile.MobileTest
import dsl.testing.Skip
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.slf4j.LoggerFactory

/**
 * Тестовый класс для проверки работы аннотации @Skip.
 */
class SkipAnnotationTest : MobileTest() {
    private val logger = LoggerFactory.getLogger(SkipAnnotationTest::class.java)

    @BeforeEach
    fun beforeEachTest() {
        logger.info("BeforeEach метод выполнен")
    }

//    @Test
    @DisplayName("Тест без аннотации Skip всегда выполняется")
    fun testWithoutSkip() {
        context.run {
            "Тест без аннотации Skip" {
                logger.info("Тест без аннотации Skip выполнен")
            }
        }
    }

//    @Test
    @Skip()
    @DisplayName("Тест с аннотацией Skip всегда пропускается")
    fun testWithSkipTrue() {
        context.run {
            "Этот тест не должен выполняться" {
                logger.info("Если вы видите это сообщение, то аннотация Skip не работает")
            }
        }
    }

//    @Test
    @DisplayName("Тест без аннотации Skip всегда выполняется")
    fun testWithSkipFalse() {
        context.run {
            "Тест без аннотации Skip" {
                logger.info("Тест без аннотации Skip выполнен")
            }
        }
    }

//    @Test
    @Skip(platform = "android")
    @DisplayName("Тест пропускается только на Android")
    fun testSkipOnAndroid() {
        context.run {
            "Этот тест выполняется только на iOS" {
                logger.info("Тест выполняется на iOS, но пропускается на Android")
            }
        }
    }

//    @Test
    @Skip(platform = "ios")
    @DisplayName("Тест пропускается только на iOS")
    fun testSkipOnIOS() {
        context.run {
            "Этот тест выполняется только на Android" {
                logger.info("Тест выполняется на Android, но пропускается на iOS")
            }
        }
    }
}
