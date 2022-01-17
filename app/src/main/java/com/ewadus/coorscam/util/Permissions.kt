package com.ewadus.coorscam.util

import android.app.Activity
import android.content.Context
import com.ewadus.coorscam.util.Constant.Companion.REQUEST_BACKGROUND_LOCATION_CODE_PERMISSIONS
import com.ewadus.coorscam.util.Constant.Companion.REQUEST_CAMERA_CODE_PERMISSIONS
import com.ewadus.coorscam.util.Constant.Companion.REQUEST_LOCATION_CODE_PERMISSIONS
import com.vmadalin.easypermissions.EasyPermissions

object Permissions {

    fun hasPermissionCamera(context: Context): Boolean {
        return EasyPermissions.hasPermissions(context, android.Manifest.permission.CAMERA)
    }

    fun requestCameraPermission(activity: Activity) {
        EasyPermissions.requestPermissions(
            activity,
            "This app need camera permission",
            REQUEST_CAMERA_CODE_PERMISSIONS,
            android.Manifest.permission.CAMERA
        )
    }

    fun hasPermissionLocation(context: Context): Boolean {
        return EasyPermissions.hasPermissions(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun requestLocationPermission(activity: Activity) {
        EasyPermissions.requestPermissions(
            activity,
            "This app need GPS permission",
            REQUEST_LOCATION_CODE_PERMISSIONS,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun hasPermissionCoarseLocation(context: Context): Boolean {

        return EasyPermissions.hasPermissions(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun requestCoarseLocationPermission(activity: Activity) {
        EasyPermissions.requestPermissions(
            activity, "This app need GPS permission",
            REQUEST_BACKGROUND_LOCATION_CODE_PERMISSIONS,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

}