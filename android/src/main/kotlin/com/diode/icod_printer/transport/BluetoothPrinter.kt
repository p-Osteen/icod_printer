package com.diode.icod_printer.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.diode.icod_printer.core.Printer
import com.diode.icod_printer.core.PrinterStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.OutputStream
import java.util.*

/**
 * Implementation of a printer over Bluetooth Classic (SPP).
 * UUID used is the standard Serial Port Profile UUID.
 */
class BluetoothPrinter(
    private val macAddress: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : Printer {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val _status = MutableSharedFlow<PrinterStatus>(replay = 1)
    override val status: SharedFlow<PrinterStatus> = _status

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    init {
        _status.tryEmit(PrinterStatus.Disconnected)
    }

    @SuppressLint("MissingPermission") // Caller must handle permissions
    override suspend fun connect() {
        withContext(Dispatchers.IO) {
            try {
                _status.emit(PrinterStatus.Connecting)
                
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: throw Exception("Bluetooth not supported")
                val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
                
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                adapter.cancelDiscovery() // Always cancel discovery before connecting
                
                socket?.connect()
                outputStream = socket?.outputStream
                
                _status.emit(PrinterStatus.Connected)
                
            } catch (e: Exception) {
                _status.emit(PrinterStatus.Error("Bluetooth connection failed: ${e.message}", e))
                cleanup()
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            cleanup()
            _status.emit(PrinterStatus.Disconnected)
        }
    }

    override suspend fun send(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (socket?.isConnected != true || outputStream == null) {
                return@withContext Result.failure(Exception("Printer not connected"))
            }

            _status.emit(PrinterStatus.Printing)
            
            // Chunking for Bluetooth SPP stability (small buffer)
            val chunkSize = 512
            var offset = 0
            while (offset < data.size) {
                val end = (offset + chunkSize).coerceAtMost(data.size)
                outputStream?.write(data, offset, end - offset)
                offset = end
                // Small delay to prevent Bluetooth buffer overflow
                delay(20)
            }
            
            outputStream?.flush()
            _status.emit(PrinterStatus.Connected)
            
            Result.success(Unit)
        } catch (e: Exception) {
            _status.emit(PrinterStatus.Error("Bluetooth send error: ${e.message}", e))
            Result.failure(e)
        }
    }

    override suspend fun queryStatus(): PrinterStatus {
        return if (socket?.isConnected == true) PrinterStatus.Connected else PrinterStatus.Disconnected
    }

    override suspend fun requestPermission(): Boolean {
        // Bluetooth permissions are usually handled at the App level on Android 12+
        // But we can check if the adapter is enabled.
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return adapter?.isEnabled == true
    }

    private fun cleanup() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            outputStream = null
            socket = null
        }
    }
}
