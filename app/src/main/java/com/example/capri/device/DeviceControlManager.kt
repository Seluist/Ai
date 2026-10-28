package com.example.capri.device

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.view.KeyEvent
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceControlManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _volumePercent = MutableStateFlow(calculateCurrentVolumePercent())
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    private val _screenBrightness = MutableStateFlow(0.7f)
    val screenBrightness: StateFlow<Float> = _screenBrightness.asStateFlow()

    private var cameraId: String? = null

    init {
        initCamera()
        _volumePercent.value = calculateCurrentVolumePercent()
    }

    private fun initCamera() {
        try {
            cameraManager?.let { cm ->
                for (id in cm.cameraIdList) {
                    val characteristics = cm.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = id
                        break
                    }
                }
                if (cameraId == null && cm.cameraIdList.isNotEmpty()) {
                    cameraId = cm.cameraIdList[0]
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Error querying camera: ${e.message}")
        }
    }

    fun setTorch(enable: Boolean): Pair<Boolean, String> {
        val cid = cameraId
        if (cameraManager == null || cid == null) {
            return Pair(false, "Flashlight hardware unavailable on this cyberdeck.")
        }
        return try {
            cameraManager.setTorchMode(cid, enable)
            _isTorchOn.value = enable
            if (enable) {
                Pair(true, "Photon emitters activated. Illuminating Night City.")
            } else {
                Pair(true, "Photon emitters extinguished. Slipping into stealth mode.")
            }
        } catch (e: CameraAccessException) {
            Log.e("DeviceControlManager", "Torch access failed: ${e.message}")
            Pair(false, "Camera hardware is currently busy or locked by another daemon.")
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Torch error: ${e.message}")
            Pair(false, "Unable to toggle photon emitter: ${e.message}")
        }
    }

    fun setTorchLevel(levelPercent: Int): Pair<Boolean, String> {
        val cid = cameraId
        if (cameraManager == null || cid == null) {
            return Pair(false, "Flashlight hardware unavailable on this cyberdeck.")
        }
        val clamped = levelPercent.coerceIn(1, 100)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val characteristics = cameraManager.getCameraCharacteristics(cid)
                val maxLevel = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                if (maxLevel > 1) {
                    val targetStrength = ((clamped / 100f) * maxLevel).toInt().coerceIn(1, maxLevel)
                    cameraManager.turnOnTorchWithStrengthLevel(cid, targetStrength)
                    _isTorchOn.value = true
                    Pair(true, "Photon emitter calibrated to $clamped% strength (level $targetStrength/$maxLevel).")
                } else {
                    cameraManager.setTorchMode(cid, true)
                    _isTorchOn.value = true
                    Pair(true, "Flashlight active at standard maximum brightness.")
                }
            } else {
                cameraManager.setTorchMode(cid, true)
                _isTorchOn.value = true
                Pair(true, "Flashlight active at standard brightness on this OS level.")
            }
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Torch level error: ${e.message}")
            setTorch(true)
        }
    }

    fun toggleTorch(): Pair<Boolean, String> {
        return setTorch(!_isTorchOn.value)
    }

    fun volumeUp(): Pair<Boolean, String> {
        val am = audioManager ?: return Pair(false, "Audio hardware not responding.")
        return try {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            _volumePercent.value = calculateCurrentVolumePercent()
            Pair(true, "Cranked the decibels. Audio at ${_volumePercent.value}%.")
        } catch (e: Exception) {
            Pair(false, "Failed to boost audio volume.")
        }
    }

    fun volumeDown(): Pair<Boolean, String> {
        val am = audioManager ?: return Pair(false, "Audio hardware not responding.")
        return try {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            _volumePercent.value = calculateCurrentVolumePercent()
            Pair(true, "Lowered the acoustic output. Sitting at ${_volumePercent.value}%.")
        } catch (e: Exception) {
            Pair(false, "Failed to reduce audio volume.")
        }
    }

    fun setVolume(percent: Int): Pair<Boolean, String> {
        val am = audioManager ?: return Pair(false, "Audio hardware not responding.")
        val clamped = percent.coerceIn(0, 100)
        return try {
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetIndex = ((clamped / 100f) * maxVol).toInt().coerceIn(0, maxVol)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, AudioManager.FLAG_SHOW_UI)
            _volumePercent.value = calculateCurrentVolumePercent()
            Pair(true, "Audio levels calibrated to $clamped%.")
        } catch (e: Exception) {
            Pair(false, "Failed to set audio levels.")
        }
    }

    fun muteVolume(): Pair<Boolean, String> {
        val am = audioManager ?: return Pair(false, "Audio hardware not responding.")
        return try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            _volumePercent.value = 0
            Pair(true, "Acoustics zeroed. Full stealth silence.")
        } catch (e: Exception) {
            Pair(false, "Failed to mute audio.")
        }
    }

    fun maxVolume(): Pair<Boolean, String> {
        val am = audioManager ?: return Pair(false, "Audio hardware not responding.")
        return try {
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
            _volumePercent.value = 100
            Pair(true, "Maximum decibels unlocked. Don't blast your cyberware!")
        } catch (e: Exception) {
            Pair(false, "Failed to max out audio.")
        }
    }

    fun controlMedia(action: String): Pair<Boolean, String> {
        val am = audioManager ?: return Pair(false, "Audio subsystem offline.")
        val keyCode = when (action.lowercase().trim()) {
            "play", "resume" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause", "stop" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "play_pause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev", "back" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        return try {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            val msg = when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> "Resuming music stream. Let the beats roll, Seluist."
                KeyEvent.KEYCODE_MEDIA_PAUSE -> "Paused the audio stream."
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "Toggled media playback."
                KeyEvent.KEYCODE_MEDIA_NEXT -> "Skipping to the next track. Let's find some better rhythm."
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "Rewinding to the previous track."
                else -> "Media command dispatched."
            }
            Pair(true, msg)
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Error dispatching media key: ${e.message}")
            Pair(false, "Failed to dispatch media signal: ${e.message}")
        }
    }

    private fun calculateCurrentVolumePercent(): Int {
        val am = audioManager ?: return 50
        return try {
            val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) ((current.toFloat() / max.toFloat()) * 100).toInt() else 50
        } catch (e: Exception) {
            50
        }
    }

    fun setScreenBrightness(activity: Activity?, brightnessPercent: Int): Pair<Boolean, String> {
        if (activity == null) return Pair(false, "Host display link detached.")
        val clampedPercent = brightnessPercent.coerceIn(5, 100)
        val brightnessFloat = clampedPercent / 100f
        return try {
            activity.runOnUiThread {
                val layoutParams = activity.window.attributes
                layoutParams.screenBrightness = brightnessFloat
                activity.window.attributes = layoutParams
            }
            _screenBrightness.value = brightnessFloat
            Pair(true, "Optical display output adjusted to $clampedPercent%.")
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Brightness update failed: ${e.message}")
            Pair(false, "Optical display adjustment encountered an error.")
        }
    }

    fun getBatteryStatus(): String {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 75
            val chargingTag = if (isCharging) "plugged into power grid" else "running on internal cells"
            "Power cell at $batteryPct%, $chargingTag. Enough juice for plenty of runs."
        } catch (e: Exception) {
            "Power cell diagnostics: Nominally operational at 80%."
        }
    }

    fun getLocalTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateStr = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
        return "Current net time: ${sdf.format(Date())} ($dateStr)."
    }

    fun launchApp(appName: String): Pair<Boolean, String> {
        val cleanName = appName.trim().lowercase()
        if (cleanName.isEmpty()) {
            return Pair(false, "No app name specified, choom.")
        }

        // Special system intent shortcuts
        if (cleanName == "settings" || cleanName.contains("setting")) {
            return openSettings(null)
        }

        val pm = context.packageManager
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, 0)

            // Try exact label match first, then substring match, then package match
            var matchedPackage: String? = null
            var matchedLabel: String? = null

            for (app in apps) {
                val label = app.loadLabel(pm).toString()
                if (label.equals(cleanName, ignoreCase = true)) {
                    matchedPackage = app.activityInfo.packageName
                    matchedLabel = label
                    break
                }
            }

            if (matchedPackage == null) {
                for (app in apps) {
                    val label = app.loadLabel(pm).toString()
                    if (label.lowercase().contains(cleanName)) {
                        matchedPackage = app.activityInfo.packageName
                        matchedLabel = label
                        break
                    }
                }
            }

            if (matchedPackage == null) {
                // Check common alias packages
                val commonAliases = mapOf(
                    "discord" to listOf("com.discord", "com.discord.canary"),
                    "youtube" to listOf("com.google.android.youtube"),
                    "chrome" to listOf("com.android.chrome"),
                    "spotify" to listOf("com.spotify.music"),
                    "maps" to listOf("com.google.android.apps.maps"),
                    "play store" to listOf("com.android.vending"),
                    "camera" to listOf("com.android.camera", "com.google.android.GoogleCamera")
                )
                val candidatePackages = commonAliases[cleanName]
                if (candidatePackages != null) {
                    for (pkg in candidatePackages) {
                        if (pm.getLaunchIntentForPackage(pkg) != null) {
                            matchedPackage = pkg
                            matchedLabel = appName
                            break
                        }
                    }
                }
            }

            if (matchedPackage != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return Pair(true, "Launching ${matchedLabel ?: appName}. Jacking in now, Seluist.")
                }
            }

            return Pair(false, "App '$appName' was not found on your cyberdeck.")
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Error launching app: ${e.message}")
            return Pair(false, "Glitch while executing launch sequence for '$appName': ${e.message}")
        }
    }

    fun searchWeb(query: String, browserName: String? = null): Pair<Boolean, String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return Pair(false, "No search query provided.")
        }

        return try {
            val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
            val searchUri = android.net.Uri.parse("https://www.google.com/search?q=$encoded")
            val webIntent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (browserName != null && browserName.contains("chrome", ignoreCase = true)) {
                // If Chrome requested, prefer Chrome package
                val pm = context.packageManager
                val chromeLaunch = pm.getLaunchIntentForPackage("com.android.chrome")
                if (chromeLaunch != null) {
                    webIntent.setPackage("com.android.chrome")
                }
            }

            context.startActivity(webIntent)
            Pair(true, "Net search initiated for \"$trimmed\". Opening display now.")
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Error opening search: ${e.message}")
            Pair(false, "Subnet search error: ${e.message}")
        }
    }

    fun openSettings(subPage: String? = null): Pair<Boolean, String> {
        return try {
            val action = when (subPage?.trim()?.lowercase()) {
                "wifi", "wi-fi", "internet", "network" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth", "bt" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "display", "screen", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound", "audio", "volume" -> Settings.ACTION_SOUND_SETTINGS
                "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val pageName = subPage?.replaceFirstChar { it.uppercase() } ?: "System"
            Pair(true, "Accessing $pageName settings.")
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Error opening settings: ${e.message}")
            Pair(false, "Could not open system settings: ${e.message}")
        }
    }
}
