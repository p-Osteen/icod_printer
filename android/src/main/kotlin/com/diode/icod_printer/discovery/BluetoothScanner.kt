package com.diode.icod_printer.discovery

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.diode.icod_printer.models.PrinterConfig
import com.diode.icod_printer.models.TransportType

/**
 * Scanner for Bluetooth devices.
 * Lists paired devices by default.
 */
class BluetoothScanner(private val context: Context) {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    @SuppressLint("MissingPermission")
    fun listPairedDevices(): List<PrinterConfig> {
        if (adapter == null || !adapter.isEnabled) return emptyList()

        return adapter.bondedDevices.map { device ->
            PrinterConfig(
                transportType = TransportType.BLUETOOTH,
                name = device.name ?: device.address,
                address = device.address
            )
        }
    }
}
