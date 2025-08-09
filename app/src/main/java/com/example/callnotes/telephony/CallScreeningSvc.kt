package com.example.callnotes.telephony

import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.callnotes.data.AppDatabase
import com.example.callnotes.util.PhoneNormalizer
import com.example.callnotes.util.ContactsUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallScreeningSvc : CallScreeningService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle // tel: URI
        val number = handle?.schemeSpecificPart ?: return
        val n = PhoneNormalizer.normalize(number)
        val ctx = this

        scope.launch {
            val dao = AppDatabase.get(ctx).dao()
            val existing = dao.getByNumber(n)
            val isUnknown = ContactsUtil.isUnknownNumber(ctx, n)

            // Only prompt if incoming and unknown, or if we already have a note for this number
            val incoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING
            if ((incoming && isUnknown) || existing != null) {
                OverlayService.showPrompt(ctx, number, existing != null)
            }
        }

        // We are not blocking or silencing the call; just allow it.
        respondToCall(callDetails, CallResponse.Builder().build())
    }
}
