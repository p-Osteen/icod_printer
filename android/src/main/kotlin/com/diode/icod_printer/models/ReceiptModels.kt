package com.diode.icod_printer.models

/**
 * High-level structures for building receipts.
 * These will be mapped to ESC/POS commands by the generator.
 */
data class Receipt(
    val id: String,
    val header: Header? = null,
    val items: List<ReceiptItem>,
    val footer: Footer? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class Header(
    val storeName: String,
    val address: String? = null,
    val logoBase64: String? = null
)

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val price: Double,
    val note: String? = null
)

data class Footer(
    val subtotal: Double,
    val tax: Double,
    val total: Double,
    val message: String? = null
)
