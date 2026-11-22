# Metrolist UI port

## Pinned source and target

- Upstream: `MetrolistGroup/Metrolist`
- Upstream source revision inspected: `5f58b40817399c59af3cd80aec3890e9ee22652b`
- Metrofy base: `main` at `1fd0d29b84700fc8788fabc0c024c63f8e93ce7d`
- Working branch: `feature/metrolist-native-ui-rebuild-20261122`

The goal is a screen-level port of the current Metrolist Android UI, not a reskin of Metrofy's existing screens and not a handful of copied widgets.

## Integration boundary

Port Metrolist's actual Compose presentation, layout, interaction patterns, and motion. Keep Metrofy's application contracts unless a specific adapter is required:

- Hilt ViewModels and Metrofy's existing state flows.
- `NavController` and `navigationBuilder` route contracts.
- `LocalPlayerConnection`, Media3 playback, queue/session handling.
- Metrofy database entities, download state, and image providers.
- Metrofy-only feature surfaces and settings.

Do not copy upstream Koin, KMP-only resource APIs, shared `Navigator`, or upstream persistence/playback implementations into the Android app as a shortcut. Translate those UI calls to Metrofy equivalents.

## Metrofy functionality that must survive

Spotify login/library/playlists and Spotify playback; Canvas; word-by-word synced lyrics and lyric providers; Pear live lyrics; Smart Shuffle; Listen Together and guest restrictions; Qobuz/stream resolution; downloads/offline/local files; podcast shelves; recognition; history/statistics; account and integrations; existing deep links, intents, and notification/media-session behavior.

## Port order

- [ ] App shell, navigation states, top-level insets, mini-player placement, and transitions.
- [ ] Home: shelf/card layout, loading/offline/error/refresh states, contextual actions, shuffle, podcast and Spotify sections.
- [ ] Library: filter shelf, unified collection presentation, grid/list, sorting/search, selection, playlists, downloads, local files, Spotify.
- [ ] Search: input/focus/keyboard lifecycle, source filters, history, result cards, URL handling, swipe actions, empty/loading/error states.
- [ ] Player: full player, artwork/video/Canvas presentation, lyrics, controls, progress/seek gestures, options, and accessibility.
- [ ] Mini-player and queue: gestures, current-track state, queue sections, reorder/remove/undo, and menu actions.
- [ ] Settings and nested settings: group structure, category navigation, controls, integrations, and all Metrofy-specific options.
- [ ] Detail surfaces: album, artist, playlist, podcast, history, recognition, and Listen Together.
- [ ] Theme/typography/shapes, expressive controls, semantics, edge-to-edge/insets, and responsive behavior.

## Acceptance gates

1. Each migrated screen must use the ported Metrolist screen structure/components; changing only shared chip/card styling does not count.
2. Metrofy actions and state must remain connected to the existing ViewModels and services.
3. Build all supported variants after each coherent migration slice; fix the actual compile/runtime cause rather than removing features to get a green build.
4. Review the diff against the pinned upstream source and verify every Metrofy-only feature listed above still has an entry point.
5. Do not merge or publish a release until all core screens are migrated, all variants pass, and the resulting APKs have been checked.
