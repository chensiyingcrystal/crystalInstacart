package com.chensiyingcrystal.crystalinstacart.firebase

import android.location.Location
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.chensiyingcrystal.crystalinstacart.user.User
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseConnectImpl @Inject constructor() :
  FirebaseConnect {
  private val auth: FirebaseAuth
  private val db: FirebaseDatabase
  private val geoFireDriver : GeoFire
  private val geoFireUser: GeoFire

  init {
    auth = FirebaseAuth.getInstance()
    db = FirebaseDatabase.getInstance()
    geoFireDriver = GeoFire(db.getReference("Drivers"))
    geoFireUser  = GeoFire(db.getReference("Users"))
  }

  override fun registerNewUser(user: User): ListenableFuture<FirebaseConnectResult> {
    Log.d("FirebaseConnect", "registerNewUser")
    val users = db.getReference("Users")

    val registerFuture =
      CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<FirebaseConnectResult> ->
        auth.createUserWithEmailAndPassword(user.email, user.password).addOnSuccessListener {
          users.child(auth.currentUser!!.uid)
            .setValue(user)
            .addOnSuccessListener {
              Log.d("FirebaseConnect", "registerNewUser success")
              completer.set(FirebaseConnectResult(true, null))

            }
            .addOnFailureListener {
              Log.d("FirebaseConnect", "registerNewUser failed" + it.message)
              completer.set(FirebaseConnectResult(false, it.message))
            }
        }.addOnFailureListener {
          Log.d("FirebaseConnect", "registerNewUser" + it.message)
          completer.set(FirebaseConnectResult(false, it.message))
        }
      }
    return registerFuture
  }

  override fun signIn(email: String, password: String): ListenableFuture<FirebaseConnectResult> {
    val signInFuture =
      CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<FirebaseConnectResult> ->
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
          Log.d("FirebaseConnect", "signIn success" + it)
          completer.set(FirebaseConnectResult(true, null))
        }.addOnFailureListener {
          Log.w("FirebaseConnect", "signIn fail" + it.message)
          completer.set(FirebaseConnectResult(false, it.message))
        }
      }
    return signInFuture
  }

  override fun updateLocation(location: Location, isDriver: Boolean): ListenableFuture<Boolean> {
    Log.d("FirebaseConnect", "updateLocation")
    val geoFire = if (isDriver) { geoFireDriver } else { geoFireUser }
    val updateLocationFuture =
      CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Boolean> ->
        geoFire.setLocation(
          auth.currentUser!!.uid,
          GeoLocation(location.latitude, location.longitude),
          { key: String, error: DatabaseError ->
            if (error != null) {
              Log.w("FirebaseConnect", "updateLocation failed")
              completer.set(false)
            } else {
              Log.w("FirebaseConnect", "updateLocation success")
              completer.set(true)
            }
          }
        )
      }
    return updateLocationFuture
  }
}