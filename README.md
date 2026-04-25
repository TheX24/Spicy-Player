![Spicy Player Banner](spicyplayer.png)

# Spicy Player

Spicy Player is an offline music player for Android with a port/recreation of [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics) - A [Spicetify](https://spicetify.app/) Extension, designed to achieve **visual parity** with Spicy Lyrics' rendering. Built using **Jetpack Compose (Canvas API)** and **ExoPlayer**.

> [!WARNING]
> This is a work in progress. The app is not yet complete and may have bugs.

---

## Key Features

### High-Fidelity Lyrics Rendering

- **Sub-Pixel Text Positioning**: Uses Compose's `TextMeasurer` for exact glyph calculation.
- **Duet-Aware Layout**: Identifies primary (`v1`) and guest (`v2`) artists from TTML metadata.
- **Intelligent Alignment**: Primary artists are justified to the left, and guest artists to the right, with multi-line blocks correctly right-aligned for balance.

### Physics-Driven Motion

- **Analytic Spring Engine**: Replaces traditional Euler/Verlet integration with a mathematically exact (closed-form) solution for damped harmonic oscillators. This ensures that a target set 500ms in the future is reached exactly, with zero energy drift.
- **Critical Damping (ζ = 1.0)**: Used for all auto-scroll centering to provide the fastest non-oscillatory return possible.
- **Under-damped Springs**: Used for word-bouncing and interlude-dot expansions to provide a lively, bouncy character.

### Synchronized Animations

- **Held Word Bounce**: Syllables with a duration `>= 1000ms` automatically receive a bouncy scale and Y-offset animation, highlighting them letter-by-letter.
- **Instrumental Break Dots**: Injected automatically for gaps `>= 3000ms`, featuring a "breathe" pulse effect synced with the song's timing.
- **Seamless Seek**: Clicking any lyric line instantly re-bases the physics spring to the current visual position, ensuring no "snap-back" jumps when transitioning from manual scrolling back to auto-focus.

---

## Project Architecture & Core Logic

### 1. The Rendering Pipeline (`LyricsRenderer.kt`, `SpicyLyricsView.kt`)

The app uses a single-pass rendering loop that updates at the device's native refresh rate (60Hz/90Hz/120Hz).

- **Coordinate Systems**:
  - **`Canvas Space`**: (0, 0) is the top-left of the view.
  - **`Scroll Space`**: A virtual Y-coordinate where the lyrics live.
  - **`Screen Center`**: Used as the anchor point for the active line focus.
- **Calculations**: `targetY = -clusterCenterY` (where the cluster is the average Y-position of all currently active lines).

### 2. The Physics Solver (`SpringSimulation.kt`)

Solving $x'' + 2\zeta\omega x' + \omega^2 x = \omega^2 \cdot \text{goal}$ for $x(t)$:

- **Critically Damped ($\zeta = 1$)**: $x(t) = (x_0 + (v_0 + \omega x_0)t)e^{-\omega t}$
- **Under-damped ($\zeta < 1$)**: $x(t) = e^{-\zeta\omega t}(A \cos(\omega_d t) + B \sin(\omega_d t))$
  This ensures perfect smoothness regardless of fluctuating frame times.

### 3. TTML Parser (`TtmlLyricsParser.kt`)

A stateful XML parser that:

1. Scans for `<ttm:agent>` metadata to identify primary artists.
2. Tokenizes `<p>` tags into high-precision `Word` objects.
3. Injects virtual `Line` objects for instrumental breaks.

---

## Roadmap

Find the full feature and bug roadmap [here](plan.md).

---

## Tech Stack

- **Jetpack Compose**: For the entire UI declaration and Canvas manipulation.
- **Media3 (ExoPlayer)**: Industrial-grade media decoding and playback.
- **Kotlin Coroutines**: For non-blocking IO during TTML and audio file scanning.
- **Custom XML Pull Parser**: For lightweight, low-memory performance on large lyric files.

## License

This project is licensed under the **AGPL-3.0 License**, inherited from the [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics) project. See the [LICENSE](LICENSE) file for the full text.

---

_Made by [TX24](https://tx24.is-a.dev) with the help of Antigravity's available models. Based on [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics) - A [Spicetify](https://spicetify.app/) Extension_
