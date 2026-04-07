package com.diode.icod_printer.discovery

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import com.diode.icod_printer.models.PrinterConfig
import com.diode.icod_printer.models.TransportType

/**
 * Scanner for USB-OTG devices.
 * Identifies devices with printer interfaces.
 */
class UsbScanner(private val context: Context) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    fun listPrinters(): List<PrinterConfig> {
        val deviceList = usbManager.deviceList
        return deviceList.values.mapNotNull { device ->
            // Try to identify if it's a printer by checking its interface class
            var isPrinter = false
            for (i in 0 until device.interfaceCount) {
                if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                    isPrinter = true
                    break
                }
            }

            // In some Kiosk devices, internal USB printers might not expose the standard USB_CLASS_PRINTER.
            // We'll return it anyway if it is connected via USB for the user to try.
            PrinterConfig(
                transportType = TransportType.USB,
                name = device.productName ?: "USB Printer (${device.deviceId})",
                usbDeviceName = device.deviceName,
                address = "${device.vendorId}:${device.productId}"
            )
        }
    }
}
