package com.diode.icod_printer

import android.content.Context
import com.diode.icod_printer.core.Printer
import com.diode.icod_printer.models.PrinterConfig
import com.diode.icod_printer.models.TransportType
import com.diode.icod_printer.transport.BluetoothPrinter
import com.diode.icod_printer.transport.SerialPrinter
import com.diode.icod_printer.transport.TcpPrinter
import com.diode.icod_printer.transport.UsbPrinter
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton manager to handle multiple printer instances and discovery.
 */
object KioskPrinterManager {
    private val printers = ConcurrentHashMap<String, Printer>()

    fun getPrinter(context: Context, config: PrinterConfig): Printer {
        val key = "${config.transportType}_${config.address ?: config.usbDeviceName ?: "default"}"
        return printers.getOrPut(key) {
            when (config.transportType) {
                TransportType.USB -> UsbPrinter(context, config.usbDeviceName ?: "")
                TransportType.BLUETOOTH -> BluetoothPrinter(config.address ?: "")
                TransportType.TCP -> TcpPrinter(config.address ?: "", config.port ?: 9100)
                TransportType.SERIAL -> SerialPrinter(config.address ?: "", config.baudRate)
            }
        }
    }

    fun removePrinter(config: PrinterConfig) {
        val key = "${config.transportType}_${config.address ?: config.usbDeviceName ?: "default"}"
        printers.remove(key)
    }
}
