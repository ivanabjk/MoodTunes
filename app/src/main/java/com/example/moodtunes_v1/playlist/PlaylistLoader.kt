package com.example.moodtunes_v1.playlist

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object PlaylistLoader {
    fun preloadPlaylists(scope: CoroutineScope, playlistDao: PlaylistDao) {

        scope.launch {
            // Check if playlists already exist to avoid duplicates
            var existingPlaylists = playlistDao.getPlaylistsByMood("Happy")
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PL4C44E2875308A280",
                        mood = "Happy",
                        genre = "90s Pop Hits",
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLx2Jv96o522ORh69HaDKClrz2Midqj8AE",
                        mood = "Happy",
                        genre = "Upbeat Songs",
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLlLxrl-tbz-z3RqzMCOc_uQqdsbsAMGAq",
                        mood = "Happy",
                        genre = "Rock Classics",
                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMood("Sad")
            if (existingPlaylists.isEmpty()) {

                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLRJriok9d2H8eNwmsdh2MSYsBXFlzZU8H",
                        mood = "Sad",
                        genre = "Melancholy Tunes",
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLJH9QWrouDvTIqgnrB1XtwOkw4ZgQjaZq",
                        mood = "Sad",
                        genre = "Soft Acoustic",
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLWwVW5BHHeYVM4rAKmNOjKAXn21R6XAKV",
                        mood = "Sad",
                        genre = "Emo Rock",
                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMood("Angry")
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLqrHHabBzX0nY0NU5xFJ6NDYR1R-jopi0",
                        mood = "Angry",
                        genre = "Heavy Metal",
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLfF4wIXCvi2NJOY808YLGmcd3AABPs1__",
                        mood = "Angry",
                        genre = "Techno Rage",
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLYFbDTv39GlnLFu6rJeNRCAlP2dTuiP3C",
                        mood = "Angry",
                        genre = "Hardcore Punk",
                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMood("Calm")
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLQ_PIlf6OzqIq5aQe0uTHBmli1Nc1HTpB",
                        mood = "Calm",
                        genre = "Ambient Sounds",
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLOzDu-MXXLlj7croDcwz33c-a5rpNEBNe",
                        mood = "Calm",
                        genre = "Lo-Fi Beats",
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        url = "https://www.youtube.com/playlist?list=PLW68_wbsDJYmg51TTOrpPWG0NfZL8SU6I",
                        mood = "Calm",
                        genre = "Classical Relaxation",
                    )
                )
            }
        }
    }
}