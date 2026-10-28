package com.example.capri.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

object AvatarManager {

    private const val PREFS_NAME = "capri_avatar_prefs"
    private const val KEY_CUSTOM_AVATAR_PATH = "custom_avatar_path"
    private const val AVATAR_FILE_NAME = "capri_custom_avatar.png"

    private val _customAvatarBitmap = MutableStateFlow<Bitmap?>(null)
    val customAvatarBitmap: StateFlow<Bitmap?> = _customAvatarBitmap.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadStoredAvatar(context)
    }

    private fun loadStoredAvatar(context: Context) {
        try {
            val file = File(context.filesDir, AVATAR_FILE_NAME)
            if (file.exists() && file.length() > 0) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    _customAvatarBitmap.value = bitmap
                    Log.i("AvatarManager", "Loaded custom avatar from ${file.absolutePath}")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("AvatarManager", "Failed to load custom avatar: ${e.message}")
        }
        _customAvatarBitmap.value = null
    }

    fun saveCustomAvatar(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val file = File(context.filesDir, AVATAR_FILE_NAME)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                _customAvatarBitmap.value = bitmap
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_CUSTOM_AVATAR_PATH, file.absolutePath).apply()
                Log.i("AvatarManager", "Successfully saved new custom avatar (${bitmap.width}x${bitmap.height})")
                true
            } else {
                file.delete()
                false
            }
        } catch (e: Exception) {
            Log.e("AvatarManager", "Error saving custom avatar: ${e.message}")
            false
        }
    }

    fun resetToDefault(context: Context) {
        try {
            val file = File(context.filesDir, AVATAR_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_CUSTOM_AVATAR_PATH).apply()
            _customAvatarBitmap.value = null
            Log.i("AvatarManager", "Reset avatar to bundled default")
        } catch (e: Exception) {
            Log.e("AvatarManager", "Error resetting avatar: ${e.message}")
        }
    }

    fun hasCustomAvatar(context: Context): Boolean {
        val file = File(context.filesDir, AVATAR_FILE_NAME)
        return file.exists() && file.length() > 0
    }

    fun getAvatarBitmap(context: Context): Bitmap? {
        if (!isInitialized) {
            init(context)
        }
        return _customAvatarBitmap.value
    }
}
