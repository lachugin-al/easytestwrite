package utils

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Utility class for interacting with the terminal and emulation/simulation devices.
 *
 * Allows executing operating system commands and retrieving information about Android emulators and iOS simulators.
 */
object TerminalUtils {

    private val logger = LoggerFactory.getLogger(TerminalUtils::class.java)

    /**
     * Result of a system command execution.
     */
    data class CommandResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
        val timedOut: Boolean
    )

    /**
     * Executes a system command with extended configuration.
     *
     * @param command Command and arguments (without a shell). For shell constructs, use `shell = true`.
     * @param timeout Maximum execution duration. Once exceeded — the process will be terminated.
     * @param workingDir Working directory of the process.
     * @param env Environment variables (added to existing ones).
     * @param charset Charset for process output streams.
     * @param shell If true, the command will be executed through a shell (bash -lc / cmd /c).
     */
    fun runCommand(
        command: List<String>,
        timeout: Duration = Duration.ofMinutes(5),
        workingDir: File? = null,
        env: Map<String, String> = emptyMap(),
        charset: Charset = Charsets.UTF_8,
        shell: Boolean = false
    ): CommandResult {
        require(command.isNotEmpty()) { "Command must not be empty" }

        val actualCommand: List<String> = if (shell) {
            if (isWindows()) listOf("cmd", "/c") + command.joinToString(" ")
            else listOf("bash", "-lc", command.joinToString(" "))
        } else {
            command
        }

        val builder = ProcessBuilder(actualCommand)
            .redirectErrorStream(false)

        if (workingDir != null) builder.directory(workingDir)
        val environment = builder.environment()
        env.forEach { (k, v) -> environment[k] = v }

        var process: Process? = null
        val executor = Executors.newFixedThreadPool(2)
        try {
            process = builder.start()

            val stdoutFuture = executor.submit<String> {
                process.inputStream.bufferedReader(charset).use { it.readText() }
            }
            val stderrFuture = executor.submit<String> {
                process.errorStream.bufferedReader(charset).use { it.readText() }
            }

            val finishedInTime = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)
            val stdout = runCatching { stdoutFuture.get(100, TimeUnit.MILLISECONDS) }.getOrElse { "" }
            val stderr = runCatching { stderrFuture.get(100, TimeUnit.MILLISECONDS) }.getOrElse { "" }

            if (!finishedInTime) {
                process.destroy()
                if (process.isAlive) {
                    process.destroyForcibly()
                }
                val msg = "Process timed out after $timeout: ${actualCommand.joinToString(" ")}"
                logger.warn(msg)
                return CommandResult(
                    stdout = stdout,
                    stderr = if (stderr.isNotBlank()) stderr else msg,
                    exitCode = -1,
                    timedOut = true
                )
            }

            val exit = process.exitValue()
            return CommandResult(stdout = stdout, stderr = stderr, exitCode = exit, timedOut = false)
        } catch (e: Exception) {
            process?.let {
                if (it.isAlive) {
                    it.destroy()
                    if (it.isAlive) it.destroyForcibly()
                }
            }
            val err = e.message ?: e.toString()
            logger.error("Command execution failed: ${actualCommand.joinToString(" ")}: $err", e)
            return CommandResult(
                stdout = "",
                stderr = err,
                exitCode = -1,
                timedOut = false
            )
        } finally {
            executor.shutdownNow()
        }
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name")?.lowercase()?.contains("win") == true

    /**
     * Executes a system command and processes the result.
     *
     * Legacy contract: returns true if `exitCode == 0`, otherwise false; logs are printed to stdout/stderr.
     * Backward compatibility preserved.
     */
    fun runCommand(command: List<String>, errorMessage: String): Boolean {
        val result = runCommand(command = command, timeout = Duration.ofMinutes(5))
        return if (!result.timedOut && result.exitCode == 0) {
            println("Command executed successfully: ${command.joinToString(" ")}")
            true
        } else {
            val reason = buildString {
                if (result.timedOut) append("timeout; ")
                if (result.stderr.isNotBlank()) append("stderr: ${result.stderr}")
                else if (result.stdout.isNotBlank()) append("stdout: ${result.stdout}")
                else append("unknown error")
            }
            println("$errorMessage: $reason")
            false
        }
    }

    /**
     * Executes a system command and returns the execution result as a string (stdout).
     *
     * Legacy contract: returns an empty string in case of an exception.
     * Backward compatibility preserved.
     */
    fun runCommand(command: List<String>): String {
        val result = runCommand(command = command, timeout = Duration.ofMinutes(5))
        return if (!result.timedOut && result.exitCode == 0) {
            result.stdout
        } else {
            if (result.timedOut) {
                System.err.println("Command timed out: ${command.joinToString(" ")}")
            } else if (result.stderr.isNotBlank()) {
                System.err.println("Command failed (${result.exitCode}): ${result.stderr}")
            }
            ""
        }
    }
}
