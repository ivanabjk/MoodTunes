package com.example.moodtunes_v1.playlist

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object PlaylistLoader {
    fun preloadPlaylists(scope: CoroutineScope, playlistDao: PlaylistDao, userEmail: String) {

        scope.launch {
            // Check if playlists already exist to avoid duplicates
            var existingPlaylists = playlistDao.getPlaylistsByMoodForUser("Happy", userEmail)
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PL4C44E2875308A280",
                        mood = "Happy",
                        genre = "90s Pop Hits",
                        userEmail = userEmail,

                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLx2Jv96o522ORh69HaDKClrz2Midqj8AE",
                        mood = "Happy",
                        genre = "Upbeat Songs",
                        userEmail = userEmail,

                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLlLxrl-tbz-z3RqzMCOc_uQqdsbsAMGAq",
                        mood = "Happy",
                        genre = "Rock Classics",
                        userEmail = userEmail,

                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMoodForUser("Sad", userEmail)
            if (existingPlaylists.isEmpty()) {

                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLRJriok9d2H8eNwmsdh2MSYsBXFlzZU8H",
                        mood = "Sad",
                        genre = "Melancholy Tunes",
                        userEmail = userEmail,

                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLJH9QWrouDvTIqgnrB1XtwOkw4ZgQjaZq",
                        mood = "Sad",
                        genre = "Soft Acoustic",
                        userEmail = userEmail,

                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLWwVW5BHHeYVM4rAKmNOjKAXn21R6XAKV",
                        mood = "Sad",
                        genre = "Emo Rock",
                        userEmail = userEmail,

                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMoodForUser("Angry", userEmail)
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLqrHHabBzX0nY0NU5xFJ6NDYR1R-jopi0",
                        mood = "Angry",
                        genre = "Heavy Metal",
                        userEmail = userEmail,

                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLfF4wIXCvi2NJOY808YLGmcd3AABPs1__",
                        mood = "Angry",
                        genre = "Techno Rage",
                        userEmail = userEmail,

                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLYFbDTv39GlnLFu6rJeNRCAlP2dTuiP3C",
                        mood = "Angry",
                        genre = "Hardcore Punk",
                        userEmail = userEmail,

                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMoodForUser("Calm", userEmail)
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLQ_PIlf6OzqIq5aQe0uTHBmli1Nc1HTpB",
                        mood = "Calm",
                        genre = "Ambient Sounds",
                        userEmail = userEmail,

                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLOzDu-MXXLlj7croDcwz33c-a5rpNEBNe",
                        mood = "Calm",
                        genre = "Lo-Fi Beats",
                        userEmail = userEmail,

                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLW68_wbsDJYmg51TTOrpPWG0NfZL8SU6I",
                        mood = "Calm",
                        genre = "Classical Relaxation",
                        userEmail = userEmail,

                    )
                )
            }
        }
    }
}