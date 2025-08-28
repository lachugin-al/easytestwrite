package reporting.artifacts.screenshot

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream

/**
 * Масштабирует изображение и сжимает PNG.
 */
object ImageProcessor {

    /**
     * Масштабирует изображение и задаёт степень сжатия PNG.
     */
    fun processImage(
        bytes: ByteArray,
        scale: Double,
        quality: Int
    ): ByteArray {
        return try {
            val original: BufferedImage = ImageIO.read(ByteArrayInputStream(bytes))
            // масштаб
            val width = (original.width * scale).toInt()
            val height = (original.height * scale).toInt()
            val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
                createGraphics().also { g ->
                    g.drawImage(original, 0, 0, width, height, null)
                    g.dispose()
                }
            }
            // сжатие PNG
            val writer: ImageWriter = ImageIO.getImageWritersByFormatName("png").next()
            val param: ImageWriteParam = writer.defaultWriteParam.apply {
                if (canWriteCompressed()) {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT

                    val compLevel = (9 * (100 - quality) / 99f)
                    compressionQuality = 1f - compLevel / 9f
                }
            }
            val outStream = ByteArrayOutputStream().also { baos ->
                val ios: ImageOutputStream = ImageIO.createImageOutputStream(baos)
                writer.output = ios
                writer.write(null, IIOImage(resized, null, null), param)
                ios.close()
                writer.dispose()
            }
            outStream.toByteArray()
        } catch (_: Exception) {
            bytes
        }
    }
}
