package com.chensiyingcrystal.crystalinstacart.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.chensiyingcrystal.crystalinstacart.firebase.FirebaseConnectResult
import com.chensiyingcrystal.crystalinstacart.permission.PermissionChecker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationControllerImpl @Inject constructor(
  private val permissionChecker: PermissionChecker,
  private @ApplicationContext val context: Context,
) : LocationController {
  private var locationRequest: LocationRequest
  private val fusedLocationClient: FusedLocationProviderClient

  init {
    locationRequest = LocationRequest()
      .setInterval(5000)
      .setFastestInterval(3000).setSmallestDisplacement(10.0f)
      .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
  }

  override fun startLocationUpdate(locationCallback: LocationCallback) {
    Log.d("LocationController","startLocationUpdate")

    if (!checkLocationPermissions()) {
      Log.w("LocationController","No location permission")
      return
    }
    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
    null)
  }

  override fun stopLocationUpdate(locationCallback: LocationCallback) {
    Log.d("LocationController","stopLocationUpdate")

    if (!checkLocationPermissions()) {
      Log.w("LocationController","No location permission")
      return
    }
    fusedLocationClient.removeLocationUpdates(locationCallback)
  }

  override fun getLatestLocation(): ListenableFuture<Location>  {
    Log.d("LocationController","getLatestLocation")
    if (!checkLocationPermissions()) {
      Log.w("LocationController","No location permission")
      return Futures.immediateFailedFuture(IllegalAccessException("No location permission"))
    }
    val getLatestLocationFuture =
      CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Location> ->
        fusedLocationClient.lastLocation.addOnSuccessListener {
          completer.set(it)
        }.addOnFailureListener {
          completer.setException(it)
        }
      }
    return getLatestLocationFuture
  }

  private fun checkLocationPermissions(): Boolean {
    return permissionChecker.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
      permissionChecker.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
  }
}