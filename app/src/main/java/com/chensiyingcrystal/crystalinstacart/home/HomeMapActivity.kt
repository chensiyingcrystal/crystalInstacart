package com.chensiyingcrystal.crystalinstacart.home

import android.animation.ValueAnimator
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
import com.google.android.gms.maps.model.CameraPosition
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
  private lateinit var destination: String
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
      map.addMarker(MarkerOptions()
                      .position(latLng).title("Yourself"))
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f))
  }

  private fun getDirection() {
    if (lastLocation == null) {
      Log.d("HomeMapActivity", "Can't locate the current position")
      return
    }
    currentPosition = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
    Futures.addCallback(
      locationController.getDirection(lastLocation!!, destination),
      object : FutureCallback<List<LatLng>> {
        override fun onSuccess(polylineList: List<LatLng>?) {
          if (polylineList == null) {
            Log.d("HomeMapActivity", "Can't find the polylines")
            return
          }
          drawRoute(polylineList)
          drawMovingCar(polylineList)
        }

        override fun onFailure(t: Throwable) {
        }

      },
      // causes the callbacks to be executed on the main (UI) thread
      this.mainExecutor)
  }

  private fun drawRoute(polylineList: List<LatLng>) {
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

    val pickupLatLng = polylineList.get(polylineList.size-1)
    map.addMarker(MarkerOptions().position(pickupLatLng).title("Pickup Location"))

    // Animation
    val polyLineAnimator = ValueAnimator.ofInt(0, 100)
    polyLineAnimator.duration = 2000
    polyLineAnimator.interpolator = LinearInterpolator()
    polyLineAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
      override fun onAnimationUpdate(animation: ValueAnimator) {
        val points = greyPolyline.points
        val percentValue = animation.animatedValue.toString().toInt()
        val size = points.size
        val newPointSize = (size * percentValue/100.0f).toInt()
        val p = points.subList(0, newPointSize)
        blackPolyline.points = p
      }
    })
    polyLineAnimator.start()

  }

  private fun drawMovingCar(polylineList: List<LatLng>) {
    currentMarker = map.addMarker(MarkerOptions().position(currentPosition).flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.car_logo)) )

    val handler = Handler(Looper.getMainLooper())
    var index = -1
    var next = 0
    val polylineSize = polylineList.size
    var startPosition: LatLng? = null
    var endPosition: LatLng? = null
    handler.postDelayed(object : Runnable {
      override fun run() {
        Log.d("HomeMapActivity", "drawMovingCar")

        if (index < polylineSize -1) {
          index++
          next = index+1
        }
        if (index < polylineSize -1) {
          startPosition = polylineList.get(index)
          endPosition = polylineList.get(next)
        }
        var valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 3000
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.addUpdateListener { animation ->
          val v = animation.animatedFraction
          Log.d("HomeMapActivity", "drawMovingCar animation " + v)
          val lng = v* endPosition!!.longitude + (1-v)*startPosition!!.longitude
          val lat = v*endPosition!!.latitude + (1-v)*startPosition!!.latitude
          val newPos = LatLng(lat, lng)
          currentMarker!!.position = newPos
          currentMarker!!.setAnchor(0.5f, 0.5f)
          currentMarker!!.rotation = bearing(startPosition!!, endPosition!!)
          map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder()
                                                                 .target(newPos)
                                                                 .zoom(15f)
                                                                 .build()))

        }
        valueAnimator.start()
        handler.postDelayed(this, 3000)
      }
    }, 3000)

  }

  private fun bearing(startPosition: LatLng, endPosition: LatLng): Float {
    val lat = Math.abs(startPosition.latitude - endPosition.latitude)
    val lng = Math.abs(startPosition.longitude - endPosition.longitude)
    if (startPosition.latitude < endPosition.latitude && startPosition.longitude < endPosition.longitude) {
      return Math.toDegrees(Math.atan(lng/lat)).toFloat()
    }
    if (startPosition.latitude >= endPosition.latitude && startPosition.longitude < endPosition.longitude) {
      return 180 - Math.toDegrees(Math.atan(lng/lat)).toFloat()
    }
    if (startPosition.latitude < endPosition.latitude && startPosition.longitude < endPosition.longitude) {
      return Math.toDegrees(Math.atan(lng/lat)).toFloat() + 180
    }
    return 360- Math.toDegrees(Math.atan(lng/lat)).toFloat()
  }
}