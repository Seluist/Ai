package com.example.capri.tool

import android.app.Activity
import android.util.Log
import com.example.capri.audio.TtsManager
import com.example.capri.device.DeviceControlManager
import org.json.JSONArray
import org.json.JSONObject

data class ToolParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = false
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>
) {
    fun toGeminiJson(): JSONObject {
        val root = JSONObject()
        root.put("name", name)
        root.put("description", description)

        val paramsObj = JSONObject()
        paramsObj.put("type", "OBJECT")

        val propsObj = JSONObject()
        val reqArr = JSONArray()

        for (param in parameters) {
            val prop = JSONObject()
            prop.put("type", param.type)
            prop.put("description", param.description)
            propsObj.put(param.name, prop)

            if (param.required) {
                reqArr.put(param.name)
            }
        }

        paramsObj.put("properties", propsObj)
        if (reqArr.length() > 0) {
            paramsObj.put("required", reqArr)
        }

        root.put("parameters", paramsObj)
        return root
    }
}

data class ToolExecutionResult(
    val toolName: String,
    val isSuccess: Boolean,
    val spokenResponse: String,
    val technicalDetails: String? = null
)

class CapriToolRegistry(
    private val deviceControlManager: DeviceControlManager,
    private val ttsManager: TtsManager
) {
    val supportedTools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "launch_app",
            description = "Launch an installed application such as Discord, YouTube, Chrome, Settings, Spotify, Camera, or any app on the user's phone.",
            parameters = listOf(
                ToolParameter("app_name", "STRING", "The display name or package identifier of the app to launch (e.g. 'Discord', 'YouTube', 'Chrome')", required = true)
            )
        ),
        ToolDefinition(
            name = "control_flashlight",
            description = "Turn the flashlight on or off, or calibrate brightness strength.",
            parameters = listOf(
                ToolParameter("enabled", "BOOLEAN", "True to turn on, False to turn off", required = true),
                ToolParameter("level", "INTEGER", "Optional brightness percentage from 1 to 100", required = false)
            )
        ),
        ToolDefinition(
            name = "control_volume",
            description = "Adjust or set the device media volume level.",
            parameters = listOf(
                ToolParameter("percent", "INTEGER", "Specific volume percentage from 0 to 100", required = false),
                ToolParameter("direction", "STRING", "Direction or mode: 'up', 'down', 'mute', or 'max'", required = false)
            )
        ),
        ToolDefinition(
            name = "control_brightness",
            description = "Adjust or set the device screen brightness.",
            parameters = listOf(
                ToolParameter("percent", "INTEGER", "Brightness percentage from 5 to 100", required = false),
                ToolParameter("direction", "STRING", "Direction: 'up', 'down', 'max', or 'dim'", required = false)
            )
        ),
        ToolDefinition(
            name = "web_search",
            description = "Search the web or open Chrome to search for specific content (e.g. 'Minecraft shaders').",
            parameters = listOf(
                ToolParameter("query", "STRING", "Search terms or keywords", required = true),
                ToolParameter("browser", "STRING", "Target browser name such as 'Chrome'", required = false)
            )
        ),
        ToolDefinition(
            name = "open_settings",
            description = "Open Android Settings or specific sub-settings (e.g. wifi, bluetooth, display, sound, apps).",
            parameters = listOf(
                ToolParameter("subpage", "STRING", "Settings subpage such as 'wifi', 'bluetooth', 'display', 'sound', 'apps', or 'general'", required = false)
            )
        ),
        ToolDefinition(
            name = "stop_speaking",
            description = "Interrupt Capri immediately and stop speech playback.",
            parameters = emptyList()
        ),
        ToolDefinition(
            name = "get_device_status",
            description = "Check battery power percentage, charging status, or current local time.",
            parameters = listOf(
                ToolParameter("type", "STRING", "Status type: 'battery', 'time', or 'all'", required = false)
            )
        ),
        ToolDefinition(
            name = "control_music",
            description = "Control music or audio media playback (play, pause, next track, previous track, toggle).",
            parameters = listOf(
                ToolParameter("action", "STRING", "Playback action: 'play', 'pause', 'next', 'previous', or 'toggle'", required = true)
            )
        )
    )

    fun getGeminiToolsDeclarationJson(): JSONArray {
        val funcs = JSONArray()
        for (tool in supportedTools) {
            funcs.put(tool.toGeminiJson())
        }
        val wrapper = JSONObject().apply {
            put("function_declarations", funcs)
        }
        return JSONArray().put(wrapper)
    }

    fun executeTool(name: String, args: Map<String, Any?>, activity: Activity? = null): ToolExecutionResult {
        Log.d("CapriToolRegistry", "Executing tool '$name' with args: $args")

        return when (name.lowercase()) {
            "launch_app" -> {
                val appName = args["app_name"]?.toString() ?: ""
                val (success, msg) = deviceControlManager.launchApp(appName)
                ToolExecutionResult(
                    toolName = name,
                    isSuccess = success,
                    spokenResponse = msg,
                    technicalDetails = "Launched app: $appName"
                )
            }
            "control_flashlight" -> {
                val enabled = args["enabled"]?.let {
                    when (it) {
                        is Boolean -> it
                        is String -> it.toBooleanStrictOrNull() ?: (it.lowercase() == "true")
                        else -> true
                    }
                } ?: true

                val level = (args["level"] as? Number)?.toInt()

                val (success, msg) = if (enabled && level != null && level > 0) {
                    deviceControlManager.setTorchLevel(level)
                } else {
                    deviceControlManager.setTorch(enabled)
                }

                ToolExecutionResult(
                    toolName = name,
                    isSuccess = success,
                    spokenResponse = msg,
                    technicalDetails = "Torch: enabled=$enabled, level=$level"
                )
            }
            "control_volume" -> {
                val percent = (args["percent"] as? Number)?.toInt()
                val direction = args["direction"]?.toString()?.lowercase()

                val (success, msg) = when {
                    percent != null -> deviceControlManager.setVolume(percent)
                    direction == "up" -> deviceControlManager.volumeUp()
                    direction == "down" -> deviceControlManager.volumeDown()
                    direction == "mute" -> deviceControlManager.muteVolume()
                    direction == "max" -> deviceControlManager.maxVolume()
                    else -> deviceControlManager.volumeUp()
                }

                ToolExecutionResult(
                    toolName = name,
                    isSuccess = success,
                    spokenResponse = msg,
                    technicalDetails = "Volume: percent=$percent, direction=$direction"
                )
            }
            "control_brightness" -> {
                val percent = (args["percent"] as? Number)?.toInt()
                val direction = args["direction"]?.toString()?.lowercase()

                val target = when {
                    percent != null -> percent
                    direction == "up" || direction == "max" -> 100
                    direction == "down" || direction == "dim" -> 15
                    else -> 75
                }

                val (success, msg) = deviceControlManager.setScreenBrightness(activity, target)
                ToolExecutionResult(
                    toolName = name,
                    isSuccess = success,
                    spokenResponse = msg,
                    technicalDetails = "Brightness: target=$target%"
                )
            }
            "web_search" -> {
                val query = args["query"]?.toString() ?: ""
                val browser = args["browser"]?.toString() ?: "Chrome"
                val (success, msg) = deviceControlManager.searchWeb(query, browser)
                ToolExecutionResult(
                    toolName = name,
                    isSuccess = success,
                    spokenResponse = msg,
                    technicalDetails = "Web search: query='$query', browser='$browser'"
                )
            }
            "open_settings" -> {
                val subpage = args["subpage"]?.toString()
                val (success, msg) = deviceControlManager.openSettings(subpage)
                ToolExecutionResult(
                    toolName = name,
                    isSuccess = success,
                    spokenResponse = msg,
                    technicalDetails = "Settings: subpage=$subpage"
                )
            }
            "stop_speaking" -> {
                ttsManager.stop()
                ToolExecutionResult(
                    toolName = name,
                    isSuccess = true,
                    spokenResponse = "Muted synthesizer. Standing by, Seluist.",
                    technicalDetails = "TTS stopped"
                )
            }
            "get_device_status" -> {
                val type = args["type"]?.toString()?.lowercase() ?: "all"
                val response = when (type) {
                    "battery" -> deviceControlManager.getBatteryStatus()
                    "time" -> deviceControlManager.getLocalTime()
                    else -> "${deviceControlManager.getBatteryStatus()} ${deviceControlManager.getLocalTime()}"
                }
                ToolExecutionResult(
                    toolName = name,
                    isSuccess = true,
                    spokenResponse = response,
                    technicalDetails = "Status: $type"
                )
            }
            "control_music" -> {
                val action = args["action"]?.toString() ?: "toggle"
                val (success, msg) = deviceControlManager.controlMedia(action)
                ToolExecutionResult(
                    toolName = name,
                    isSuccess = success,
                    spokenResponse = msg,
                    technicalDetails = "Media: action=$action"
                )
            }
            else -> {
                ToolExecutionResult(
                    toolName = name,
                    isSuccess = false,
                    spokenResponse = "Unrecognized cyber tool '$name', choom.",
                    technicalDetails = "Unknown tool"
                )
            }
        }
    }
}
