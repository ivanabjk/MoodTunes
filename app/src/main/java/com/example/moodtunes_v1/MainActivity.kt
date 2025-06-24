package com.example.moodtunes_v1

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.moodtunes_v1.favorites.FavoritesFragment
import com.example.moodtunes_v1.history.HistoryFragment
import com.example.moodtunes_v1.home.HomeFragment
import com.example.moodtunes_v1.playlist.MoodTunesDatabase
import com.example.moodtunes_v1.playlist.PlaylistLoader
import com.example.moodtunes_v1.user_auth.ProfileFragment
import com.example.moodtunes_v1.user_auth.AuthService
import com.example.moodtunes_v1.user_auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.MainScope


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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Room DB and Playlist Loader
        val db = MoodTunesDatabase.getDatabase(this)
        val playlistDao = db.playlistDao()
        PlaylistLoader.preloadPlaylists(scope, playlistDao)

        authService = AuthService(this)
        //Navigation

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.nav_home -> {
//                    replaceFragment(HomeFragment())
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


}
