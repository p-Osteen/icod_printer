package com.diode.icod_printer.utils

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream

/**
 * Utility class for converting Bitmaps to ESC/POS raster bit images.
 */
object ImageUtils {

    /**
     * Converts a Bitmap to the ESC/POS GS v 0 (Raster bit image) command.
     * Thresholding is used to convert the image to black and white.
     */
    fun decodeBitmap(bitmap: Bitmap, maxWidth: Int = 576): ByteArray {
        // Resize bitmap if it exceeds maxWidth
        val scaledBitmap = if (bitmap.width > maxWidth) {
            resizeForPrinter(bitmap, maxWidth)
        } else {
            bitmap
        }

        val width = scaledBitmap.width
        val height = scaledBitmap.height
        
        // Width in bytes: (width + 7) / 8
        val widthBytes = (width + 7) / 8
        val data = ByteArray(widthBytes * height)
        
        var byteIndex = 0
        for (y in 0 until height) {
            for (x in 0 until widthBytes) {
                var currentByte = 0
                for (bit in 0 until 8) {
                    val pixelX = x * 8 + bit
                    if (pixelX < width) {
                        val pixel = scaledBitmap.getPixel(pixelX, y)
                        val alpha = Color.alpha(pixel)
                        val red = Color.red(pixel)
                        val green = Color.green(pixel)
                        val blue = Color.blue(pixel)
                        
                        // Treat transparent pixels (alpha < 128) as white
                        if (alpha >= 128) {
                            val gray = (red + green + blue) / 3
                            if (gray < 128) {
                                // Set bit (1 is black)
                                currentByte = currentByte or (1 shl (7 - bit))
                            }
                        }
                    }
                }
                data[byteIndex++] = currentByte.toByte()
            }
        }

        val output = ByteArrayOutputStream()
        // GS v 0 command: 0x1D, 0x76, 0x30, m
        output.write(0x1D)
        output.write(0x76)
        output.write(0x30)
        output.write(0x00) // m = 0 (Normal)
        
        // xL, xH (width in bytes)
        output.write(widthBytes and 0xFF)
        output.write((widthBytes shr 8) and 0xFF)
        
        // yL, yH (height in dots)
        output.write(height and 0xFF)
        output.write((height shr 8) and 0xFF)
        
        output.write(data)
        
        return output.toByteArray()
    }

    /**
     * Resizes a bitmap to fit a specific width (e.g., 384 for 58mm, 576 for 80mm).
     */
    fun resizeForPrinter(bitmap: Bitmap, targetWidth: Int): Bitmap {
        if (bitmap.width == targetWidth) return bitmap
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = (targetWidth * aspectRatio).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
