package unit.reporting.artifacts.screenshot

import org.junit.jupiter.api.Test
import reporting.artifacts.screenshot.ImageProcessor
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ImageProcessorTest {

    private fun pngBytes(width: Int = 10, height: Int = 10): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = Color.RED
        g.fillRect(0, 0, width, height)
        g.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        return baos.toByteArray()
    }

    @Test
    fun `processImage scales and keeps PNG format`() {
        val input = pngBytes(10, 10)
        val processed = ImageProcessor.processImage(input, scale = 0.5, quality = 50, format = ImageProcessor.Format.PNG)
        val pngSig = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
        assertContentEquals(pngSig.toList(), processed.take(8))
        val img = ImageIO.read(processed.inputStream())
        assertNotNull(img)
        assertEquals(5, img.width)
        assertEquals(5, img.height)
    }

    @Test
    fun `processImage returns original on error`() {
        val bad = byteArrayOf(1, 2, 3)
        val out = ImageProcessor.processImage(bad, 0.5, 50, format = ImageProcessor.Format.PNG)
        assertContentEquals(bad.toList(), out.toList())
    }
}
