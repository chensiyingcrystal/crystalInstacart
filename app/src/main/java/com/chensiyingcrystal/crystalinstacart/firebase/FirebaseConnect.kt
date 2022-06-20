package com.chensiyingcrystal.crystalinstacart.firebase

import android.location.Location
import com.chensiyingcrystal.crystalinstacart.user.User
import com.google.common.util.concurrent.ListenableFuture

interface FirebaseConnect {
  fun registerNewUser(user: User): ListenableFuture<FirebaseConnectResult>
  fun signIn(email: String, password: String): ListenableFuture<FirebaseConnectResult>
  fun updateLocation(location: Location,  isDriver: Boolean): ListenableFuture<Boolean>
}