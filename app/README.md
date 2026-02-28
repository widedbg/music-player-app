# Music Player App

A modern **Android music player** built with **Kotlin** and **Jetpack Compose**, featuring:

- Play local songs from `raw` resources or downloaded files.
- Fetch trending tracks from **Audius API**.
- Download tracks to local storage.
- Play, pause, next, previous controls.
- Seek within songs with a slider.
- Beautiful dark-themed UI using Compose.

---

## Features

- **Local & downloaded songs**: Supports songs in the app and downloaded from Audius API.
- **Trending tracks**: Fetch and display trending songs from [Audius API](https://api.audius.co/v1/tracks/trending).
- **Download & play**: Download tracks and add them to your playlist.
- **Music controls**: Play, pause, next, previous, seek.
- **Modern UI**: Dark theme, circular album previews, slider for progress, and dynamic playlist display.

---

## Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **Retrofit** (for API calls)
- **OkHttp** (for downloading music)
- **MediaPlayer** (for music playback)
- **Android Service** (background playback)
- **Material3** components

---

## Setup

1. Clone the repo:

```bash
git clone https://github.com/widedbg/music-player-app.git