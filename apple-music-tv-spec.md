# Chora TV — "Apple Music on tvOS" Visual & Interaction Spec

Status: **Draft, ready for implementation planning**
Date: 2026-11-15
Target: Android TV build of Chora (`:app`, `isTv` path)
Scope owner: whoever implements the TV reskin
Reference app: Apple Music on Apple TV (tvOS 17+/iOS 26 "Liquid Glass" era)

---

## 0. One-paragraph summary

Replace the current Android-TV look of Chora — Material 3 `tv-material3` surfaces, purple dynamic color, left `NavigationDrawer`, small focus lift — with a hand-built, TV-first recreation of Apple Music on Apple TV: a floating translucent **top tab bar**, **neutral black/white chrome with Apple-red accent**, **SF Pro** type at 10-foot sizes, **glass panels and pills** rendered with real backdrop blur/refraction, and a **"full Apple TV" focus language** (lift + scale + artwork parallax + focus halo + spring physics). The information architecture becomes Apple's five tabs (Listen Now / Browse / Radio / Library / Search) with Settings moved inside Library. Now Playing becomes full-bleed artwork with a slide-in glass lyrics panel and a fully editable glass Up Next queue. **This is a visual/UX reskin only.** No MusicKit, no Apple Music service integration, no new providers. Phone UI is untouched until a later phase.

---

## 1. Interview decisions (locked)

These were answered explicitly by the requester and are binding for this spec.

| # | Question | Decision |
|---|---|---|
| 1 | Is this a visual reskin or real Apple Music integration? | **Visual/UX reskin only.** No MusicKit / Apple Music API. |
| 2 | Which Apple design era? | **iOS 26 "Liquid Glass"** aesthetic (translucency, specular edges, depth). Replace the current look entirely. |
| 3 | Platform scope? | **Android TV only for now.** Phone stays on Material You purple. TV and phone may share *tokens*, but not component implementations. |
| 4 | TV information architecture? | Adopt Apple's five tabs: **Listen Now, Browse, Radio, Library, Search**. **Settings lives inside Library.** |
| 5 | TV navigation chrome? | **Top glass tab bar** replaces the side `NavigationDrawer`. |
| 6 | Focus behavior? | **"Full Apple TV feel"**: lift + scale, artwork parallax, focus halo, spring transitions. |
| 7 | Color model? | **Neutral Apple surfaces (black/white/grays) + fixed Apple red accent.** Dynamic color (Material You) **removed on TV**. |
| 8 | Typography? | Bundle **SF Pro** (Display + Text, static weights). Licensing risk accepted by requester ("Apple allows personal use, just get it online"). See §14.1. |
| 9 | Glass implementation? | `io.github.kyant0:backdrop:2.0.1` (AndroidLiquidGlass). No high-level widgets — all glass controls hand-built. |
| 10 | Performance target? | **The requester's own TV**, Android 14+ (**API 34+**). Real blur/refraction can be assumed; fallback tier still documented. |
| 11 | Motion system? | **Tune per screen.** No central token object mandated. Drops `MotionScheme.expressive()` on TV. |
| 12 | Input model? | **D-pad only.** No swipe-to-scrub, no touch gestures. |
| 13 | Search keyboard? | **Keep the system IME.** Restyle the search field only; no custom Apple keyboard. |
| 14 | Branding? | **Reduce branding**: almost no in-app logos; rely on tab labels and artwork. "Chora" name stays. |
| 15 | Reduced motion? | Honor **system animator duration scale** *plus* an in-app **Reduce Motion** toggle in TV settings. |
| 16 | Up Next / queue depth? | **Full editing**: reorder, remove, clear, jump-to-track, "Play Next"/"Add to Queue" from Library, provider queue sync where supported. |
| 17 | Artwork quality? | **Full-resolution requests** + **gradient/blurred tinted placeholder** while loading and when art is missing. |
| 18 | Priority interactions? | Full-bleed lyrics, artwork zoom + parallax, floating glass tab bar, scroll-driven header collapse. |
| 19 | Screen coverage? | Listen Now, Library (albums/artists/songs/playlists), Settings (appearance/playback/providers/lyrics), Now Playing + lyrics + Up Next, Search, Dialogs/onboarding/provider setup. (**Radio was not in the written coverage list but is one of the five tabs — see §15 Q1.**) |
| 20 | Browse tab content? | **Genres + a configurable grid + curated rails.** |
| 21 | TV Now Playing layout? | **Full-bleed artwork + slide-in right-hand glass lyrics panel.** |
| 22 | Existing TV appearance options? | **Fixed design, accessibility only.** Hide/remove old appearance toggles except Reduce Motion, Large Text, OLED Protection. |
| 23 | Verification? | Requester builds and screenshots themselves after implementation. |

---

## 2. Non-goals

- No Apple Music / MusicKit account, catalog, or playback integration.
- No changes to phone Compose UI, phone navigation, or phone theme.
- No new media providers, no change to provider/auth logic.
- No custom on-screen keyboard, no voice search.
- No touch gestures on TV (no swipe-to-seek, no pinch).
- No Android TV home-screen channels / recommendations work (unless trivially reusing existing code — out of scope).
- No tablet / foldable layouts.
- No localization expansion; new strings go into `res/values/strings*.xml` for Crowdin.
- No icon-set replacement across phone; TV gets its own icon set only where specified.

---

## 3. Codebase audit (what actually has to change)

Facts gathered from the cloned tree (`Chora/`, shallow clone, master `9b5799b`). Use these as the implementation starting points.

### 3.1 Entry / shell

- `app/src/main/java/com/craftworks/music/MainActivity.kt` (782 lines)
  - Phone: `Scaffold` + `AnimatedBottomNavBar` + `BottomSheetScaffold` Now Playing.
  - TV: `SetupNavGraph(navController, mediaController)` — **no shared TV scaffold**. The TV branch has no top-level chrome; each destination supplies its own.
  - `TvSideNavigation(...)` is defined here (~line 397) and is the current TV chrome.
- `app/src/main/java/com/craftworks/music/NavGraph.kt`
  - Single `NavHost` with type-safe routes; `isTv` branch chosen via `LocalConfiguration`.
  - **`TvSideNavigation` is invoked 8 times**, once per TV destination (lines ~145, 181, 193, 208, 242, 274, 311, 430). Replacing the drawer with a top bar therefore means introducing a **single TV scaffold** and deleting those wrappers — not just restyling the drawer.
  - `composable<Screen.Search>` wraps **both** phone and TV in `TvSideNavigation` (line ~430) — existing bug; the new scaffold fixes it as a side effect.
- `app/src/main/java/com/craftworks/music/data/model/Screen.kt` — sealed type-safe routes. Contains `Home`, `Radios`, `Search`, `SettingsList`, `S_*` settings screens, `Album`, `Artist`, `Playlist`, `Songs`, `NowPlayingLandscape`, etc. **No `Browse`, no `Library`.**

### 3.2 Theme

- `ui/theme/Theme.kt` — `MusicPlayerTheme` branches: phone uses `MaterialExpressiveTheme` + `MotionScheme.expressive()`; TV uses `androidx.tv.material3.MaterialTheme` with default tv color scheme. Dynamic color is applied on both.
- `ui/theme/Color.kt` — single purple palette, light/dark schemes.
- `ui/theme/Type.kt` — one default text style; **no bundled fonts, no typography scale.**
- There is **no TV-specific theme file**. This spec adds one (`ui/theme/tv/`).

### 3.3 TV screens

`ui/screens/tv/`: `TvHomeScreen.kt`, `TvSearchScreen.kt` (material3 `TextField` + system IME), `TvAlbumScreen.kt`, `TvAlbumDetailsScreen.kt`, `TvArtistScreen.kt`, `TvArtistDetailsScreen.kt`, `TvPlaylistScreen.kt`, `TvPlaylistDetailsScreen.kt`, `TvSongsScreen.kt`, `TvRadioScreen.kt`, `TvSettingScreen.kt`, `settings/` (TV setting sub-screens incl. `TvSettingsAppearance.kt`).

### 3.4 TV components / dialogs

`ui/elements/tv/`: `TvAlbumCard`, `TvArtistCard`, `TvPlaylistCard`, `TvRadioCard`, `TvSongCard`, `TvProviderCard` — all `tv-material3` `Card`/`Surface` based, with default focus scale. `ui/dialogs/tv/` holds TV dialogs and provider setup.

### 3.5 Now Playing

- `ui/playing/tv/TvNowPlaying.kt` — auto-hides controls after 5 s, inline `LyricsView`, `PlaybackProgressSlider`, no queue overlay layout of its own.
- `ui/playing/PlayQueue.kt` — already has queue editing (`mediaController.moveMediaItem`, line ~227) for phone; TV can reuse the controller calls with a new shell.
- `player/SongHelper.kt` — `addMediaItems` helpers for "play next / add to queue" style actions.

### 3.6 Settings & persistence

- `managers/settings/AppearanceSettingsManager.kt` — DataStore-backed. Keys include `OLEDProtectionMode`, `AppTheme`, background style, layout alignments, navigation item ordering, home item ordering, lyrics alignment/blur/animation speed. TV currently exposes most of them in `TvSettingsAppearance.kt` (username, theme, background style, OLED, screen standby, navbar items, home items, lyrics alignment, lyrics blur, auto-scroll, recenter, more info, provider logo, refresh animation, track numbers, lyrics animation speed).
- **TV target state: prune this.** Keep OLED protection, screen standby, lyrics auto-scroll/recenter/animation speed (legibility), provider logo toggle, plus two new keys: `reduceMotion`, `largeText`. Remove background style, theme choice, navbar/home ordering, alignment, more-info, refresh animation from TV. Phone keeps everything.
- Provider queue sync already exists in the media layer: `MediaProvider.getPlayQueue()` / `savePlayQueue(songs, currentIndex, positionMs)` (`MediaProvider.kt` lines ~103/119).

### 3.7 Build / versions

- `gradle/libs.versions.toml`: Compose BOM `2026.09.00`, Kotlin `2.4.21`, `androidx.tv:tv-material 1.1.0`, `tv-foundation 1.0.0`, Media3 `1.11.1`.
- `app/build.gradle.kts`: `compileSdk`/`targetSdk` 37, `minSdk` 24, versionName `1.31.1`.
- Genres: `MediaProvider` supports `getGenreList(MediaQuery.GenreListQuery)` + `supportedGenreSort`, but there is **no repository/ViewModel path for genres today** — Browse's genre rail needs a small data addition.
- No `src/test` or `src/androidTest` sources; JUnit deps declared but nothing to run. **No automated tests can gate this work.**

### 3.8 Environment limitation

This workspace has no Android SDK and cannot run Gradle (Termux, x86_64 host without SDK). The spec's verification section is written for the requester to run. Nothing here has been compiled or visually verified.

---

## 4. Design language

### 4.1 Principles

1. **Content is the color.** Black canvas, art-driven tinting, chrome stays neutral. Red is reserved for action/selection/now-playing state — never for backgrounds at scale.
2. **Chrome floats.** Navigation and controls are glass layers above content, with generous margins; nothing is docked with a hard divider.
3. **Depth through translucency, not shadows.** Elevation is communicated by blur+tint+lens edge; shadows are subtle and only used on focus lift.
4. **Focus is the primary state.** Every focusable element must look obviously focused from 3 m away: scale, halo, brightness, and label emphasis together.
5. **Motion is short and physical.** Springs, ≤ 400 ms for navigation, ≤ 180 ms for focus. No long, showy transitions that fight D-pad repeat.
6. **Quiet typography.** Few weights, tight tracking at large sizes, secondary text at 70% white.

### 4.2 Color tokens (TV, dark-first)

TV is **dark-only** in this phase (tvOS Music is dark; light mode adds surface-sampling complexity for no benefit). Light tokens are reserved for the phone phase.

```
bg.base          #000000   // full-bleed canvas, Now Playing, video-like screens
bg.canvas        #0B0B0F   // scrolling screens (near-black, slight blue cast)
bg.canvasTop     #131318   // top gradient stop behind tab bar

surface.1        #FFFFFF 6%   over canvas
surface.2        #FFFFFF 10%
surface.3        #FFFFFF 14%
surface.pressed  #FFFFFF 20%

label.primary    #FFFFFF
label.secondary  #FFFFFF 70%
label.tertiary   #FFFFFF 45%
label.disabled   #FFFFFF 30%

separator        #FFFFFF 12%
hairline         #FFFFFF 10%   // 1dp glass edge

accent           #FA2D48       // Apple Music red, solid
accent.gradient  #FA233B → #FB5C74
accent.pressed   #D81F38
onAccent         #FFFFFF
selectionTint    #FA2D48 18%
destructive      #FF453A

focusRing        #FFFFFF 90%   // 3dp outer stroke + 8dp white glow at 12%
scrim            #000000 45%
```

Rules:
- Text over artwork always sits on a scrim (`#000` 40–55%) or inside a glass panel — never raw over art.
- Red is used for: active tab indicator, primary action buttons, progress fill, "playing" equalizer glyph, destructive confirms. Not for section headers or large fills.
- Dynamic color APIs (`dynamicDarkColorScheme`, `dynamicLightColorScheme`) must **not** be called on the TV path.

### 4.3 Typography

Bundle **SF Pro Display** (sizes ≥ 20) and **SF Pro Text** (< 20), static weights: Regular (400), Medium (500), Semibold (600), Bold (700). Files as `.otf` in `app/src/main/res/font/`:

```
sf_pro_display_regular.otf
sf_pro_display_medium.otf
sf_pro_display_semibold.otf
sf_pro_display_bold.otf
sf_pro_text_regular.otf
sf_pro_text_medium.otf
sf_pro_text_semibold.otf
```

Use a `res/font/sf_pro_display.xml` family and `res/font/sf_pro_text.xml` family so Compose can reference one `FontFamily` per optical size. If a static `.otf` fails to load, fall back to `FontFamily.Default` at build-time selection (see §14.1) — never crash.

10-foot scale (sp; TV viewing distance ≈ 3 m):

| Token | Size / line / tracking | Weight | Use |
|---|---|---|---|
| `displayHero` | 76 / 80 / -1.5 | Bold | Listen Now hero title |
| `screenTitle` | 56 / 60 / -1.0 | Bold | Browse / Library / Search headers |
| `sectionTitle` | 34 / 40 / -0.5 | Semibold | Rail headers ("Recently Played") |
| `cardTitle` | 28 / 32 / -0.3 | Semibold | Tile titles |
| `cardSubtitle` | 22 / 28 / 0 | Regular, 70% | Tile subtitles |
| `body` | 24 / 32 / 0 | Regular | Settings rows, dialogs |
| `caption` | 20 / 24 / 0 | Regular, 45–70% | Metadata, durations |
| `button` | 24 / 24 / 0 | Semibold | Buttons |
| `tab` | 22 / 24 / -0.2 | Semibold | Top tab bar |
| `npTitle` | 48 / 52 / -0.8 | Bold | Now Playing track title |
| `npArtist` | 30 / 36 / 0 | Regular, 70% | Now Playing artist |
| `lyricsActive` | 44 / 52 / -0.5 | Bold | Current lyric line |
| `lyricsInactive` | 36 / 44 / -0.3 | Semibold, 35% | Other lyric lines |

Rules:
- `sp` for all text; clamp `fontScale` at **1.3×** for layout math and text (system large-text support), then reflow rather than shrink.
- No all-caps. No letter-spacing except the negative tracking above.
- Numeric durations use tabular figures if the bundled font has them (`FontFeatureSetting("tnum")`); otherwise fixed-width by monospace-free layout (reserve width).

### 4.4 Spacing, sizes, radii

```
space: 4, 8, 12, 16, 24, 32, 48, 64
screen margin: 64dp horizontal, 48dp top (below tab bar), 48dp bottom
rail gap: 32dp; rail title → content: 20dp
tab bar: 96dp tall glass, centered, 40dp item gap
tile: 220dp square (album/playlist/radio), 200dp circle (artist)
hero tile: 1280×720 max, typically 960×540
icon: 28dp inline, 32dp controls, 40dp transport
radius: 8 (chips), 12 (rows), 16 (tiles), 24 (panels), 28 (glass sheets), 36 (pills)
hairline: 1dp
```

Overscan: keep all focusable content inside a 5% safe area (Android TV guidance); the tab bar sits at y = 24dp within that.

### 4.5 Glass materials

All glass is hand-built from Backdrop primitives:

```kotlin
Modifier
  .layerBackdrop(backdrop)          // samples the content layer behind
  .drawBackdrop(
      shape = RoundedCornerShape(28.dp),
      effects = {
          colorFilter(tint)          // 1. tint first
          blur(radius, edgeTreatment) // 2. blur
          lens(refractionHeight, amount) // 3. lens (API 33+, needs CornerBasedShape)
      }
  )
```

Documented material presets (start values, tune on device):

| Preset | Blur | Tint | Lens | Radius | Border | Use |
|---|---|---|---|---|---|---|
| `chrome` | 24dp | black 25% | 8dp / 8 | 28dp | white 10% | top tab bar, floating nav |
| `panel` | 30dp | black 40% | 12dp / 10 | 28dp | white 12% | lyrics panel, queue sheet |
| `pill` | 20dp | white 6% (dark) | 12dp / 8 | full | white 14% | transport controls, buttons |
| `overlay` | 40dp | black 45% | none | 24dp | white 8% | dialogs, menus |
| `chip` | 12dp | white 8% | 4dp / 4 | 12dp | white 10% | genre chips, tags |

Rules:
- Lens only where the shape is a `CornerBasedShape`; circles/pills must use `CircleShape`-compatible rounded corners at 50% radius.
- Never animate blur radius. Animate tint alpha, translation, or scale instead.
- Cap simultaneous large glass surfaces at **2** (tab bar + one panel) plus ≤ 3 pills; see §12.
- Every glass surface gets a 1dp inner highlight on its top edge (white 14%) and a soft outer shadow at 20% for specularity. Implement as `drawWithContent` overlay, not a second backdrop.

### 4.6 Focus language

Per focused element:

- **Scale** 1.00 → 1.06 (cards/tiles), 1.04 (tabs), 1.03 (buttons/rows).
- **Lift** translateY = −8dp, with shadow growing to 24dp ambient / 10dp spot.
- **Halo**: 3dp outer ring at white 90%, plus 8dp glow at white 12%; ring sits outside the clip.
- **Brightness**: non-focused tiles dim to 82% alpha; focused tile returns to 100% and its title switches from 70% to 100% white.
- **Artwork parallax**: the artwork bitmap inside the focused tile translates ±4dp opposite to the tile's position relative to screen center (X only), 250 ms linear-to-spring; clipped by the tile. Never move the tile itself beyond the lift.
- Focus enters within **180 ms** via `spring(dampingRatio = 0.75f, stiffness = 400f)`; leave in **140 ms** via `spring(0.9f, 300f)`.
- Only one element is focused at a time; the first focus on a screen is deterministic (hero card on Listen Now, first tab on cold start).
- Do **not** stack tv-material3's built-in focus scale on top: pass `scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)` or build on raw `Modifier` + `onFocusChanged`.

### 4.7 Motion baselines (per-screen tuning allowed)

| Interaction | Spec |
|---|---|
| Focus in/out | 180 ms / 140 ms spring (above) |
| Tab switch content | 250 ms crossfade + 24dp horizontal slide in direction of travel |
| Push (detail) | 350 ms `FastOutSlowIn` slide 48dp + fade; pop 250 ms |
| Header collapse on scroll | title scales 56→34 sp and moves up 32dp, driven by scroll offset (no duration) |
| Artwork crossfade (track change) | 450 ms; artwork scales 0.97→1.00 with `spring(0.85, 350)` |
| Lyrics line change | 300 ms color/scale transition; active line 400 ms spring into place |
| Queue/lyrics panel in | 400 ms `spring(0.85, 380)` slide from right; backdrop tint 200 ms |
| Controls auto-hide | fade 250 ms after 5 s of no input (keep existing 5 s) |
| Dialog in | 300 ms scale 0.94→1 + fade; out 200 ms |
| Press feedback | scale 0.97, 90 ms |

Reduced motion (system scale 0 **or** in-app toggle):
- All durations → 0 except opacity crossfades (≤ 120 ms).
- Disable lift and parallax; focus is communicated by ring + brightness only.
- Disable auto-hide of controls (they stay visible).
- Disable artwork scale on track change; keep crossfade.

---

## 5. Information architecture

### 5.1 Tab bar

Top-center, floating glass bar, five text tabs in this order:

```
Listen Now    Browse    Radio    Library    Search
```

- **Text-only** labels (`tab` style). No app logo, no provider logo.
- Active tab: label 100% white + 3dp red dot 8dp below the baseline.
- Focused tab: glass pill (white 14%) behind label, scale 1.04, ring.
- On scroll, the bar stays pinned; content scrolls beneath and blurs through it.
- On screens with a hero (Listen Now, Browse, Album/Artist/Playlist details), the bar starts transparent (tint 10%) and increases to `chrome` tint after 64dp of scroll.
- Search tab focuses the search field on entry.

### 5.2 Route mapping

| Apple tab | Existing route | Action |
|---|---|---|
| Listen Now | `Screen.Home` | Keep route, rebuild UI |
| Browse | **new** `Screen.Browse` | Add route + screen |
| Radio | `Screen.Radios` | Keep route, restyle |
| Library | **new** `Screen.Library` | Add route; becomes hub for album/artist/song/playlist lists + Settings |
| Search | `Screen.Search` | Keep route, restyle |
| Settings | `Screen.SettingsList` | Reachable only from Library's bottom row (and from Now Playing → overflow? no — Library only) |

`Screen.Library` layout: 2×2 category tiles (Albums, Artists, Songs, Playlists) + a "Recently Added" rail + a bottom **Settings** row (gear icon, `body` style). Selecting a category pushes the existing TV list screens (`TvAlbumScreen`, `TvArtistScreen`, `TvSongsScreen`, `TvPlaylistScreen`), restyled.

Additions to `Screen.kt`: `@Serializable data object Browse`, `@Serializable data object Library`. No other route renames (keeps deep links/back stack stable).

### 5.3 TV scaffold

Introduce one wrapper used by every TV destination:

```
TvAppScaffold(
  currentTab: TvTab,
  onTabSelected: (TvTab) -> Unit,
  content: @Composable (PaddingValues) -> Unit
)
```

- Renders `bg.canvas`, the Backdrop source, `TvTopTabBar`, and the content slot with `PaddingValues(top = 96.dp + 24.dp)`.
- Destinations that need immersive layout (Now Playing, details hero) pass `chrome = Chrome.Hidden` / `Chrome.AutoHideOnScroll`.
- Delete the eight `TvSideNavigation(...)` wrappers in `NavGraph.kt`; the phone branches stay byte-identical.
- Keep a `TvAppScaffold` around details screens too, so the tab bar persists (Apple TV keeps it on detail pages) — with the active tab reflecting the section the user came from.

### 5.4 Back behavior

1. Lyrics/queue panel open → close panel, restore focus to its trigger.
2. Dialog/menu open → dismiss.
3. Detail screen → previous screen, restore focus to the originating tile.
4. Tab root with tab bar not focused → focus the active tab (do not exit).
5. Tab bar focused → system back exits (default).

Implement 1–4 with `BackHandler` in the TV scaffold + per-screen; never swallow back silently.

---

## 6. Screen specs

### 6.1 Listen Now (`TvHomeScreen`)

Vertical scroll, sections driven by existing `HomeScreenViewModel` collections:

1. **Hero** — 960×540 (or 16:9 at 88% width) artwork of the most recent/most played album, full-bleed within its container with a bottom-up black gradient (transparent → 85%). Overlay: `displayHero` title, `body` artist, two buttons ("Play", "Shuffle") using `pill` material; red primary for Play. Hero crossfades every time the underlying item changes, 450 ms.
2. **Recently Played** — rail of 220dp tiles, `sectionTitle` + "See All" chevron (pushes `HomeLists`).
3. **Recently Added** — same.
4. **Most Played** — same.
5. **Shuffle / Made For You** — same, from `shuffledAlbums`.

Behavior:
- Header collapse: as the hero scrolls out, the screen title "Listen Now" appears in the tab bar's leading area? **No** — keep the bar clean (interview: reduce branding). Instead the hero simply scrolls; no sticky title.
- D-pad down from tab bar focuses hero Play; left/right within buttons; down enters first rail.
- Empty/loading: hero skeleton (artwork gradient placeholder + 3 shimmer bars) and 3 skeleton rails.
- Error (provider offline): inline glass card with retry button.

### 6.2 Browse (new)

`Screen.Browse`, new `TvBrowseScreen` + `BrowseViewModel`.

Content, top to bottom:
1. **Curated rails** (provider-driven, same shape as Listen Now rails): e.g. "New Releases", "Editor's Picks" where the provider supports them; otherwise reuse recently added/genre-derived rails. Each rail is a horizontal list of 220dp tiles.
2. **Genres** — grid of `chip`-material genre buttons, 4 columns, from `MediaProvider.getGenreList(...)`. Selecting a genre pushes a genre detail list (albums for that genre, reusing the album grid).
3. **Configurable grid** — the requester's "configurable grid" item: a `TvBrowseGrid` that renders provider sections as a uniform grid whose column count and tile aspect are configurable via remote config constant (not a user setting in v1): default 5 columns, square tiles, at most 3 sections stacked ("Because You Like", "Decades", "Moods" where available). Keep this data-optional: sections that return empty are skipped.

Data work required:
- Add `GenreRepository` (or methods on `SongRepository`) exposing `getGenreList()` and genre album queries via `MediaProvider`.
- Add `BrowseViewModel` mirroring `HomeScreenViewModel` patterns (StateFlow + `collectAsStateWithLifecycle`).
- No new provider capabilities.

Empty state: "Nothing to browse yet — connect a provider" glass card with a button to `Screen.S_Media_Providers`.

### 6.3 Radio (`TvRadioScreen`)

- Keep existing data (`RadioScreenViewModel`).
- Layout: `screenTitle` "Radio", then a 3-column grid of `TvRadioCard` restyled to a 16:9 station tile with logo/artwork, station name (`cardTitle`), and a small red "LIVE" chip when applicable.
- Focus: lift + parallax, station artwork slightly zoomed (1.04) on focus.
- Detail (if the provider exposes station detail) uses the same header pattern as album detail.

### 6.4 Library (new hub)

`Screen.Library` → `TvLibraryScreen`.

- `screenTitle` "Library".
- 2×2 grid of large category tiles (440×220): Albums, Artists, Songs, Playlists. Each tile: neutral `surface.1` fill (not artwork), 40dp glyph, `sectionTitle` label, item count (`caption`) when cheap to compute (else omit).
- **Recently Added** rail (220dp tiles) below.
- **Settings** row pinned at the end of the scroll: gear icon + "Settings" + chevron; pushes `Screen.SettingsList` (existing TV settings hub, restyled).
- Selecting a category pushes: Albums → `TvAlbumScreen`, Artists → `TvArtistScreen`, Songs → `TvSongsScreen`, Playlists → `TvPlaylistScreen`.
- Each list becomes a 5-column grid of 220dp tiles with a sticky `sectionTitle` header and no side drawer. Long lists: lazy grid with `contentType`, stable keys.
- **Add to queue / Play next** from any tile's context menu (long-press OK) or the detail screen's action row.

### 6.5 Search (`TvSearchScreen`)

- Focused rounded `pill` glass field, 720dp wide, centered at 25% height, placeholder "Artists, Songs, Albums". Magnifier glyph, no logo.
- Uses the **system IME** (existing `TextField` + IME path); restyle decoration only, keep `BasicTextField` semantics and the current query debounce.
- While the field is focused, the tab bar dims to 60% to reduce visual competition.
- Results: rails in order "Top Results" (3 mixed tiles), "Artists", "Albums", "Songs", "Playlists", each reusing the standard tiles.
- Empty query: recent searches as chips (persist last 5 in DataStore? optional — if no store exists today, skip; do not add new persistence just for this).
- No results: centered glass card "No results for '<query>'".

### 6.6 Album / Artist / Playlist / Songs detail screens

Shared **detail header** component (`TvDetailHeader`):

- Left: 360dp artwork with focus scale/zoom/parallax; Right: `screenTitle` title, `body` artist/metadata, action row (`Play` red pill, `Shuffle` glass pill, `⋯` glass icon button opening the action menu with Play Next / Add to Queue / Go to Artist).
- **Scroll-driven collapse**: as the header scrolls up, artwork shrinks to 120dp and pins next to a compact title in a `chrome` mini-header; the tab bar stays visible. Collapse is offset-driven (no animation spec).
- Background: blurred, darkened artwork tint covering the top 60% of the screen, crossfading on data change. Use a pre-blurred bitmap (Coil transformation) **not** a Backdrop layer — the backdrop is reserved for chrome.
- Track lists: rows 72dp tall, `cardTitle`/`caption`, playing row shows red equalizer glyph + red title. Focus row: `surface.2` fill, scale 1.02, ring. Columns: index/#, title, artist (albums), duration right-aligned tabular.
- Artist detail: circular hero 320dp, "Top Songs" rail, "Albums" grid, "Related Artists" rail.
- Playlist detail: same as album plus owner/description and reorder affordance if the playlist is editable (only where the provider supports it — otherwise hide).

### 6.7 Now Playing (`TvNowPlaying`)

Full-screen route (`Screen.NowPlayingLandscape`), immersive: tab bar hidden, black base.

Layout:
- **Background**: full-bleed artwork scaled to cover, blurred (pre-blurred bitmap, 40dp equivalent), darkened 65%, with a slow parallax drift (±12dp over 20 s) — disabled under Reduce Motion. On track change: crossfade 450 ms + 0.97→1.0 scale.
- **Center**: artwork 560dp square, 16dp radius, soft shadow; subtle 1.02 bob on beat? **No** — no audio-reactive motion (out of scope, costly).
- **Left/bottom info**: `npTitle`, `npArtist`, provider chip (if provider logo toggle is on, small 20dp monochrome glyph), quality/format caption.
- **Transport** (bottom, auto-hides after 5 s): glass `pill` bar 720dp containing shuffle, previous, play/pause (56dp filled white circle with black glyph), next, repeat. Above it a 2dp progress bar with elapsed/remaining `caption`; red fill with white 14% track. D-pad left/right on the focused bar seeks ±10 s; up/down moves focus between progress, transport, and the two side buttons.
- **Right-side actions**: "Lyrics" and "Up Next" glass pill buttons stacked at the right edge (56dp from edge). Selecting either slides in the corresponding panel.
- **Controls overlay**: any D-pad input reveals controls and resets the 5 s timer (existing behavior, keep timeout constant in one place).
- **OLED protection**: existing setting keeps a slow pixel-shift of the whole composition (±4dp, 60 s cycle) and dims static chrome; also suppress the bright red progress fill to 80% on OLED mode.

Lyrics panel:
- Slides from the right, 40% width (max 720dp), full height, `panel` glass; 24dp gap from screen edge; radius 28dp on the left side only.
- Lines: `lyricsInactive` → `lyricsActive`, active line left-aligned with a 4dp red leading bar; auto-scroll centers the active line with 300 ms ease; blur/fade distance falloff above and below (±3 lines) if the lyrics mode setting enables it.
- Word-level sync supported if the provider supplies it; otherwise line-level. Unsynced lyrics render as a scrollable static block with `body` size.
- Empty state: "Lyrics not available for this track".
- Focus stays on the lyrics list; left D-pad returns to the Now Playing surface; Back closes the panel.

Up Next panel (queue):
- Right-side panel, same shell as lyrics, 44% width.
- Header: "Up Next" + "Clear" (destructive text button, confirm dialog).
- Rows 68dp: index or equalizer, 44dp artwork, title, artist, duration; current row red-tinted (`selectionTint`) and shows equalizer.
- Focused row: `surface.3` + ring; **long-press OK** or the `⋯` button opens a glass context menu: Play Next, Move Up, Move Down, Remove, Go to Album/Artist. "Clear" clears everything after the current track.
- Reorder uses `mediaController.moveMediaItem(from, to)` (pattern already in `PlayQueue.kt`).
- After edits, call `savePlayQueue(...)` when the active provider supports queue sync (`MediaProvider.savePlayQueue`), debounced 1 s; failures are silent (log only) so offline playback still works.
- Empty queue: "Nothing up next" + "Add from Library" button that navigates to `Screen.Library`.

Transitions:
- Now Playing opens from the mini-player/full-screen trigger with a 400 ms shared-bounds-style scale+fade from the artwork; exit reverses 300 ms.
- Lyrics/queue panels displace (not overlay) the artwork: artwork translates left by 18% and scales 0.94 with 400 ms spring when a panel is open. This preserves the full-bleed feel.

### 6.8 Settings (inside Library)

`Screen.SettingsList` (`TvSettingScreen`) → grouped, Apple-style list:

- Full-width 1100dp column, `screenTitle` "Settings", sections as glass rows (`panel`? no — plain `surface.1` rows with 1dp hairline, radius 12dp) to keep glass count low.
- Sections: **Account** (username/provider status), **Playback**, **Media Providers**, **Lyrics**, **Appearance** (a11y only), **About**.
- Rows: 76dp, label `body`, trailing value `caption` 45% or chevron; focused row white 8% fill + ring, no scale beyond 1.02.

TV appearance screen (`TvSettingsAppearance.kt`) — prune to:
- OLED Protection (existing)
- Screen Standby (existing)
- **Reduce Motion** (new toggle)
- **Large Text** (new toggle, or a 3-step size control)
- Lyrics: auto-scroll, recenter, animation speed (legibility only)
- Provider logo toggle (keep; it controls the small provider glyph)
Remove from TV: theme/light-dark, background style, navbar items ordering, home items ordering, lyrics alignment (fixed), lyrics blur (fixed to preset), more info, refresh animation, track numbers.

New DataStore keys in `AppearanceSettingsManager`: `reduceMotion: Boolean`, `largeText: Boolean`. Defaults false/false. These are TV-only in effect; phone may ignore them.

Large Text behavior: multiply the type scale by 1.15 (cap at 1.3 with system fontScale) and recompute row/tile heights so nothing clips; rails show 4 tiles instead of 5 at the largest step.

### 6.9 Dialogs, onboarding, provider setup

- All dialogs use `overlay` glass on a 45% black scrim; 24dp radius; width max 720dp; `screenTitle`-sized title (40sp), `body` copy, right-aligned actions; primary action is red-filled pill, secondary is glass.
- Focus traps inside the dialog; first focus on the primary action; Back dismisses (same as Cancel).
- Provider setup (`TvProviderCard` flows): 3-step glass wizard — pick provider → credentials (system IME fields, restyled pills) → done. Keep all validation/error strings; errors appear as a red-tinted inline row, never a toast-only message.
- Onboarding: if the app shows a welcome/setup screen on first launch, restyle to a centered hero (no logo, just "Chora" wordmark in `screenTitle` + one-line value prop + red "Get Started" pill). No new onboarding steps.
- First-run provider picker: grid of provider cards with 96dp logos on `surface.1`, focused card lifts 8dp and shows a red "Select" pill.

---

## 7. Component inventory & file plan

### 7.1 New theme files

```
ui/theme/tv/TvColors.kt        // AppleTvColors data class + dark palette + LocalTvColors
ui/theme/tv/TvTypography.kt    // FontFamily from res/font + TvTypography tokens
ui/theme/tv/TvShapes.kt        // radii tokens
ui/theme/tv/TvTheme.kt         // TvTheme { CompositionLocals for colors/type/shapes/motion/glass }
ui/theme/tv/TvGlass.kt         // GlassPreset enum + Modifier.tvGlass(preset)
ui/theme/tv/TvMotion.kt        // named specs (focusIn/out, push, panel) — screens may override
ui/theme/tv/ReduceMotion.kt    // LocalReduceMotion + duration helper
```

`TvTheme` must **not** wrap content in `androidx.tv.material3.MaterialTheme` unless a legacy component needs it; instead keep `MaterialTheme` out of the TV path and provide `LocalTvColors/Type` to new components. Any remaining tv-material3 component gets explicit colors (no dynamic scheme).

### 7.2 New shell / navigation

```
ui/tv/TvAppScaffold.kt         // backdrop source + tab bar + content padding + BackHandler
ui/tv/TvTopTabBar.kt           // 5 text tabs, glass pill focus, active red dot
ui/tv/TvTab.kt                 // enum: ListenNow, Browse, Radio, Library, Search + route mapping
ui/tv/TvFocus.kt               // Modifier.tvFocusable(scale, lift, halo, parallax) + TvFocusDefaults
ui/screens/tv/TvLibraryScreen.kt
ui/screens/tv/TvBrowseScreen.kt
ui/viewmodels/tv/BrowseViewModel.kt (or existing package convention)
```

### 7.3 New shared components (`ui/elements/tv/apple/`)

```
TvMediaTile.kt        // artwork tile: parallax layer, gradient scrim, title/subtitle, playing overlay
TvHero.kt             // 16:9 hero with gradient + action pills
TvSectionHeader.kt    // sectionTitle + optional "See All"
TvGlassButton.kt      // pill button (primary red / secondary glass), focusable
TvGlassIconButton.kt
TvProgressBar.kt      // 2dp track with red fill, seeks with left/right
TvDetailHeader.kt     // collapsing artwork header
TvListRow.kt          // settings/list row 76dp with focus fill
TvLyricsPanel.kt
TvQueuePanel.kt
TvSkeleton.kt         // shimmer bar + artwork gradient placeholder
TvEmptyState.kt
TvContextMenu.kt      // glass popover for tile long-press actions
TvEqualizerGlyph.kt   // 3-bar red animated "now playing" indicator
```

### 7.4 Rewritten files

- `MainActivity.kt` — TV branch only: host `TvAppScaffold`; remove/relocate `TvSideNavigation` (keep function only if referenced elsewhere; otherwise delete). Do not touch phone composables.
- `NavGraph.kt` — remove 8 `TvSideNavigation` wrappers; wrap TV destinations in `TvAppScaffold`; add `Screen.Browse` / `Screen.Library` composables; fix the phone Search path.
- `data/model/Screen.kt` — add `Browse`, `Library`.
- `ui/theme/Theme.kt` — TV branch delegates to `TvTheme`; dynamic color only on phone.
- `ui/screens/tv/*` — reskin each with the new components (Home, Browse, Radio, Library, Album/AlbumDetails, Artist/ArtistDetails, Playlist/PlaylistDetails, Songs, Search, Setting + settings sub-screens).
- `ui/elements/tv/*` — reimplement cards as thin wrappers over `TvMediaTile`/`TvGlassButton` (keep file names to limit churn).
- `ui/playing/tv/TvNowPlaying.kt` — new full-bleed layout + lyrics/queue panels; keep the 5 s auto-hide and media-controller wiring.
- `ui/dialogs/tv/*` — restyle to `overlay` glass; keep logic.
- `managers/settings/AppearanceSettingsManager.kt` — add `reduceMotion`, `largeText`; no key removals (phone still uses them; TV just stops reading some).
- `ui/screens/tv/settings/TvSettingsAppearance.kt` — prune rows per §6.8.

### 7.5 Resources

- `app/src/main/res/font/` — SF Pro OTFs + family XML.
- `app/src/main/res/values/strings.xml` (+ `strings_screens.xml`, `strings_settings.xml` where the project splits): add "Listen Now", "Browse", "Library", "Up Next", "Reduce Motion", "Large Text", "Nothing up next", "Add from Library", "Lyrics not available for this track", "Clear", "Play Next", "Move Up", "Move Down", "Remove", "Nothing to browse yet", "No results for %1$s".
- `res/drawable/` — glyphs: shuffle, previous, play, pause, next, repeat, lyrics, queue, gear, chevron, ellipsis, magnifier, equalizer (vector, 24dp grid). Prefer drawn vectors over a new icon dependency.

### 7.6 Build files

`gradle/libs.versions.toml`:

```toml
backdrop = "2.0.1"
# [libraries]
backdrop = { module = "io.github.kyant0:backdrop", version.ref = "backdrop" }
```

`app/build.gradle.kts`: `implementation(libs.backdrop)`.

Do **not** add Coil blur separately — Coil is already used for artwork; use its `blur`/`BlurTransformation`-equivalent request option available in the current version, or a pre-blurred bitmap helper with `RenderEffect` on API 31+.

---

## 8. Dependencies & integration risk

### 8.1 Backdrop

- `io.github.kyant0:backdrop:2.0.1`, Apache-2.0, Maven Central, minSdk 21.
- `Modifier.layerBackdrop` samples a content layer; `Modifier.drawBackdrop(shape, effects)` draws the material on top. Effects order is **colorFilter → blur → lens**.
- Blur requires **API 31+** (`RenderEffect`); lens requires **API 33+** and a `CornerBasedShape`.
- The library is Compose Multiplatform; on Android it resolves to the JetBrains Compose runtime. **Risk:** version skew against Compose BOM `2026.09.00` / Kotlin `2.4.21`. Mitigations, in order:
  1. Add the dependency and run `./gradlew :app:dependencies --configuration debugRuntimeClasspath` to inspect resolved `androidx.compose.*` / `org.jetbrains.compose.*`.
  2. If Gradle pulls older Compose artifacts through the CMP dependency, exclude `org.jetbrains.compose.*` groups from the backdrop dependency and rely on the AndroidX equivalents (the Android artifacts of Backdrop depend on AndroidX Compose at runtime).
  3. If binary incompatibility appears (NoSuchMethodError at runtime), pin Backdrop to the version built against the project's Compose version, or vendor the two small modifier files (Apache-2.0 permits it) with attribution. **Vendoring is the fallback, not the default.**
- No high-level widgets exist, so all chrome is hand-built (matches the interview decision).

### 8.2 SF Pro

- Bundling SF Pro is **not** covered by Apple's free license for app distribution; Apple's license covers use in UI mockups/design for Apple-platform software. Redistribution inside an Android APK is a licensing violation and a **F-Droid policy violation** (F-Droid ships only freely licensed assets). The requester accepted this risk.
- Mitigations to record in code/release notes:
  - Keep the fonts in a dedicated `tv` source set or resource folder so they can be swapped for an open metric-compatible face (e.g. Inter Display / SF-alike) without touching layout.
  - Add `README`/`NOTICE` line: "SF Pro is a trademark of Apple Inc.; bundled here at the project owner's discretion."
  - If F-Droid distribution matters, build a `foss` flavor that excludes the fonts and falls back to `FontFamily.Default` with adjusted tracking.
- The spec does **not** endorse the licensing position; it records the decision and the escape hatch.

### 8.3 Other

- `androidx.tv:tv-material 1.1.0` stays for components not worth rebuilding (e.g. `Surface` focus semantics in dialogs), but must be fed explicit colors. Over time the new components supersede it.
- No new DI/Hilt changes; ViewModels are constructed the existing way (`hiltViewModel(parentEntry)`).

---

## 9. API gating & fallback tier

Target device: Android 14+ (API 34+), so the primary tier assumes real blur and lens.

| API | Behavior |
|---|---|
| 34+ | Full `chrome`/`panel` glass with blur + lens; full motion. |
| 33 | Blur + lens available (API 33 = Android 13). Same as above. |
| 31–32 | Blur available, lens not: fall back to blur + tint (drop `lens`, keep hairline). |
| 24–30 | No `RenderEffect` blur: use translucent black/white fills (alpha 0.72/0.08) + 1dp hairline; no lens; reduce shadow; everything else identical. `TvGlass` selects the tier internally so screens never branch. |

Detection lives in one place: `TvGlass.kt` (`GlassTier.Full / BlurOnly / Solid` from `Build.VERSION.SDK_INT`), exposed via `LocalGlassTier`. Do not scatter `SDK_INT` checks through screens.

---

## 10. Performance plan (measurement required)

The requester's device is the target; still, budgets:

- **16.6 ms/frame** (60 fps) on the primary device; no frame > 32 ms during focus traversal.
- At most 2 large glass surfaces visible; blur source content should be static during focus animations (freeze backdrop sampling while a focus spring runs; resume on settle) to avoid re-blurring the animated tile.
- Pre-blur artwork backgrounds once per track (cache by `MediaItem.id`); never blur per frame.
- Coil: request full resolution for the focused/hero item, and a downsampled size (`220dp` × density) for rails. Configure `.size()` explicitly rather than `Size.ORIGINAL` everywhere; prefetch the next 6 rail items.
- Avoid nested backdrops and `graphicsLayer` + backdrop combos that force offscreen layers per frame.
- Text/overlay scrims are static drawables, not animated gradients.
- Queue rows use stable keys + `contentType`; the queue panel is a `LazyColumn` with 8-item lookahead.
- Lyrics: pre-parse into `List<LyricLine>` off the main thread; only the visible window (±5 lines) composes animated transitions.

Instrumentation (requester runs):
```
adb shell dumpsys gfxinfo com.craftworks.music reset
# navigate: tab bar → rail (10 tiles) → detail → now playing → lyrics → queue
adb shell dumpsys gfxinfo com.craftworks.music
```
Accept: janky frames < 5% on the primary device, 90th percentile < 16 ms.

---

## 11. Accessibility

- **Focus visibility**: never rely on color alone; ring + scale + brightness together. Ring contrast ≥ 3:1 against both black canvas and artwork.
- **Text contrast**: primary white on black ≥ 15:1; secondary 70% white ≥ 4.5:1 on `bg.canvas`; over artwork always use a scrim. Red text only at ≥ 24sp semibold (large-text exception) — otherwise red is a fill, not text.
- **TalkBack**: every tile has a merged semantics description: "Album, <title>, by <artist>"; buttons expose action labels; the equalizer glyph is decorative (`clearAndSetSemantics {}`); progress exposes `ProgressBarRangeInfo` and seek actions.
- **D-pad only**: all interactive elements reachable with 4-way + OK; no element requires long-press as the *only* path (context menus also reachable via `⋯` button). Focus order is spatial and matches visual order.
- **Large Text**: up to 1.3× with reflow; tiles grow, columns drop from 5→4; no clipped titles (max 2 lines with ellipsis).
- **Reduce Motion**: system scale 0 or in-app toggle → no lift/parallax/scale, crossfades only, controls never auto-hide.
- **OLED protection**: keep pixel shift + dim; respect it in Now Playing (already bright).
- **Audio cues**: none (no new sound design).
- **Timeouts**: no interactive element disappears while focused; auto-hide pauses on focus.

---

## 12. Verification & QA checklist (requester executes)

Environment: build with `./gradlew :app:assembleDebug` (JDK 21), install on the Android 14+ TV (`adb install -r`), capture screenshots at 1920×1080 and, if available, 3840×2160.

Per-screen checks:

1. **Shell** — tab bar appears on every TV destination; 5 tabs in order; active dot follows the current section; focus pill visible; content blurs through the bar; no side drawer remains anywhere.
2. **Motion** — focus spring feels 180/140 ms; tab switch crossfade+slide; detail push/pop; hero artwork crossfade; nothing stutters while holding D-pad.
3. **Listen Now** — hero + 4 rails render with real data; "See All" pushes lists; skeletons on cold start; error card on provider failure.
4. **Browse** — curated rails, genre chips (all genres from provider), configurable grid columns; empty sections skipped; empty state when provider absent.
5. **Library** — 4 category tiles push lists; recently added rail; Settings row opens settings; long-press menu has Play Next / Add to Queue; items land in the queue.
6. **Search** — system keyboard opens; field is restyled; results rails; back closes IME then returns focus to the field; no-results state.
7. **Album/Artist/Playlist/Songs** — collapsing header; blurred art tint; track rows; playing row indicator; long lists scroll smoothly; action row Play/Shuffle/⋯.
8. **Now Playing** — full-bleed art; controls auto-hide after 5 s and reappear on any D-pad input; seek ±10 s; play/pause; shuffle/repeat state; lyrics button opens right panel, art displaces; queue button opens queue; Back closes panel then screen.
9. **Lyrics** — synced lines highlight and auto-scroll; unsynced block scrolls; word-sync if provider supplies; empty state.
10. **Queue** — reorder (Move Up/Down), remove, clear (with confirm), jump to track; current row tinted; provider queue persists after app restart when supported; empty state.
11. **Settings** — grouped rows; appearance shows only OLED, standby, Reduce Motion, Large Text, lyrics trio, provider logo; playback/providers/lyrics/about open; values persist across restart.
12. **Dialogs / setup** — glass overlay, focus trap, primary red, Back = Cancel; provider wizard 3 steps; error rows legible.
13. **A11y** — set animator scale to 0 (`adb shell settings put global animator_duration_scale 0`), verify no motion and controls stay visible; Large Text at 1.3× reflows without clipping; TalkBack reads tiles and buttons.
14. **Fallback** — temporarily force `GlassTier.Solid` (debug flag) and verify no visual breakage (simulates API < 31).
15. **Performance** — gfxinfo run above; note janky % and any > 32 ms frames.

Screenshots to capture: Listen Now, Browse, Library, Search, Album detail (collapsed + expanded header), Now Playing, Now Playing + lyrics, Queue, Settings, provider setup, dialog, Large Text variant.

---

## 13. Implementation phases

Ordered so the app is always runnable, and phone stays untouched.

**P0 — Foundations (no visual change yet)**
- Add `backdrop` dependency; resolve version skew (§8.1).
- Add SF Pro fonts + family XML; `TvTypography`, `TvColors`, `TvShapes`, `TvTheme`, `TvGlass`, `TvMotion`, `ReduceMotion`.
- Switch the TV branch of `MusicPlayerTheme` to `TvTheme` (screen still looks old, but colors/type change).
- Verify build.

**P1 — Shell**
- `TvTab`, `TvTopTabBar`, `TvAppScaffold`; remove 8 `TvSideNavigation` wrappers; add `Browse`/`Library` routes and placeholder screens; wire tab mapping.
- Result: five tabs navigate to existing (restyled-later) content; phone unchanged.

**P2 — Components + screens**
- `TvMediaTile`, `TvHero`, `TvSectionHeader`, `TvGlassButton/IconButton`, `TvSkeleton`, `TvEmptyState`, `TvFocus`.
- Reskin Home, Radio, Library (categories + Settings row), lists (Album/Artist/Playlist/Songs), artist/album/playlist detail headers.
- Browse: genre repository + `BrowseViewModel` + rails + grid.

**P3 — Now Playing**
- Full-bleed layout, transport pill, progress seek, auto-hide, art crossfade/parallax, OLED behavior.
- Lyrics panel + queue panel with full editing and provider queue sync.

**P4 — Settings / dialogs / setup**
- Prune TV appearance rows; add Reduce Motion + Large Text (DataStore keys + behavior).
- Restyle settings hub, dialogs, provider setup, onboarding.

**P5 — Polish**
- Motion tuning per screen, glass presets on device, perf pass (gfxinfo), a11y pass (TalkBack, large text, animator scale 0), fallback-tier check.

Definition of done: all §12 checks pass, phone build unaffected (`assembleDebug` + smoke test phone screens), no unresolved TODOs in TV files, no `SDK_INT` checks outside `TvGlass.kt`.

---

## 14. Risks

1. **SF Pro licensing / F-Droid** — as described in §8.2; accepted by requester. Escape hatch: font swap + `foss` flavor.
2. **Backdrop vs Compose/Kotlin versions** — highest technical risk; resolve in P0 before any UI work. If unresolvable, vendor the Apache-2.0 sources.
3. **Blur cost at 4K** — mitigate with frozen sampling during focus animation, at most 2 large surfaces, pre-blurred artwork, and measuring. On the target API 34 device, expect 1080p rendering; if the TV renders 4K UI, budget will be tighter.
4. **SF Pro metric differences** — Android text layout differs from tvOS; expect ±1 line wraps. Design with max-lines + ellipsis, not exact line counts.
5. **Genre data absent** — Browse's genre rail depends on new repository plumbing over an existing provider API; if a provider returns empty genres, the section must disappear cleanly.
6. **Queue provider sync** — `savePlayQueue` support varies by provider; treat as best-effort, debounce, never block UI.
7. **tv-material3 focus conflicts** — default scale/shadow on `Card`/`Surface` fights the custom lift; explicitly disable per component.
8. **No automated tests / no local build** — every change is verified only by the requester's manual pass; keep phases small and self-contained.
9. **Scope creep into phone** — enforced by keeping TV code in `ui/theme/tv`, `ui/tv`, `ui/elements/tv/apple`, and TV screens; shared files touched only where listed.

---

## 15. Open questions

1. **Radio tab content** — Radio is one of the five tabs but was not in the written coverage list. Assumed here: keep the existing radio stations grid, restyled (§6.3). Confirm whether Radio should instead become Apple-style "Apple Music Radio"-like curated station rails (requires provider data) or be hidden if the provider has no stations.
2. **Light mode on TV** — assumed dark-only. Confirm.
3. **Hero auto-rotation** — should the Listen Now hero rotate automatically (tvOS does), and if so at what interval? Spec currently: static until data changes.
4. **"Configurable grid" semantics** — assumed developer-configurable column/aspect constants in v1. Confirm whether it is meant as a user-facing setting (would add a Settings row and DataStore keys).
5. **SF Pro optical sizes** — whether to bundle both Display and Text families (≈8 files) or only Display with negative tracking (≈4 files). Spec assumes both.
6. **Onboarding existence** — verify whether a first-run onboarding screen exists today; if not, this section applies only to provider setup.
7. **Phone phase** — later: reuse tokens or a separate iOS-26 phone design (tab bar bottom, sheet Now Playing)? Out of scope now; note for roadmap.

---

## 16. Acceptance criteria

- [ ] TV app shows a floating glass top tab bar with exactly: Listen Now, Browse, Radio, Library, Search; no side drawer anywhere.
- [ ] Settings is reachable only via Library's Settings row, and TV appearance exposes only OLED, standby, Reduce Motion, Large Text, lyrics legibility, provider logo.
- [ ] No Material You purple and no dynamic color on TV; palette is neutral + `#FA2D48`.
- [ ] All TV text renders in bundled SF Pro at the §4.3 scale; no default Roboto visible in TV chrome.
- [ ] Focus everywhere uses lift + scale + halo (+ parallax on artwork tiles), 180/140 ms springs.
- [ ] Glass chrome uses real blur (and lens on API 33+) with a solid fallback below API 31.
- [ ] Now Playing is full-bleed with auto-hiding glass transport, a right-side glass lyrics panel, and a full-editing Up Next panel (reorder/remove/clear/jump + provider sync where supported).
- [ ] Detail screens have collapsing artwork headers with blurred art tint.
- [ ] Listen Now rails, Browse (rails + genres + configurable grid), Library hub, Search with system IME, Radio grid all functional with real provider data.
- [ ] Reduce Motion works from both the system setting and the in-app toggle; controls stop auto-hiding when enabled.
- [ ] Large Text at 1.3× reflows without clipping.
- [ ] Phone UI is visually unchanged; `assembleDebug` succeeds for both paths.
- [ ] gfxinfo run shows < 5% janky frames on the target device.

---

## 17. Reference notes

- Backdrop API: `io.github.kyant0:backdrop:2.0.1` — `layerBackdrop`, `drawBackdrop(shape, effects)`, effects order colorFilter → blur → lens; blur API 31+, lens API 33+ & `CornerBasedShape`.
- Existing behavior to preserve: 5 s controls auto-hide (`TvNowPlaying.kt`), media-controller queue edits (`PlayQueue.kt`), provider queue sync (`MediaProvider.getPlayQueue/savePlayQueue`), OLED protection and lyrics settings (`AppearanceSettingsManager`).
- Existing behavior to remove on TV: dynamic color, `MotionScheme.expressive()` (already phone-only), side drawer, appearance toggles listed in §6.8, provider logo prominence (keep tiny glyph only), any in-app Chora logo in chrome.
