package com.chensiyingcrystal.crystalinstacart.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.chensiyingcrystal.crystalinstacart.R
import com.chensiyingcrystal.crystalinstacart.firebase.FirebaseConnectResult
import com.chensiyingcrystal.crystalinstacart.permission.PermissionChecker
import com.chensiyingcrystal.crystalinstacart.retrofit.RetrofitClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class LocationControllerImpl @Inject constructor(
  private val permissionChecker: PermissionChecker,
  private @ApplicationContext val context: Context,
  private val retrofitClient: RetrofitClient,
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

  override fun getDirection(currentLocation: Location, destination: String): ListenableFuture<List<LatLng>> {
    val requestApi = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
      currentLocation.latitude + "," + currentLocation.longitude + "&&destination=" +
      destination + "&key=" + context.resources.getString(R.string.google_direction_api)
    Log.d("LocationController", "Get Direction " + requestApi)
    val getDirectionFuture =
      CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<List<LatLng>> ->
        retrofitClient.getGoogleApi().getPath(requestApi)
          .enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
              Log.d("LocationController", "Get Direction Succeed");
              val jsonObject = JSONObject(response.body().toString())
              val jsonArray = jsonObject.getJSONArray("routes")
              Log.d("LocationController", "Get Direction Succeed" + jsonObject);
              var polyLineList = ArrayList<LatLng>()
              for (i in 0 until jsonArray.length()) {
                val route = jsonArray.getJSONObject(i)
                val poly = route.getJSONObject("overview_polyline")
                val polyline = poly.getString("points")
                polyLineList = decodePoly(polyline)
                Log.d("LocationController", "Get PolyLineList %s" + polyLineList.size);
              }
              completer.set(polyLineList)
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
              Log.e("LocationController", "Get Direction Fail", t);
              completer.setException(t)
            }
          })
      }
    return getDirectionFuture
  }

  private fun checkLocationPermissions(): Boolean {
    return permissionChecker.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
      permissionChecker.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
  }

  private fun decodePoly(polyline: String): ArrayList<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len: Int = polyline.length
    var lat = 0
    var lng = 0

    while (index < len) {
      var b: Int
      var shift = 0
      var result = 0
      do {
        b = polyline.get(index++).code - 63
        result = result or (b and 0x1f shl shift)
        shift += 5
      } while (b >= 0x20)
      val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
      lat += dlat
      shift = 0
      result = 0
      do {
        b = polyline.get(index++).code - 63
        result = result or (b and 0x1f shl shift)
        shift += 5
      } while (b >= 0x20)
      val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
      lng += dlng
      val p = LatLng(lat.toDouble() / 1E5,
                     lng.toDouble() / 1E5)
      poly.add(p)
    }
    return poly
  }
}