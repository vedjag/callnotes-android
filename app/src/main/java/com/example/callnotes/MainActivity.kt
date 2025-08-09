package com.example.callnotes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.callnotes.util.Permissions

class MainActivity : AppCompatActivity() {
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val overlayLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request runtime permissions
        permLauncher.launch(Permissions.RUNTIME)
        // Overlay permission (needed for in‑call popup)
        Permissions.ensureOverlayPermission(this, overlayLauncher)

        // Ask user to grant Call Screening role (incoming support)
        val roleMgr = getSystemService(android.app.role.RoleManager::class.java)
        if (roleMgr != null &&
            roleMgr.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING) &&
            !roleMgr.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
            val i = roleMgr.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
            startActivity(i)
            Toast.makeText(this, "Please grant Call Screening to enable incoming popups", Toast.LENGTH_LONG).show()
        }
    }
}
