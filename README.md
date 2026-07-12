![Spicy Player Banner](spicyplayer.png)

# Spicy Player

Spicy Player is an offline music player for Android with a port of [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics) - A [Spicetify](https://spicetify.app/) Extension, designed to achieve **visual parity** with Spicy Lyrics' rendering. Built using **Jetpack Compose (Canvas API)** and **ExoPlayer**.

> [!WARNING]
> This is a work in progress. The app is not yet complete and may have bugs.

---

## Key Features

- **Karaoke-style lyrics**: sub-pixel word/letter positioning, duet-aware left/right alignment, and a closed-form spring physics engine for scroll and word-bounce animation (frame-rate independent, no snap-back on seek).
- **Word-synced, line-synced, and static lyrics**, with RTL script support and on-device romanization (Japanese via Kuromoji, Chinese via pinyin4j).
- **Dynamic Background**: blurred cover-art backdrop, including a GPU shader-driven "Kawarp" engine alongside the legacy blur, selectable in settings.
- **Local library**: MediaStore-backed scanning of on-device music, with albums, playlists, and folder-based auto-pairing of audio files to their TTML lyrics.
- **Tag editor**: built-in metadata/tag editing (via jaudiotagger).

---

## Roadmap

Find the full feature and bug roadmap [here](https://lab.tx24.dev/b/9ihk5rxetqnK8awg6/project-spicy-player).

---

## Tech Stack

- **Jetpack Compose**: For the entire UI declaration and Canvas manipulation.
- **Media3 (ExoPlayer)**: Industrial-grade media decoding and playback.
- **Kotlin Coroutines**: For non-blocking IO during TTML and audio file scanning.
- **Custom XML Pull Parser**: For lightweight, low-memory performance on large lyric files.

## License

This project is licensed under the **AGPL-3.0 License**, inherited from the [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics) project. See the [LICENSE](LICENSE) file for the full text.

---

_Made by [TX24](https://tx24.is-a.dev) with the help of Antigravity and Claude Code. Based on [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics) - A [Spicetify](https://spicetify.app/) Extension_
