package com.example.lumanotifier

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

object BluetoothLink {
    var send: ((String) -> Unit)? = null
}

class NotificationForwarderService : NotificationListenerService() {

    private val tag = "Notifier"

    override fun onListenerConnected() {
        Log.d(tag, "Notification listener connected.")
        super.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        Log.w(tag, "Listener disconnected. Scheduling rebind.")
        try {
            requestRebind(ComponentName(this, NotificationForwarderService::class.java))
        } catch (e: Exception) {
            Log.e(tag, "Rebind failed", e)
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName

            // Filter by user-selected apps
            val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
            val allowed = prefs.getStringSet("allowed_apps", emptySet()) ?: emptySet()
            if (allowed.isNotEmpty() && !allowed.contains(pkg)) return

            // Extract notification content
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: "(no title)"
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val lines = extras.getCharSequenceArray("android.textLines")

            val fullText = when {
                lines != null && lines.isNotEmpty() -> lines.joinToString("\n") { it.toString() }
                text.isNotEmpty() -> text
                else -> "(no text)"
            }

            // Format and send
            val msg = "[$pkg]\n$title\n$fullText"
            Log.d(tag, "Forwarding: $msg")

            BluetoothLink.send?.invoke(msg)
        } catch (e: Exception) {
            Log.e(tag, "Error handling notification", e)
        }
    }
}
