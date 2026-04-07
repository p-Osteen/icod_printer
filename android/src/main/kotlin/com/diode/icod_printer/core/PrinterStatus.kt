package com.diode.icod_printer.core

/**
 * Represents the current state of a printer connection.
 * Used for UI updates and internal state management.
 */
sealed class PrinterStatus {
    object Idle : PrinterStatus()
    object Connecting : PrinterStatus()
    object Connected : PrinterStatus()
    object Printing : PrinterStatus()
    object Disconnected : PrinterStatus()
    data class Error(val message: String, val exception: Throwable? = null) : PrinterStatus()
    
    // Status indicators (if supported by hardware)
    object OutOfPaper : PrinterStatus()
    object PaperLow : PrinterStatus()
    object CoverOpen : PrinterStatus()
}
