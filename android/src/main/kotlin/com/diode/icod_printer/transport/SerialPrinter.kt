package com.diode.icod_printer.transport

import com.diode.icod_printer.core.Printer
import com.diode.icod_printer.core.PrinterStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Implementation of a printer over a Serial Port (UART).
 * Common for inbuilt printers in Android Kiosk devices.
 * Uses direct FileOutputStream for /dev/ttyS* or /dev/ttyUSB* devices.
 */
class SerialPrinter(
    private val devicePath: String,
    private val baudRate: Int = 115200,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : Printer {

    private val statusFlow = MutableSharedFlow<PrinterStatus>(replay = 1)
    override val status: SharedFlow<PrinterStatus> = statusFlow

    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isConnected = false

    init {
        statusFlow.tryEmit(PrinterStatus.Disconnected)
    }

    override suspend fun connect() {
        withContext(Dispatchers.IO) {
            try {
                statusFlow.emit(PrinterStatus.Connecting)
                
                // Attempt to grant permissions and set baud rate via shell
                tryGrantPermission()
                
                val device = File(devicePath)
                if (!device.exists()) {
                    throw Exception("Serial device $devicePath not found")
                }
                
                outputStream = FileOutputStream(device)
                inputStream = FileInputStream(device)
                isConnected = true
                
                statusFlow.emit(PrinterStatus.Connected)
                
            } catch (e: Exception) {
                statusFlow.emit(PrinterStatus.Error("Serial connection failed: ${e.message}", e))
                cleanup()
                throw e // Propagate error to plugin
            }
        }
    }

    private fun tryGrantPermission() {
        try {
            // Attempt to chmod for read/write access
            Runtime.getRuntime().exec(arrayOf("sh", "-c", "chmod 666 $devicePath")).waitFor()
            // Attempt to set baud rate via stty (common on Linux/Android)
            Runtime.getRuntime().exec(arrayOf("sh", "-c", "stty -F $devicePath $baudRate")).waitFor()
        } catch (e: Exception) {
            android.util.Log.w("SerialPrinter", "Shell command failed (normal on non-root): ${e.message}")
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
            if (!isConnected || outputStream == null) {
                return@withContext Result.failure(Exception("Serial printer not connected"))
            }

            statusFlow.emit(PrinterStatus.Printing)
            
            // Implementation of chunking for stability (Optimized for RK3568 UART)
            val chunkSize = 128
            var offset = 0
            while (offset < data.size) {
                val end = (offset + chunkSize).coerceAtMost(data.size)
                outputStream?.write(data, offset, end - offset)
                offset = end
                // 25ms delay to prevent internal UART buffer overflow
                delay(25)
            }
            
            outputStream?.flush()
            statusFlow.emit(PrinterStatus.Connected)
            
            Result.success(Unit)
        } catch (e: Exception) {
            statusFlow.emit(PrinterStatus.Error("Serial send error: ${e.message}", e))
            Result.failure(e)
        }
    }

    override suspend fun queryStatus(): PrinterStatus {
        return if (isConnected) PrinterStatus.Connected else PrinterStatus.Disconnected
    }

    override suspend fun receive(timeoutMs: Int): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (!isConnected || inputStream == null) return@withContext null

            val startTime = System.currentTimeMillis()
            while (inputStream?.available() ?: 0 == 0) {
                if (System.currentTimeMillis() - startTime > timeoutMs) return@withContext null
                delay(10)
            }

            val available = inputStream?.available() ?: 0
            val buffer = ByteArray(available)
            val read = inputStream?.read(buffer) ?: 0
            
            if (read > 0) buffer.copyOfRange(0, read) else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun requestPermission(): Boolean {
        // Serial permissions are handled via file access
        val device = File(devicePath)
        return device.exists() && device.canWrite()
    }

    private fun cleanup() {
        try {
            outputStream?.close()
            inputStream?.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            outputStream = null
            inputStream = null
            isConnected = false
        }
    }
}
