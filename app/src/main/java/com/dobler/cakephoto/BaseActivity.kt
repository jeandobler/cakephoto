package com.dobler.cakephoto

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


abstract class BaseActivity : AppCompatActivity() {

    val RequestCodePermission = 10
    val RequiredPermissions = arrayOf(Manifest.permission.CAMERA)

    internal fun allPermissionsGranted() = RequiredPermissions.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == RequestCodePermission) {
            if (!allPermissionsGranted()) {
                finish()
            }
        }
    }

    fun initCameraPermission() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, RequiredPermissions, RequestCodePermission
            )
        }
    }

    abstract fun startCamera()
}
