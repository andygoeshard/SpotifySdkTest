# 🎧 Spotify SDK Test App

This is a basic Android application built in Kotlin using Jetpack Compose that integrates the [Spotify Android SDK](https://developer.spotify.com/documentation/android/) to test authentication and remote playback control.

## 🚀 Features

- Connect to Spotify using the App Remote SDK
- Control playback (Play, Pause)
- Display current track metadata (artist, title, album image)
- Shows connection status and logs

## 📱 Tech Stack

- Kotlin
- Jetpack Compose
- Spotify App Remote SDK
- Android ViewModel + State management
- Material3 UI

## 🛠️ Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/andygoeshard/SpotifySdkTest.git
   ## 🧩 Setup Instructions

2. **Create an application** at the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard/).
3. Add your **Client ID** and **Redirect URI** in `SpotifyViewModel.kt`:

  ```kotlin
  private val CLIENT_ID = "your_spotify_client_id"
  private val REDIRECT_URI = "your_redirect_uri" 
  ```

4.Make sure the Spotify app is installed on your device.

⚠️ First-time connection requires Spotify to be opened manually.

## ⚠️ Known Issues
On first launch, if the Spotify app is not running, the SDK will fail to connect.

As a workaround, the app shows a message prompting the user to open Spotify manually.

Once the app has been opened at least once, subsequent connections will work as expected.


