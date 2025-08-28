package unit.device

import device.model.Simulator
import device.model.SimulatorsResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SimctlModelsTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun `serialization and deserialization round trip works`() {
        val devicesMap = mapOf(
            "iOS 18.4" to listOf(
                Simulator(udid = "UUID-1", name = "iPhone 16 Plus", state = "Booted"),
                Simulator(udid = "UUID-2", name = "iPhone 15 Pro", state = "Shutdown")
            ),
            "iOS 17.0" to emptyList()
        )
        val original = SimulatorsResponse(devices = devicesMap)

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SimulatorsResponse>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `decoding ignores unknown keys and handles empty maps`() {
        val sample = """
            {
              "devices": {
                "iOS 18.4": [
                  {"udid":"A","name":"iPhone 16 Plus","state":"Booted","extra":"ignored"}
                ],
                "iOS 17.0": [],
                "unknown": [ {"udid":"B","name":"X","state":"Shutdown"} ]
              },
              "unknownRootField": 123
            }
        """.trimIndent()

        val decoded = json.decodeFromString<SimulatorsResponse>(sample)
        assertEquals(3, decoded.devices.size)
        assertEquals(1, decoded.devices["iOS 18.4"]?.size)
        assertEquals("A", decoded.devices["iOS 18.4"]?.first()?.udid)
    }
}
