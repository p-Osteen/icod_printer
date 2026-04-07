package com.diode.icod_printer.commands

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * Fluent API for building ESC/POS byte commands for thermal printers.
 */
class EscPosBuilder(
    private val paperSize: Int = 80,
    private val charset: Charset = Charset.forName("GBK")
) {

    private val outputStream = ByteArrayOutputStream()
    private val maxChars: Int = if (paperSize == 80) 48 else 32

    companion object {
        // Core Commands
        private val ESC: Byte = 0x1B
        private val GS: Byte = 0x1D
        private val LF: Byte = 0x0A
        
        // Alignment
        const val ALIGN_LEFT = 0
        const val ALIGN_CENTER = 1
        const val ALIGN_RIGHT = 2
        
        // Text Size
        const val SIZE_NORMAL = 0x00
        const val SIZE_DOUBLE_HEIGHT = 0x01
        const val SIZE_DOUBLE_WIDTH = 0x10
        const val SIZE_BIG = 0x11
    }

    init {
        initialize()
    }

    fun initialize(): EscPosBuilder {
        write(byteArrayOf(ESC, 0x40)) // ESC @
        return this
    }

    fun text(content: String, align: Int = ALIGN_LEFT, size: Int = SIZE_NORMAL, bold: Boolean = false): EscPosBuilder {
        setAlignment(align)
        setTextSize(size)
        setBold(bold)
        write(content.toByteArray(charset))
        return this
    }

    fun line(content: String = "", align: Int = ALIGN_LEFT, size: Int = SIZE_NORMAL, bold: Boolean = false): EscPosBuilder {
        text(content, align, size, bold)
        feed()
        return this
    }

    fun feed(lines: Int = 1): EscPosBuilder {
        for (i in 0 until lines) {
            write(LF)
        }
        return this
    }

    fun setAlignment(align: Int): EscPosBuilder {
        write(byteArrayOf(ESC, 0x61, align.toByte()))
        return this
    }

    fun setTextSize(size: Int): EscPosBuilder {
        write(byteArrayOf(GS, 0x21, size.toByte()))
        return this
    }

    fun setBold(isBold: Boolean): EscPosBuilder {
        write(byteArrayOf(ESC, 0x45, if (isBold) 0x01 else 0x00))
        return this
    }

    fun divider(char: Char = '-'): EscPosBuilder {
        line(char.toString().repeat(maxChars)) 
        return this
    }

    fun cut(): EscPosBuilder {
        feed(3) // Feed before cutting
        write(byteArrayOf(GS, 0x56, 0x42, 0x00)) // GS V m (Partial cut)
        return this
    }

    fun openDrawer(): EscPosBuilder {
        write(byteArrayOf(0x1B, 0x70, 0x00, 0x3C, 0xFF.toByte()))
        return this
    }
    
    fun qrCode(content: String, size: Int = 8): EscPosBuilder {
        val data = content.toByteArray(charset)
        val pL = ((data.size + 3) and 0xFF).toByte()
        val pH = (((data.size + 3) shr 8) and 0xFF).toByte()

        // 1. Set QR Code Module Size
        write(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.toByte()))
        
        // 2. Set Error Correction Level (L)
        write(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30))
        
        // 3. Store Data in Symbol Storage Area
        write(byteArrayOf(GS, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        write(data)
        
        // 4. Print Symbol
        write(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
        
        return this
    }

    fun barcode(content: String, type: Int = 67): EscPosBuilder {
        // type 67 = EAN13 in GS k m n d1...dn format
        val data = content.toByteArray(charset)
        write(byteArrayOf(GS, 0x6B, type.toByte(), data.size.toByte()))
        write(data)
        return this
    }

    fun underline(enabled: Boolean): EscPosBuilder {
        write(byteArrayOf(ESC, 0x2D, if (enabled) 0x01 else 0x00))
        return this
    }

    fun reverseColor(enabled: Boolean): EscPosBuilder {
        write(byteArrayOf(GS, 0x42, if (enabled) 0x01 else 0x00))
        return this
    }

    fun setDoubleHeight(enabled: Boolean): EscPosBuilder {
        write(byteArrayOf(GS, 0x21, if (enabled) 0x01 else 0x00))
        return this
    }

    fun setDoubleWidth(enabled: Boolean): EscPosBuilder {
        write(byteArrayOf(GS, 0x21, if (enabled) 0x10 else 0x00))
        return this
    }

    fun setLineSpacing(dots: Int): EscPosBuilder {
        write(byteArrayOf(ESC, 0x33, dots.toByte()))
        return this
    }

    fun resetLineSpacing(): EscPosBuilder {
        write(byteArrayOf(ESC, 0x32))
        return this
    }

    fun image(bitmapData: ByteArray): EscPosBuilder {
        write(bitmapData)
        return this
    }

    fun write(data: ByteArray): EscPosBuilder {
        outputStream.write(data)
        return this
    }

    fun write(data: Byte): EscPosBuilder {
        outputStream.write(data.toInt())
        return this
    }

    fun build(): ByteArray {
        return outputStream.toByteArray()
    }
}
