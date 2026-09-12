<div align="center">

# 🐾 mrowserF

**A sideload Android TV browser that finally gets video sync right.**

Point it at a site your TV has no app for, let it find the stream, and watch it play back in a real native player — no lip-sync drift, no WebView jank, all driven from a plain D-pad remote.

[![Build](https://github.com/AmakerGame/mrowserF/actions/workflows/build.yml/badge.svg)](https://github.com/AmakerGame/mrowserF/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/AmakerGame/mrowserF?include_prereleases&sort=semver)](https://github.com/AmakerGame/mrowserF/releases)
[![Platform](https://img.shields.io/badge/platform-Android%20TV-success.svg)](#requirements)
[![minSdk](https://img.shields.io/badge/minSdk-23-blue.svg)](#requirements)

[Overview](#overview) • [Features](#features) • [How it works](#how-it-works) • [Install](#installation) • [Remote controls](#remote-controls) • [Build from source](#build-from-source) • [FAQ](#faq)

</div>

---

## Overview

Most Android TV boxes are locked into whatever the app store carries. If a website has the video you want and no app to match, you're stuck — and the handful of TV browsers that do exist play video *inside* the WebView, where audio and picture slowly drift out of sync over a two-hour film.

**mrowserF** is built around one job: turn any HLS-streaming website into something your TV plays properly, without ever needing a mouse.

1. **It watches your traffic.** As a page loads in a normal `WebView`, mrowserF sniffs the network requests for an HLS manifest (`.m3u8`).
2. **It hands off to a real player.** The moment a stream is found, a play chip appears and the manifest — plus its cookies, User-Agent, and subtitle tracks — is passed to a native **Media3 / ExoPlayer** activity.
3. **It gives you a cursor.** A virtual, D-pad-driven mouse pointer makes ordinary point-and-click websites navigable from six feet away with a remote.

> **mrowserF ships no content.** No preset sites, no bookmarks, no bundled streams — you type in the URL. It replays only the standard HLS manifest a page has already loaded in your own session; DRM/Widevine content is left untouched in the WebView. See [Scope & disclaimer](#scope--disclaimer).

---

## Features

| | |
|---|---|
| 🎯 **Automatic stream detection** | Sniffs every WebView request, classifies HLS manifests and subtitle files, and surfaces a play chip the instant a stream appears — auto-handoff is a toggle if you'd rather confirm first. |
| 🎬 **True native playback** | Media3/ExoPlayer handles the actual video, so a two-hour film keeps audio and picture locked — something the WebView's built-in player can't guarantee. |
| 📝 **Live-syncable subtitles** | Side-loaded VTT and SRT tracks are rendered by the app itself, so timing can be nudged ±0.5s per press (up to ±30s) **instantly**, with no rebuffering. |
| 🖱️ **D-pad virtual cursor** | An accelerating, edge-aware pointer driven entirely by the directional pad and OK button — no mouse, no touchscreen, no keyboard required. |
| ⭐ **Favorites & history** | A home-screen grid for quick launches, plus a deduplicated, newest-first browsing history with relative timestamps. |
| 🚫 **Smart pop-up handling** | Pop-ups the *page* opens on its own are blocked; ones the *user* clicks still open (so `target="_blank"` links behave), and BACK returns you cleanly. |
| 🌐 **Multi-language UI** | Full interface translations for **English**, **Українська**, and **Русский**, plus a System default option — switch anytime from Settings, no reinstall needed. |
| 🖥️ **Desktop site mode** | Requests a page's desktop layout instead of its mobile/TV one, for sites that hide content or streams behind a stripped-down mobile view. |
| ⚙️ **Live settings** | Auto-open-player, pop-up blocking, cursor speed, desktop mode, language, and clear-history-on-exit all apply immediately — no restart. |
| 🪶 **Built for weak hardware** | Plain Kotlin, framework `Activity` + XML layouts, no AndroidX/Compose bloat beyond Media3 itself. |
| 🔓 **No Google dependency** | No Play Services, no analytics, no telemetry — works on de-Googled and minimal Android TV builds. |

---

## How it works

```
WebView (D-pad cursor)
   │  every resource request
   ▼
SniffingWebViewClient ──▶ StreamSniffer ──▶ MediaUrlClassifier
   │                         (off UI thread)   (.m3u8 → HLS · .vtt/.srt → subtitle)
   │  first HLS manifest found
   ▼
play chip + automatic handoff
   │
   ▼
StreamCandidateSelector           (picks the manifest + subtitle tracks,
   │                               attaches User-Agent / Referer / Cookie headers)
   ▼
HandoffController ──▶ PlaybackRequest ──▶ PlayerActivity
                                            (Media3 / ExoPlayer + app-rendered subtitles)
```

The pure decision-making — URL classification, candidate selection, subtitle-cue parsing, cursor geometry — lives in plain Kotlin objects with no Android dependency, so it's covered by JVM unit tests under `app/src/test/`. The Android-specific glue (WebView, ExoPlayer, views) stays thin around it.

---

## Installation

mrowserF is **sideload-only**: no Play Store listing, no Google account, no Play Services.

1. Grab the latest `app-release.apk` from [**Releases**](https://github.com/AmakerGame/mrowserF/releases).
2. Get it onto the TV — a USB stick, a sideloading app like *Downloader* / *Send files to TV*, or `adb install app-release.apk` from a computer all work.
3. Allow **install from unknown sources** when prompted.
4. Launch **mrowserF** from the Android TV launcher.

### Keep it updated automatically

[Obtainium](https://github.com/ImranR98/Obtainium) tracks GitHub Releases and updates sideloaded apps for you — no store account needed. Add mrowserF with:

```
https://github.com/AmakerGame/mrowserF
```

---

## Remote controls

Everything runs off a standard D-pad remote — arrows, OK, BACK, and MENU where present.

**Browsing**

| Key | Action |
|---|---|
| D-pad | Move the virtual cursor (accelerates the longer you hold it) |
| OK | Click at the cursor position |
| OK (long-press) | Switch between **cursor** mode and **focus** mode |
| MENU, or BACK (long-press) | Open the address bar |
| BACK (tap) | Step back — address bar → page history → confirm-close → home |
| ★ | Add or remove the current page from favorites |

**Player**

| Key | Action |
|---|---|
| BACK | Return to the browser |
| Settings gear | Quality / audio track / playback speed |
| Sync-box CC button | Toggle subtitles, pick a track |
| Sync-box `−` / `+` | Nudge subtitle timing ±0.5s, live |

For the full control spec and edge cases, see [`docs/superpowers/`](docs/superpowers/).

---

## Build from source

**Toolchain:** JDK 17 · AGP 9.1.1 · Gradle 9.3.1 · `compileSdk 36` / `minSdk 23` / `targetSdk 34`. Requires SDK packages `platforms;android-36` and `build-tools;36.0.0`. Versions are pinned in `gradle/libs.versions.toml`.

```bash
./gradlew test               # JVM unit tests, no device needed
./gradlew assembleDebug      # -> app/build/outputs/apk/debug/
./gradlew assembleRelease    # -> app/build/outputs/apk/release/ (unsigned without keystore.properties)
```

Run one test class:

```bash
./gradlew test --tests "com.EdS.mrowserF.stream.MediaUrlClassifierTest"
```

Release builds sign against a git-ignored `keystore.properties`; CI signs release APKs from repository secrets. See [`.github/workflows/build.yml`](.github/workflows/build.yml).

**Stack:** Kotlin, single `:app` Gradle module, framework `Activity` + XML views (no Compose), AndroidX Media3/ExoPlayer for playback, JUnit for pure-logic tests. No Google Play Services, no analytics, no bundled content.

---

## Requirements

- Android TV (registers as a Leanback launcher app) — also runs on phones/tablets in landscape
- Android 6.0+ (`minSdk 23`, `targetSdk 34`)
- No Google Play Services
- Only the `INTERNET` permission

---

## FAQ

**Does it work on every streaming site?**
It works on anything served over **HLS** (`.m3u8`), which is most of the streaming web. Out of scope: DASH-only sites (YouTube included), progressive MP4 files, and DRM/Widevine content — those still load and play in the WebView, just without the native-player handoff.

**Why not just play video inside the WebView?**
Android TV WebViews sync audio and video poorly over long playback. mrowserF hands the stream to a native ExoPlayer session instead, which keeps them locked together.

**What subtitle formats are supported?**
Side-loaded WebVTT (`.vtt`) and SubRip (`.srt`). Tracks muxed into the HLS stream itself are intentionally left alone — it's the side-loaded ones that go out of sync and need the live nudge.

**Does it bundle any sites or content?**
No. It's a general-purpose browser with nothing pre-loaded. You're responsible for what you visit and for complying with the law in your jurisdiction.

**Will it run without Google Play Services?**
Yes — that's a hard requirement of the project, not an afterthought.

**Can I change the language?**
Yes — Settings → Language. Choose System default, Ukrainian, English, or Russian; it applies right away.

**Does it keep my browsing history?**
Locally on-device only, never uploaded anywhere. Turn on **Clear history on exit** in Settings if you'd rather it not persist between sessions.

---

## Contributing

1. Read [`CLAUDE.md`](CLAUDE.md) and the relevant plan under [`docs/superpowers/`](docs/superpowers/) before touching cursor input or the handoff pipeline.
2. Keep pure logic (decisions, parsing, geometry) separate from Android glue, and cover the pure parts with JVM tests.
3. Run `./gradlew test && ./gradlew assembleDebug` before opening a PR.
4. Keep commits focused and conventionally formatted.

---

## Scope & disclaimer

mrowserF is a general-purpose web browser. It hosts, bundles, or links to **no content** of its own — every page you open, you typed in yourself.

Under the hood, it renders pages in a standard `WebView` and — much like a browser's built-in dev tools — observes the page's own network traffic to recognize a standard HLS manifest, so that stream can be replayed in a native player with correct sync. It does **not** circumvent DRM or any protection measure (protected content stays in the WebView), and it does not download, record, or redistribute anything.

Use it only to access content you're authorized to access, in line with the law that applies to you. The software is provided **"as is"**, without warranty of any kind — see [LICENSE](LICENSE).

---

## License

MIT — see [LICENSE](LICENSE).

The original upstream project is preserved at [README_original.md](README_original.md).

---

<div align="center">

Maintained by **Edytor Studio** · originally created by Mohammad Salehivaziri · built with help from Anthropic's Claude

</div>
