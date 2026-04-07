package com.diode.icod_printer.transport

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import com.diode.icod_printer.core.Printer
import com.diode.icod_printer.core.PrinterStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Implementation of a printer over USB-OTG.
 * Handles device discovery, permission requests, and data transmission.
 */
class UsbPrinter(
    private val context: Context,
    private val deviceName: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : Printer {

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val statusFlow = MutableSharedFlow<PrinterStatus>(replay = 1)
    override val status: SharedFlow<PrinterStatus> = statusFlow

    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpoint: UsbEndpoint? = null
    private var usbEndpointIn: UsbEndpoint? = null

    init {
        statusFlow.tryEmit(PrinterStatus.Disconnected)
    }

    override suspend fun connect() {
        withContext(Dispatchers.IO) {
            try {
                statusFlow.emit(PrinterStatus.Connecting)
                
                val device = usbManager.deviceList[deviceName] ?: throw Exception("USB Device $deviceName not found")
                
                if (!usbManager.hasPermission(device)) {
                    statusFlow.emit(PrinterStatus.Error("Permission required for USB device"))
                    return@withContext
                }

                connection = usbManager.openDevice(device) ?: throw Exception("Failed to open USB connection")
                
                for (i in 0 until device.interfaceCount) {
                    val iface = device.getInterface(i)
                    var epOut: UsbEndpoint? = null
                    var epIn: UsbEndpoint? = null
                    
                    for (j in 0 until iface.endpointCount) {
                        val ep = iface.getEndpoint(j)
                        if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (ep.direction == UsbConstants.USB_DIR_OUT) epOut = ep
                            if (ep.direction == UsbConstants.USB_DIR_IN) epIn = ep
                        }
                    }
                    
                    if (epOut != null) {
                        val tempConn = usbManager.openDevice(device) ?: throw Exception("Failed to open connection")
                        if (tempConn.claimInterface(iface, true)) {
                            connection = tempConn
                            usbInterface = iface
                            usbEndpoint = epOut
                            usbEndpointIn = epIn // May be null on some printers, but typically exists.
                            break
                        } else {
                            tempConn.close()
                        }
                    }
                }
                if (usbEndpoint == null) {
                    throw Exception("Could not find a valid printer interface/endpoint on $deviceName")
                }

                statusFlow.emit(PrinterStatus.Connected)
                
            } catch (e: Exception) {
                statusFlow.emit(PrinterStatus.Error("USB connection failed: ${e.message}", e))
                cleanup()
                throw e // Propagate error to plugin
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            cleanup()
            statusFlow.emit(PrinterStatus.Disconnected)
        }
    }

    override suspend fun send(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (connection == null || usbEndpoint == null) {
                return@withContext Result.failure(Exception("Printer not connected"))
            }

            statusFlow.emit(PrinterStatus.Printing)
            
            // Chunking for USB stability (Optimized for larger images)
            val chunkSize = 1024
            var offset = 0
            while (offset < data.size) {
                val end = (offset + chunkSize).coerceAtMost(data.size)
                val length = end - offset
                val chunk = data.sliceArray(offset until end)
                
                // Increased timeout to 10s for heavy raster bitual blocks
                val transferResult = connection?.bulkTransfer(usbEndpoint, chunk, length, 10000) ?: -1
                if (transferResult < 0) {
                    throw Exception("USB bulk transfer failed at offset $offset")
                }
                offset = end
            }

            statusFlow.emit(PrinterStatus.Connected)
            Result.success(Unit)
        } catch (e: Exception) {
            statusFlow.emit(PrinterStatus.Error("USB send error: ${e.message}", e))
            Result.failure(e)
        }
    }

    override suspend fun requestPermission(): Boolean {
        return withContext(Dispatchers.Main) {
            val device = usbManager.deviceList[deviceName] ?: return@withContext false
            if (usbManager.hasPermission(device)) return@withContext true

            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent("com.diode.icod_printer.USB_PERMISSION"),
                PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
            
            // Poll for permission for a few seconds (simplification for plugin use)
            var attempts = 0
            while (attempts < 10 && !usbManager.hasPermission(device)) {
                delay(1000)
                attempts++
            }
            usbManager.hasPermission(device)
        }
    }

    override suspend fun queryStatus(): PrinterStatus {
        return if (connection != null) PrinterStatus.Connected else PrinterStatus.Disconnected
    }

    override suspend fun receive(timeoutMs: Int): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (connection == null || usbEndpointIn == null) return@withContext null

            val buffer = ByteArray(64)
            val read = connection?.bulkTransfer(usbEndpointIn, buffer, buffer.size, timeoutMs) ?: -1
            
            if (read > 0) buffer.copyOfRange(0, read) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanup() {
        try {
            if (usbInterface != null) {
                connection?.releaseInterface(usbInterface)
            }
            connection?.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            connection = null
            usbInterface = null
            usbEndpoint = null
            usbEndpointIn = null
        }
    }
}
