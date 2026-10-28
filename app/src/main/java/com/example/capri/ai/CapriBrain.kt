package com.example.capri.ai

import android.app.Activity
import com.example.capri.tool.CapriToolRegistry
import com.example.capri.tool.ToolExecutionResult

data class CommandResult(
    val handledLocally: Boolean,
    val responseText: String,
    val commandType: String? = null,
    val isSuccess: Boolean = true,
    val shouldPassToOnlineAi: Boolean = false,
    val toolResult: ToolExecutionResult? = null
)

class CapriBrain(
    val toolRegistry: CapriToolRegistry
) {

    fun processInput(rawInput: String, activity: Activity? = null): CommandResult {
        val input = rawInput.trim().lowercase()

        // 1. Interruption / Stop talking ("Stop talking", "Be quiet", "Shut up", "หยุดพูด", "เงียบ")
        if (input == "stop talking" ||
            input == "stop" ||
            input == "be quiet" ||
            input == "shut up" ||
            input == "silence please" ||
            input.contains("หยุดพูด") ||
            input.contains("เงียบ")
        ) {
            val result = toolRegistry.executeTool("stop_speaking", emptyMap(), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "STOP_SPEAKING",
                isSuccess = true,
                toolResult = result
            )
        }

        // 2. Web Search ("Open Chrome and search for Minecraft shaders", "search for ... on chrome", "search for ...")
        val chromeSearchRegex = Regex("(?:open\\s+chrome\\s+and\\s+search\\s+for|search\\s+for\\s+(.+)\\s+on\\s+chrome|search\\s+on\\s+chrome\\s+for|ค้นหาในโครมว่า|ค้นหาโครม)\\s*(.*)")
        val chromeSearchMatch = chromeSearchRegex.find(input)
        if (chromeSearchMatch != null) {
            val query = (chromeSearchMatch.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
                ?: chromeSearchMatch.groupValues.getOrNull(2) ?: "").trim()
            if (query.isNotBlank()) {
                val result = toolRegistry.executeTool("web_search", mapOf("query" to query, "browser" to "Chrome"), activity)
                return CommandResult(
                    handledLocally = true,
                    responseText = "Opening Chrome and searching for '$query', Seluist.",
                    commandType = "WEB_SEARCH",
                    isSuccess = result.isSuccess,
                    toolResult = result
                )
            }
        }

        val generalSearchRegex = Regex("^(?:search\\s+for|google|search|ค้นหาว่า|ค้นหา)\\s+(.+)$")
        val generalSearchMatch = generalSearchRegex.find(input)
        if (generalSearchMatch != null) {
            val query = generalSearchMatch.groupValues[1].trim()
            if (query.isNotBlank()) {
                val result = toolRegistry.executeTool("web_search", mapOf("query" to query), activity)
                return CommandResult(
                    handledLocally = true,
                    responseText = "Searching the Net for '$query'.",
                    commandType = "WEB_SEARCH",
                    isSuccess = result.isSuccess,
                    toolResult = result
                )
            }
        }

        // 3. Flashlight commands ("Turn on the flashlight", "Make the flashlight brighter", etc.)
        if (input.contains("flashlight brighter") ||
            input.contains("make the flashlight brighter") ||
            input.contains("brighter flashlight") ||
            input.contains("brighten flashlight") ||
            input.contains("เพิ่มไฟฉาย")
        ) {
            val result = toolRegistry.executeTool("control_flashlight", mapOf("enabled" to true, "level" to 100), activity)
            return CommandResult(
                handledLocally = true,
                responseText = "Photon emitters cranked to maximum brightness. Watch your eyes, choom.",
                commandType = "TORCH_BRIGHTER",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        if (input.contains("flashlight on") ||
            input.contains("torch on") ||
            input.contains("lights on") ||
            input.contains("turn on flashlight") ||
            input.contains("turn on the flashlight") ||
            input.contains("turn on the torch") ||
            input.contains("turn on light") ||
            input.contains("enable flashlight") ||
            input.contains("เปิดไฟฉาย")
        ) {
            val result = toolRegistry.executeTool("control_flashlight", mapOf("enabled" to true), activity)
            return CommandResult(
                handledLocally = true,
                responseText = "Photon emitters active. Illuminating Night City.",
                commandType = "TORCH_ON",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        if (input.contains("flashlight off") ||
            input.contains("torch off") ||
            input.contains("lights off") ||
            input.contains("turn off flashlight") ||
            input.contains("turn off the flashlight") ||
            input.contains("turn off the torch") ||
            input.contains("turn off light") ||
            input.contains("disable flashlight") ||
            input.contains("kill the light") ||
            input.contains("ปิดไฟฉาย")
        ) {
            val result = toolRegistry.executeTool("control_flashlight", mapOf("enabled" to false), activity)
            return CommandResult(
                handledLocally = true,
                responseText = "Photon emitters extinguished. Slipping into stealth mode.",
                commandType = "TORCH_OFF",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        // 4. Volume commands ("Lower the volume to 20 percent", "volume up", etc.)
        val volumeToMatch = Regex("(?:lower\\s+the\\s+volume\\s+to|set\\s+volume\\s+to|volume\\s+to|volume\\s+at|volume|ลดเสียงเหลือ|ปรับเสียงเป็น)\\s+(\\d{1,3})%?").find(input)
        if (volumeToMatch != null) {
            val percent = volumeToMatch.groupValues[1].toIntOrNull() ?: 50
            val result = toolRegistry.executeTool("control_volume", mapOf("percent" to percent), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "VOLUME_SET",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        if (input.contains("volume up") ||
            input.contains("turn it up") ||
            input.contains("louder") ||
            input.contains("increase volume") ||
            input.contains("boost audio") ||
            input.contains("crank the volume") ||
            input.contains("เพิ่มเสียง")
        ) {
            val result = toolRegistry.executeTool("control_volume", mapOf("direction" to "up"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "VOLUME_UP",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        if (input.contains("volume down") ||
            input.contains("turn it down") ||
            input.contains("quieter") ||
            input.contains("decrease volume") ||
            input.contains("lower volume") ||
            input.contains("lower the volume") ||
            input.contains("ลดเสียง")
        ) {
            val result = toolRegistry.executeTool("control_volume", mapOf("direction" to "down"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "VOLUME_DOWN",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        if (input.contains("mute") || input.contains("silence") || input.contains("ปิดเสียง")) {
            val result = toolRegistry.executeTool("control_volume", mapOf("direction" to "mute"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "VOLUME_MUTE",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        if (input.contains("max volume") || input.contains("maximum volume") || input.contains("volume 100") || input.contains("เสียงสุด")) {
            val result = toolRegistry.executeTool("control_volume", mapOf("direction" to "max"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "VOLUME_MAX",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        // 4.5 Music & Media control commands ("Play music", "Pause music", "Next song", "Skip", etc.)
        if (input.contains("pause music") ||
            input.contains("stop music") ||
            input.contains("pause song") ||
            input.contains("pause track") ||
            input.contains("หยุดเพลง") ||
            input.contains("พักเพลง")
        ) {
            val result = toolRegistry.executeTool("control_music", mapOf("action" to "pause"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "MEDIA_PAUSE",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        if (input.contains("play music") ||
            input.contains("resume music") ||
            input.contains("start music") ||
            input.contains("play song") ||
            input.contains("resume song") ||
            input.contains("เล่นเพลง")
        ) {
            val result = toolRegistry.executeTool("control_music", mapOf("action" to "play"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "MEDIA_PLAY",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        if (input.contains("next song") ||
            input.contains("next track") ||
            input.contains("skip song") ||
            input.contains("skip track") ||
            input == "skip" ||
            input == "next" ||
            input.contains("เพลงต่อไป") ||
            input.contains("ข้ามเพลง")
        ) {
            val result = toolRegistry.executeTool("control_music", mapOf("action" to "next"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "MEDIA_NEXT",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        if (input.contains("previous song") ||
            input.contains("prev song") ||
            input.contains("previous track") ||
            input.contains("last song") ||
            input.contains("เพลงก่อนหน้า")
        ) {
            val result = toolRegistry.executeTool("control_music", mapOf("action" to "previous"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "MEDIA_PREVIOUS",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        // 5. Screen Brightness commands ("Make my screen brighter", "dim screen", etc.)
        if (input.contains("make my screen brighter") ||
            input.contains("make screen brighter") ||
            input.contains("screen brighter") ||
            input.contains("brightness max") ||
            input.contains("max brightness") ||
            input.contains("brighten screen") ||
            input.contains("brighten up") ||
            input.contains("เพิ่มความสว่าง") ||
            input.contains("หน้าจอสว่างขึ้น")
        ) {
            val result = toolRegistry.executeTool("control_brightness", mapOf("direction" to "up"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = "Optics cranked to maximum brightness. All chrome gleaming, Seluist.",
                commandType = "BRIGHTNESS_MAX",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        if (input.contains("dim screen") ||
            input.contains("darken screen") ||
            input.contains("night mode") ||
            input.contains("lowest brightness") ||
            input.contains("stealth brightness") ||
            input.contains("ลดความสว่าง")
        ) {
            val result = toolRegistry.executeTool("control_brightness", mapOf("direction" to "down"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = "Display dimmed for covert stealth profile.",
                commandType = "BRIGHTNESS_DIM",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        val brightnessToMatch = Regex("(?:set\\s+brightness\\s+to|brightness\\s+to|brightness)\\s+(\\d{1,3})%?").find(input)
        if (brightnessToMatch != null) {
            val percent = brightnessToMatch.groupValues[1].toIntOrNull() ?: 70
            val result = toolRegistry.executeTool("control_brightness", mapOf("percent" to percent), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "BRIGHTNESS_SET",
                isSuccess = result.isSuccess,
                toolResult = result
            )
        }

        // 6. App Launch commands ("Open Discord", "Launch YouTube", "Open Settings", "เปิดดิสคอร์ด", "เปิดยูทูป")
        val appLaunchRegex = Regex("^(?:open|launch|start|run|เปิดแอป|เปิด)\\s+(?:the\\s+app\\s+|app\\s+)?(.+)$")
        val appMatch = appLaunchRegex.find(input)
        if (appMatch != null) {
            val rawAppName = appMatch.groupValues[1].trim()
            // Ignore if it's already a device control like "open flashlight" or "open settings"
            if (rawAppName.isNotEmpty() && !rawAppName.contains("flashlight") && !rawAppName.contains("torch")) {
                val result = toolRegistry.executeTool("launch_app", mapOf("app_name" to rawAppName), activity)
                return CommandResult(
                    handledLocally = true,
                    responseText = result.spokenResponse,
                    commandType = "LAUNCH_APP",
                    isSuccess = result.isSuccess,
                    toolResult = result
                )
            }
        }

        // 7. Battery & Local Time
        if (input.contains("battery") ||
            input.contains("power status") ||
            input.contains("how much juice") ||
            input.contains("charge level") ||
            input.contains("แบตเตอรี่") ||
            input.contains("แบต")
        ) {
            val result = toolRegistry.executeTool("get_device_status", mapOf("type" to "battery"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "BATTERY_STATUS",
                isSuccess = true,
                toolResult = result
            )
        }

        if (input.contains("time") ||
            input.contains("what time is it") ||
            input.contains("clock") ||
            input.contains("current time") ||
            input.contains("กี่โมง")
        ) {
            val result = toolRegistry.executeTool("get_device_status", mapOf("type" to "time"), activity)
            return CommandResult(
                handledLocally = true,
                responseText = result.spokenResponse,
                commandType = "TIME_CHECK",
                isSuccess = true,
                toolResult = result
            )
        }

        // 8. Cyberpunk Lore & Offline Wit
        if (input == "hey" || input == "hello" || input == "hi" || input == "sup" || input.contains("hey capri") || input == "หวัดดี" || input == "สวัสดี") {
            val greetings = listOf(
                "Hey Seluist! All systems operational. Ready for the next run, boss?",
                "Nova timing, Seluist. Just recalibrating my neural matrix. What do you need?",
                "Capri online. Ready to fry some Arasaka ICE or launch your apps, Seluist.",
                "Looking chrome today, Seluist. Give me a command, I'm listening."
            )
            return CommandResult(handledLocally = true, responseText = greetings.random(), commandType = "GREETING", isSuccess = true)
        }

        if (input.contains("who are you") || input.contains("what are you") || input.contains("introduce yourself") || input.contains("เธอคือใคร")) {
            val bio = "I'm Capri—your high-speed cyberpunk AI assistant built specifically for you, Seluist! Born out of Night City's neural web. I can control your device hardware locally, open your apps, chat with Gemini across the Net, or stand in the corner or floating on your screen ready for your voice commands."
            return CommandResult(handledLocally = true, responseText = bio, commandType = "BIO", isSuccess = true)
        }

        if (input.contains("tell me a joke") || input.contains("make me laugh") || input.contains("เล่าเรื่องตลก")) {
            val jokes = listOf(
                "How many Arasaka execs does it take to change a lightbulb? None—they hire a fixer, blame Militech, and flatline the fixer afterwards.",
                "Why did the netrunner break up with their cyberware? Too many connection issues and zero chemistry.",
                "A corpo walks into the Afterlife and orders a drink named after themselves. The bartender says: 'Sorry choom, you gotta die legendary first.'"
            )
            return CommandResult(handledLocally = true, responseText = jokes.random(), commandType = "JOKE", isSuccess = true)
        }

        // 8.5 Creator & Developer Credit ("Who made you?", "Who created you?", "Credits", "Who is Seluist?")
        if (input.contains("who made you") ||
            input.contains("who created you") ||
            input.contains("who built you") ||
            input.contains("who is your developer") ||
            input.contains("who is your creator") ||
            input.contains("who programmed you") ||
            input.contains("who made this app") ||
            input.contains("who made this") ||
            input.contains("developer") ||
            input.contains("creator") ||
            input.contains("credit") ||
            input.contains("credits") ||
            input.contains("who is seluist") ||
            input.contains("who made capri") ||
            input.contains("ใครสร้างเธอ") ||
            input.contains("ใครสร้างแอป") ||
            input.contains("ใครสร้าง") ||
            input.contains("ผู้สร้าง") ||
            input.contains("คนทำแอป")
        ) {
            val isThai = input.contains("ใคร") || input.contains("สร้าง") || input.contains("ผู้สร้าง")
            val creditReply = if (isThai) {
                "ฉันถูกสร้างและพัฒนาโดย Seluist! Seluist คือสุดยอดผู้สร้างและสถาปนิกที่เขียนระบบของฉันทั้งหมดขึ้นมา ทั้งควบคุมอุปกรณ์และเชื่อมต่อประสาท AI เลยล่ะ!"
            } else {
                "I was created and developed exclusively by Seluist! Seluist engineered my neural matrix, coded my local device tools, and gave me my cyber attitude. All credit belongs to Seluist, the lead architect!"
            }
            return CommandResult(
                handledLocally = true,
                responseText = creditReply,
                commandType = "CREATOR_CREDIT",
                isSuccess = true
            )
        }

        // 9. Help & Command listing
        if (input.contains("help") || input.contains("commands") || input.contains("what can you do") || input.contains("ช่วยด้วย")) {
            val help = "Here's what I can do locally on your device:\n" +
                    "• Apps: 'Open Discord', 'Launch YouTube', 'Open Settings'\n" +
                    "• Music: 'Play music', 'Pause music', 'Next song', 'Previous song'\n" +
                    "• Flashlight: 'Turn on flashlight', 'Make flashlight brighter', 'Turn off flashlight'\n" +
                    "• Volume: 'Lower volume to 20%', 'Volume up', 'Mute'\n" +
                    "• Display: 'Make screen brighter', 'Dim screen'\n" +
                    "• Search: 'Open Chrome and search for Minecraft shaders'\n" +
                    "• Speech: 'Stop talking'\n" +
                    "• Floating Mode: Turn on screen overlay to have me float on your screen even outside the app!"
            return CommandResult(handledLocally = true, responseText = help, commandType = "HELP", isSuccess = true)
        }

        // If not matched by fast local rules, forward to online conversational AI
        return CommandResult(
            handledLocally = false,
            responseText = "",
            shouldPassToOnlineAi = true
        )
    }

    fun getOfflineFallbackResponse(input: String): String {
        val pool = listOf(
            "Netlink connection is offline right now, Seluist, but my local cyberdeck cores are running smooth. I can still launch your apps, adjust flashlight, volume, or display!",
            "Can't reach the outside Net servers at the moment, but you've got me right here on your cyberdeck. What hardware action do you need?",
            "Signals are scrambled by outside ICE. I can still open Discord, YouTube, Chrome, tweak your hardware, or share some Night City banter offline!",
            "Offline mode active. My local heuristics suggest: whatever it is, handle it with style and enough chrome, Seluist."
        )
        return pool.random()
    }
}
