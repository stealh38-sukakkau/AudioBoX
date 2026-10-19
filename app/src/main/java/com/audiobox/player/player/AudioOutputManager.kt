package com.audiobox.player.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.audiobox.player.model.AudioDeviceCategory
import com.audiobox.player.model.AudioOutputState
import com.audiobox.player.model.AudioStreamInfo
import com.audiobox.player.model.BitPerfectStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manajer Output Audio & Deteksi USB DAC Bit-Perfect.
 * Memantau perangkat output aktif (USB DAC, 3.5mm Jack, Bluetooth, Speaker),
 * dan pada Android 14+ (API 34) mengonfigurasi AudioMixerAttributes BIT_PERFECT secara jujur dan aman.
 */
class AudioOutputManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _outputState = MutableStateFlow(AudioOutputState())
    val outputState: StateFlow<AudioOutputState> = _outputState.asStateFlow()

    private var currentStreamInfo = AudioStreamInfo()
    private var isUserBitPerfectRequested = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            evaluateAudioRouting()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            evaluateAudioRouting()
        }
    }

    init {
        try {
            audioManager?.registerAudioDeviceCallback(deviceCallback, mainHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        evaluateAudioRouting()
    }

    fun updateStreamInfo(streamInfo: AudioStreamInfo) {
        currentStreamInfo = streamInfo
        evaluateAudioRouting()
    }

    fun setBitPerfectRequested(enabled: Boolean) {
        isUserBitPerfectRequested = enabled
        evaluateAudioRouting()
    }

    fun evaluateAudioRouting() {
        val manager = audioManager ?: return
        val outputDevices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        // Prioritas deteksi: USB DAC > 3.5mm Wired Headset > Bluetooth > Speaker
        val usbDevice = outputDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        }

        val wiredDevice = outputDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        }

        val bluetoothDevice = outputDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }

        val activeDevice = usbDevice ?: wiredDevice ?: bluetoothDevice ?: outputDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }

        val deviceCategory = when {
            usbDevice != null -> AudioDeviceCategory.USB_DAC
            wiredDevice != null -> AudioDeviceCategory.WIRED_HEADSET
            bluetoothDevice != null -> AudioDeviceCategory.BLUETOOTH
            activeDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceCategory.BUILTIN_SPEAKER
            else -> AudioDeviceCategory.UNKNOWN
        }

        val deviceName = activeDevice?.productName?.toString().takeIf { !it.isNullOrBlank() }
            ?: deviceCategory.displayName

        if (usbDevice != null) {
            handleUsbDacRouting(usbDevice, deviceName)
        } else {
            // Non-USB Output (Jack 3.5mm, Speaker, Bluetooth)
            val description = when (deviceCategory) {
                AudioDeviceCategory.WIRED_HEADSET -> {
                    "Standard Android AudioFlinger Output (3.5 mm). Audio is resampled and mixed by the Android audio HAL; bit-perfect mode cannot be verified."
                }
                AudioDeviceCategory.BLUETOOTH -> {
                    "Bluetooth A2DP wireless stream. Audio is compressed/transcoded by Bluetooth codec (SBC/AAC/LDAC)."
                }
                AudioDeviceCategory.BUILTIN_SPEAKER -> {
                    "Internal phone speaker output via standard Android audio pipeline."
                }
                else -> {
                    "Standard system audio routing."
                }
            }

            _outputState.update {
                it.copy(
                    activeDeviceName = deviceName,
                    deviceCategory = deviceCategory,
                    isUsbDac = false,
                    bitPerfectStatus = BitPerfectStatus.STANDARD_OUTPUT,
                    statusDescription = description,
                    isBitPerfectEnabledByUser = isUserBitPerfectRequested
                )
            }
        }
    }

    private fun handleUsbDacRouting(usbDevice: AudioDeviceInfo, deviceName: String) {
        val manager = audioManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (API 34) AudioMixerAttributes API
            try {
                val supportedMixers = manager.getSupportedMixerAttributes(usbDevice)
                val isBitPerfectSupported = supportedMixers.any {
                    it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
                }

                if (!isBitPerfectSupported) {
                    _outputState.update {
                        it.copy(
                            activeDeviceName = deviceName,
                            deviceCategory = AudioDeviceCategory.USB_DAC,
                            isUsbDac = true,
                            bitPerfectStatus = BitPerfectStatus.UNAVAILABLE,
                            statusDescription = "USB DAC detected ($deviceName), but device reports no hardware bit-perfect mixer behavior support.",
                            isBitPerfectEnabledByUser = isUserBitPerfectRequested
                        )
                    }
                    return
                }

                if (isUserBitPerfectRequested) {
                    val applied = applyBitPerfectMixer(manager, usbDevice)
                    if (applied) {
                        _outputState.update {
                            it.copy(
                                activeDeviceName = deviceName,
                                deviceCategory = AudioDeviceCategory.USB_DAC,
                                isUsbDac = true,
                                bitPerfectStatus = BitPerfectStatus.REQUESTED,
                                statusDescription = "Bit-perfect mode ACTIVE via Android 14 AudioMixerAttributes. Audio stream bypasses Android AudioFlinger resampling.",
                                isBitPerfectEnabledByUser = true
                            )
                        }
                    } else {
                        _outputState.update {
                            it.copy(
                                activeDeviceName = deviceName,
                                deviceCategory = AudioDeviceCategory.USB_DAC,
                                isUsbDac = true,
                                bitPerfectStatus = BitPerfectStatus.FAILED,
                                statusDescription = "Bit-perfect mixer rejected by device for current audio sample rate (${currentStreamInfo.sampleRate} Hz). Fallback to standard USB output.",
                                isBitPerfectEnabledByUser = false
                            )
                        }
                    }
                } else {
                    _outputState.update {
                        it.copy(
                            activeDeviceName = deviceName,
                            deviceCategory = AudioDeviceCategory.USB_DAC,
                            isUsbDac = true,
                            bitPerfectStatus = BitPerfectStatus.SUPPORTED,
                            statusDescription = "USB DAC supports Bit-Perfect Mode! Tap toggle to request bit-perfect mixer bypass.",
                            isBitPerfectEnabledByUser = false
                        )
                    }
                }
            } catch (e: Exception) {
                _outputState.update {
                    it.copy(
                        activeDeviceName = deviceName,
                        deviceCategory = AudioDeviceCategory.USB_DAC,
                        isUsbDac = true,
                        bitPerfectStatus = BitPerfectStatus.FAILED,
                        statusDescription = "Error configuring USB Mixer: ${e.localizedMessage}",
                        isBitPerfectEnabledByUser = false
                    )
                }
            }
        } else {
            // Android 13 kebawah (API < 34)
            _outputState.update {
                it.copy(
                    activeDeviceName = deviceName,
                    deviceCategory = AudioDeviceCategory.USB_DAC,
                    isUsbDac = true,
                    bitPerfectStatus = BitPerfectStatus.UNAVAILABLE,
                    statusDescription = "USB DAC detected ($deviceName). Android OS Bit-Perfect Mixer API requires Android 14+ (API 34); audio routed via standard Android USB audio driver.",
                    isBitPerfectEnabledByUser = false
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun applyBitPerfectMixer(manager: AudioManager, usbDevice: AudioDeviceInfo): Boolean {
        return try {
            val sampleRate = if (currentStreamInfo.sampleRate > 0) currentStreamInfo.sampleRate else 44100
            val channelMask = if (currentStreamInfo.channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
            val encoding = AudioFormat.ENCODING_PCM_16BIT

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelMask)
                .setEncoding(encoding)
                .build()

            val mixerAttributes = AudioMixerAttributes.Builder(audioFormat)
                .setMixerBehavior(AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT)
                .build()

            val mediaAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            manager.setPreferredMixerAttributes(mediaAudioAttributes, usbDevice, mixerAttributes)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun release() {
        try {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
