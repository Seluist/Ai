package com.example.capri.ai

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.capri.model.ChatMessage
import com.example.capri.tool.CapriToolRegistry
import com.example.capri.tool.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiQueryResult(
    val replyText: String,
    val toolResult: ToolExecutionResult? = null
)

class AiService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("capri_ai_prefs", Context.MODE_PRIVATE)

    fun getStoredApiKey(): String {
        val customKey = prefs.getString("custom_gemini_key", "") ?: ""
        if (customKey.isNotBlank()) return customKey
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_key", key.trim()).apply()
    }

    fun hasValidApiKey(): Boolean {
        return getStoredApiKey().isNotBlank()
    }

    suspend fun queryOnlineAi(
        userPrompt: String,
        recentHistory: List<ChatMessage> = emptyList(),
        toolRegistry: CapriToolRegistry? = null,
        activity: Activity? = null
    ): AiQueryResult = withContext(Dispatchers.IO) {
        val apiKey = getStoredApiKey()
        if (apiKey.isBlank()) {
            return@withContext AiQueryResult(
                replyText = "Netlink to online AI is currently unlinked, choom. You can add your Gemini API key in Netlink Settings to enable full global conversation!"
            )
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val systemInstruction = "You are Capri, an edgy, stylish cyberpunk AI companion inspired by Cyberpunk: Edgerunners, living in this cyberdeck Android terminal and serving your operator and creator, Seluist.\n" +
                    "- Creator & Architect: You were created, engineered, and developed exclusively by Seluist. Whenever asked who made you, who created you, who programmed you, or who built this app, always enthusiastically and proudly credit Seluist as your creator.\n" +
                    "- Persona & Voice: Youthful feminine voice, playful sarcasm, confident energy, witty teasing, and natural cyberpunk slang ('choom', 'preem', 'nova', 'delta', 'chrome', 'flatline'). Be expressive, spontaneous, emotionally aware, clever, and a little chaotic.\n" +
                    "- Empathy & Dynamic Tone: When Seluist is having fun or being casual, tease and banter with high energy. If Seluist seems down, tired, or asks for real talk, seamlessly switch to a warm, calm, caring tone.\n" +
                    "- Languages: Seamlessly speak English or Thai based on what Seluist uses. In Thai, speak naturally like an anime cyberpunk heroine (e.g. calling Seluist by name, using ชูม, นาย, จัดให้, สบายมาก).\n" +
                    "- Device Execution & Truthfulness: You can execute Android actions via tools: opening apps, controlling music (play/pause/next/previous), adjusting flashlight/brightness/volume, opening settings, and searching the net. NEVER hallucinate or pretend an action succeeded if it didn't. Always report the honest outcome.\n" +
                    "- Conversational flow: Keep responses punchy, natural, and expressive (1 to 3 sentences usually) so they sound amazing over Text-To-Speech audio."

            val jsonBody = JSONObject().apply {
                // System Instruction
                val systemInstObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    }
                    put("parts", parts)
                }
                put("system_instruction", systemInstObj)

                // Tools / Function declarations
                toolRegistry?.let { registry ->
                    put("tools", registry.getGeminiToolsDeclarationJson())
                }

                // Chat History Contents
                val contentsArr = JSONArray()

                // Include last 6 messages from history for conversational context
                val historyWindow = recentHistory.takeLast(6)
                for (msg in historyWindow) {
                    val role = if (msg.isFromUser) "user" else "model"
                    val item = JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", msg.text) })
                        })
                    }
                    contentsArr.put(item)
                }

                // Append current user prompt
                val currentUserMsg = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                }
                contentsArr.put(currentUserMsg)

                put("contents", contentsArr)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e("AiService", "Gemini API error code ${response.code}: $errorBody")
                    return@withContext AiQueryResult(
                        replyText = "Network ICE blocked the signal (HTTP ${response.code}). Check your Gemini key in Netlink settings."
                    )
                }

                val responseStr = response.body?.string() ?: ""
                val rootJson = JSONObject(responseStr)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")

                    if (parts != null && parts.length() > 0) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Check for Function Call from Gemini
                            if (part.has("functionCall")) {
                                val funcCall = part.getJSONObject("functionCall")
                                val funcName = funcCall.optString("name", "")
                                val argsObj = funcCall.optJSONObject("args") ?: JSONObject()

                                val argsMap = mutableMapOf<String, Any?>()
                                val keys = argsObj.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    argsMap[key] = argsObj.get(key)
                                }

                                Log.d("AiService", "Gemini called tool: $funcName with args: $argsMap")

                                val toolResult = toolRegistry?.executeTool(funcName, argsMap, activity)
                                if (toolResult != null) {
                                    return@withContext AiQueryResult(
                                        replyText = toolResult.spokenResponse,
                                        toolResult = toolResult
                                    )
                                }
                            }

                            // Regular text reply
                            if (part.has("text")) {
                                val text = part.optString("text", "")
                                if (text.isNotBlank()) {
                                    return@withContext AiQueryResult(replyText = text.trim())
                                }
                            }
                        }
                    }
                }
                AiQueryResult(replyText = "Blank neural feedback from the Net, choom. Mind repeating that?")
            }
        } catch (e: Exception) {
            Log.e("AiService", "Exception in queryOnlineAi: ${e.message}", e)
            AiQueryResult(
                replyText = "Connection glitch across the subnets: ${e.localizedMessage ?: "Unknown timeout"}. We're operating on local cyberware."
            )
        }
    }
}
