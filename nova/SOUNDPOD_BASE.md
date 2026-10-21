# Metrofy Nova source base

The Nova preview build uses the actual SoundPod Android project rather than trying to reproduce its interface with unrelated Metrofy screens.

- Upstream project: https://github.com/arunnechully/SoundPod
- Pinned upstream commit: https://github.com/arunnechully/SoundPod/commit/03dcb75b0158eb5c8dccbd82a131e8bce09a876d
- Upstream license: GPL-3.0 (the upstream `LICENSE` file is retained in the generated source archive)
- Preview application ID: `com.metrofy.nova`
- Launcher label: `Metrofy Nova`
- Preview version: `0.1.0-nova.1`

The preview workflow checks out the pinned source, applies the reproducible changes in `scripts/prepare-metrofy-nova.py`, builds a universal release APK, validates its package identity, and uploads both the APK and modified source archive.

This gives Nova SoundPod's real screens, navigation, and player implementation. It is a SoundPod-derived preview application, not a claim that Metrofy's existing backend or every Metrofy-specific integration has been ported into SoundPod. The stable Metrofy branch is not modified.
