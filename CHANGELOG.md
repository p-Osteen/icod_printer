# Changelog

All notable changes to this project will be documented in this file.

## [0.1.0] - 2026-04-01

### Added
- **Initial release** of the Kiosk Printer SDK.
- Support for **USB**, **Bluetooth**, and **TCP/IP** (Network) printer interfaces.
- `EscPosBuilder` for advanced receipt formatting, including:
  - Text alignment, sizing, and bolding.
  - Custom line spacing.
  - Table row layout support.
  - Image printing from raster bytes.
  - **QR Code** and **Barcode** (CODE128) generation.
- Real-time **Printer Status** reporting (Paper Out, Near-end, Error states).
- Built-in device **Discovery** for USB and Bluetooth printers.
