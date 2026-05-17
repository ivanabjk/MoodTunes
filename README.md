# MoodTunes

MoodTunes is an Android app that uses **speech recognition** and an **integrated AI model** to detect the user’s mood and generate personalized playlists based on their preferences.

---

## Table of Contents
- [Authentication](#authentication)
- [Home Screen](#home-screen)
- [Playlists](#playlists)
- [History](#history)
- [Favorites](#favorites)
- [Tech Stack](#tech-stack)

---

## Authentication
- Built with **Firebase Authentication**  
- Includes **Login** and **Register** screens  

<p align="center">
  <img src="app/src/main/assets/screenshots/LoginScreen.png" alt="Login Screen" width="250"/>
  <img src="app/src/main/assets/screenshots/RegisterScreen.png" alt="Register Screen" width="250"/>
</p>

---

## Home Screen
- Users can **type their mood** in the search bar or **speak** using the microphone button.  
- When the **Get Mood** button is pressed, the AI model analyzes the input and generates the most fitting mood.
- Currently supported moods: **Happy**, **Sad**, **Angry**  

<p align="center">
  <img src="app/src/main/assets/screenshots/HomeScreen.png" alt="Home Screen" width="220"/>
  <img src="app/src/main/assets/screenshots/MoodHappy.png" alt="Happy Mood" width="220"/>
  <img src="app/src/main/assets/screenshots/MoodSad.png" alt="Sad Mood" width="220"/>
  <img src="app/src/main/assets/screenshots/MoodAngry.png" alt="Angry Mood" width="220"/>
</p>

---

## Playlists
- Once a mood is generated, the **See Playlists** button becomes available.  
- Playlists are created based on the user’s preferences (**2 playlists per preference**).  
- Preferences can be managed in the **Profile screen**, where users can:
  - Add a profile image  
  - Add or remove preferences for specific moods  
- When a playlist is clicked, the user is redirected to **YouTube**, where the first song of the playlist begins playing.  

<p align="center">
  <img src="app/src/main/assets/screenshots/Playlists.png" alt="Playlists" width="220"/>
  <img src="app/src/main/assets/screenshots/PlaylistInYoutube.png" alt="Playlist played in YouTube" width="220"/>
  <img src="app/src/main/assets/screenshots/ProfileScreen.png" alt="Profile Screen" width="220"/>
  <img src="app/src/main/assets/screenshots/AddGenrePreference.png" alt="Add Preference" width="220"/>
</p>

---

## History
- The **History screen** allows users to:
  - View past searches  
  - Delete individual entries or clear all history  
  - Filter by mood  
  - Use the search bar for quick lookup  

<p align="center">
  <img src="app/src/main/assets/screenshots/HistoryScreen.png" alt="History Screen" width="250"/>
  <img src="app/src/main/assets/screenshots/FilterHistory.png" alt="Filter History" width="250"/>
  <img src="app/src/main/assets/screenshots/SearchHistory.png" alt="Search History" width="250"/>
</p>

---

## Favorites
- The **Favorites screen** displays playlists marked as favorites.  
- Users can:
  - Unmark favorites  
  - Filter playlists  
  - Search for specific playlists  

<p align="center">
  <img src="app/src/main/assets/screenshots/FavoritesScreen.png" alt="Favorites Screen" width="250"/>
  <img src="app/src/main/assets/screenshots/FilterFavorites.png" alt="Filter Favorites" width="250"/>
  <img src="app/src/main/assets/screenshots/SearchFavorites.png" alt="Search Favorites" width="250"/>
</p>

---

## Tech Stack
- **Kotlin**  
- **Firebase Authentication & Firestore**  
- **Room Database**  
- **MVVM Architecture** with Coroutines & Reactive Flows  
- **Integrated AI Model**  
  - Hugging Face model converted to TensorFlow  
  - Runs locally in the app (no external API calls)  
- **YouTube API** for playlist integration  

---
