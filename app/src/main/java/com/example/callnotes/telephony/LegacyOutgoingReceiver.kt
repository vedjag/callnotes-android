package com.example.callnotes.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LegacyOutgoingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.extras?.getString(Intent.EXTRA_PHONE_NUMBER) ?: return
        OverlayService.showPrompt(context, number, hasExisting = true)
    }
}
