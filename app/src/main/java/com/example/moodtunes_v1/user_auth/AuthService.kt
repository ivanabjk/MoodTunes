package com.example.moodtunes_v1.user_auth

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.moodtunes_v1.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class AuthService(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val sharedPref = SharedPref(context)

    fun register(email: String, password: String, onResult: (String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sharedPref.setEmail(email)
                    onResult("Success")
                    context.startActivity(Intent(context, LoginActivity::class.java))
                } else {
                    val message = when (val exception = task.exception) {
                        is FirebaseAuthWeakPasswordException -> "The password provided is too weak."
                        is FirebaseAuthUserCollisionException -> "The account already exists for that email."
                        else -> exception?.message
                    }
                    onResult(message)
                }
            }
    }

    fun login(email: String, password: String, onResult: (String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sharedPref.setEmail(email)
                    sharedPref.setLogged(true)

                    Log.d("AUTH", "User logged in: ${sharedPref.getEmail()} | Logged Status: ${sharedPref.isLogged()}")

                    onResult("Success")
                    context.startActivity(Intent(context, MainActivity::class.java))
                } else {
                    val message = when (val exception = task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Invalid login credentials."
                        else -> exception?.message
                    }
                    onResult(message)
                }
            }
    }

    fun logout() {
        auth.signOut()
        sharedPref.setLogged(false)
        sharedPref.setEmail(null)
        context.startActivity(Intent(context, MainActivity::class.java))
    }

    fun getEmail(): String? {
        return sharedPref.getEmail()
    }
}
