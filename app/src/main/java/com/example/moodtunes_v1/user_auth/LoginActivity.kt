package com.example.moodtunes_v1.user_auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.moodtunes_v1.MainActivity
import com.example.moodtunes_v1.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var showPasswordCheckBox: CheckBox
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView

    private lateinit var authService: AuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authService = AuthService(this)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        showPasswordCheckBox = findViewById(R.id.showPasswordCheckBox)
        loginButton = findViewById(R.id.loginButton)
        registerTextView = findViewById(R.id.registerTextView)

        // Get email from intent extras (passed from Registration page)
        val emailFromRegister = intent.getStringExtra("email")
        if (!emailFromRegister.isNullOrEmpty()) {
            emailEditText.setText(emailFromRegister)
        }

        showPasswordCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                passwordEditText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Keep cursor at end
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (!validateInputs(email, password)) return@setOnClickListener

            lifecycleScope.launch(Dispatchers.IO) { // Avoid blocking main thread
                authService.login(email, password) { result ->
                    lifecycleScope.launch(Dispatchers.Main) { // Ensure safe UI updates
                        handleLoginResult(result)
                    }
                }
            }
        }

        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let {
            val emailFromRegister = it.getStringExtra("email")
            if (!emailFromRegister.isNullOrEmpty()) {
                emailEditText.setText(emailFromRegister)
            }
        }
    }
    private fun handleLoginResult(result: String?) {
        if (result == "Success") {
            showToast("Logged in successfully")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Prevent user from navigating back to login
        } else {
            showToast(result ?: "Login failed")
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isEmpty() || password.isEmpty() -> {
                showToast("Please fill all fields")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showToast("Email not valid!")
                false
            }
            password.length < 6 -> {
                showToast("Password should be at least 6 characters")
                false
            }
            else -> true
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
