# Pear Music lyrics widget attribution

Metrofy's live home-screen lyrics widget is adapted from **Pear Music**:

- Upstream project: https://github.com/dragonzords/Pear-Music
- Upstream license: GNU General Public License, version 3 (GPL-3.0)

The port retains the upstream widget's two-page `ViewFlipper` lyric transition,
throttled karaoke word-fill updates, album-art backdrop, cached low-resolution
three-pass box blur, widget layout, drawables, and animations.

The Metrofy integration connects the widget to `MusicService`,
`LyricsWidgetManager`, `LyricsWidgetState`, `LyricsHelper`, `LyricsUtils`,
Room lyrics storage, and the Media3 playback clock. The lock-screen metadata
implementation is also adapted from Pear Music's Media3 `ForwardingPlayer`
approach.

The Metrofy adaptation is modified from the upstream project and remains
under GPL-3.0. Metrofy-specific changes are dated 2026-11-19.
