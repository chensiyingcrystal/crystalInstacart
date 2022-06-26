package com.chensiyingcrystal.crystalinstacart.home

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.chensiyingcrystal.crystalinstacart.R
import com.chensiyingcrystal.crystalinstacart.databinding.ActivityHomeMapBinding
import com.chensiyingcrystal.crystalinstacart.firebase.FirebaseConnect
import com.chensiyingcrystal.crystalinstacart.location.LocationController
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.SquareCap
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeMapActivity : AppCompatActivity(), OnMapReadyCallback {
  @Inject lateinit var locationController: LocationController
  @Inject lateinit var firebaseConnect: FirebaseConnect

  private lateinit var map: GoogleMap
  private lateinit var binding: ActivityHomeMapBinding
  private var currentMarker: Marker? = null
  private var lastLocation: Location? = null
  private lateinit var locationCallback: LocationCallback
  private lateinit var polylineOptions: PolylineOptions
  private lateinit var blackPolylineOptions: PolylineOptions
  private lateinit var blackPolyline: Polyline
  private lateinit var greyPolyline: Polyline
  private var polylineList: List<LatLng> = ArrayList()
  private lateinit var destination: String
  private lateinit var startPosition: LatLng
  private lateinit var endPosition: LatLng
  private lateinit var currentPosition: LatLng

  override fun onCreate(savedInstanceState: Bundle?) {
    Log.d("HomeMapActivity", "#onCreate")
    super.onCreate(savedInstanceState)

    binding = ActivityHomeMapBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    val mapFragment = supportFragmentManager
      .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)

    locationCallback = object : LocationCallback() {
      override fun onLocationResult(locationResult: LocationResult?) {
        updateLocation()
      }
    }

    binding.btnGo.setOnClickListener {
      destination = binding.edtPlace.text.toString().replace(" ", "+")
      Log.d("HomeMapActivity", "Get new destination " + destination)
      getDirection()
    }
  }

  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  override fun onMapReady(googleMap: GoogleMap) {
    Log.d("HomeMapActivity", "#onMapReady")
    map = googleMap
    map.mapType = GoogleMap.MAP_TYPE_NORMAL
    map.setTrafficEnabled(false)
    map.setIndoorEnabled(false)
    map.setBuildingsEnabled(false)
    map.uiSettings.setZoomControlsEnabled(true)

    locationController.startLocationUpdate(locationCallback)
    updateLocation()
  }

  private fun updateLocation() {
    Futures.addCallback(
      locationController.getLatestLocation(),
      object : FutureCallback<Location> {
        override fun onSuccess(location: Location?) {
          renderMarker(location)
        }

        override fun onFailure(t: Throwable) {
          Log.w("HomeMapActivity", "Something went wrong during get latest location " + t.message)
        }

      },
      // causes the callbacks to be executed on the main (UI) thread
      this.mainExecutor)
  }

  private fun renderMarker(location: Location?) {
    Log.w("HomeMapActivity", "Render location " + location)
    if (location == null) {
      return
    }
    lastLocation = location
    currentMarker?.remove()
    val latLng = LatLng(location.latitude, location.longitude)
    currentMarker =
      map.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.car_logo))
                      .position(latLng).title("Yourself"))
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f))
    rotateMarker()
  }

  private fun rotateMarker() {
    val handler = Handler(Looper.getMainLooper())
    val start = SystemClock.uptimeMillis()
    val startRoation = currentMarker!!.rotation
    val duration = 1500
    val interpolator = LinearInterpolator()
    handler.post(object : Runnable {
      override fun run() {
        val elapsed = SystemClock.uptimeMillis() - start
        val t = interpolator.getInterpolation(elapsed.toFloat() / duration)
        val rot = t * (-360) + (1 - t) * startRoation
        val changeRot = if (-rot > 180) {
          rot / 2
        } else {
          rot
        }
        currentMarker!!.setRotation(changeRot)
        if (t < 1.0) {
          handler.postDelayed(this, 16)
        }
      }
    })
  }

  private fun getDirection() {
    if (lastLocation == null) {
      Log.d("HomeMapActivity", "Can't locate the current position")
      return
    }
    Futures.addCallback(
      locationController.getDirection(lastLocation!!, destination),
      object : FutureCallback<List<LatLng>> {
        override fun onSuccess(polylineList: List<LatLng>?) {
          if (polylineList == null) {
            Log.d("HomeMapActivity", "Can't find the polylines")
            return
          }
          Log.d("HomeMapActivity", "Polylines size " + polylineList.size)
          val latLngBoundsBuilder = LatLngBounds.builder()
          for (latLng in polylineList) {
            latLngBoundsBuilder.include(latLng)
          }
          val latLngBounds = latLngBoundsBuilder.build()
          map.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 2))
          val greyPolylineOptions =
            PolylineOptions().color(Color.GRAY).width(5.0f).startCap(SquareCap()).endCap(SquareCap()).jointType(JointType.ROUND).addAll(polylineList)
          val greyPolyline = map.addPolyline(greyPolylineOptions)

          val blackPolylineOptions =
            PolylineOptions().color(Color.BLACK).width(5.0f).startCap(SquareCap()).endCap(SquareCap()).jointType(JointType.ROUND)
          val blackPolyline = map.addPolyline(blackPolylineOptions)

          map.addMarker(MarkerOptions().position(polylineList.get(polylineList.size-1)).title("Pickup Location"))

        }

        override fun onFailure(t: Throwable) {
        }

      },
      // causes the callbacks to be executed on the main (UI) thread
      this.mainExecutor)
  }
}