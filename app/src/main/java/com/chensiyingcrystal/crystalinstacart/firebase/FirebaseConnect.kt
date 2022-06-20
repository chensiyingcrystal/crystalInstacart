package com.chensiyingcrystal.crystalinstacart.firebase

import com.chensiyingcrystal.crystalinstacart.user.User
import com.google.common.util.concurrent.ListenableFuture

interface FirebaseConnect {
  fun registerNewUser(user: User): ListenableFuture<FirebaseConnectResult>
  fun signIn(email: String, password: String): ListenableFuture<FirebaseConnectResult>
}