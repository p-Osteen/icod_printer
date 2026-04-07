package com.diode.icod_printer.transport

import com.diode.icod_printer.core.Printer
import com.diode.icod_printer.core.PrinterStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Implementation of a printer over TCP/IP (usually port 9100).
 */
class TcpPrinter(
    private val host: String,
    private val port: Int = 9100,
    private val connectionTimeout: Int = 5000,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : Printer {

    private val _status = MutableSharedFlow<PrinterStatus>(replay = 1)
    override val status: SharedFlow<PrinterStatus> = _status

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null

    init {
        _status.tryEmit(PrinterStatus.Disconnected)
    }

    override suspend fun connect() {
        withContext(Dispatchers.IO) {
            try {
                _status.emit(PrinterStatus.Connecting)
                
                socket = Socket()
                socket?.connect(InetSocketAddress(host, port), connectionTimeout)
                
                if (socket?.isConnected == true) {
                    outputStream = socket?.getOutputStream()
                    _status.emit(PrinterStatus.Connected)
                } else {
                    _status.emit(PrinterStatus.Error("Failed to connect to $host:$port"))
                }
                
            } catch (e: Exception) {
                _status.emit(PrinterStatus.Error("Connection error: ${e.message}", e))
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
            
            // Chunking for TCP stability
            val chunkSize = 4096
            var offset = 0
            while (offset < data.size) {
                val end = (offset + chunkSize).coerceAtMost(data.size)
                outputStream?.write(data, offset, end - offset)
                offset = end
            }
            
            outputStream?.flush()
            _status.emit(PrinterStatus.Connected)
            
            Result.success(Unit)
        } catch (e: Exception) {
            _status.emit(PrinterStatus.Error("Send error: ${e.message}", e))
            Result.failure(e)
        }
    }

    override suspend fun queryStatus(): PrinterStatus {
        // TCP doesn't always support easy bidirectional status without specific protocols
        // For now, we return the current state of connection
        return if (socket?.isConnected == true) PrinterStatus.Connected else PrinterStatus.Disconnected
    }

    override suspend fun requestPermission(): Boolean {
        // TCP typically doesn't need system-level permissions beyond Internet
        return true
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
