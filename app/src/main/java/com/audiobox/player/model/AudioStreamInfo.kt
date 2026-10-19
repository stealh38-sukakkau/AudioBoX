package com.audiobox.player.model

data class AudioStreamInfo(
    val codecName: String = "Unknown",
    val sampleRate: Int = 0,
    val bitDepth: String = "16-bit",
    val channelCount: Int = 2,
    val bitrateKbps: Int = 0,
    val mimeType: String = ""
) {
    val isHiRes: Boolean
        get() = sampleRate >= 88200 || bitDepth.contains("24") || bitDepth.contains("32")

    fun formattedSummary(): String {
        val srText = if (sampleRate > 0) "${sampleRate / 1000.0} kHz".replace(".0", "") else "Standard Rate"
        val chText = if (channelCount == 1) "Mono" else "Stereo"
        return "$codecName • $srText / $bitDepth • $chText"
    }
}
