# MeloX Android

This directory contains the native Android port of MeloX.

## Goals

- Preserve the visual language and interaction model of the SwiftUI app as closely as practical.
- Keep the Android client native: Kotlin + Jetpack Compose.
- Use Android platform media APIs instead of emulating Apple-only system features.
- Keep Xiaomi HyperOS enhancements optional so the app remains usable on other Android devices.

## Current stack

- Kotlin (AGP built-in Kotlin)
- Jetpack Compose
- Navigation Compose
- AndroidX Media3 / ExoPlayer
- MediaSessionService
- HyperOS focus-notification bridge (optional platform adapter)

## Platform mapping

| iOS | Android |
| --- | --- |
| SwiftUI | Jetpack Compose |
| NavigationStack | Navigation Compose |
| AVPlayer / AVFoundation | Media3 ExoPlayer |
| MPNowPlayingInfoCenter / remote commands | MediaSession |
| Live Activity / Dynamic Island | Standard media notification + optional HyperOS focus notification / Super Island |
| Core ML AutoMix model | Planned ONNX/TFLite path |

## Porting milestones

1. **Bootstrap**: buildable Compose shell, theme, navigation shell, Media3 service.
2. **Core data**: models, NetEase request client, login/session persistence.
3. **Usable player**: home/search/playlist/song playback, mini player, full player, MediaSession metadata.
4. **Lyrics**: LRC/YRC parsing, translation/romanization, timed lyric rendering.
5. **Offline**: download queue, cache, local playback.
6. **Advanced UI**: landscape player, Apple Music-style lyrics, EVA/skyline views.
7. **Advanced playback**: equalizer, crossfade, dual-deck AutoMix and on-device beat analysis.
8. **Platform polish**: HyperOS Super Island extras and other OEM-specific integrations behind adapters.

## Important design rule

Root access must never be required by the application. A rooted device may be useful for development and diagnostics, but production behavior should use documented Android and OEM APIs with graceful fallback.

## Build

Open the `android/` directory in a current Android Studio version, install Android SDK 37, and build the `app` module with JDK 17.
