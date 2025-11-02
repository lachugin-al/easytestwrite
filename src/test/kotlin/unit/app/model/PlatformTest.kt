package unit.app.model

import app.model.Platform
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for the [Platform] enum.
 *
 * Verifies core functionality of the Platform enum:
 * - Presence of all expected values
 * - Expected number of values
 * - Correct value names
 */
class PlatformTest {

    /**
     * Verifies that the enum contains all expected values.
     */
    @Test
    fun `test platform enum contains all expected values`() {
        // Ensure the enum contains all expected values
        val platforms = Platform.values()

        assertTrue(platforms.contains(Platform.ANDROID))
        assertTrue(platforms.contains(Platform.IOS))
    }

    /**
     * Verifies the number of values in the enum.
     */
    @Test
    fun `test platform enum has expected number of values`() {
        // Ensure the enum has the expected number of values
        val platforms = Platform.values()

        assertEquals(2, platforms.size)
    }

    /**
     * Verifies the names of the enum values.
     */
    @Test
    fun `test platform enum values have correct names`() {
        // Ensure the enum value names match expectations
        assertEquals("ANDROID", Platform.ANDROID.name)
        assertEquals("IOS", Platform.IOS.name)
    }

    /**
     * Verifies the ordinals of the enum values.
     */
    @Test
    fun `test platform enum values have correct ordinals`() {
        // Ensure the ordinal positions match expectations
        assertEquals(0, Platform.ANDROID.ordinal)
        assertEquals(1, Platform.IOS.ordinal)
    }

    /**
     * Verifies the valueOf method for the enum.
     */
    @Test
    fun `test platform enum valueOf method`() {
        // Ensure valueOf returns the correct enum constants
        assertEquals(Platform.ANDROID, Platform.valueOf("ANDROID"))
        assertEquals(Platform.IOS, Platform.valueOf("IOS"))
    }

    /**
     * Verifies that valueOf throws for an invalid value.
     */
    @Test
    fun `test platform enum valueOf throws exception for invalid value`() {
        // Ensure valueOf throws when given an invalid name
        assertThrows(IllegalArgumentException::class.java) {
            Platform.valueOf("INVALID_PLATFORM")
        }
    }
}
