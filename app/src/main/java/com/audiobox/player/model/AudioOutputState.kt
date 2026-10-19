package com.audiobox.player.model

enum class AudioDeviceCategory(val displayName: String) {
    USB_DAC("USB Audio DAC"),
    WIRED_HEADSET("Wired Headphones (3.5 mm)"),
    BLUETOOTH("Bluetooth Wireless Audio"),
    BUILTIN_SPEAKER("Built-in Phone Speaker"),
    UNKNOWN("Default System Output")
}

enum class BitPerfectStatus(val label: String) {
    STANDARD_OUTPUT("Standard Output"),
    SUPPORTED("Bit-Perfect Supported"),
    REQUESTED("Bit-Perfect Active (Exclusive)"),
    FAILED("Bit-Perfect Rejected"),
    UNAVAILABLE("Bit-Perfect Unavailable")
}

data class AudioOutputState(
    val activeDeviceName: String = "Internal Speaker",
    val deviceCategory: AudioDeviceCategory = AudioDeviceCategory.BUILTIN_SPEAKER,
    val isUsbDac: Boolean = false,
    val bitPerfectStatus: BitPerfectStatus = BitPerfectStatus.STANDARD_OUTPUT,
    val statusDescription: String = "Routed through Android AudioFlinger standard mixer (resampled/mixed).",
    val isBitPerfectEnabledByUser: Boolean = false
)
