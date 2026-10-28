package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.capri.ai.CapriBrain
import com.example.capri.device.DeviceControlManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Capri", appName)
  }

  @Test
  fun `capri brain processes local commands`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val deviceManager = DeviceControlManager(context)
    val ttsManager = com.example.capri.audio.TtsManager(context)
    val toolRegistry = com.example.capri.tool.CapriToolRegistry(deviceManager, ttsManager)
    val brain = CapriBrain(toolRegistry)

    val torchResult = brain.processInput("turn on flashlight")
    assertTrue(torchResult.handledLocally)
    assertEquals("TORCH_ON", torchResult.commandType)

    val volumeResult = brain.processInput("volume up")
    assertTrue(volumeResult.handledLocally)
    assertEquals("VOLUME_UP", volumeResult.commandType)

    val loreResult = brain.processInput("tell me a joke")
    assertTrue(loreResult.handledLocally)
    assertNotNull(loreResult.responseText)
  }

  @Test
  fun `capri recognizes operator Seluist`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val deviceManager = DeviceControlManager(context)
    val ttsManager = com.example.capri.audio.TtsManager(context)
    val toolRegistry = com.example.capri.tool.CapriToolRegistry(deviceManager, ttsManager)
    val brain = CapriBrain(toolRegistry)

    val greeting = brain.processInput("hey")
    assertTrue(greeting.handledLocally)
    assertTrue(greeting.responseText.contains("Seluist"))
  }
}
