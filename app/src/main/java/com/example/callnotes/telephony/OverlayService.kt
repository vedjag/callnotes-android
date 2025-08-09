package com.example.callnotes.telephony

import android.app.DatePickerDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat
import com.example.callnotes.R
import com.example.callnotes.data.AppDatabase
import com.example.callnotes.data.CallNote
import com.example.callnotes.notify.NotificationActionReceiver
import com.example.callnotes.util.PhoneNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var viewPrompt: View? = null
    private var viewForm: View? = null
    private var selectedWhenMillis: Long? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createChannel()
        startForeground(42, buildNotification("Ready", null))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val number = intent?.getStringExtra(EXTRA_NUMBER) ?: return START_NOT_STICKY
        val hasExisting = intent.getBooleanExtra(EXTRA_HAS_EXISTING, false)
        showPrompt(number, hasExisting)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeViews()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun windowParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 16; y = 150 }

    private fun showPrompt(number: String, hasExisting: Boolean) {
        removeViews()
        val inflater = LayoutInflater.from(this)
        viewPrompt = inflater.inflate(R.layout.overlay_prompt, null)
        val root = viewPrompt!!
        root.findViewById<TextView>(R.id.tvNumber).text = number
        val btnYes = root.findViewById<Button>(R.id.btnYes)
        val btnNo = root.findViewById<Button>(R.id.btnNo)
        val btnOpen = root.findViewById<Button>(R.id.btnOpen)

        btnOpen.visibility = if (hasExisting) View.VISIBLE else View.GONE
        btnOpen.setOnClickListener { showForm(number) }
        btnYes.setOnClickListener { showForm(number) }
        btnNo.setOnClickListener { stopSelf() }

        wm.addView(root, windowParams())
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(42, buildNotification("Tap to open form", number))
    }

    private fun showForm(number: String) {
        val inflater = LayoutInflater.from(this)
        viewForm = inflater.inflate(R.layout.overlay_form, null)
        val root = viewForm!!
        val etCompany = root.findViewById<EditText>(R.id.etCompany)
        val etSubject = root.findViewById<EditText>(R.id.etSubject)
        val etRole = root.findViewById<EditText>(R.id.etRole)
        val etSalary = root.findViewById<EditText>(R.id.etSalary)
        val etNotice = root.findViewById<EditText>(R.id.etNotice)
        val etDesc = root.findViewById<EditText>(R.id.etDescription)
        val swFollow = root.findViewById<Switch>(R.id.swFollow)
        val btnPickWhen = root.findViewById<Button>(R.id.btnPickWhen)
        val tvWhen = root.findViewById<TextView>(R.id.tvWhenValue)
        val btnSave = root.findViewById<Button>(R.id.btnSave)
        val btnCancel = root.findViewById<Button>(R.id.btnCancel)

        btnPickWhen.setOnClickListener {
            pickDateTime { millis ->
                selectedWhenMillis = millis
                tvWhen.text = if (millis != null) java.text.DateFormat.getDateTimeInstance().format(java.util.Date(millis)) else getString(R.string.not_set)
            }
        }

        btnCancel.setOnClickListener { removeViews(); stopSelf() }
        btnSave.setOnClickListener {
            scope.launch {
                val n = PhoneNormalizer.normalize(number)
                val whenMillis = if (swFollow.isChecked) selectedWhenMillis else null
                val note = CallNote(
                    rawNumber = number,
                    normalizedNumber = n,
                    company = etCompany.text.toString().ifBlank { null },
                    subject = etSubject.text.toString().ifBlank { null },
                    role = etRole.text.toString().ifBlank { null },
                    salary = etSalary.text.toString().ifBlank { null },
                    noticePeriod = etNotice.text.toString().ifBlank { null },
                    description = etDesc.text.toString().ifBlank { null },
                    followUpNeeded = swFollow.isChecked,
                    followUpAtMillis = whenMillis
                )
                AppDatabase.get(this@OverlayService).dao().upsert(note)
                if (note.followUpNeeded && whenMillis != null && whenMillis > System.currentTimeMillis()) {
                    com.example.callnotes.notify.FollowUpWorker.scheduleFollowUp(
                        this@OverlayService,
                        n,
                        note.company ?: note.rawNumber,
                        whenMillis
                    )
                }
                stopSelf()
            }
        }

        // Replace prompt with form
        viewPrompt?.let { wm.removeView(it) }
        viewPrompt = null
        wm.addView(root, windowParams())
    }

    private fun removeViews() {
        runCatching { viewPrompt?.let { wm.removeView(it) } }
        runCatching { viewForm?.let { wm.removeView(it) } }
        viewPrompt = null
        viewForm = null
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(id) == null) {
            nm.createNotificationChannel(NotificationChannel(id, "CallNotes", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun buildNotification(text: String, number: String?): Notification {
        val nmPending = PendingIntent.getBroadcast(
            this, 100,
            NotificationActionReceiver.intentOpen(this, number ?: ""),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissPending = PendingIntent.getBroadcast(
            this, 101,
            NotificationActionReceiver.intentDismiss(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Call note")
            .setContentText(text)
            .addAction(0, getString(R.string.open), nmPending)
            .addAction(0, getString(R.string.dismiss), dismissPending)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "callnotes"
        private const val EXTRA_NUMBER = "extra_number"
        private const val EXTRA_HAS_EXISTING = "extra_has"

        fun showPrompt(ctx: Context, number: String, hasExisting: Boolean) {
            val i = Intent(ctx, OverlayService::class.java)
            i.putExtra(EXTRA_NUMBER, number)
            i.putExtra(EXTRA_HAS_EXISTING, hasExisting)
            ctx.startForegroundService(i)
        }
    }

    private fun pickDateTime(onPicked: (Long?) -> Unit) {
        val now = Calendar.getInstance()
        val d = DatePickerDialog(
            this,
            { _, y, m, day ->
                val t = android.app.TimePickerDialog(
                    this,
                    { _, h, min ->
                        val cal = Calendar.getInstance().apply { set(y, m, day, h, min, 0); set(Calendar.MILLISECOND, 0) }
                        onPicked(cal.timeInMillis)
                    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true
                )
                t.window?.setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                t.show()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
        )
        d.datePicker.minDate = System.currentTimeMillis() - 1000
        d.window?.setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        d.show()
    }
}
