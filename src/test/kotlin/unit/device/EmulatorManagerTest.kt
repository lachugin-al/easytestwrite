package unit.device

import app.config.AppConfig
import app.model.Platform
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.TerminalUtils
import device.EmulatorManager
import utils.TerminalUtils.CommandResult

class EmulatorManagerTest {

    @BeforeEach
    fun setUp() {
        mockkObject(TerminalUtils)
        mockkObject(AppConfig)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getEmulatorId returns first online emulator`() {
        val stdout = "List of devices attached\n" +
                "emulator-5554\tdevice\n" +
                "emulator-5556\toffline\n" +
                "127.0.0.1:5555\tdevice\n"
        every { TerminalUtils.runCommand(command = any(), timeout = any()) } returns CommandResult(stdout, "", 0, false)

        val id = EmulatorManager.getEmulatorId()
        assertEquals("emulator-5554", id)
    }

    @Test
    fun `getEmulatorId returns null on timeout or error`() {
        every { TerminalUtils.runCommand(command = any(), timeout = any()) } returns CommandResult("", "", 0, true)
        val id1 = EmulatorManager.getEmulatorId()
        assertEquals(null, id1)

        every { TerminalUtils.runCommand(command = any(), timeout = any()) } throws RuntimeException("adb failed")
        val id2 = EmulatorManager.getEmulatorId()
        assertEquals(null, id2)
    }

    @Test
    fun `getSimulatorId returns udid for booted simulator with matching name`() {
        val json = """
            {
              "devices": {
                "iOS 18.4": [
                  {"udid":"UDID-1","name":"iPhone 16 Plus","state":"Booted"},
                  {"udid":"UDID-2","name":"iPhone 15 Pro","state":"Shutdown"}
                ]
              }
            }
        """.trimIndent()
        every { TerminalUtils.runCommand(command = match { it.contains("xcrun") }, timeout = any()) } returns CommandResult(json, "", 0, false)

        val udid = EmulatorManager.getSimulatorId("iPhone 16 Plus")
        assertEquals("UDID-1", udid)
    }

    @Test
    fun `getSimulatorId returns null when not booted or name mismatch or blank json`() {
        val json = """
            {"devices": {"iOS 18.4": [{"udid":"X","name":"Other","state":"Booted"}]}}
        """.trimIndent()
        every { TerminalUtils.runCommand(command = match { it.contains("xcrun") }, timeout = any()) } returnsMany listOf(
            CommandResult(json, "", 0, false),
            CommandResult("", "", 0, false)
        )
        val udid1 = EmulatorManager.getSimulatorId("iPhone 16 Plus")
        assertEquals(null, udid1)
        val udid2 = EmulatorManager.getSimulatorId("iPhone 16 Plus")
        assertEquals(null, udid2)
    }

    @Test
    fun `startEmulator routes to Android start and returns its result`() {
        every { AppConfig.getPlatform() } returns Platform.ANDROID
        val manager = spyk(EmulatorManager, recordPrivateCalls = true)
        every { manager invoke "startAndroidEmulator" withArguments emptyList<Any>() } returns true
        assertTrue(manager.startEmulator())
        verify { manager invoke "startAndroidEmulator" withArguments emptyList<Any>() }
    }

    @Test
    fun `startEmulator routes to iOS start and handles exception`() {
        every { AppConfig.getPlatform() } returns Platform.IOS
        val manager = spyk(EmulatorManager, recordPrivateCalls = true)
        every { manager invoke "startIosSimulator" withArguments emptyList<Any>() } throws RuntimeException("boom")
        assertFalse(manager.startEmulator())
        verify { manager invoke "startIosSimulator" withArguments emptyList<Any>() }
    }

    @Test
    fun `stopEmulator routes to Android stop and returns its result`() {
        every { AppConfig.getPlatform() } returns Platform.ANDROID
        val manager = spyk(EmulatorManager, recordPrivateCalls = true)
        every { manager invoke "stopAndroidEmulator" withArguments emptyList<Any>() } returns true
        assertTrue(manager.stopEmulator())
        verify { manager invoke "stopAndroidEmulator" withArguments emptyList<Any>() }
    }

    @Test
    fun `stopEmulator routes to iOS stop and returns false on exception`() {
        every { AppConfig.getPlatform() } returns Platform.IOS
        val manager = spyk(EmulatorManager, recordPrivateCalls = true)
        every { manager invoke "stopIosSimulator" withArguments emptyList<Any>() } throws IllegalStateException("fail")
        assertFalse(manager.stopEmulator())
        verify { manager invoke "stopIosSimulator" withArguments emptyList<Any>() }
    }
}
