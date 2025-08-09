package com.example.callnotes.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactsUtil {
    fun isUnknownNumber(context: Context, number: String): Boolean {
        if (number.isBlank()) return true
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        ).use { c ->
            return !(c != null && c.moveToFirst())
        }
    }
}
