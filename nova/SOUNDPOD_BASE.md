# Metrofy Nova UI preview

Metrofy Nova is a separate preview flavor built from **Metrofy's own source tree**. SoundPod is a UI reference only; the preview must not replace Metrofy's playback engine, database, YouTube/Spotify integrations, navigation routes, or settings with SoundPod's app implementation.

- UI reference: https://github.com/arunnechully/SoundPod
- Reference license: GPL-3.0
- Preview application ID: `com.metrofy.nova`
- Launcher label: `Metrofy Nova`
- Preview version: `0.1.0-nova.1`
- Working branch: `feature/soundpod-ui-nova`

Nova-only visual changes are gated by `BuildConfig.NOVA_PREVIEW`; the ordinary Metrofy build should continue to use its existing UI. The preview workflow builds `:app:assembleFossDebug` from this repository and verifies the distinct package identity.

This is an iterative UI adaptation, not yet a claim of pixel-perfect reproduction. The preview APK still needs hands-on device testing for layout, navigation, and playback. Do not merge this branch into stable Metrofy without explicit approval.
