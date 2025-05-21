package com.example.moodtunes_v1.user_auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.moodtunes_v1.R
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginRedirect: TextView
    private lateinit var togglePasswordButton: ImageView

    private var isPasswordVisible = false

    private lateinit var authService: AuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register) // See layout below

        authService = AuthService(this)

        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        registerButton = findViewById(R.id.registerButton)
        loginRedirect = findViewById(R.id.loginRedirect)
        togglePasswordButton = findViewById(R.id.togglePasswordButton)

        togglePasswordButton.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            passwordEditText.inputType =
                if (isPasswordVisible) android.text.InputType.TYPE_CLASS_TEXT
                else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (!isValidEmail(email)) {
                emailEditText.error = "Invalid email"
            } else if (password.length < 6) {
                passwordEditText.error = "Password must be at least 6 characters"
            } else {
                registerUser(email, password)
            }
        }

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun registerUser(email: String, password: String) {
        authService.register(email, password) { result ->
            runOnUiThread {
                if (result == "Success") {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    // Navigate to LoginActivity or main screen
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("email", email)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Registration failed: $result", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
