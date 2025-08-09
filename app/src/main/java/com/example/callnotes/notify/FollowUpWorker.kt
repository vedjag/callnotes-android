package com.example.callnotes.notify

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.example.callnotes.R

class FollowUpWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val number = inputData.getString(KEY_NUMBER) ?: return Result.success()
        val title = "Follow-up reminder"
        val text = inputData.getString(KEY_LABEL) ?: number
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .addAction(0, applicationContext.getString(R.string.open),
                android.app.PendingIntent.getBroadcast(
                    applicationContext, 200,
                    NotificationActionReceiver.intentOpen(applicationContext, number),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        nm.notify(number.hashCode(), notif)
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "callnotes"
        private const val KEY_NUMBER = "number"
        private const val KEY_LABEL = "label"

        fun scheduleFollowUp(ctx: Context, normalizedNumber: String, label: String, whenMillis: Long) {
            val delay = (whenMillis - System.currentTimeMillis()).coerceAtLeast(0)
            val data = Data.Builder()
                .putString(KEY_NUMBER, normalizedNumber)
                .putString(KEY_LABEL, label)
                .build()
            val req = OneTimeWorkRequestBuilder<FollowUpWorker>()
                .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("followup_$normalizedNumber")
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniqueWork("followup_$normalizedNumber", ExistingWorkPolicy.REPLACE, req)
        }
    }
}
