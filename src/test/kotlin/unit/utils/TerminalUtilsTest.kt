package unit.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import utils.TerminalUtils
import java.time.Duration

class TerminalUtilsTest {

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")

    private fun shellEchoCommand(text: String): List<String> =
        if (isWindows()) listOf("echo $text") else listOf("echo $text")

    private fun shellStdErrCommand(): List<String> =
        if (isWindows()) listOf("echo out & echo err 1>&2") else listOf("echo out; echo err 1>&2")

    private fun shellSleepShort(): List<String> =
        if (isWindows()) listOf("timeout /T 2 /NOBREAK") else listOf("sleep 2")

    private fun shellPrintCwd(): List<String> =
        if (isWindows()) listOf("cd") else listOf("pwd")

    private fun shellEchoEnv(varName: String): List<String> =
        if (isWindows()) listOf("echo %$varName%") else listOf("echo \$$varName")

    private fun compatEchoHiCommand(): List<String> =
        if (isWindows()) listOf("cmd", "/c", "echo hi") else listOf("bash", "-lc", "echo hi")

    private fun compatExitWith(code: Int): List<String> =
        if (isWindows()) listOf("cmd", "/c", "exit /b $code") else listOf("bash", "-lc", "exit $code")

    @Test
    fun runCommand_shell_success_stdout() {
        val res = TerminalUtils.runCommand(
            command = shellEchoCommand("hello"),
            shell = true,
            timeout = Duration.ofSeconds(10)
        )
        assertFalse(res.timedOut, "Process should not time out")
        assertEquals(0, res.exitCode, "Exit code should be 0")
        assertTrue(res.stdout.trim().equals("hello", ignoreCase = false), "Stdout should contain exact 'hello'")
        assertTrue(res.stderr.isBlank(), "Stderr should be blank on success echo")
    }

    @Test
    fun runCommand_separates_stdout_and_stderr() {
        val res = TerminalUtils.runCommand(
            command = shellStdErrCommand(),
            shell = true,
            timeout = Duration.ofSeconds(10)
        )
        assertFalse(res.timedOut)
        assertEquals(0, res.exitCode)
        val out = res.stdout.trim()
        val err = res.stderr.trim()
        assertEquals("out", out, "stdout should be 'out'")
        assertEquals("err", err, "stderr should be 'err'")
    }

    @Test
    fun runCommand_times_out_and_kills_process() {
        val res = TerminalUtils.runCommand(
            command = shellSleepShort(),
            shell = true,
            timeout = Duration.ofMillis(200)
        )
        assertTrue(res.timedOut, "Process must time out")
        assertEquals(-1, res.exitCode, "Exit code should be -1 on timeout")
    }

    @Test
    fun runCommand_respects_working_directory() {
        val tempDir = createTempDir(prefix = "termutils-test-")
        try {
            val res = TerminalUtils.runCommand(
                command = shellPrintCwd(),
                shell = true,
                workingDir = tempDir,
                timeout = Duration.ofSeconds(10)
            )
            assertEquals(0, res.exitCode)
            val printed = res.stdout.trim().replace('\\', '/')
            val expected = tempDir.absolutePath.replace('\\', '/')
            assertTrue(
                printed.endsWith(expected) || expected.endsWith(printed),
                "Working directory output should reference the temp dir. Printed='$printed', expected='$expected'"
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun runCommand_passes_environment_variables() {
        val envVar = "FOO_BAR_TEST"
        val res = TerminalUtils.runCommand(
            command = shellEchoEnv(envVar),
            shell = true,
            env = mapOf(envVar to "baz"),
            timeout = Duration.ofSeconds(10)
        )
        assertEquals(0, res.exitCode)
        assertEquals("baz", res.stdout.trim())
    }

    @Test
    fun compat_overload_returns_stdout_string() {
        val out = TerminalUtils.runCommand(compatEchoHiCommand())
        assertEquals("hi", out.trim())
    }

    @Test
    fun compat_overload_boolean_false_on_non_zero_exit() {
        val ok = TerminalUtils.runCommand(compatExitWith(3), "Expected failure")
        assertFalse(ok, "Boolean overload should be false for non-zero exit")
    }
}
