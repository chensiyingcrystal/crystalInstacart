package com.chensiyingcrystal.crystalinstacart.location

import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.maps.model.LatLng
import com.google.common.util.concurrent.ListenableFuture

interface LocationController {
  fun startLocationUpdate(locationCallback: LocationCallback)
  fun stopLocationUpdate(locationCallback: LocationCallback)
  fun getLatestLocation(): ListenableFuture<Location>
  fun getDirection(currentLocation: Location, destination: String): ListenableFuture<List<LatLng>>
}