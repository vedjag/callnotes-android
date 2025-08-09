package com.example.callnotes.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.callnotes.telephony.OverlayService

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_OPEN -> {
                val number = intent.getStringExtra(EXTRA_NUMBER) ?: ""
                OverlayService.showPrompt(context, number, hasExisting = true)
            }
            ACTION_DISMISS -> { /* no-op */ }
        }
    }

    companion object {
        const val ACTION_OPEN = "com.example.callnotes.OPEN"
        const val ACTION_DISMISS = "com.example.callnotes.DISMISS"
        private const val EXTRA_NUMBER = "n"

        fun intentOpen(ctx: Context, number: String) =
            Intent(ctx, NotificationActionReceiver::class.java).apply {
                action = ACTION_OPEN
                putExtra(EXTRA_NUMBER, number)
            }
        fun intentDismiss(ctx: Context) =
            Intent(ctx, NotificationActionReceiver::class.java).apply { action = ACTION_DISMISS }
    }
}
