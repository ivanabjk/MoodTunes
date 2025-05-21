package com.example.moodtunes_v1.user_auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var authService: AuthService

    private lateinit var btnLogout: Button


    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Auth Service
        authService = AuthService(this)
        btnLogout = findViewById(R.id.btnLogout)

        // Set email dynamically (replace with actual data retrieval logic)
        val userEmail = authService.getEmail()
        binding.emailText.text = userEmail

        // Image picker
        binding.cameraIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//            startActivityForResult(intent, PICK_IMAGE_REQUEST)
            pickImageLauncher.launch(intent)
        }

        //Logout
        btnLogout.setOnClickListener {
            authService.logout()
            finish()
        }
    }
}
