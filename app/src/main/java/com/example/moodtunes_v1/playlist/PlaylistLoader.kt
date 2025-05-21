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
                        mood = "Happy",
                        genre = "90s Pop Hits",
                        url = "https://www.youtube.com/playlist?list=PL4C44E2875308A280"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Happy",
                        genre = "Upbeat Songs",
                        url = "https://www.youtube.com/playlist?list=PLx2Jv96o522ORh69HaDKClrz2Midqj8AE"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Happy",
                        genre = "Rock Classics",
                        url = "https://www.youtube.com/playlist?list=PLlLxrl-tbz-z3RqzMCOc_uQqdsbsAMGAq"
                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMood("Sad")
            if (existingPlaylists.isEmpty()) {

                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Sad",
                        genre = "Melancholy Tunes",
                        url = "https://www.youtube.com/playlist?list=PLRJriok9d2H8eNwmsdh2MSYsBXFlzZU8H"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Sad",
                        genre = "Soft Acoustic",
                        url = "https://www.youtube.com/playlist?list=PLJH9QWrouDvTIqgnrB1XtwOkw4ZgQjaZq"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Sad",
                        genre = "Emo Rock",
                        url = "https://www.youtube.com/playlist?list=PLWwVW5BHHeYVM4rAKmNOjKAXn21R6XAKV"
                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMood("Angry")
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Angry",
                        genre = "Heavy Metal",
                        url = "https://www.youtube.com/playlist?list=PLqrHHabBzX0nY0NU5xFJ6NDYR1R-jopi0"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Angry",
                        genre = "Techno Rage",
                        url = "https://www.youtube.com/playlist?list=PLfF4wIXCvi2NJOY808YLGmcd3AABPs1__"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Angry",
                        genre = "Hardcore Punk",
                        url = "https://www.youtube.com/playlist?list=PLYFbDTv39GlnLFu6rJeNRCAlP2dTuiP3C"
                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMood("Calm")
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Calm",
                        genre = "Ambient Sounds",
                        url = "https://www.youtube.com/playlist?list=PLQ_PIlf6OzqIq5aQe0uTHBmli1Nc1HTpB"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Calm",
                        genre = "Lo-Fi Beats",
                        url = "https://www.youtube.com/playlist?list=PLOzDu-MXXLlj7croDcwz33c-a5rpNEBNe"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Calm",
                        genre = "Classical Relaxation",
                        url = "https://www.youtube.com/playlist?list=PLW68_wbsDJYmg51TTOrpPWG0NfZL8SU6I"
                    )
                )
            }
        }
    }
}