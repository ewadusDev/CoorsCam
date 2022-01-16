package com.ewadus.coorscam.util

import android.app.Activity
import android.content.Context
import com.ewadus.coorscam.util.Constant.Companion.REQUEST_CODE_PERMISSIONS
import com.vmadalin.easypermissions.EasyPermissions

object Permissions {

    fun hasPermissionCamera(context: Context): Boolean {
        return EasyPermissions.hasPermissions(context, android.Manifest.permission.CAMERA)
    }

    fun requestCameraPermission(activity: Activity) {
        EasyPermissions.requestPermissions(
            activity,
            "This app need camera permission",
            REQUEST_CODE_PERMISSIONS,
            android.Manifest.permission.CAMERA
        )
    }
}