package com.example

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.capri.service.CapriOverlayService
import com.example.capri.ui.MainScreen
import com.example.capri.viewmodel.CapriViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CapriViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // When returning to the app, stop overlay so she returns to in-app deck
        try {
            stopService(Intent(this, CapriOverlayService::class.java))
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping overlay service: ${e.message}")
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.isOverlayOnLeaveEnabled.value) {
            if (Settings.canDrawOverlays(this)) {
                try {
                    val serviceIntent = Intent(this, CapriOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error starting overlay service: ${e.message}")
                    enterPipMode()
                }
            } else {
                enterPipMode()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_START_VOICE_COMMAND", false)) {
            viewModel.startListening()
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(9, 16))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to enter PiP: ${e.message}")
            }
        }
    }
}
