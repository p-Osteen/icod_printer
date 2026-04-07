package com.diode.icod_printer.models

/**
 * Configuration for connecting to a printer.
 * Supports USB, Bluetooth MAC addresses, and TCP/IP addresses.
 */
data class PrinterConfig(
    val transportType: TransportType,
    val name: String? = null,
    val address: String? = null, // MAC for BT, IP for TCP, path for Serial
    val port: Int? = 9100, // For TCP
    val baudRate: Int = 9600, // For Serial
    val usbDeviceName: String? = null, // For USB
    val paperSize: Int = 80 // 58 or 80 mm
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "transport" to transportType.name.lowercase(),
        "name" to name,
        "address" to address,
        "port" to port,
        "baudRate" to baudRate,
        "usbDeviceName" to usbDeviceName,
        "paperSize" to paperSize
    )
}
