# Fliz TV - IPTV Player

Android IPTV player with VLC & ExoPlayer playback, EPG, recordings, Chromecast, and ad integration. Built with Kotlin Multiplatform + Compose Multiplatform.

## Features
- **Dual player**: VLC primary, ExoPlayer fallback on AC-3 or crash
- **EPG**: India-specific guide from `iptv-epg.org`, 20 country alternatives
- **Recordings**: HLS download with metadata persistence across restarts
- **Chromecast**: User-agent/referer passthrough
- **Ads**: Full-screen (10min delay, 10s display, 5min gap) + top banner at `alpha(0.1f)`
- **Multi-language**: Per-channel language inference via keywords

## Build
```bash
./gradlew :androidApp:assembleDebug
```

## Test
```bash
./gradlew :shared:testAndroidHostTest
```

## APK
Output: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`
