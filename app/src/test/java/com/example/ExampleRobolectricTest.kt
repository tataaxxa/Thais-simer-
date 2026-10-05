package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.device.AndroidActionBridge
import com.example.device.ActionResult
import com.example.device.DeviceActionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Arushi AI Assistant", appName)
  }

  @Test
  fun `test action manager search contacts finds contacts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val actionManager = DeviceActionManager(context)

    val momContacts = actionManager.searchContacts("Mummy")
    assertTrue(momContacts.isNotEmpty())

    val rahulContacts = actionManager.searchContacts("Rahul")
    assertEquals(2, rahulContacts.size)
  }

  @Test
  fun `test android action bridge is available`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val actionManager = DeviceActionManager(context)
    val bridge = AndroidActionBridge(actionManager)

    assertTrue(bridge.isBridgeAvailable())
  }
}
