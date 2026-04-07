package com.diode.icod_printer.core

import kotlinx.coroutines.flow.SharedFlow

/**
 * Common interface for all printer transport types (USB, Bluetooth, TCP).
 * Defines basic operations and status monitoring.
 */
interface Printer {
    val status: SharedFlow<PrinterStatus>
    
    suspend fun connect()
    suspend fun disconnect()
    suspend fun send(data: ByteArray): Result<Unit>
    suspend fun requestPermission(): Boolean
    
    // Optional status query if supported by printer
    suspend fun queryStatus(): PrinterStatus
    
    /**
     * Reads pending bytes from the printer buffers. 
     * Useful for receiving real-time hardware status updates.
     */
    suspend fun receive(timeoutMs: Int = 1000): ByteArray? { return null }
}
