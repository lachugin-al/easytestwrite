package device

import app.config.AppConfig
import app.model.Platform
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.TerminalUtils
import device.model.SimulatorsResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Class for managing the lifecycle of emulators and simulators.
 *
 * Provides methods to start and stop Android emulators and iOS simulators.
 * The implementation accounts for various edge cases and errors that may occur when working with emulators.
 */
object EmulatorManager {
    private val logger: Logger = LoggerFactory.getLogger(EmulatorManager::class.java)

    // Mutex to prevent races when starting/stopping emulators in parallel
    private val emulatorMutex = Mutex()

    // Timeouts
    private const val EMULATOR_BOOT_TIMEOUT_SECONDS = 120
    private const val EMULATOR_STARTUP_TIMEOUT_SECONDS = 60
    private const val COMMAND_TIMEOUT_SECONDS = 30

    // Generic condition wait without using Thread.sleep
    private fun waitForCondition(
        timeout: Duration,
        pollInterval: Duration = Duration.ofMillis(500),
        onTick: ((elapsed: Duration) -> Unit)? = null,
        condition: () -> Boolean
    ): Boolean {
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        val latch = CountDownLatch(1)
        val startNs = System.nanoTime()

        val task = Runnable {
            try {
                val elapsed = Duration.ofNanos(System.nanoTime() - startNs)
                if (condition()) {
                    latch.countDown()
                } else {
                    onTick?.invoke(elapsed)
                }
            } catch (t: Throwable) {
                // Do not break the scheduler because of exceptions in condition/onTick
                logger.debug("Error in wait task: ${t.message}")
            }
        }

        val future = scheduler.scheduleAtFixedRate(
            task,
            0,
            pollInterval.toMillis(),
            TimeUnit.MILLISECONDS
        )

        val completed = try {
            latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)
        } finally {
            future.cancel(true)
            scheduler.shutdownNow()
        }

        return completed
    }

    /**
     * Starts an emulator or simulator depending on the current platform.
     *
     * Handles the following edge cases:
     * - Unknown platform
     * - Unknown/empty device name
     * - Emulator/simulator already running but not responsive
     * - Unable to obtain device ID
     * - Error starting emulator/simulator process
     * - Emulator/simulator does not start within timeout
     * - Parallel start/races
     * - Output localization/encoding
     * - Runtime exceptions
     * - Command execution errors/hangs
     *
     * @return `true` if the start is successful, otherwise `false`.
     */
    fun startEmulator(): Boolean {
        return try {
            // Use a mutex to prevent races during parallel start
            if (!emulatorMutex.tryLock()) {
                logger.warn("Another thread is already performing emulator operations, waiting...")
                val acquired = waitForCondition(
                    timeout = Duration.ofSeconds(5),
                    pollInterval = Duration.ofMillis(200)
                ) {
                    emulatorMutex.tryLock()
                }
                if (!acquired) {
                    logger.error("Failed to acquire lock to start emulator")
                    return false
                }
            }

            // Check platform
            when (val platform = AppConfig.getPlatform()) {
                Platform.ANDROID -> startAndroidEmulator()
                Platform.IOS -> startIosSimulator()
                else -> {
                    logger.info("Emulator start is not required for platform $platform")
                    true
                }
            }
        } catch (e: Exception) {
            logger.error("Unexpected error when starting emulator: ${e.message}", e)
            false
        } finally {
            if (emulatorMutex.isLocked) {
                emulatorMutex.unlock()
            }
        }
    }

    /**
     * Stops an emulator or simulator depending on the current platform.
     *
     * Handles the following edge cases:
     * - Unknown platform
     * - Unsuccessful device stop (and it keeps hanging)
     * - Parallel stop/races
     * - Output localization/encoding
     * - Runtime exceptions
     * - Command execution errors/hangs
     *
     * @return `true` if the stop is successful, otherwise `false`.
     */
    fun stopEmulator(): Boolean {
        return try {
            // Use a mutex to prevent races during parallel stop
            if (!emulatorMutex.tryLock()) {
                logger.warn("Another thread is already performing emulator operations, waiting...")
                val acquired = waitForCondition(
                    timeout = Duration.ofSeconds(5),
                    pollInterval = Duration.ofMillis(200)
                ) {
                    emulatorMutex.tryLock()
                }
                if (!acquired) {
                    logger.error("Failed to acquire lock to stop emulator")
                    return false
                }
            }

            // Check platform
            when (val platform = AppConfig.getPlatform()) {
                Platform.ANDROID -> stopAndroidEmulator()
                Platform.IOS -> stopIosSimulator()
                else -> {
                    logger.info("Emulator stop is not required for platform $platform")
                    true
                }
            }
        } catch (e: Exception) {
            logger.error("Unexpected error when stopping emulator: ${e.message}", e)
            false
        } finally {
            if (emulatorMutex.isLocked) {
                emulatorMutex.unlock()
            }
        }
    }

    /**
     * Checks for required utilities to work with emulators/simulators.
     *
     * @param commands List of commands to check.
     * @return `true` if all utilities are available, otherwise `false`.
     */
    private fun checkRequiredTools(commands: List<String>): Boolean {
        for (command in commands) {
            try {
                val cmd = if (System.getProperty("os.name").lowercase().contains("windows")) {
                    listOf("where", command)
                } else {
                    listOf("which", command)
                }
                val result = TerminalUtils.runCommand(
                    command = cmd,
                    timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                )
                if (result.timedOut || result.exitCode != 0) {
                    logger.error("Utility '$command' not found on the system")
                    return false
                }
            } catch (e: Exception) {
                logger.error("Error while checking utility '$command': ${e.message}", e)
                return false
            }
        }
        return true
    }

    /**
     * Waits for Android emulator to fully boot.
     *
     * Handles the following edge cases:
     * - Emulator does not start within timeout
     * - Command execution errors/hangs
     * - Output localization/encoding
     *
     * @param deviceId ID of the emulator to wait for.
     * @return `true` if the emulator has successfully booted, otherwise `false`.
     */
    private fun waitForEmulatorBoot(deviceId: String): Boolean {
        if (deviceId.isBlank()) {
            logger.error("Cannot wait for emulator boot: empty device ID")
            return false
        }

        val maxAttempts = EMULATOR_BOOT_TIMEOUT_SECONDS / 2 // Check every 2 seconds
        for (i in 1..maxAttempts) {
            try {
                val result = TerminalUtils.runCommand(
                    command = listOf("adb", "-s", deviceId, "shell", "getprop", "sys.boot_completed"),
                    timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                )

                if (result.timedOut) {
                    logger.warn("Timeout while checking boot status of emulator $deviceId, attempt $i/$maxAttempts")
                    continue
                }

                val bootCompleted = result.stdout.trim()

                if (bootCompleted == "1") {
                    // Additional responsiveness check
                    if (isEmulatorResponsive(deviceId)) {
                        logger.info("Emulator $deviceId is fully booted and ready.")
                        return true
                    } else {
                        logger.warn("Emulator $deviceId booted but is not responding to commands")
                    }
                } else {
                    logger.info("Emulator $deviceId is not ready yet (sys.boot_completed=$bootCompleted), attempt $i/$maxAttempts")
                }
            } catch (e: Exception) {
                logger.warn("Error while checking boot status of emulator $deviceId: ${e.message}")
            }

            // Non-blocking wait of 2 seconds between checks
            waitForCondition(
                timeout = Duration.ofSeconds(2),
                pollInterval = Duration.ofMillis(200)
            ) { false }
        }

        logger.error("Emulator $deviceId did not boot within the allotted time ($EMULATOR_BOOT_TIMEOUT_SECONDS seconds)")
        return false
    }

    /**
     * Checks whether the emulator responds to commands.
     *
     * @param deviceId ID of the emulator to check.
     * @return `true` if the emulator responds to commands, otherwise `false`.
     */
    private fun isEmulatorResponsive(deviceId: String): Boolean {
        try {
            val result = TerminalUtils.runCommand(
                command = listOf("adb", "-s", deviceId, "shell", "pm", "list", "packages"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Timeout while checking responsiveness of emulator $deviceId")
                return false
            }
            val output = result.stdout
            return output.contains("package:") && result.exitCode == 0
        } catch (e: Exception) {
            logger.warn("Error while checking responsiveness of emulator $deviceId: ${e.message}")
            return false
        }
    }

    /**
     * Starts the Android emulator.
     *
     * Handles the following edge cases:
     * - Unknown/empty device name
     * - Emulator already running but not responsive
     * - Unable to obtain device ID
     * - Error starting the emulator process
     * - Emulator does not start within timeout
     * - Missing or incorrect versions of external utilities
     * - Output localization/encoding
     * - Runtime exceptions
     * - Command execution errors/hangs
     *
     * @return `true` if the start is successful, otherwise `false`.
     */
    private fun startAndroidEmulator(): Boolean {
        try {
            // Check required utilities
            if (!checkRequiredTools(listOf("adb", "emulator"))) {
                logger.error("Required utilities for starting Android emulator are missing")
                return false
            }

            // Get device name
            val deviceName = AppConfig.getAndroidDeviceName()
            if (deviceName.isBlank()) {
                logger.error("Android device name for start is not specified")
                return false
            }

            logger.info("Starting Android emulator: $deviceName")

            // Verify the emulator exists
            if (!checkEmulatorExists(deviceName)) {
                logger.error("Emulator named '$deviceName' not found on the system")
                return false
            }

            // Check if emulator already running
            val emulatorId = getEmulatorId()
            if (emulatorId != null) {
                logger.info("Android emulator already running with ID: $emulatorId")

                // Check responsiveness
                if (isEmulatorResponsive(emulatorId)) {
                    logger.info("Android emulator with ID: $emulatorId is responsive")
                    return true
                } else {
                    logger.warn("Android emulator with ID: $emulatorId is not responding, trying to restart")
                    // Force stop the non-responsive emulator
                    forceStopAndroidEmulator(emulatorId)
                }
            }

            // Start the emulator with extra stability flags
            val commandList = mutableListOf(
                "emulator",
                "-avd",
                deviceName,
                "-no-snapshot-load",
                "-no-boot-anim",
                "-gpu", "swiftshader_indirect",
                "-no-audio"
            )

            // Add "-no-window" only if headless mode is enabled
            if (AppConfig.isAndroidHeadlessMode()) {
                commandList.add("-no-window")  // Headless start for stability in CI/CD
            }

            val command = commandList

            logger.info("Running command: ${command.joinToString(" ")}")

            // Start the emulator in a separate process so as not to block test execution
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            // Create a thread to read process output to avoid buffer blocking
            Thread {
                try {
                    process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            logger.debug("Emulator output: $line")
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Error while reading emulator output: ${e.message}")
                }
            }.start()

            // Wait for the emulator to start and get an ID, with timeout
            val maxAttempts = EMULATOR_STARTUP_TIMEOUT_SECONDS / 2 // Check every 2 seconds
            var newEmulatorId: String? = null

            for (i in 1..maxAttempts) {
                // Check if process exited with an error
                if (!process.isAlive && process.exitValue() != 0) {
                    logger.error("Emulator process exited with error, code: ${process.exitValue()}")
                    return false
                }

                newEmulatorId = getEmulatorId()
                if (newEmulatorId != null) {
                    logger.info("Android emulator started with ID: $newEmulatorId after $i attempts")
                    break
                } else {
                    logger.info("Waiting for Android emulator to start, attempt $i/$maxAttempts")
                    // Non-blocking wait of 2 seconds between checks
                    waitForCondition(
                        timeout = Duration.ofSeconds(2),
                        pollInterval = Duration.ofMillis(200)
                    ) { false }
                }
            }

            if (newEmulatorId != null) {
                logger.info("Android emulator started with ID: $newEmulatorId")
                // Wait for full boot
                val bootSuccess = waitForEmulatorBoot(newEmulatorId)

                if (!bootSuccess) {
                    // If emulator did not fully boot, try to stop it
                    logger.warn("Emulator did not fully boot, attempting to stop it")
                    forceStopAndroidEmulator(newEmulatorId)
                    return false
                }

                // Network configuration: enable Wi-Fi and disable mobile data, then verify status
                val networkConfigured = configureAndroidNetwork(newEmulatorId)
                if (!networkConfigured) {
                    logger.error("Failed to configure network (Wi-Fi ON, Data OFF) on emulator $newEmulatorId")
                    // We could try to gently restart the network/emulator, but per requirements — return failure
                    return false
                }

                return true
            }

            logger.error("Failed to start Android emulator within the allotted time ($EMULATOR_STARTUP_TIMEOUT_SECONDS seconds)")
            // Kill emulator process if still running
            if (process.isAlive) {
                process.destroyForcibly()
            }
            return false

        } catch (e: Exception) {
            logger.error("Error while starting Android emulator: ${e.message}", e)
            return false
        }
    }

    /**
     * Checks whether an emulator with the given name exists.
     *
     * @param deviceName Emulator name to check.
     * @return `true` if the emulator exists, otherwise `false`.
     */
    private fun checkEmulatorExists(deviceName: String): Boolean {
        try {
            val result = TerminalUtils.runCommand(
                command = listOf("emulator", "-list-avds"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Timeout while retrieving emulator list")
                return false
            }
            val output = result.stdout
            return output.lines().any { it.trim() == deviceName }
        } catch (e: Exception) {
            logger.warn("Error while checking emulator existence: ${e.message}")
            return false
        }
    }

    /**
     * Gets the ID of a running Android emulator.
     *
     * @return Emulator ID or `null` if no emulator is running.
     */
    fun getEmulatorId(): String? {
        try {
            val result = TerminalUtils.runCommand(
                command = listOf("adb", "devices"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Timeout while retrieving device list")
                return null
            }
            val output = result.stdout
            // Look for a line containing both "emulator-" and "device"
            return output.lines()
                .filter { it.contains("emulator-") && it.contains("device") }
                .map { it.split("\\s+".toRegex()).first() }
                .firstOrNull()
        } catch (e: Exception) {
            logger.warn("Error while obtaining emulator ID: ${e.message}")
            return null
        }
    }

    /**
     * Force stops the Android emulator.
     *
     * @param emulatorId Emulator ID to stop.
     */
    private fun forceStopAndroidEmulator(emulatorId: String) {
        try {
            // First try to stop via adb emu kill
            val result1 = TerminalUtils.runCommand(
                command = listOf("adb", "-s", emulatorId, "emu", "kill"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result1.timedOut) {
                logger.warn("Timeout while stopping emulator via adb emu kill")
            }

            // Wait up to 2 seconds, giving the emulator a chance to stop without Thread.sleep
            waitForCondition(
                timeout = Duration.ofSeconds(2),
                pollInterval = Duration.ofMillis(200)
            ) { getEmulatorId() == null }

            // Check if emulator has stopped
            if (getEmulatorId() != null) {
                logger.warn("Emulator did not stop via adb emu kill, trying killall")

                // If not stopped, try killall (Linux/Mac)
                if (!System.getProperty("os.name").lowercase().contains("windows")) {
                    TerminalUtils.runCommand(
                        command = listOf("killall", "-9", "qemu-system-x86_64"),
                        timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                    )
                } else {
                    // For Windows use taskkill
                    TerminalUtils.runCommand(
                        command = listOf("taskkill", "/F", "/IM", "qemu-system-x86_64.exe"),
                        timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("Error during forced stop of Android emulator: ${e.message}")
        }
    }

    /**
     * Stops the Android emulator.
     *
     * Handles the following edge cases:
     * - Unsuccessful device stop (and it keeps hanging)
     * - Missing or incorrect versions of external utilities
     * - Output localization/encoding
     * - Runtime exceptions
     * - Command execution errors/hangs
     *
     * @return `true` if the stop is successful, otherwise `false`.
     */
    private fun stopAndroidEmulator(): Boolean {
        try {
            // Check required utilities
            if (!checkRequiredTools(listOf("adb"))) {
                logger.error("Required utilities for stopping Android emulator are missing")
                return false
            }

            val emulatorId = getEmulatorId()
            if (emulatorId == null) {
                logger.info("Android emulator is not running, no stop required")
                return true
            }

            logger.info("Stopping Android emulator with ID: $emulatorId")

            // Try to stop the emulator in a standard way
            val result = TerminalUtils.runCommand(
                command = listOf("adb", "-s", emulatorId, "emu", "kill"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Timeout while stopping Android emulator")
            }

            // After sending the stop command, wait giving the emulator a chance to stop gracefully.
            // Use env ANDROID_EMULATOR_WAIT_TIME_BEFORE_KILL (seconds), if provided.
            val envWaitSeconds = System.getenv("ANDROID_EMULATOR_WAIT_TIME_BEFORE_KILL")?.toIntOrNull()
            // By default the emulator waits 20s itself, give a bit more headroom.
            val gracefulWaitSeconds = (envWaitSeconds ?: 20) + 5

            val stoppedGracefully = waitForCondition(
                timeout = Duration.ofSeconds(gracefulWaitSeconds.toLong()),
                pollInterval = Duration.ofSeconds(1),
                onTick = { elapsed ->
                    val elapsedSec = elapsed.seconds
                    if (elapsedSec == 0L || elapsedSec % 5L == 0L) {
                        logger.debug("Waiting for emulator to stop... ${'$'}elapsedSec/${'$'}gracefulWaitSeconds s")
                    }
                }
            ) {
                getEmulatorId() == null
            }

            if (!stoppedGracefully && getEmulatorId() != null) {
                logger.warn("Android emulator did not stop gracefully within ${'$'}gracefulWaitSeconds s, applying forced stop")
                forceStopAndroidEmulator(emulatorId)

                // Check again with a short wait without Thread.sleep
                val stoppedAfterForce = waitForCondition(
                    timeout = Duration.ofSeconds(5),
                    pollInterval = Duration.ofMillis(500)
                ) {
                    getEmulatorId() == null
                }
                val stillRunningAfterForce = !stoppedAfterForce
                if (stillRunningAfterForce) {
                    logger.error("Failed to stop Android emulator even forcibly")
                    return false
                }
            }

            logger.info("Android emulator stopped successfully")
            return true
        } catch (e: Exception) {
            logger.error("Error while stopping Android emulator: ${e.message}", e)
            return false
        }
    }

    /**
     * Starts the iOS simulator.
     *
     * Handles the following edge cases:
     * - Unknown/empty device name
     * - Simulator already running but not responsive
     * - Unable to obtain device ID
     * - Error starting the simulator process
     * - Simulator does not start within timeout
     * - Error parsing JSON output from the simulator
     * - Missing or incorrect versions of external utilities
     * - Output localization/encoding
     * - Runtime exceptions
     * - Command execution errors/hangs
     *
     * @return `true` if the start is successful, otherwise `false`.
     */
    private fun startIosSimulator(): Boolean {
        try {
            // Ensure we are on macOS (iOS simulators work only on macOS)
            if (!System.getProperty("os.name").contains("Mac")) {
                logger.error("iOS simulators are supported only on macOS")
                return false
            }

            // Check required utilities
            if (!checkRequiredTools(listOf("xcrun"))) {
                logger.error("Required utilities for starting iOS simulator are missing")
                return false
            }

            // Get device name
            val deviceName = AppConfig.getIosDeviceName()
            if (deviceName.isBlank()) {
                logger.error("iOS device name for start is not specified")
                return false
            }

            logger.info("Starting iOS simulator: $deviceName")

            // Check if simulator already running
            val simulatorId = getSimulatorId(deviceName)
            if (simulatorId != null) {
                logger.info("iOS simulator already running with ID: $simulatorId")

                // Check responsiveness
                if (isSimulatorResponsive(simulatorId)) {
                    logger.info("iOS simulator with ID: $simulatorId is responsive")
                    return true
                } else {
                    logger.warn("iOS simulator with ID: $simulatorId is not responding, attempting restart")
                    // Force stop the non-responsive simulator
                    forceStopIosSimulator(simulatorId)
                }
            }

            // Get list of all available simulators
            val simulatorsJson = getSimulatorsList()
            if (simulatorsJson.isBlank()) {
                logger.error("Failed to obtain list of available iOS simulators")
                return false
            }

            // Parse JSON with error handling
            try {
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }

                val simulatorsResponse = json.decodeFromString<SimulatorsResponse>(simulatorsJson)

                // Find simulator with the desired name
                var foundSimulatorId: String? = null

                for (runtime in simulatorsResponse.devices.values) {
                    val simulator = runtime.find { it.name == deviceName }
                    if (simulator != null) {
                        foundSimulatorId = simulator.udid
                        break
                    }
                }

                if (foundSimulatorId == null) {
                    logger.error("iOS simulator with name '$deviceName' not found")
                    return false
                }

                // Start the simulator
                logger.info("Booting iOS simulator with ID: $foundSimulatorId")

                val result = TerminalUtils.runCommand(
                    command = listOf("xcrun", "simctl", "boot", foundSimulatorId),
                    timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                )
                if (result.timedOut) {
                    logger.error("Timeout while booting iOS simulator")
                    return false
                }
                if (result.exitCode != 0) {
                    val error = if (result.stderr.isNotBlank()) result.stderr else result.stdout
                    logger.error("Error while booting iOS simulator: $error")
                    return false
                }

                // Wait until the simulator is fully booted
                val maxAttempts = EMULATOR_BOOT_TIMEOUT_SECONDS / 2 // Check every 2 seconds
                for (i in 1..maxAttempts) {
                    if (isSimulatorResponsive(foundSimulatorId)) {
                        logger.info("iOS simulator successfully started and ready")
                        return true
                    } else {
                        logger.info("Waiting for iOS simulator to boot, attempt $i/$maxAttempts")
                        waitForCondition(
                            timeout = Duration.ofSeconds(2),
                            pollInterval = Duration.ofMillis(200)
                        ) { false }
                    }
                }

                logger.error("iOS simulator did not become responsive within the allotted time")
                return false

            } catch (e: Exception) {
                logger.error("Error while parsing simulator JSON output: ${e.message}", e)
                return false
            }
        } catch (e: Exception) {
            logger.error("Error while starting iOS simulator: ${e.message}", e)
            return false
        }
    }

    /**
     * Retrieves a list of all available iOS simulators in JSON format.
     *
     * @return JSON string with the list of simulators or an empty string in case of error.
     */
    private fun getSimulatorsList(): String {
        try {
            val result = TerminalUtils.runCommand(
                command = listOf("xcrun", "simctl", "list", "--json"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Timeout while retrieving list of iOS simulators")
                return ""
            }
            return result.stdout
        } catch (e: Exception) {
            logger.warn("Error while retrieving list of iOS simulators: ${e.message}")
            return ""
        }
    }

    /**
     * Gets the ID of a running iOS simulator by its name.
     *
     * @param simulatorName Simulator name.
     * @return Simulator ID or `null` if the simulator is not running.
     */
    fun getSimulatorId(simulatorName: String): String? {
        try {
            val simulatorsJson = getSimulatorsList()
            if (simulatorsJson.isBlank()) {
                return null
            }

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            val simulatorsResponse = json.decodeFromString<SimulatorsResponse>(simulatorsJson)

            // Look for a running simulator with the desired name
            for (runtime in simulatorsResponse.devices.values) {
                val simulator = runtime.find { it.name == simulatorName && it.state == "Booted" }
                if (simulator != null) {
                    return simulator.udid
                }
            }

            return null
        } catch (e: Exception) {
            logger.warn("Error while obtaining iOS simulator ID: ${e.message}")
            return null
        }
    }

    /**
     * Checks whether the iOS simulator responds to commands.
     *
     * @param simulatorId Simulator ID to check.
     * @return `true` if the simulator responds to commands, otherwise `false`.
     */
    private fun isSimulatorResponsive(simulatorId: String): Boolean {
        try {
            val result = TerminalUtils.runCommand(
                command = listOf("xcrun", "simctl", "list", "--json"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Timeout while checking iOS simulator state")
                return false
            }
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            val simulatorsResponse = json.decodeFromString<SimulatorsResponse>(result.stdout)
            // Consider the simulator responsive if it has state "Booted"
            val isBooted = simulatorsResponse.devices.values.any { runtimeList ->
                runtimeList.any { it.udid == simulatorId && it.state == "Booted" }
            }
            return isBooted
        } catch (e: Exception) {
            logger.warn("Error while checking iOS simulator state: ${e.message}")
            return false
        }
    }

    /**
     * Force stops the iOS simulator.
     *
     * @param simulatorId Simulator ID to stop.
     */
    private fun forceStopIosSimulator(simulatorId: String) {
        try {
            // First attempt the standard way
            val result1 = TerminalUtils.runCommand(
                command = listOf("xcrun", "simctl", "shutdown", simulatorId),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result1.timedOut) {
                logger.warn("Timeout while stopping iOS simulator")
            }

            // Wait up to 2 seconds without Thread.sleep, giving the simulator a chance to stop
            waitForCondition(
                timeout = Duration.ofSeconds(2),
                pollInterval = Duration.ofMillis(200)
            ) { false }

            // If that didn't help, try stopping all simulators
            TerminalUtils.runCommand(
                command = listOf("xcrun", "simctl", "shutdown", "all"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )

            // If still not stopped, try killall
            TerminalUtils.runCommand(
                command = listOf("killall", "Simulator"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
        } catch (e: Exception) {
            logger.warn("Error during forced stop of iOS simulator: ${e.message}")
        }
    }

    /**
     * Stops the iOS simulator.
     *
     * Handles the following edge cases:
     * - Unsuccessful device stop (and it keeps hanging)
     * - Missing or incorrect versions of external utilities
     * - Output localization/encoding
     * - Runtime exceptions
     * - Command execution errors/hangs
     *
     * @return `true` if the stop is successful, otherwise `false`.
     */
    private fun stopIosSimulator(): Boolean {
        try {
            // Ensure we are on macOS (iOS simulators work only on macOS)
            if (!System.getProperty("os.name").contains("Mac")) {
                logger.error("iOS simulators are supported only on macOS")
                return false
            }

            // Check required utilities
            if (!checkRequiredTools(listOf("xcrun"))) {
                logger.error("Required utilities for stopping iOS simulator are missing")
                return false
            }

            val deviceName = AppConfig.getIosDeviceName()
            if (deviceName.isBlank()) {
                logger.error("iOS device name for stop is not specified")
                return false
            }

            val simulatorId = getSimulatorId(deviceName)
            if (simulatorId == null) {
                logger.info("iOS simulator is not running, no stop required")
                return true
            }

            logger.info("Stopping iOS simulator with ID: $simulatorId")

            // Try to stop the simulator in a standard way
            val result = TerminalUtils.runCommand(
                command = listOf("xcrun", "simctl", "shutdown", simulatorId),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Timeout while stopping iOS simulator")
            }

            // Wait up to 2 seconds without Thread.sleep, giving the simulator a chance to stop
            waitForCondition(
                timeout = Duration.ofSeconds(2),
                pollInterval = Duration.ofMillis(200)
            ) { getSimulatorId(deviceName) == null }

            // Check if the simulator has stopped
            val stillRunning = getSimulatorId(deviceName) != null
            if (stillRunning) {
                logger.warn("iOS simulator did not stop in a standard way, applying forced stop")
                forceStopIosSimulator(simulatorId)

                // Check again with a wait up to 5 seconds without Thread.sleep
                val stoppedAfterForce = waitForCondition(
                    timeout = Duration.ofSeconds(5),
                    pollInterval = Duration.ofMillis(200)
                ) { getSimulatorId(deviceName) == null }
                val stillRunningAfterForce = !stoppedAfterForce
                if (stillRunningAfterForce) {
                    logger.error("Failed to stop iOS simulator even forcibly")
                    return false
                }
            }

            logger.info("iOS simulator stopped successfully")
            return true
        } catch (e: Exception) {
            logger.error("Error while stopping iOS simulator: ${e.message}", e)
            return false
        }
    }

    /**
     * Public method to enforce Wi-Fi usage on the Android emulator before tests.
     * Returns true if Wi-Fi could be enabled and mobile data disabled.
     */
    fun ensureAndroidWifiConnectivity(): Boolean {
        return try {
            if (AppConfig.getPlatform() != Platform.ANDROID) return true
            val emulatorId = getEmulatorId() ?: run {
                logger.warn("No running Android emulator found for network configuration")
                return false
            }
            configureAndroidNetwork(emulatorId)
        } catch (t: Throwable) {
            logger.error("Error ensuring Wi-Fi connectivity on emulator: ${t.message}", t)
            false
        }
    }

    /**
     * Configures network on the Android emulator: enables Wi-Fi and disables mobile data,
     * then verifies the state with retries within a limited time.
     */
    private fun configureAndroidNetwork(deviceId: String): Boolean {
        if (deviceId.isBlank()) return false

        fun runAdb(vararg args: String): TerminalUtils.CommandResult =
            TerminalUtils.runCommand(
                command = listOf("adb", "-s", deviceId, "shell") + args,
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )

        try {
            // Commands to enable Wi-Fi and disable mobile data
            runAdb("svc", "wifi", "enable")
            runAdb("svc", "data", "disable")

            // Additional attempt to disable mobile data via settings for older APIs
            runAdb("settings", "put", "global", "mobile_data", "0")

            val timeout = Duration.ofSeconds(20)
            val ok = waitForCondition(
                timeout = timeout,
                pollInterval = Duration.ofMillis(500)
            ) {
                val wifiEnabled = try {
                    val res1 = runAdb("settings", "get", "global", "wifi_on")
                    val value = res1.stdout.trim()
                    value == "1" || value.equals("true", ignoreCase = true)
                } catch (_: Exception) { false }

                val wifiCmdOk = try {
                    val res2 = TerminalUtils.runCommand(
                        command = listOf("adb", "-s", deviceId, "shell", "cmd", "wifi", "status"),
                        timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                    )
                    val out = (res2.stdout + "\n" + res2.stderr).lowercase()
                    out.contains("enabled") || out.contains("wifi is enabled")
                } catch (_: Exception) { false }

                val dataDisabled = try {
                    val res3 = runAdb("settings", "get", "global", "mobile_data")
                    res3.stdout.trim() == "0"
                } catch (_: Exception) { true /* some APIs don't return a value; consider ok if svc executed */ }

                (wifiEnabled || wifiCmdOk) && dataDisabled
            }

            if (!ok) {
                logger.warn("Failed to confirm network state: Wi-Fi ON and Data OFF within the allotted time")
            }
            return ok
        } catch (t: Throwable) {
            logger.error("Error during Android network configuration: ${t.message}", t)
            return false
        }
    }
}
