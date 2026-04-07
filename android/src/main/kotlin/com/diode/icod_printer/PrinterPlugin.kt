package com.diode.icod_printer

import android.content.Context
import androidx.annotation.NonNull
import com.diode.icod_printer.models.PrinterConfig
import com.diode.icod_printer.models.TransportType
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import com.diode.icod_printer.discovery.UsbScanner
import com.diode.icod_printer.discovery.BluetoothScanner
import kotlinx.coroutines.*

/** 
 * PrinterPlugin
 */
class PrinterPlugin: FlutterPlugin, MethodCallHandler {
  private lateinit var channel : MethodChannel
  private lateinit var context: Context
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.diode.icod_printer/methods")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "discover" -> {
        val typeStr = call.argument<String>("type") ?: "all"
        val printers = mutableListOf<Map<String, Any?>>()
        
        if (typeStr == "usb" || typeStr == "all") {
          try {
            printers.addAll(UsbScanner(context).listPrinters().map { it.toMap() })
          } catch (e: Exception) {
            android.util.Log.e("PrinterPlugin", "USB discovery error: ${e.message}")
          }
        }
        if (typeStr == "bluetooth" || typeStr == "all") {
          try {
            printers.addAll(BluetoothScanner(context).listPairedDevices().map { it.toMap() })
          } catch (e: Exception) {
            android.util.Log.e("PrinterPlugin", "Bluetooth discovery error: ${e.message}")
          }
        }
        
        result.success(printers)
      }
      "connect" -> {
        val config = parseConfig(call.arguments as Map<String, Any>)
        scope.launch {
          try {
            val printer = KioskPrinterManager.getPrinter(context, config)
            printer.connect()
            result.success(true)
          } catch (e: Exception) {
            result.error("CONNECT_ERROR", e.message, null)
          }
        }
      }
      "requestPermission" -> {
        val config = parseConfig(call.arguments as Map<String, Any>)
        scope.launch {
          try {
            val printer = KioskPrinterManager.getPrinter(context, config)
            val granted = printer.requestPermission()
            result.success(granted)
          } catch (e: Exception) {
            result.error("PERMISSION_ERROR", e.message, null)
          }
        }
      }
      "disconnect" -> {
        val configMap = call.arguments as Map<String, Any>
        val config = parseConfig(configMap)
        scope.launch {
          KioskPrinterManager.getPrinter(context, config).disconnect()
          result.success(true)
        }
      }
      "decodeImage" -> {
        val bytes = call.argument<ByteArray>("bytes")
        val width = call.argument<Int>("width") ?: 384
        
        if (bytes == null) {
          result.error("INVALID_ARGUMENT", "Image bytes are missing", null)
          return
        }

        scope.launch {
          try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap == null) {
              result.error("DECODE_ERROR", "Could not decode bitmap", null)
              return@launch
            }
            
            val resized = com.diode.icod_printer.utils.ImageUtils.resizeForPrinter(bitmap, width)
            val escPosBytes = com.diode.icod_printer.utils.ImageUtils.decodeBitmap(resized, width)
            result.success(escPosBytes)
          } catch (e: Exception) {
            result.error("DECODE_ERROR", e.message, null)
          }
        }
      }
      "getStatus" -> {
        val configMap = call.arguments as Map<String, Any>
        val config = parseConfig(configMap)
        
        scope.launch {
          try {
            val printer = KioskPrinterManager.getPrinter(context, config)
            val results = mutableListOf<Int>()
            
            // Standard ESC/POS DLE EOT n commands (1-4)
            for (n in 1..4) {
              val cmd = byteArrayOf(0x10, 0x04, n.toByte())
              printer.send(cmd)
              val response = printer.receive(200) // 200ms timeout per byte
              val byte = response?.getOrNull(0)
              results.add(if (byte != null) byte.toInt() and 0xFF else -1)
            }
            
            result.success(results)
          } catch (e: Exception) {
            result.error("STATUS_ERROR", e.message, null)
          }
        }
      }
      "print" -> {
        val data = call.argument<ByteArray>("data") ?: return result.error("INVALID_DATA", "No data provided", null)
        val configMap = call.argument<Map<String, Any>>("config") ?: return result.error("INVALID_CONFIG", "No config provided", null)
        val config = parseConfig(configMap)
        
        scope.launch {
          val printer = KioskPrinterManager.getPrinter(context, config)
          val printResult = printer.send(data)
          if (printResult.isSuccess) {
            result.success(true)
          } else {
            result.error("PRINT_ERROR", printResult.exceptionOrNull()?.message, null)
          }
        }
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  private fun parseConfig(map: Map<String, Any>): PrinterConfig {
    val transportStr = map["transport"] as String
    val transport = TransportType.valueOf(transportStr.uppercase())
    return PrinterConfig(
      transportType = transport,
      name = map["name"] as? String,
      address = map["address"] as? String,
      port = (map["port"] as? Int) ?: 9100,
      baudRate = (map["baudRate"] as? Int) ?: 115200,
      usbDeviceName = map["usbDeviceName"] as? String,
      paperSize = (map["paperSize"] as? Int) ?: 80
    )
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    scope.cancel()
  }
}
