package com.chensiyingcrystal.crystalinstacart.firebase

import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.chensiyingcrystal.crystalinstacart.user.User
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import javax.inject.Inject

class FirebaseConnectImpl @Inject constructor() :
  FirebaseConnect {
  private var auth: FirebaseAuth
  private val db: FirebaseDatabase

  init {
    auth = FirebaseAuth.getInstance()
    db = FirebaseDatabase.getInstance()

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
}