package com.example.moodtunes_v1

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.example.moodtunes_v1.favorites.FavoritesFragment
import com.example.moodtunes_v1.history.HistoryFragment
import com.example.moodtunes_v1.home.HomeFragment
import com.example.moodtunes_v1.playlist.MoodTunesDatabase
import com.example.moodtunes_v1.playlist.PlaylistDao
import com.example.moodtunes_v1.playlist.PlaylistLoader
import com.example.moodtunes_v1.user_auth.ProfileFragment
import com.example.moodtunes_v1.user_auth.AuthService
import com.example.moodtunes_v1.user_auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class MainActivity : AppCompatActivity() {

    private lateinit var authService: AuthService
    private val scope = MainScope()

//     Bottom nav
    private lateinit var bottomNavigationView: BottomNavigationView

    private val homeFragment = HomeFragment()
    private val favoritesFragment = FavoritesFragment()
    private val profileFragment = ProfileFragment()
    private val historyFragment = HistoryFragment()

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
//        installSplashScreen()

        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)

        // Room DB and Playlist Loader
        val db = MoodTunesDatabase.getDatabase(this)
        val playlistDao = db.playlistDao()


        authService = AuthService(this)
        //Navigation

        if (!authService.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Prevent user from returning to MainActivity without logging in
            return
        }


        val rawEmail = authService.getEmailFromFireStoreAuth()
        val userEmail = rawEmail ?: "" // Support guest mode
        scope.launch {
            PlaylistLoader.preloadPlaylists(scope, playlistDao, userEmail)

            if (authService.isLoggedIn()) {
                syncFavoritesFromFireStore(userEmail, playlistDao)
            }

            if (savedInstanceState == null) {
                replaceFragment(homeFragment) // UI shows *after* sync completes
            }
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.nav_home -> {
                    switchTo(homeFragment)
                    true
                }
                R.id.nav_history, R.id.nav_favorites, R.id.nav_profile -> {
                    if (authService.isLoggedIn()) {
                        replaceFragment(
                            when (menuItem.itemId) {
                                R.id.nav_history -> HistoryFragment()
                                R.id.nav_favorites -> FavoritesFragment()
                                else -> ProfileFragment()
                            }
                        )
                    } else {
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            replaceFragment(homeFragment)
            bottomNavigationView.selectedItemId = R.id.nav_home
        }

    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()

    }
    private fun switchTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    suspend fun syncFavoritesFromFireStore(userEmail: String, playlistDao: PlaylistDao) {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("user_favorites")
            .document(userEmail)
            .collection("favorites")
            .get()
            .await()

        val urls = snapshot.documents.mapNotNull { it.getString("url") }

        Log.d("Sync", "FireStore favorite URLs: $urls")

        playlistDao.clearAllFavorites()
        playlistDao.setFavoritesByUrls(urls)
    }
//    override fun onResume() {
//        super.onResume()
//        if (authService.isLoggedIn()) {
//            bottomNavigationView.selectedItemId = R.id.nav_home
//        }
//    }

}
