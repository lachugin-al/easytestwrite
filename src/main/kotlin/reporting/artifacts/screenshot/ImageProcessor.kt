package reporting.artifacts.screenshot

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageProcessor {

    enum class Format { JPEG, PNG }

    /**
     * Scales the image and re-encodes it into the specified format.
     *
     * @param bytes   source bytes (usually PNG from Appium)
     * @param scale   0.1..1.0 (resolution reduction)
     * @param quality 1..100 (for JPEG this significantly affects size; for PNG — more of a hint)
     * @param format  target format (default: JPEG)
     * @param background Background color for JPEG (alpha is not supported)
     */
    @JvmStatic
    fun processImage(
        bytes: ByteArray,
        scale: Double,
        quality: Int,
        format: Format = Format.JPEG,
        background: Color = Color.WHITE
    ): ByteArray {
        val safeScale = scale.coerceIn(0.1, 1.0)
        val safeQuality = quality.coerceIn(1, 100)

        return try {
            val original = ImageIO.read(ByteArrayInputStream(bytes))
                ?: return bytes

            val w = max(1, (original.width * safeScale).roundToInt())
            val h = max(1, (original.height * safeScale).roundToInt())

            val targetType = when (format) {
                Format.JPEG -> BufferedImage.TYPE_INT_RGB
                Format.PNG -> BufferedImage.TYPE_INT_ARGB
            }

            val canvas = BufferedImage(w, h, targetType)

            val g = canvas.createGraphics()
            try {
                g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
                )
                g.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
                )
                g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                )

                if (format == Format.JPEG) {
                    g.color = background
                    g.fillRect(0, 0, w, h)
                }
                g.drawImage(original, 0, 0, w, h, null)
            } finally {
                g.dispose()
            }

            when (format) {
                Format.JPEG -> writeJpeg(canvas, safeQuality)
                Format.PNG -> writePng(canvas, safeQuality)
            }
        } catch (e: Exception) {
            System.err.println("[ImageProcessor] ${e.javaClass.simpleName}: ${e.message}")
            bytes
        }
    }

    private fun writeJpeg(img: BufferedImage, quality: Int): ByteArray {
        val writer: ImageWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
        val param: ImageWriteParam = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = quality / 100f // 0.0..1.0
        }

        return ByteArrayOutputStream().use { baos ->
            ImageIO.createImageOutputStream(baos).use { ios: ImageOutputStream ->
                writer.output = ios
                writer.write(null, IIOImage(img, null, null), param)
            }
            writer.dispose()
            baos.toByteArray()
        }
    }

    private fun writePng(img: BufferedImage, @Suppress("UNUSED_PARAMETER") quality: Int): ByteArray {
        val writer: ImageWriter = ImageIO.getImageWritersByFormatName("png").next()
        val param: ImageWriteParam = writer.defaultWriteParam // can be left as default

        return ByteArrayOutputStream().use { baos ->
            ImageIO.createImageOutputStream(baos).use { ios: ImageOutputStream ->
                writer.output = ios
                writer.write(null, IIOImage(img, null, null), param)
            }
            writer.dispose()
            baos.toByteArray()
        }
    }
}
