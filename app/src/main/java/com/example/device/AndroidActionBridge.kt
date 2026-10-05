package com.example.device

import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject

/**
 * JavaScript-to-native bridge for Android actions as specified in Section 3 of specification.
 * Exposed to WebViews as `window.AndroidBridge`.
 */
class AndroidActionBridge(
    private val actionManager: DeviceActionManager,
    private val onActionExecuted: ((String, String, Boolean) -> Unit)? = null
) {

    @JavascriptInterface
    fun isBridgeAvailable(): Boolean {
        return true
    }

    @JavascriptInterface
    fun openWhatsApp(): String {
        val result = actionManager.openWhatsApp()
        val json = JSONObject().apply {
            put("action", "openWhatsApp")
            put("success", result.success)
            put("message", result.message)
        }
        onActionExecuted?.invoke("openWhatsApp", result.message, result.success)
        return json.toString()
    }

    @JavascriptInterface
    fun openApp(appName: String): String {
        val result = actionManager.openApp(appName)
        val json = JSONObject().apply {
            put("action", "openApp")
            put("appName", appName)
            put("success", result.success)
            put("message", result.message)
        }
        onActionExecuted?.invoke("openApp($appName)", result.message, result.success)
        return json.toString()
    }

    @JavascriptInterface
    fun makeCall(phoneNumber: String): String {
        val result = actionManager.makeCall(phoneNumber)
        val json = JSONObject().apply {
            put("action", "makeCall")
            put("phoneNumber", phoneNumber)
            put("success", result.success)
            put("message", result.message)
        }
        onActionExecuted?.invoke("makeCall($phoneNumber)", result.message, result.success)
        return json.toString()
    }

    @JavascriptInterface
    fun callContact(contactName: String): String {
        val result = actionManager.callContact(contactName)
        val json = JSONObject().apply {
            put("action", "callContact")
            put("contactName", contactName)
            when (result) {
                is ActionResult.Success -> {
                    put("success", true)
                    put("status", "calling")
                    put("message", result.message)
                }
                is ActionResult.MultipleContacts -> {
                    put("success", false)
                    put("status", "multiple_found")
                    put("message", result.message)
                    val contactsArray = JSONArray()
                    for (c in result.contacts) {
                        val cJson = JSONObject().apply {
                            put("name", c.name)
                            put("phoneNumber", c.phoneNumber)
                            put("type", c.type)
                        }
                        contactsArray.put(cJson)
                    }
                    put("contacts", contactsArray)
                }
                is ActionResult.ContactNotFound -> {
                    put("success", false)
                    put("status", "not_found")
                    put("message", result.message)
                }
                is ActionResult.Failure -> {
                    put("success", false)
                    put("status", "failure")
                    put("message", result.message)
                }
            }
        }
        onActionExecuted?.invoke("callContact($contactName)", result.message, result.success)
        return json.toString()
    }

    @JavascriptInterface
    fun openUrl(url: String): String {
        val result = actionManager.openUrl(url)
        val json = JSONObject().apply {
            put("action", "openUrl")
            put("url", url)
            put("success", result.success)
            put("message", result.message)
        }
        onActionExecuted?.invoke("openUrl($url)", result.message, result.success)
        return json.toString()
    }
}
