package com.ewadus.coorscam.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import java.lang.ref.WeakReference

object LocationManager {

    private const val TAG = "LocationManager"
    private lateinit var activity: WeakReference<Activity>
    private lateinit var locationRequest: LocationRequest
    private lateinit var onUpdateLocation: WeakReference<(latitude: Double, longitude: Double, accuracy: Float) -> Unit>
    private var interval: Long = 2000
    private var fastestInterval: Long = 1000
    private var priority: Int = LocationRequest.PRIORITY_HIGH_ACCURACY

    private var locationCallbacks = object : LocationCallback() {
        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            super.onLocationAvailability(locationAvailability)
            Log.i(TAG, locationAvailability.isLocationAvailable.toString())

            if (locationAvailability.isLocationAvailable) {

                //if user turn off gps then go to setting
            } else {
                activity.get()?.let {
                    val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    it.startActivity(intent)
                }
            }

        }

        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            onUpdateLocation.get()?.invoke(
                locationResult.lastLocation.latitude,
                locationResult.lastLocation.longitude,
                locationResult.lastLocation.accuracy,
            )

        }

    }

    fun request(onUpdateLocation: (latitude: Double, longitude: Double, accuracy: Float) -> Unit) {
        this.onUpdateLocation = WeakReference(onUpdateLocation)
        requestWithoutService()
    }

    private fun requestWithoutService() {
        activity.get()?.let {


            if (ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                Toast.makeText(it, "need location permission", Toast.LENGTH_SHORT).show()
                return
            }
            LocationServices.getFusedLocationProviderClient(it).requestLocationUpdates(
                locationRequest, locationCallbacks, Looper.getMainLooper()
            )

        }
    }

    fun stop(activity: Activity) {
        removeCallback(activity)
    }

    private fun removeCallback(activity: Activity) {
        LocationServices.getFusedLocationProviderClient(activity).removeLocationUpdates(
            locationCallbacks
        )
    }


    object Builder {
        fun build(): Builder {
            return this
        }

        fun setInterval(interval: Long): Builder {
            LocationManager.interval = interval
            return this
        }

        fun setFastestInterval(fastestInterval: Long): Builder {
            LocationManager.fastestInterval = fastestInterval
            return this

        }

        fun setPriority(priority: Int): Builder {
            LocationManager.priority = priority
            return this

        }

        fun create(activity: Activity): LocationManager {
            LocationManager.activity = WeakReference(activity)
            locationRequest = LocationRequest.create().setInterval(interval).setFastestInterval(
                fastestInterval
            ).setPriority(priority)
            return LocationManager
        }

    }
}