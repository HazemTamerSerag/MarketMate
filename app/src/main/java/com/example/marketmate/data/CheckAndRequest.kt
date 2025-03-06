package com.example.marketmate.data

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

const val FEEDBACK_PERMISSION_REQUEST_CODE = 200
// Updated permission check function
fun checkAndRequestFeedbackPermissions(context: Context): Boolean {
    // Define required permissions based on Android version
    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

    // Add storage permission for versions below Android 10
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    val missingPermissions = permissions.filter { permission ->
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    return if (missingPermissions.isNotEmpty()) {
        // Ensure context is an Activity before requesting permissions
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                missingPermissions.toTypedArray(),
                FEEDBACK_PERMISSION_REQUEST_CODE
            )
            false
        } else {
            Log.e("PermissionCheck", "Context is not an Activity. Cannot request permissions.")
            false
        }
    } else {
        true
    }
}