package com.chensiyingcrystal.crystalinstacart.login

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chensiyingcrystal.crystalinstacart.R
import com.chensiyingcrystal.crystalinstacart.databinding.LoginFragmentBinding
import com.chensiyingcrystal.crystalinstacart.firebase.FirebaseConnect
import com.chensiyingcrystal.crystalinstacart.firebase.FirebaseConnectResult
import com.chensiyingcrystal.crystalinstacart.home.HomeMapActivity
import com.chensiyingcrystal.crystalinstacart.user.User
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.rengwuxian.materialedittext.MaterialEditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The login/register Fragment
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

  @Inject lateinit var firebaseConnect: FirebaseConnect
  private var _binding: LoginFragmentBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {

    _binding = LoginFragmentBinding.inflate(inflater, container, false)


    return binding.root

  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.btnRegister.setOnClickListener {
      Log.i("LoginFragment", "Register onClick")
      showRegisterDialog()
    }

    binding.btnSignIn.setOnClickListener {
      Log.i("LoginFragment", "SignIn onClick")
      showLoginDialog()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun showRegisterDialog() {
    var dialog = AlertDialog.Builder(context);
    dialog.setTitle("Register")
    dialog.setMessage("Please use Email to register")

    val registerView = LayoutInflater.from(context).inflate(R.layout.layout_register, null)
    val editEmail = registerView.findViewById<MaterialEditText>(R.id.editEmail)
    val editPassword = registerView.findViewById<MaterialEditText>(R.id.editPassword)
    val editName = registerView.findViewById<MaterialEditText>(R.id.editName)
    val editPhone = registerView.findViewById<MaterialEditText>(R.id.editPhone)

    dialog.setPositiveButton("REGISTER") { dialogInterface: DialogInterface, i: Int ->
      val email = editEmail.text.toString()
      val password = editPassword.text.toString()
      val name = editName.text.toString()
      val phone = editPhone.text.toString()
      dialogInterface.dismiss()

      if (TextUtils.isEmpty(email)) {
        Snackbar.make(binding.loginFragment, "Please enter email address", Snackbar.LENGTH_SHORT)
          .show()
        return@setPositiveButton
      }
      if (TextUtils.isEmpty(phone)) {
        Snackbar.make(binding.loginFragment, "Please enter phone number", Snackbar.LENGTH_SHORT)
          .show()
        return@setPositiveButton
      }
      if (TextUtils.isEmpty(password) || password.length < 6) {
        Snackbar.make(binding.loginFragment,
                      "Please enter phone number at least 6",
                      Snackbar.LENGTH_SHORT).show()
        return@setPositiveButton
      }

      val user = User(email, password, name, phone)

      Futures.addCallback(
        firebaseConnect.registerNewUser(user),
        object : FutureCallback<FirebaseConnectResult> {
          override fun onSuccess(result: FirebaseConnectResult?) {
            if (result?.connectResult == true) {
              Snackbar.make(binding.loginFragment,
                            "Register new User successfully",
                            Snackbar.LENGTH_SHORT).show()
            } else {
              Snackbar.make(binding.loginFragment,
                            "Register new User failed due to " + result?.exceptionMessage,
                            Snackbar.LENGTH_SHORT).show()
            }
          }

          override fun onFailure(t: Throwable) {
            Snackbar.make(binding.loginFragment,
                          "Register new User failed in Firebase connection",
                          Snackbar.LENGTH_SHORT).show()
          }
        },
        // causes the callbacks to be executed on the main (UI) thread
        context?.mainExecutor)
    }
    dialog.setNegativeButton("CANCEL", { dialogInterface: DialogInterface, i: Int ->
      dialogInterface.dismiss()
    })
    dialog.setView(registerView)
    dialog.show();
  }

  private fun showLoginDialog() {
    var dialog = AlertDialog.Builder(context);
    dialog.setTitle("Sign in")
    dialog.setMessage("Please use Email to sign in")

    val signinView = LayoutInflater.from(context).inflate(R.layout.layout_login, null)
    val editEmail = signinView.findViewById<MaterialEditText>(R.id.editEmail)
    val editPassword = signinView.findViewById<MaterialEditText>(R.id.editPassword)

    dialog.setPositiveButton("SIGN IN", { dialogInterface: DialogInterface, i: Int ->
      val email = editEmail.text.toString()
      val password = editPassword.text.toString()
      dialogInterface.dismiss()
      binding.btnSignIn.setEnabled(false)
      binding.btnRegister.setEnabled(false)

      if (TextUtils.isEmpty(email)) {
        Snackbar.make(binding.loginFragment, "Please enter email address", Snackbar.LENGTH_SHORT)
          .show()
        return@setPositiveButton
      }
      Futures.addCallback(
        firebaseConnect.signIn(email, password),
        object : FutureCallback<FirebaseConnectResult> {
          override fun onSuccess(result: FirebaseConnectResult?) {
            if (result?.connectResult == true) {
              Snackbar.make(binding.loginFragment,
                            "SignIn successfully",
                            Snackbar.LENGTH_SHORT).show()
              // findNavController().navigate(R.id.action_LoginFragment_to_HomeFragment)
              startActivity(Intent(context, HomeMapActivity::class.java))
              activity!!.finish()
            } else {
              Snackbar.make(binding.loginFragment,
                            "SignIn failed due to " + result?.exceptionMessage,
                            Snackbar.LENGTH_SHORT).show()
              var err = result?.exceptionMessage
              if (err != null) {
                Log.e("error message", err)
              }

              binding.btnSignIn.setEnabled(true)
              binding.btnRegister.setEnabled(true)
            }
          }

          override fun onFailure(t: Throwable) {
            Snackbar.make(binding.loginFragment,
                          "SignIn failed in Firebase connection",
                          Snackbar.LENGTH_SHORT).show()
            binding.btnSignIn.setEnabled(true)
            binding.btnRegister.setEnabled(true)
          }
        },
        // causes the callbacks to be executed on the main (UI) thread
        context?.mainExecutor)
    })
    dialog.setNegativeButton("CANCEL", { dialogInterface: DialogInterface, i: Int ->
      dialogInterface.dismiss()
    })
    dialog.setView(signinView)
    dialog.show();
  }
}