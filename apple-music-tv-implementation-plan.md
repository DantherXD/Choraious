# Chora TV — Apple Music Reskin: Implementation Plan

Status: **In progress.** P0–P2 complete with execution logs (Appendices A–F);
P3 (Now Playing, lyrics, queue) implemented and compiled locally (Appendix G).
Date: 2026-11-15
Input: `apple-music-tv-spec.md` (read in full)
Repo: `Chora/` @ `9b5799b` (shallow clone), single `:app` module
Constraint: plan only; no code changes until this document is approved.

---

## 0. How to use this plan

- Work strictly **P0 → P1 → P2 → P3 → P4 → P5**. Phases are gated: a phase starts only after the previous **Gate** is signed off by the requester.
- Every task has an ID, explicit dependencies, files, and an exit check. A task is "done" only when its exit check has been run **on the requester's machine** — this workspace has no Android SDK, so nothing here can be compiled or run.
- API-tier fallback is a **cross-cutting concern**: it is implemented once in P0 (`TvGlass`), consumed by every component, and re-verified at every gate. It is not a P5 patch.
- Phone code is frozen. Any diff touching phone composables/routes is a review failure unless the plan explicitly lists it.
- One commit per task or per small task group, one tag per gate (`tv-p0`, `tv-p1`, …). Rollback = revert to the last gate tag.

---

## 1. Plan-level corrections to the spec (need approval)

These came out of reading the code, not out of preference. Each changes what P1/P3 tasks must do.

### C1. The 8 `TvSideNavigation` call sites are not the only chrome problem

Confirmed mapping of the eight wrappers in `NavGraph.kt`:

| Line | Destination | Screen |
|---|---|---|
| 145 | `Screen.Home` | Listen Now |
| 181 | `Screen.Songs` | Songs list |
| 193 | `Screen.Radios` | Radio |
| 208 | `Screen.AlbumList` | Albums grid |
| 242 | `Screen.ArtistsList` | Artists grid |
| 274 | `Screen.PlaylistList` | Playlists grid |
| 311 | `Screen.SettingsList` | Settings hub |
| 430 | `Screen.Search` | Search (wraps phone **and** TV — existing bug) |

The detail routes (`AlbumDetails` 214, `ArtistDetails` 248, `PlaylistDetails` 280) and `NowPlayingLandscape` (361) have **no** drawer today. Two additional gaps the spec's audit missed:

- `Screen.HomeLists` (line 151) has **no `isTv` branch**: "See All" from Listen Now renders the **phone** `HomeListsScreen` on TV today. A TV screen is required (`TvHomeListsScreen`), otherwise the new Listen Now rail headers lead to phone UI.
- Settings sub-screens (`S_Appearance`, `S_Media_Providers`, `S_Lyrics_Providers`, `S_Playback`, `S_Misc`) are also chrome-less and must be placed inside the scaffold.

**Plan response:** implement the scaffold as **one wrapper around the TV `NavHost`**, not eight per-destination wrappers. This is still the "single-scaffold refactor" (one chrome host, eight wrappers deleted) and it automatically covers details, settings sub-screens, and future routes. The tab bar is an overlay in a `Box`; TV screens consume `LocalTvTopInset` (120dp) for their first-content inset, so artwork can scroll under the glass bar. Now Playing opts out of chrome entirely.

### C2. TV Now Playing is reached through the phone wrapper

`NavGraph.kt:361` → `NowPlayingContent(...)` → `NowPlaying.kt:91` → `TvNowPlaying(...)`.

`NowPlayingContent` *first* computes the phone palette and draws `NowPlaying_Background(...)`, then delegates to `TvNowPlaying`. The new full-bleed design would be painted on top of the phone background and still pay for phone palette extraction.

**Plan response:** P3 starts with an **early TV branch** in `NowPlayingContent` that returns `TvNowPlaying(...)` before palette/background work, passing `navController` (needed for "Go to Album/Artist" and "Add from Library"). `TvNowPlaying`'s signature grows; `NowPlaying_Background` and `NowPlayingViewModel` palette logic stay untouched for phone.

### C3. Motion on TV currently comes from the *phone* MaterialTheme default

`NavGraph.kt:107` calls `MaterialTheme.LocalMaterialTheme.current.motionScheme.slowSpatialSpec<Float>()` using `androidx.compose.material3.MaterialTheme`. On TV, `MusicPlayerTheme` provides `androidx.tv.material3.MaterialTheme`, so this resolves to the **default** compose-material3 motion scheme (not the expressive one, and not a TV one).

**Plan response:** P1 replaces the TV `NavHost` transitions with explicit TV specs from `TvMotion` (350 ms push / 250 ms pop, §4.7). The phone branch keeps the existing `animationSpec` line unchanged.

### C4. Reduce Motion must exist before animations, not after

The spec puts the in-app toggle in P4, but every animation in P2/P3 must consult it. Retrofitting is exactly the late-phase patch the plan is told to avoid.

**Plan response:** the DataStore keys (`reduceMotion`, `largeText`) and the `LocalReduceMotion` provider (system animator scale **or** stored flag) land in **P0**. P4 only adds the settings rows that flip the already-wired flag.

### C5. Font resource naming must be neutral for the flavor swap

The spec names files `sf_pro_display_bold.otf` etc. If the `foss` flavor ships Inter under those filenames, the resource names become a lie and the swap is confusing to maintain.

**Plan response:** use neutral resource names (`chora_display_regular/medium/semibold/bold`, `chora_text_regular/medium/semibold`) and build `FontFamily`s in Kotlin. `sfpro` flavor ships SF Pro files under those names; `foss` flavor ships an OFL face under the same names. No XML font families needed.

---

## 2. Additional verified facts the plan relies on

- `Theme.kt` TV branch: `androidx.tv.material3.MaterialTheme(tv dark/light scheme, default typography)`; dynamic color only on the phone branch. There is no TV token file.
- `MainActivity.kt` TV branch sets `window.decorView` background from `tv.MaterialTheme.colorScheme.background` (≈ line 195) and calls `SetupNavGraph(...)` with `bottomPadding = 0.dp`. No TV mini-player, no TV bottom sheet.
- `TvNowPlaying.kt` is 398 lines, holds `controlsVisible` + 5 s `debounce`, reads `LyricsState.lyrics`, and renders `LyricsView` inline. It takes `(mediaController, iconColor, metadata, onRefreshLyrics)` — no navigation callback.
- `AppearanceSettingsManager` is `@Singleton @Inject constructor(@ApplicationContext context)`, DataStore name `settings`; screens instantiate it directly (`AppearanceSettingsManager(LocalContext.current)`), not via Hilt VM. New keys follow the same pattern.
- Repositories call `MediaProviderManager.currentProvider.value` directly (`AlbumRepository`, `SongRepository`, …). There is no genre repository; `getGenreList(MediaQuery.GenreListQuery)` exists only on `MediaProvider` and is implemented by `LocalMediaProvider` (312) and `SubsonicMediaProvider` (473).
- `HomeScreenViewModel` loads four album lists with `async` + `DataRefreshManager.dataSourceChangedEvent`. `BrowseViewModel` should copy this structure.
- `Screen.NowPlayingLandscape` is navigated to from TV detail screens and `TvHomeScreen` after starting playback.
- Strings live in `res/values/strings{,_actions,_dialogs,_screens,_settings}.xml`; ~5 files, Crowdin-managed.
- No `src/test` / `src/androidTest`; JUnit deps exist but nothing runs. Build gates are manual.
- Build config: Compose BOM `2026.09.00`, Kotlin `2.4.21`, `kotlin("plugin.serialization") 2.3.21` (existing mismatch — do not "fix" it as part of this work), AGP 9.4.1, Gradle 9.6, JDK 21, compile/target 37, minSdk 24, Coil 3.6.3, tv-material 1.1.0.
- Backdrop 2.0.1 is a Compose Multiplatform artifact built against Kotlin 2.3.x / CMP 1.11 (per upstream release notes). Kotlin 2.4.21 should read 2.3 metadata; whether the CMP runtime artifacts conflict with BOM `2026.09.00` is exactly what P0 must prove.

---

## 3. Dependency map (overview)

```
P0  ── Gate 0 ──►  P1  ── Gate 1 ──►  P2  ── Gate 2 ──►  P3  ── Gate 3 ──►  P4  ── Gate 4 ──►  P5
 │                  │                  │                  │                  │
 │                  │                  ├── P2-A primitives ──► all P2 screens
 │                  │                  ├── P2-B browse data ──► P2-F browse UI (parallel with P2-A)
 │                  │                  └── font/glass/tier consumed everywhere
 │                  └── scaffold + tab bar + routes
 ├── dependency proof (blocks all glass UI)
 ├── font strategy (blocks all type UI)
 ├── TV theme tokens (blocks everything visual)
 └── glass tier + reduce motion (cross-cutting, consumed by P2–P5)
```

Critical path: **P0-DEP → P0-TIER → P0-FONT → P0-THEME → P1-SCAFFOLD → P2-A → P3 → P5**.
Longest lead item: Backdrop resolution (external artifact behaviour, unknown until built).

---

## 4. Phase 0 — Foundations (no visual design work)

Goal: prove the two external risks and land the cross-cutting primitives. The app must still build and look *old* at the end of P0, except that TV colors/typography change.

### P0-DEP — Backdrop dependency resolution (highest risk; blocks all glass work)

| | |
|---|---|
| Depends on | nothing |
| Blocks | P0-TIER, P0-THEME, P1-TABBAR, every glass component |
| Files | `gradle/libs.versions.toml`, `app/build.gradle.kts` |
| Size | M (unknown-unknowns) |

Steps:
1. Capture baseline dependency graph: `./gradlew :app:dependencies --configuration debugRuntimeClasspath > /tmp/deps-before.txt`.
2. Add `backdrop = "2.0.1"` version + `backdrop = { module = "io.github.kyant0:backdrop", version.ref = "backdrop" }` library; `implementation(libs.backdrop)`.
3. Re-run the dependency task to `/tmp/deps-after.txt`; `diff` and inspect:
   - `./gradlew :app:dependencyInsight --configuration debugRuntimeClasspath --dependency io.github.kyant0:backdrop`
   - `./gradlew :app:dependencyInsight --configuration debugRuntimeClasspath --dependency org.jetbrains.compose.runtime`
   - `./gradlew :app:dependencyInsight --configuration debugRuntimeClasspath --dependency androidx.compose.runtime`
4. Decision tree:
   - **A. No conflict, one Compose runtime** → keep as-is.
   - **B. CMP `org.jetbrains.compose.*` artifacts appear alongside AndroidX** → add `exclude(group = "org.jetbrains.compose...")` on the backdrop dependency (exact groups per graph), rebuild, confirm no `NoClassDefFoundError` at runtime.
   - **C. Binary/API incompatibility (build error, or `NoSuchMethodError` in the probe)** → pin the Backdrop version matching the project's Compose, or **vendor** the two modifier source files (Apache-2.0, keep `NOTICE` attribution). Vendoring is the last resort and needs approval.
5. Device proof (requester): P0-DEBUG probe below.
6. Record the resolved outcome + versions in `apple-music-tv-spec.md` §8.1 (append "resolved as of …").

Exit check: `assembleDebug` succeeds; dependency tree shows a single Compose runtime; on the API 34 TV the probe renders blurred chrome **and** a lens edge. If C is chosen, the probe passes with the vendored implementation and the decision is written down.

**P0-DEBUG (small, temporary):** a debug-only glass probe. Add `TvGlassProbe()` rendering a `chrome` bar and `panel` over a scrollable image list; reach it from the TV branch of `MainActivity` only when `BuildConfig.DEBUG` and the launch intent has extra `glassProbe=true`. Removed in P5. This is the only way to validate blur/lens before P1/P2 exist.

### P0-FONT — Font strategy + SF Pro acquisition (blocks all type UI)

| | |
|---|---|
| Depends on | nothing (parallel with P0-DEP) |
| Blocks | P0-THEME, all screens |
| Files | `app/build.gradle.kts` (flavors), `app/src/sfpro/res/font/*`, `app/src/foss/res/font/*`, `NOTICE`/`README` |
| Size | M |

Steps:
1. Add a `fonts` flavor dimension with two flavors: `sfpro` (default for local/Play/TV distribution) and `foss` (ships an OFL-licensed face: Inter Display/Text or similar). Both flavors provide the **same 7 resource names**:
   `chora_display_regular`, `chora_display_medium`, `chora_display_semibold`, `chora_display_bold`, `chora_text_regular`, `chora_text_medium`, `chora_text_semibold`.
2. Acquire SF Pro Display/Text OTFs; verify each file (`file`, `fc-scan` or `ttx -l`), record source URL + SHA-256 in `NOTICE`.
3. Add the `foss` face files under the same names; do not rely on metric compatibility — `TvTypography` applies the spec's negative tracking, and layouts use max-lines/ellipsis, never exact line counts (spec §14.4 already flags this).
4. `NOTICE`: "SF Pro is a trademark of Apple Inc. Bundled in the `sfpro` flavor at the project owner's discretion; the `foss` flavor ships an open font." F-Droid builds use `foss`.
5. Confirm both variants build: `./gradlew :app:assembleSfproDebug :app:assembleFossDebug`.
6. Do **not** rename the application ID per flavor (keeps installs/deep links stable); TV is not flavor-specific.

Exit check: both variants compile; `sfpro` variant's fonts render in a trivial preview/on-device text sample; `foss` variant renders the open face; `NOTICE` present; no font resource is referenced from phone theme code.

### P0-TIER — API-tier glass abstraction (cross-cutting)

| | |
|---|---|
| Depends on | P0-DEP (for real effects), P0-FONT (theme locals) |
| Blocks | every glass consumer, all gates |
| Files | `ui/theme/tv/TvGlass.kt`, `ui/theme/tv/TvTheme.kt` |
| Size | S/M |

Steps:
1. `enum class GlassTier { Full, BlurOnly, Solid }` and `internal fun resolveGlassTier(sdkInt: Int): GlassTier`:
   - `>= 33` → `Full`
   - `31..32` → `BlurOnly` (no `lens`)
   - `< 31` → `Solid` (translucent fill + hairline; no blur/lens)
2. `LocalGlassTier` CompositionLocal, provided by `TvTheme`; a debug-only override (`LocalGlassTierOverride`) used by the probe and the P5 fallback check.
3. All glass presets (`chrome`, `panel`, `pill`, `overlay`, `chip`) resolve effects **inside** this file. Screens/components never branch on `SDK_INT`.
4. Enforce with a grep gate at every phase: `rg "SDK_INT|VERSION_CODES" app/src/main/java/com/craftworks/music/ui/theme/tv app/src/main/java/com/craftworks/music/ui/tv app/src/main/java/com/craftworks/music/ui/elements/tv/apple app/src/main/java/com/craftworks/music/ui/screens/tv` must return only `TvGlass.kt`.
5. Same file owns the "freeze sampling during focus animation" flag and the simultaneous-surface budget helpers (spec §4.5/§10).

Exit check: debug override renders `Solid` on the API 34 device with no crash and acceptable contrast; probe confirms `BlurOnly` path compiles (forced override); grep gate clean.

### P0-THEME — TV design tokens + reduce-motion plumbing

| | |
|---|---|
| Depends on | P0-FONT, P0-TIER |
| Blocks | all P1–P4 UI |
| Files | `ui/theme/tv/TvColors.kt`, `TvTypography.kt`, `TvShapes.kt`, `TvTheme.kt`, `TvMotion.kt`, `ReduceMotion.kt`; `ui/theme/Theme.kt`; `managers/settings/AppearanceSettingsManager.kt`; `MainActivity.kt` (decor background) |
| Size | M |

Steps:
1. `TvColors`: the spec §4.2 palette as a data class + `LocalTvColors`; dark-only. No Material You calls on TV.
2. `TvTypography`: `FontFamily` built from the neutral font resources, weight-mapped; the §4.3 scale as a data class; `LocalTvTypography`.
3. `TvShapes`: radii tokens; `LocalTvShapes`.
4. `TvMotion`: named specs (focusIn/out springs, push/pop, panel, artwork crossfade, dialog) + `LocalTvMotion`; a `duration(ms)` helper that returns `0` under reduce motion (except opacity fades ≤ 120 ms).
5. `ReduceMotion.kt`: `LocalReduceMotion` built from `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` **or** the DataStore flag; recomputed on resume/preference change.
6. Add `reduceMotion` + `largeText` keys/flows/setters to `AppearanceSettingsManager` (no UI yet; C4).
7. `MusicPlayerTheme`: TV branch → `TvTheme { content() }` (no `tv.MaterialTheme` wrapper). Phone branch untouched.
8. `MainActivity` TV branch: replace the `tv.MaterialTheme.colorScheme.background` decor color with `Color.Black` (the old theme call would otherwise resolve to defaults once `TvTheme` is in place).
9. Leave every existing TV screen rendering with old `tv-material3` components — they will pick up the new colors/type inconsistently. **Accepted for P0 only**; P1/P2 replace them. Do not "fix" old screens here.
10. `NavGraph.kt` remains untouched in P0; TV transitions still use the default motion scheme.

Exit check: build both flavors; TV app still navigates and plays; text visibly uses the new type; colors are neutral/red (not purple) on TV; phone screenshots unchanged; animator scale 0 makes no motion anywhere (already true for old screens).

### Gate 0 (requester sign-off)

- Builds: `assembleSfproDebug` + `assembleFossDebug` pass.
- Probe: blur + lens on the API 34 TV; forced `Solid` path acceptable.
- `TvTheme` applied on TV; phone unchanged; dependencies recorded.
- Decision recorded for Backdrop path A/B/C and font flavor scheme.
- **No P1 work starts until Gate 0 is approved.**

---

## 5. Phase 1 — Single-scaffold shell

Goal: five tabs, one chrome host, no drawer, no visual redesign of content yet. Phone path unchanged.

### P1-NAV — Routes + tab mapping

| | |
|---|---|
| Depends on | P0-THEME |
| Blocks | P1-SCAFFOLD, P1-TABBAR, P2-LIBRARY/BROWSE |
| Files | `data/model/Screen.kt` |
| Size | S |

- Add `@Serializable data object Browse`, `@Serializable data object Library`. No renames, no argument changes.
- `TvTab` enum (`ListenNow`, `Browse`, `Radio`, `Library`, `Search`) with a mapping from current destination → tab, covering: `Home`, `HomeLists`; `Browse`; `Radios`; `Library`, `AlbumList`, `AlbumDetails`, `ArtistsList`, `ArtistDetails`, `PlaylistList`, `PlaylistDetails`, `Songs`, `Settings` graph (all `S_*`); `Search`.
- Map tab → route for `navigate`: Listen Now → `Screen.Home`, Browse → `Screen.Browse`, Radio → `Screen.Radios`, Library → `Screen.Library`, Search → `Screen.Search`.

Exit check: unit-level reasoning only (no tests); compiles; route sealing unchanged for phone.

### P1-SCAFFOLD — `TvAppScaffold` around the TV NavHost

| | |
|---|---|
| Depends on | P1-NAV, P1-TABBAR (interface), P0-TIER |
| Blocks | every TV screen's padding/focus behaviour |
| Files | `ui/tv/TvAppScaffold.kt`, `NavGraph.kt`, `MainActivity.kt` |
| Size | M |

Steps:
1. `TvAppScaffold(currentTab, onTabSelected, chromeVisible, content)`:
   - `Box(bg.canvas)` with the Backdrop **source** for the tab bar (samples underlying content).
   - `TvTopTabBar` aligned top-center, 24dp from the safe top.
   - `CompositionLocalProvider(LocalTvTopInset provides 120.dp)`; content is drawn edge-to-edge beneath the bar.
   - `BackHandler`: only intercepts the tab-focus case (§5.4 rule 4); everything else passes through.
2. In `SetupNavGraph`, TV branch:
   - derive `currentTab` from `navController.currentBackStackEntryAsState()` + `TvTab.fromRoute(...)`;
   - `chromeVisible = currentRoute !is NowPlayingLandscape`;
   - wrap the **`NavHost`** in `TvAppScaffold`, keep `Modifier.fillMaxSize().padding(bottom = bottomPadding)` (TV passes 0);
   - tab click → `navigate(route) { launchSingleTop = true; restoreState = true; popUpTo(Screen.MainGraph) { saveState = true } }` (verify nested-graph behaviour on device; nested graphs may need `popUpTo` on the graph itself);
   - **delete the 8 `TvSideNavigation(...)` wrappers** (lines 145, 181, 193, 208, 242, 274, 311, 430) and their now-unused imports; delete `TvSideNavigation` from `MainActivity.kt` once nothing references it;
   - ~~fix `Screen.Search`: phone branch renders `SearchScreen`~~ — **premise was false (approved at P1
     close)**: no phone `SearchScreen` has ever existed (`git log --all --diff-filter=D '
     '*SearchScreen.kt'` is empty, and no phone route references `Screen.Search`). The phone branch
     therefore renders no search screen at all; TV renders `TvSearchScreen`. On phone, `Search`,
     `Browse` and `Library` redirect to Home, so a stray intent cannot render TV chrome. No new phone
     UI is in scope. See Appendix E2.2.
   - TV transitions: replace the top-level `animationSpec` usage for TV with explicit `TvMotion` specs; leave the phone `MaterialTheme...motionScheme` path intact.
3. `MainActivity` TV branch: no other changes (no mini-player, no sheet).
4. Verify `HomeLists` on TV: temporarily (P1) it will render inside the new scaffold but still with the phone content. Flagged; fixed in P2-HOMELISTS.

Exit check: TV shows five tabs on Home, lists, details, settings, search; no drawer anywhere (`rg TvSideNavigation` returns nothing); tab switching and Back behave per §5.4; phone nav/appearance unchanged.

### P1-TABBAR — `TvTopTabBar`

| | |
|---|---|
| Depends on | P0-THEME, P0-TIER, P1-NAV |
| Blocks | Gate 1 sign-off |
| Files | `ui/tv/TvTopTabBar.kt`, `ui/tv/TvFocus.kt` (minimal focus helper) |
| Size | M |

- Text-only labels, order fixed, `tab` style, 40dp gaps, glass pill focus, red dot active, `chrome` preset, tint 10%→chrome on scroll (scroll value supplied by screens later; default chrome for lists).
- Focus: scale 1.04, ring, 180/140 ms springs, reduce-motion fallback; explicit `focusProperties` so Down enters content.
- Search tab sets a `focusSearchOnEntry` flag consumed by `TvSearchScreen` (P2).

Exit check: tab bar focus traversal is clean with D-pad; active tab correct on every route; blur visible through the bar; reduce-motion variant static.

### P1-LIBRARY/BROWSE placeholders

- `TvLibraryScreen` placeholder: 4 category tiles → existing TV list screens; Settings row → `SettingsList`.
- `TvBrowseScreen` placeholder: "Browse" title + empty state; route registered.
- Both render inside the scaffold with Library/Browse tab active.

Exit check: all five tabs lead somewhere; no dead routes.

### Gate 1
- Five tabs work; no drawer; phone unchanged; **Search on phone is not "restored" — there was never a
  phone search screen to restore (false premise, see P1-SCAFFOLD step 2 and Appendix E2.2); the agreed
  behaviour is a redirect to Home**; Back rules verified.
- Screenshots: each tab, a detail screen, Search with IME.

---

## 6. Phase 2 — Components and content screens

### P2-A — Primitive components (blocks all P2 screens and P3)

| Task | Files | Depends on | Exit check |
|---|---|---|---|
| P2-A1 `TvFocus` | `ui/tv/TvFocus.kt` | P0-TIER | focus scale/lift/halo/parallax on a dummy row; tv-material focus scale explicitly disabled |
| P2-A2 `TvMediaTile` + artwork | `ui/elements/tv/apple/TvMediaTile.kt`, `TvArtwork.kt` | P2-A1, P0-FONT | full-res hero + 220dp rail requests; gradient/blurred placeholder; parallax clipped; long titles ellipsize |
| P2-A3 buttons | `TvGlassButton.kt`, `TvGlassIconButton.kt` | P2-A1 | primary red / secondary glass / disabled; D-pad + TalkBack labels |
| P2-A4 structure | `TvSectionHeader.kt`, `TvSkeleton.kt`, `TvEmptyState.kt`, `TvListRow.kt`, `TvEqualizerGlyph.kt`, `TvContextMenu.kt` | P2-A1 | skeleton shimmer static under reduce motion; menu focus trap |
| P2-A5 details header | `TvDetailHeader.kt` | P2-A2/A3 | scroll collapse 56→34sp, artwork 360→120dp, blurred art tint (pre-blurred bitmap, not backdrop) |
| P2-A6 blur helper | `ui/elements/tv/apple/ArtworkBlur.kt` | P0-TIER | cached per `MediaItem.id`; no per-frame blur; `RenderEffect` on 31+, no-op fallback below |

### P2-B — Browse data layer (parallel with P2-A)

| Task | Files | Depends on | Exit check |
|---|---|---|---|
| P2-B1 `GenreRepository` | `data/repository/GenreRepository.kt` | none | wraps `MediaProvider.getGenreList`; returns `MediaItem`-friendly genre model or a small UI model |
| P2-B2 `BrowseViewModel` | `ui/viewmodels/BrowseViewModel.kt` | P2-B1 | mirrors `HomeScreenViewModel` (async + `DataRefreshManager`); rails skip empty sections; genre list state |
| P2-B3 genre browse action | repository method using existing album queries + genre filter (provider-supported only) | P2-B1 | selecting a genre yields albums via existing `AlbumListQuery`; if a provider can't filter, hide the genre action |

### P2-C — Listen Now

| | |
|---|---|
| Depends on | P2-A1/A2/A3/A4/A6 |
| Files | `ui/screens/tv/TvHomeScreen.kt` (rewrite) |
| Size | L |
| Exit | hero + 4 rails with real data; See All pushes `HomeLists`; skeletons; error card; 5 s no jank on focus traversal |

### P2-D — Library hub + lists

| | |
|---|---|
| Depends on | P2-A2/A4, P1-LIBRARY |
| Files | `TvLibraryScreen.kt` (real), `TvAlbumScreen.kt`, `TvArtistScreen.kt`, `TvPlaylistScreen.kt`, `TvSongsScreen.kt` |
| Size | L |
| Exit | 2×2 category tiles + recently-added rail + Settings row; 5-column grids; long-press menu Play Next/Add to Queue actually queues (verify via queue panel in P3 or `adb` log if P3 not yet available) |

### P2-E — Detail screens

| | |
|---|---|
| Depends on | P2-A5, P2-C/D |
| Files | `TvAlbumDetailsScreen.kt`, `TvArtistDetailsScreen.kt`, `TvPlaylistDetailsScreen.kt` |
| Size | L |
| Exit | collapsing header; art tint; track rows; playing indicator; action row; scroll performance |

### P2-F — Browse UI

| | |
|---|---|
| Depends on | P2-A1/A2/A4, P2-B1/B2/B3 |
| Files | `TvBrowseScreen.kt` (real), `ui/elements/tv/apple/TvGenreChip.kt` if needed |
| Size | M/L |
| Exit | curated rails, genre chips, configurable grid; empty sections hidden; provider-absent empty state |

### P2-G — Radio

| | |
|---|---|
| Depends on | P2-A2 |
| Files | `TvRadioScreen.kt`, `ui/elements/tv/TvRadioCard.kt` |
| Size | S/M |
| Exit | 3-column 16:9 grid, LIVE chip, focus zoom/parallax; station playback works |

### P2-H — HomeLists TV

| | |
|---|---|
| Depends on | P2-A2/A4 |
| Files | `ui/screens/tv/TvHomeListsScreen.kt`; `NavGraph.kt` (TV branch for `Screen.HomeLists`) |
| Size | S |
| Exit | Verify from the rail itself: focus a Listen Now rail header's **"See All"** control, activate it, and confirm the **TV grid** (`TvHomeListsScreen`) renders — not the phone `HomeListsScreen`; Back returns with focus on the originating rail. (Amendment approved at P0 start: the check must exercise the See All path, not direct navigation.) |

### P2-I — resources for P2

- Add strings to the correct files (`strings_screens.xml` for tab names/sections, `strings_dialogs.xml` later): "Listen Now", "Browse", "Library", "See All", "Nothing to browse yet", "No results for %1$s".
- Add vector glyphs used by P2 (chevron, gear, magnifier, ellipsis, equalizer bars). Reuse existing drawables where they already exist.

### Gate 2
- Screenshots: Listen Now, Browse, Library, each list, each detail header (expanded + collapsed), Radio, Search.
- gfxinfo capture for a rail traversal + detail push.
- No `SDK_INT` outside `TvGlass.kt`; no phone file touched.

---

## 7. Phase 3 — Now Playing, lyrics, queue (highest UI risk)

### P3-1 — TV branch before phone background (C2)

| | |
|---|---|
| Depends on | P2-A1/A2/A3/P2-A6 |
| Files | `ui/playing/NowPlaying.kt`, `ui/playing/tv/TvNowPlaying.kt` (signature) |
| Size | S |

- At the top of `NowPlayingContent`: if TV → collect nothing phone-specific, call `TvNowPlaying(mediaController, metadata, navController, onBack = …)` and return.
- Keep phone path byte-identical below the branch (move, don't rewrite).
- Pass `navController` for Album/Artist/Library actions.
- Keep `keepScreenOn` behavior from the route (`NavGraph.kt` already handles it outside the composable); do not duplicate.

Exit check: TV Now Playing has a single black base (no phone palette tint), phone now playing unchanged.

### P3-2 — Full-bleed layout + background

| | |
|---|---|
| Depends on | P3-1, P2-A6, P0-TIER |
| Files | `TvNowPlaying.kt` |
| Exit | cover-scaled blurred artwork, 65% scrim, 560dp art, crossfade + scale on track change, parallax drift disabled under reduce motion, OLED variant |

### P3-3 — Transport + timer

| | |
|---|---|
| Depends on | P3-2, P2-A3 |
| Exit | glass pill transport, 2dp progress with ±10 s D-pad seek, auto-hide 5 s (single constant), any D-pad input reveals, no auto-hide under reduce motion, focus never disappears |

### P3-4 — Lyrics panel

| | |
|---|---|
| Depends on | P3-2, P2-A4 |
| Files | `ui/elements/tv/apple/TvLyricsPanel.kt`, `TvNowPlaying.kt` |
| Exit | right 40% glass panel; synced/unsynced/word-sync paths; active-line styling; empty state; art displaces 18%/0.94; Back closes; Left returns focus; existing `LyricsState`/`LyricsRepository` reused (no new parser) |

### P3-5 — Queue panel

| | |
|---|---|
| Depends on | P3-2, P2-A4, P2-D (for "Add from Library" route) |
| Files | `ui/elements/tv/apple/TvQueuePanel.kt`, `TvNowPlaying.kt` |
| Exit | reorder via `moveMediaItem`, remove, clear-after-current, jump-to-track; current row tinted; long-press + `⋯` menu; `savePlayQueue` debounced 1 s and non-blocking; empty state navigates to `Screen.Library` |

Implementation notes:
- Reuse the controller call patterns from `ui/playing/PlayQueue.kt` (line ~227) rather than inventing new queue state; the queue is already the controller's.
- Provider sync: call only when the provider exposes a non-null queue capability. `savePlayQueue` is abstract on `MediaProvider`, so every provider has an implementation; if a provider's is a no-op, treat success as best-effort. Wrap in `runCatching` + log; never surface failures.
- Do not add a new queue DataStore cache; the provider is the sync target.

### P3-6 — Transitions, focus restore, navigation

- Panel open/close focus restoration to the triggering pill.
- Shared-bounds-like open from the artwork where feasible; otherwise scale+fade (feasibility decided here, not promised).
- "Go to Album/Artist" closes Now Playing and pushes the detail route; verify Back stack depth does not loop.

### Gate 3
- Screenshots: Now Playing, + lyrics, + queue, OLED variant, reduce-motion variant.
- gfxinfo during track change + panel animation: no > 32 ms frames on the target TV (report actuals).
- Queue edits survive an app restart when the provider supports it.

---

## 8. Phase 4 — Settings, dialogs, onboarding

### P4-1 — Settings hub restyle

| | |
|---|---|
| Depends on | P2-A3/A4 |
| Files | `ui/screens/tv/TvSettingScreen.kt` |
| Exit | grouped sections (Account, Playback, Providers, Lyrics, Appearance, About); 76dp rows; no glass-heavy rows; focus ring only |

### P4-2 — Appearance prune + new toggles

| | |
|---|---|
| Depends on | P0-THEME (keys), P4-1 |
| Files | `ui/screens/tv/settings/TvSettingsAppearance.kt` |
| Exit | only OLED, standby, Reduce Motion, Large Text, lyrics auto-scroll/recenter/speed, provider logo; removed rows gone; values persist; reduce-motion toggle immediately affects animations (no restart) |

### P4-3 — Large Text behavior

| | |
|---|---|
| Depends on | P0-THEME, P4-2 |
| Files | `TvTheme.kt`, `TvAppScaffold.kt` (columns), tiles/screens as needed |
| Exit | 1.15× (capped 1.3 system) scale; no clipped titles; rail column count drops; settings rows grow |

### P4-4 — Dialogs

| | |
|---|---|
| Depends on | P2-A3 |
| Files | `ui/dialogs/tv/*` (inventory during the task; includes onboarding + provider setup dialogs) |
| Exit | overlay glass, scrim 45%, focus trap, primary red, Back = Cancel, error rows inline and legible |

### P4-5 — Provider setup + onboarding

| | |
|---|---|
| Depends on | P4-4, P2-A3 |
| Files | `ui/elements/tv/TvProviderCard.kt`, setup wizard composables, `OnboardingDialog` |
| Exit | 3-step wizard with system IME; provider cards focus/lift; errors inline; onboarding (if present) restyled without new steps |

### Gate 4
- Screenshots: settings hub, appearance, playback, providers, a dialog, provider wizard, onboarding.
- Confirm no removed appearance option is still reachable on TV.
- Phone settings untouched.

---

## 9. Phase 5 — Polish and verification

| Task | Depends on | Exit check |
|---|---|---|
| P5-1 Motion tuning per screen | Gate 3/4 | focus/tab/push/panel timings match §4.7 feel; no fight with D-pad repeat |
| P5-2 Glass tuning on device | Gate 2/3 | presets adjusted (blur/tint/lens) and written back into `TvGlass.kt`; ≤ 2 large surfaces |
| P5-3 Perf pass | P5-1/2 | gfxinfo < 5% janky, p90 < 16 ms; report numbers; fix top offenders |
| P5-4 A11y pass | P5-1 | TalkBack reads tiles/buttons; animator scale 0; Large Text 1.3×; focus order |
| P5-5 Fallback pass | P5-2 | forced `Solid` tier via debug override, no visual breakage; `BlurOnly` check |
| P5-6 Cleanup | all | remove probe + debug branch; delete dead `TvSideNavigation` code and unused imports/drawables; `rg` gates clean; `NOTICE` final |
| P5-7 Final QA | all | spec §12 checklist completed by requester; screenshots archived; phone smoke test |

Gate 5 / Done: spec §16 acceptance criteria all checked by the requester; no unresolved TODOs in TV code; one commit history per phase.

---

## 10. Cross-cutting rules applied to every task

1. **Phone freeze**: no edits to `HomeScreen.kt`, `NowPlayingLandscape.kt`, phone settings, `AnimatedBottomNavBar`, phone branches of `Theme.kt`/`NavGraph.kt` except inserting an `isTv` branch. Review diff by file list.
2. **No scattered API checks**: `SDK_INT` only in `TvGlass.kt`; debug override only there + probe.
3. **No scattered glass**: every translucent surface uses `Modifier.tvGlass(preset)`; no inline `blur()`/`RenderEffect` in screens.
4. **No scattered motion**: durations/springs from `TvMotion` unless a screen intentionally overrides (spec allows per-screen tuning; override must be commented with the screen name).
5. **Reduce motion**: every `animate*`/`Animatable` in TV code consults `LocalReduceMotion`; auto-hide disabled under it.
6. **Focus**: every new focusable uses `TvFocus` defaults; tv-material `Card`/`Surface` focus scale explicitly neutralized when still used.
7. **Text**: all TV text uses `TvTypography` tokens; no raw `fontSize` in screens except dev-time overrides removed before gate.
8. **Strings**: new user-visible text goes to `res/values/strings*.xml` with Crowdin-friendly names; no hardcoded English in composables.
9. **Commits**: one per task group; gate tags; no mixed phone/TV commits.
10. **Verification honesty**: no gate may be marked passed without the requester's build + screenshot/perf evidence. This agent cannot compile in this environment.

---

## 11. Risk register mapped to tasks

| Risk | Task that retires it | If it fires |
|---|---|---|
| Backdrop vs Compose/Kotlin skew | P0-DEP | option B exclusions → option C vendoring (approval) |
| SF Pro licensing / F-Droid | P0-FONT | ship `foss` flavor for F-Droid; `sfpro` for personal/Play |
| Blur cost at 4K / focus jank | P0-TIER + P5-2/P5-3 | freeze sampling, reduce surfaces, drop lens on large panels |
| tv-material3 focus scale conflict | P2-A1 | neutralize defaults; build on raw modifiers |
| Phone regressions | every gate | revert to last gate tag; phone branch diffs reviewed first |
| Queue provider sync variance | P3-5 | best-effort + silent log; local queue remains authoritative |
| Genre data gaps | P2-B1/B3 | hide genre section; rails still work |
| HomeLists gap | P2-H | TV grid; if deferred, See All stays phone UI (must be explicit, not silent) |
| Now Playing phone wrapper coupling | P3-1 | branch early; never duplicate `NowPlaying_Background` |
| No build/test automation | all gates | small phases + requester-run gates only |

---

## 12. Parallelisation (for a second pair of hands)

- **P0**: DEP and FONT are independent; TIER depends on both interfaces but can start after FONT resource names are fixed.
- **P2**: A (primitives) and B (Browse data) run in parallel. After A: C (Listen Now), D (Library/lists), G (Radio), H (HomeLists) can proceed independently; E depends on D; F depends on B + A.
- **P3**: panels (P3-4 lyrics, P3-5 queue) can be built in parallel once P3-2 lands; P3-6 needs both.
- **P4**: settings (P4-1/2/3) and dialogs (P4-4/5) are independent; both need P2-A3.

---

## 13. Explicit exclusions (not in this plan)

- Radio content redesign (still open question Q1 in the spec).
- Light-mode TV tokens.
- Hero auto-rotation.
- User-facing grid configuration (assumed developer constants).
- New tests/test infrastructure (repo has none; adding a suite is a separate request).
- Any MusicKit/Apple Music service work.
- Phone reskin.

---

## 14. Approval checklist

Please confirm or amend before P0 starts:

1. **C1 scaffold shape** — one `TvAppScaffold` around the TV `NavHost` (tab bar overlay + `LocalTvTopInset`) instead of eight per-destination wrappers. *(Still the single-scaffold refactor; deletes all 8 wrappers.)*
2. **C2** — early TV branch in `NowPlayingContent` so phone palette work never runs on TV.
3. **C4** — `reduceMotion`/`largeText` DataStore keys wired in P0; settings UI in P4.
4. **C5 font flavors** — `fonts` flavor dimension `sfpro`/`foss`, neutral resource names, no applicationId change, F-Droid builds `foss`.
5. **P0-DEP option C** — if version skew cannot be excluded, vendoring the Apache-2.0 Backdrop sources is acceptable (needs explicit approval at that point).
6. **P0-DEBUG probe** — temporary debug-only glass probe + `MainActivity` debug branch, removed in P5.
7. **Radio scope** — restyled existing stations grid (spec §6.3) is acceptable for v1.
8. **HomeLists** — adding `TvHomeListsScreen` in P2 is in scope.

Until this checklist is approved, no source files will be modified.

---

## Appendix A — P0-DEP execution log

Status: **dependency declared; Gradle-level and on-device proof pending (Gate 0 execution is the requester's).**

Changed files (only these):
- `Chora/gradle/libs.versions.toml` — `backdrop = "2.0.1"`; `backdrop = { module = "io.github.kyant0:backdrop", version.ref = "backdrop" }`.
- `Chora/app/build.gradle.kts` — `implementation(libs.backdrop)` next to the TV dependencies.

Environment limitation: this workspace has no JDK, no Android SDK and an empty Gradle cache (`java: command not found`), so `:app:dependencies` and `assembleDebug` cannot run here. No encode of "verified" applies to the items below.

### Artifact-level findings (from published Maven metadata)

- `io.github.kyant0:backdrop:2.0.1` → `backdrop-android:2.0.1` AAR, 131 KB, 63 classes in `com.kyant.backdrop.*`, `minSdkVersion 21`, Apache-2.0.
- Backdrop Android runtime deps: `kotlin-stdlib 2.4.10`, `org.jetbrains.compose.foundation:foundation 1.12.0`, `org.jetbrains.compose.ui:ui 1.12.0`, `org.jetbrains.compose.ui:ui-graphics 1.12.0`, `io.github.kyant0:shapes 1.2.1`, `org.jetbrains:annotations 26.1.0`.
- Compose BOM `2026.09.00` (Google Maven) pins `androidx.compose.runtime/ui/foundation 1.12.1`, material3 `1.4.0`.
- CMP 1.12.0 Android artifacts are **empty wrappers**: `foundation-android-1.12.0.aar` is ~6 KB with a 275-byte `classes.jar` and zero classes; all bytecode references in Backdrop resolve to `androidx.compose.*` (verified via class-file strings), not `org.jetbrains.compose.*`.
- CMP metadata contains **no `strictly` and no `reject`** constraints; only `requires` + atomic-group reasons. `foundation-android` requires `androidx.compose.foundation:foundation:1.12.0`, so Gradle will select BOM's `1.12.1` (patch upgrade).
- `org.jetbrains.compose.ui:ui-android:1.12.0` additionally brings `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.6` (JetBrains wrapper) and `androidx.lifecycle:*:2.9.x`; the project's lifecycle 2.11.0 wins on the AndroidX side.
- Backdrop API confirmed present in the AAR: `layerBackdrop` (`LayerBackdropModifierKt`), `drawBackdrop` (`DrawBackdropModifierKt`), `effects.blur`, `effects.colorFilter`, `effects.lens`, plus `highlight`, `shadow`, `innerShadow`. `lens` accepts `RoundedRectangularShape` **or** `CornerBasedShape` (error string: "Only RoundedRectangularShape or CornerBasedShape is supported in lens effects.").
- Kotlin metadata: artifacts built with Kotlin 2.4.10; project 2.4.21 is newer, so metadata is readable.

### Predicted decision-tree outcome

**Option A — clean resolve.** One AndroidX Compose runtime; CMP `org.jetbrains.compose.*` modules appear in the graph but are empty wrappers, so no duplicate classes. Expected skew: CMP 1.12.0 wrappers + AndroidX Compose 1.12.1 (BOM). Options B/C are not triggered by metadata and must not be applied pre-emptively.

### Gate commands (requester, machine with JDK 21 + Android SDK)

```bash
cd Chora
./gradlew :app:dependencies --configuration debugRuntimeClasspath > /tmp/deps-after.txt
./gradlew :app:dependencyInsight --configuration debugRuntimeClasspath --dependency io.github.kyant0:backdrop
./gradlew :app:dependencyInsight --configuration debugRuntimeClasspath --dependency androidx.compose.runtime
./gradlew :app:assembleDebug
```

Pass criteria:
1. `assembleDebug` succeeds (phone path unchanged).
2. Exactly one `androidx.compose.runtime:runtime` version resolves (expected `1.12.1`); no version of `androidx.compose.*` is downgraded to `1.12.0` by a strict CMP constraint.
3. Backdrop resolves to `backdrop-android:2.0.1` and `shapes:1.2.1`; no `Duplicate class` errors.
4. `org.jetbrains.compose.*` entries are present but resolve to empty wrapper AARs (expected, not a conflict).

If criterion 2 or 3 fails: apply option B (exclude `org.jetbrains.compose.*` groups on the Backdrop dependency) and repeat; if that fails, stop and request approval for option C (pin/vendor). Do not start P0-FONT until this is reported.

### Open item created by sequencing

The P0-DEBUG glass probe needs `TvGlass` (P0-TIER), so on-device blur/lens proof cannot happen inside P0-DEP as originally ordered. Sequence within P0 is therefore: P0-DEP metadata proof → P0-FONT → P0-TIER + probe → P0-THEME. Gate 0 evidence must include the probe result before P1 starts.

---

## Appendix B — P0-FONT execution log

Status: **written, not built** (no JDK/Android SDK here). Gate 0 font evidence still belongs to the requester.

> **Partly superseded by Appendix C (FONT-REWORK).** The approved follow-up moved the fonts
> out of `main` and into each flavor's own source set, so the overlay mechanism described
> below no longer exists; the resource contract (4 names) and the licensing stance still hold.

Approval decisions applied: Display-only family (no SF Pro Text optical size), dark-only tokens, neutral resource names alias one family.

### Resource contract — deviation from plan step 1 (4 names, not 7)

Display-only makes a separate Text family redundant, so only four names exist, each weighted:

```
res/font/chora_display_regular.ttf   400
res/font/chora_display_medium.ttf    500
res/font/chora_display_semibold.ttf  600
res/font/chora_display_bold.ttf      700
```

Text-sized styles reuse `TvFonts.Display`; negative tracking from the §4.3 scale compensates. `chora_text_*` names are **not** created. If a later gate shows Display is too tight below 20sp, adding a Text family is an additive change behind the same `TvFonts` seam.

### Mechanism

- `fonts` flavor dimension with `sfpro` and `foss`; no `applicationId` suffix, so installs, deep links and Play/F-Droid identity are unchanged.
- ~~`app/src/main/res/font/` holds the open fallback (Inter Display 4.1, OFL) for every variant.~~ Replaced in Appendix C: `app/src/foss/res/font/` holds it, `foss`-only.
- ~~`app/src/sfpro/res/font/` is an overlay point for licensed SF Pro Display files.~~ Replaced in Appendix C: each flavor owns its four files; nothing is overridden.
- `ui/theme/tv/TvFonts.kt` is the only Kotlin that touches `R.font.chora_display_*`; `TvTypography` (P0-THEME) consumes `TvFonts.Display`.
- `foss` ships the open face and no SF Pro, which is what F-Droid and CI build.

### Honest gap

SF Pro cannot be fetched here (Apple distributes it behind an Apple ID licence). At the time of writing **`sfpro` rendered Inter**, so a green `assembleSfproDebug` did not prove SF Pro was bundled. Resolved in Appendix C: the licensed files now exist locally, and the `sfpro` variant no longer compiles without them.

### Side effects to watch at the gate

- `assembleDebug` no longer exists once a flavor dimension is declared; builds are `assembleSfproDebug` / `assembleFossDebug`.
- F-Droid metadata outside this repo (`fdroiddata`) will need `gradle: ["foss"]` once this ships; not editable here.
- Bundled fallback adds ~1.66 MB of TTF, now to `foss` only (Appendix C). `sfpro` adds ~9.2 MB of OTF instead.
- Provisioning/wizard flows that assert on exact typography are unaffected: no phone code references the new resources.

### Gate commands

```bash
cd Chora
./gradlew :app:assembleSfproDebug :app:assembleFossDebug
unzip -l app/build/outputs/apk/foss/debug/app-foss-debug.apk | grep -i "res/font\|assets/licenses"
```

Pass criteria: both variants assemble; the APK contains exactly four `chora_display_*` resources plus `assets/licenses/Inter-OFL.txt`; no `chora_text_*`; phone UI unchanged; on-device sample confirms the intended face (SF Pro only after the licensed files exist).

---

## Appendix C — Batched P0 execution log (FONT-REWORK + TIER + THEME)

Status: **written, not built** (no JDK/Android SDK in the agent environment). Every gate
below still belongs to the requester.

### C1 FONT-REWORK — flavors own their fonts (no overlay)

Approved change: `sfpro` supplies the licensed SF Pro Display files from its **own** source
set and `foss` keeps Inter; the flavor split is the only mechanism. Resolved layout:

```
app/src/sfpro/res/font/chora_display_{regular,medium,semibold,bold}.otf   SF Pro (personal use, gitignored)
app/src/foss/res/font/chora_display_{regular,medium,semibold,bold}.ttf    Inter Display 4.1 (OFL, tracked)
app/src/main/res/font/                                                     removed
```

Why this and not the earlier "overlay by resource name": the two sets would otherwise share
resource names across `main`/flavor, which is a same-name override across different file
extensions (`.ttf` vs `.otf`) and depends on merge-order behaviour nobody verified. With
each flavor owning its files there is no conflict to reason about, and `foss` provably
cannot contain SF Pro.

Consequences applied in this change:

- `.gitignore` now has `/app/src/sfpro/res/font/`. The OTFs were untracked but **not**
  ignored, so the routine `git add -A` would have published ~9.2 MB of proprietary font.
- CI builds `foss` (`assembleFossDebug`, artifact `apk/foss/debug/app-foss-debug.apk`).
  `sfpro` cannot build on CI: its fonts are, by design, not in the repo.
- A clean checkout has no `app/src/sfpro/res/font/`, so `assembleSfproDebug` fails with
  unresolved `R.font.chora_display_*`. Deliberate; documented in `FONTS.md` and `NOTICE`.
- `TvFonts.kt` stays a single definition in `main` (the `R` ids resolve per variant); no
  per-flavor copy of the file and no resource IDs anywhere else in the codebase.

### C2 P0-TIER — glass abstraction

`ui/theme/tv/TvGlass.kt`:

- `GlassTier { Full, BlurOnly, Solid }`; `resolveGlassTier(sdkInt)` = ≥33 Full, 31–32
  BlurOnly, <31 Solid. `currentGlassTier()` is the only `Build.VERSION` reader.
- `LocalGlassTier` (default `Solid`), `LocalGlassFrozen` for the freeze-sampling flag,
  `GlassSurfaceBudget` (2 large surfaces + 3 pills).
- `TvGlassPreset` + `TvGlassPresets.{chrome,panel,pill,overlay,chip}` with the §4.5
  blur/tint/lens/radius values.
- `Modifier.tvGlass(backdrop, preset, tier, shape)`: `Solid` or frozen → translucent fill
  only; otherwise `drawBackdrop` with blur, plus lens when the tier allows and the preset
  declares one. Lens is skipped for `overlay` (spec §4.5).
- Grep gate result: `SDK_INT|VERSION_CODES` appears only in `TvGlass.kt` under
  `ui/theme/tv`; no TV screen/component reads it.

Deviation: the plan's `LocalGlassTierOverride` was not added. The debug override is the
probe's launch extra `glassTier=…`, which re-provides `LocalGlassTier` for the probe subtree;
a second, unused composition local would have been dead API.

### C3 P0-THEME — tokens, motion, reduce motion

New files in `ui/theme/tv/`: `TvColors.kt`, `TvTypography.kt`, `TvShapes.kt`, `TvMotion.kt`,
`ReduceMotion.kt`, `TvTheme.kt`.

- `TvColors` — full §4.2 palette as an immutable data class + `LocalTvColors`. Dark-only;
  no dynamic color on the TV path.
- `TvTypography` — `TvTypeScale` with all 13 §4.3 styles built on `TvFonts.Display`
  (negative tracking, no Text optical size) + `LocalTvTypography`.
- `TvShapes` — radius tokens; `TvDimens` carries the §4.4 spacing/size/focus constants.
- `TvMotion` — named specs for every §4.7 interaction + `LocalTvMotion`; `spec()` snaps and
  `duration(ms)` returns 0 under reduced motion, except fades ≤ 120 ms.
- `ReduceMotion.kt` — `LocalReduceMotion` = system `ANIMATOR_DURATION_SCALE == 0` **or** the
  in-app DataStore flag; the system value is re-read on `ON_RESUME`.
- `AppearanceSettingsManager` — `reduceMotionFlow` / `largeTextFlow` + setters, no UI (C4).
- `MusicPlayerTheme` TV branch is now `TvTheme { content() }`; phone branch untouched.
- `MainActivity` TV branch sets the decor to `Color.Black` and can host the probe.

Deviation worth recording: `TvTheme` still provides `androidx.tv.material3.MaterialTheme`,
now with the red/neutral scheme. The plan said "no tv.MaterialTheme wrapper", but the
un-rewritten TV screens read that theme; without it they fall back to the library's purple
defaults, which the P0-THEME exit check explicitly forbids. It is one provider, replaced by
the token locals as P1/P2 rewrite screens, and it disappears with the last tv-material3
screen.

### C4 Probe (P0-DEBUG)

`ui/theme/tv/TvGlassProbe.kt`: chrome bar + panel + 3 pills over a scrolling gradient list,
labelled with the active tier and its blur/lens support. Reachable only when
`BuildConfig.DEBUG` and the intent carries `glassProbe=true`. Removed in P5.

```bash
# install a debug build and launch the probe
./gradlew :app:installSfproDebug
adb shell am start -n com.craftworks.music/.MainActivity --ez glassProbe true

# force a tier for the fallback checks (Solid = API<31 simulation)
adb shell am start -n com.craftworks.music/.MainActivity --ez glassProbe true --es glassTier Solid
adb shell am start -n com.craftworks.music/.MainActivity --ez glassProbe true --es glassTier BlurOnly
```

Pass: blur visible under `chrome`/`panel`, a lens edge on the API 33+ device, `Solid` gives
readable translucent surfaces with no crash, and the phone app is unchanged.

### C5 Verification status

Not compiled here. Static checks done: no `R.font` reference outside `TvFonts.kt`, no
`SDK_INT` outside `TvGlass.kt` in the TV tree, `Theme.kt` no longer references
`tv.material3` directly, SF Pro files untracked and ignored, Inter files moved with `git mv`.

### C6 CI result on `4f0f180` (first compile of P0)

CI ran `:app:assembleFossDebug` and failed at `compileFossDebugKotlin` with exactly six
`UNRESOLVED_REFERENCE` errors, all in the two Backdrop call sites:

| Site | Wrong | Correct |
|---|---|---|
| `TvGlass.kt` | `com.kyant.backdrop.blur` | `com.kyant.backdrop.effects.blur` |
| `TvGlass.kt` | `com.kyant.backdrop.lens` | `com.kyant.backdrop.effects.lens` |
| `TvGlassProbe.kt` | `com.kyant.backdrop.rememberLayerBackdrop` | `com.kyant.backdrop.backdrops.rememberLayerBackdrop` |
| `TvGlassProbe.kt` | `com.kyant.backdrop.layerBackdrop` | `com.kyant.backdrop.backdrops.layerBackdrop` |

Verified against the published `backdrop-2.0.1-sources.jar`
(`sha256 8277e21a2dc270931a3dfb175805dac5303f541e20e01bf05d1a714e8f6c032a`, from
Maven Central `io/github/kyant0/backdrop/2.0.1/`), not from memory. Confirmed signatures:

- `@Composable fun rememberLayerBackdrop(graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
  onDraw: ContentDrawScope.() -> Unit = { drawContent() }): LayerBackdrop` — `backdrops/LayerBackdrop.kt`
- `fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier` — `backdrops/LayerBackdropModifier.kt`
- `fun BackdropEffectScope.blur(@FloatRange(from = 0.0) radius: Float, edgeTreatment: TileMode = TileMode.Clamp)` — `effects/Blur.kt`
- `fun BackdropEffectScope.lens(@FloatRange(from = 0.0) refractionHeight: Float, @FloatRange(from = 0.0) refractionAmount: Float, depthEffect: Boolean = false, chromaticAberration: Boolean = false)` — `effects/Lens.kt`
- `fun Modifier.drawBackdrop(backdrop: Backdrop, shape: () -> Shape, effects: BackdropEffectScope.() -> Unit, highlight: (() -> Highlight?)? = { Highlight.Default }, shadow: … , onDrawSurface: (DrawScope.() -> Unit)? = null, …)` — `DrawBackdropModifier.kt`

Consequences recorded for P0 gate:

- `drawBackdrop` defaults `highlight`/`shadow` to `Highlight.Default`/`Shadow.Default`, so every
  glass surface gets a rim highlight and drop shadow unless a preset opts out with `null`.
  Deliberate: the spec's glass has a rim; revisit in P2 if the shadow reads too heavy on TV.
- `lens` **throws** `UnsupportedOperationException` for any shape that is not
  `RoundedRectangularShape`, `AbsoluteRoundedCornerShape` or `CornerBasedShape`. All five presets
  use `RoundedCornerShape` (a `CornerBasedShape`), so this is safe — but a future preset using a
  custom `Shape` would crash at draw time on API 33+.
- `blur`/`lens` self-disable via `isRenderEffectSupported()`/`isRuntimeShaderSupported()`, so the
  tier gate in `tvGlass` is a performance guard, not a crash guard.

Assumptions 2 and 3 below are now **proven** by the same run: `TvTheme.kt` (tv-material3
`darkColorScheme`) and `TvFonts.kt` (`R.font.chora_display_*` resolved by the flavor) compiled
clean, and Gradle accepted `flavorDimensions += "fonts"` with per-flavor `res/font`.

Fix commit `9dfa987` passed CI (`:app:assembleFossDebug`, run 43770337390). The uploaded
`app-foss-debug.apk` was inspected directly:

```
res/H3.ttf 00010000  416668   (== chora_display_medium.ttf)
res/LN.ttf 00010000  418856   (== chora_display_bold.ttf)
res/bk.ttf 00010000  408972   (== chora_display_regular.ttf)
res/pa.ttf 00010000  418540   (== chora_display_semibold.ttf)
.ttf = 4   .otf = 0   (any .otf in the whole APK: 0)
```

So the `foss` APK carries exactly the four Inter faces and no OpenType/CFF font. Note AGP
shortens `res/font/chora_display_regular.ttf` to `res/bk.ttf` in the packaged APK, so a
grep for `chora_display` **fails on a correct build** — check extensions and count instead.

### C7 Remaining unverified assumptions

1. Runtime behaviour of the probe at each tier (needs a device).
2. Whether `assembleSfproDebug` resolves the same `R.font` names (needs a machine holding the
   gitignored OTFs — the `foss` build proves the resource *names*, not the `sfpro` resource set).

## Appendix D — P1 (shell) execution log

Single commit, one shell for every TV destination. Phone branches are untouched except for the
Search/Browse/Library route fallback described in D3.

### D1 Files

| File | Change |
|---|---|
| `data/model/Screen.kt` | added `Browse`, `Library` objects; no renames, no argument changes |
| `ui/tv/TvTab.kt` | **new** — `TvTab` enum (label + route) and `NavDestination?.toTvTab()` / `isRootOf()` |
| `ui/tv/TvFocus.kt` | **new** — `LocalTvTopInset`, `LocalTvFocusSearchOnEntry`, `Modifier.tvFocusScale` |
| `ui/tv/TvTopTabBar.kt` | **new** — five labels, chrome glass, active dot, focus pill |
| `ui/tv/TvAppScaffold.kt` | **new** — canvas gradient, backdrop source, bar overlay, Back rule 4 |
| `ui/screens/tv/TvLibraryScreen.kt` | **new** — 4 category tiles + Settings row (placeholder) |
| `ui/screens/tv/TvBrowseScreen.kt` | **new** — title + empty state (placeholder) |
| `NavGraph.kt` | NavHost wrapped in `TvAppScaffold`; 8 `TvSideNavigation` wrappers deleted; TV transitions from `TvMotion`; `Browse`/`Library`/`Search` branches |
| `MainActivity.kt` | `TvSideNavigation` deleted with its imports (~230 lines) |
| `res/values/strings.xml`, `strings_screens.xml` | `nav_listen_now`, `nav_browse`, `nav_library`, two Browse empty-state strings |

### D2 Decisions and deviations

1. **`TvTab` lives in `ui/tv/`, not `data/model/Screen.kt`.** The plan put the enum next to the
   routes, but the destination→tab mapping needs `androidx.navigation` (`hierarchy`, `hasRoute`);
   keeping it out of the data model avoids a UI dependency there. `Screen.kt` only gained the two
   objects, as the spec's §5.2 text specifies.
2. **Content is edge-to-edge under the bar**, with `LocalTvTopInset` (96 + 24dp) available, per the
   P1 plan. The spec's §5.3 wording ("content slot with `PaddingValues(top = 120.dp)`") would leave
   the bar blurring nothing, because a padded scroll container clips its content at the padding
   edge. The Library and Browse placeholders already add the inset to their own top padding and
   show the intended pattern; the eight existing TV screens adopt it in P2, so **until then their
   first row sits behind the bar** (expected at Gate 1, not a regression).
3. **The phone has never had a search screen.** `git log --all --diff-filter=D -- '*SearchScreen.kt'`
   is empty and no phone route references `Screen.Search`; the only entry point was the TV drawer.
   The plan's "phone branch renders `SearchScreen`" cannot be satisfied without new phone UI, which
   the spec excludes. Instead `Search`/`Browse`/`Library` on phone redirect to Home, so a stray
   intent or deep link can no longer render TV chrome on a phone. Verified: no drawer symbols
   remain anywhere in `app/src`.
4. **Focus ring is an inset border for now.** §4.6 wants a 3dp ring sitting *outside* the clip plus
   an 8dp glow; drawing outside the bounds needs the P2-A primitive component, so the tab and the
   placeholder tiles draw a half-width ring inside their bounds.
5. **Scroll-driven tint (10% → chrome) is deferred.** It needs a scroll offset from each screen;
   the bar is always `chrome` in P1, which is exactly what the plan permits ("default chrome for
   lists").
6. **`LocalTvFocusSearchOnEntry`** is provided by the shell (true while Search is the active tab)
   and consumed by `TvSearchScreen` in P2 — no click handler on the screen itself.

### D3 Gate 1 checklist for the requester

```bash
./gradlew :app:assembleSfproDebug :app:assembleFossDebug
./gradlew :app:installSfproDebug
adb shell am start -n com.craftworks.music/.MainActivity
```

Manual: five tabs in order on Home, album/artist/playlist lists, details, settings and search; no
drawer anywhere; active dot follows the section (Library active inside Albums/Artists/Songs/
Playlists/Settings); tab bar blurs the content behind it; Search shows `TvSearchScreen` without
chrome; Back on a tab root focuses the bar, Back again exits; phone portrait/landscape and the
phone bottom rail unchanged.

Known, expected at Gate 1: existing screens' first row sits behind the bar (D2.2); HomeLists still
renders the phone list inside the new shell (fixed in P2-H); Now Playing still renders the old
screen with the bar hidden.

---

## Appendix E — P2 (components and screens) execution log

Commits (all pushed to `origin/master`):

| Commit | Content |
|---|---|
| `a9d25c2` | P2-A primitives |
| `ed298b7` | P2-A compile fixes + P2-B data layer |
| `213c918` | P2-C Listen Now + P2-H HomeLists grid |
| `18890f9` | P2-D library lists + Library hub |
| `9b88a0b` | P2-E details, P2-F Browse, P2-G Radio, P2-I inset + strings/glyphs |
| `b9b8379`, `6b6a0a7` | compile fixes (missing imports, glyph name) |
| `33e4204` | decode/perf fixes: remembered `ImageRequest`, quantised header size |

### E1 Files

New — `ui/elements/tv/apple/`: `TvArtwork.kt`, `TvMediaTile.kt` (+`TvArtistTile`), `TvGlassButton.kt`,
`TvGlassIconButton.kt`, `TvSectionHeader.kt`, `TvSkeleton.kt`, `TvEmptyState.kt`, `TvListRow.kt`
(+`TvTrackRow`), `TvEqualizerGlyph.kt`, `TvContextMenu.kt`, `TvDetailHeader.kt` (+`TvDetailMiniHeader`),
`ArtworkBlur.kt`, `TvMediaGrid.kt`.

New — data/UI: `data/repository/GenreRepository.kt`, `ui/viewmodels/BrowseViewModel.kt`,
`ui/screens/tv/TvHomeListsScreen.kt`.

Rewritten — `ui/tv/TvFocus.kt` (halo/lift/parallax/long-press/focus-on-appearance),
`ui/screens/tv/`: `TvHomeScreen.kt`, `TvLibraryScreen.kt`, `TvAlbumScreen.kt`, `TvArtistScreen.kt`,
`TvPlaylistScreen.kt`, `TvSongsScreen.kt`, `TvAlbumDetailsScreen.kt`, `TvArtistDetailsScreen.kt`,
`TvPlaylistDetailsScreen.kt`, `TvBrowseScreen.kt`, `TvRadioScreen.kt`; `ui/elements/tv/TvRadioCard.kt`.

Touched for the inset: `TvSettingScreen.kt`, `TvSearchScreen.kt`, `settings/TvSettingsAppearance.kt`,
`settings/TvSettingsPlayback.kt`, `settings/TvSettingsProviders.kt`; `NavGraph.kt` (HomeLists TV
branch, album list `mediaController`); `ui/playing/tv/TvNowPlaying.kt` (focus helper moved);
`strings_screens.xml`, `strings_actions.xml`; five new glyphs (`round_chevron_right_24`,
`round_chevron_left_24`, `round_search_24`, `round_more_horiz_24`, `round_play_arrow_24`).

### E2 Decisions and deviations

1. **The focus ring is now the real §4.6 halo.** `tvFocusHalo` strokes a slightly enlarged copy of the
   shape's outline, so the ring lands outside the clip instead of straddling it, with the 8dp glow
   outside that. Widths are divided by their own canvas scale so the ring stays exactly 3dp.
2. **The phone-Search premise was wrong (requester-approved).** No phone `SearchScreen` ever existed
   (`git log --all --diff-filter=D` empty, no `Screen.Search` reference in phone code); P1's redirect
   to Home is the final behaviour, not a shortcut. No new phone UI.
3. **`TvTab` stays in `ui/tv/`** because the mapping needs `androidx.navigation`.
4. **Long press is key-timed, not `combinedClickable`.** Foundation's clickable only turns key-ups
   into clicks (no key long press), so a held OK is timed in `onPreviewKeyEvent` and the closing
   key-up is consumed so the click does not also fire. Caveat: if focus leaves mid-press, that
   key-up is consumed nowhere and no click happens — cosmetic, not a lost action.
5. **Genre browsing cannot work yet — provider gap, not a UI gap.** *Every* provider's
   `getGenreList` is `TODO("Not yet implemented")` (`SubsonicMediaProvider`, inherited by
   `NavidromeMediaProvider`, and `LocalMediaProvider`), which throws `NotImplementedError`.
   `GenreRepository` catches it, returns `null`, and Browse hides the genre row entirely. Genre
   filtering passes the genre *name* in `genreIds` because `MediaModel.Genre` has no id and
   Subsonic's album query already treats that field as a name. Implementing the endpoint is a
   provider task outside P2-B's "wrap what exists".
6. **`TvMediaGrid` is shared** by albums/artists/playlists instead of three near-identical grids;
   sort labels are maps with a `sort.name` fallback, because the enums carry values with no string
   (`COMMUNITY_RATING`, `CRITIC_RATING`) and an exhaustive `when` would not compile.
7. **Secondary buttons do not sample a backdrop.** They draw the `pill` preset's tint directly,
   which is exactly what `GlassTier.Solid` does and keeps small controls inside the §4.5 glass
   budget. `TvGlassButton`/`TvGlassIconButton` therefore take no `Backdrop` parameter.
8. **Artwork tint is a cached 48px upscale**, not a per-frame blur: one small decode keyed
   `"${cacheKey}-blur"`, then `Modifier.blur(40.dp)` (RenderEffect, a no-op below API 31 where the
   48px source is already soft) plus a 65% scrim. No `SDK_INT` outside `TvGlass.kt`.
9. **The collapsing header quantises its decode.** The box shrinks continuously (360→120dp) but the
   request steps in 60dp increments, so a scroll costs ~5 decodes rather than one per frame.
   `TvArtwork` also `remember`s its `ImageRequest`, because a rebuilt request restarts the load.
10. **Hero is 540dp tall inside the 64dp screen margin**, not truly full-bleed; spec §6 says
    "full-bleed within its container", and the container here is the content column.
11. **Radio `LIVE` chip is shown on every station.** Providers expose no liveness metadata and every
    station in the list is a live stream, so the chip is decorative rather than conditional.

### E3 Gate 2 checklist for the requester

```bash
./gradlew :app:assembleSfproDebug :app:assembleFossDebug
./gradlew :app:installSfproDebug
adb shell am start -n com.craftworks.music/.MainActivity
```

Screenshots: Listen Now, Library hub, each list, each detail header expanded + collapsed, Browse,
Radio, Search.

Manual checks that only a device can answer:

- Focus ring is visibly **outside** the artwork edge with its glow, and nothing is clipped at a rail's
  first or last tile.
- Tiles lift and drift their artwork as focus moves along a rail; titles do not move.
- Long-press OK on a tile/row opens the context menu; "Play Next" / "Add to Queue" change the queue.
- See All on a Listen Now rail opens the TV grid, and Back returns with focus on the originating rail.
- Detail headers collapse to the mini header instead of jumping; scrolling a long list does not stutter.
- Skeleton shimmer is static with reduce-motion on; equalizer bars are static.
- Settings and Search now start below the tab bar.
- `gfxinfo` for a rail traversal and a detail push.

Not attempted, and not claimed: any device run, any screenshot, any gfxinfo capture.

---

## Appendix F — Density correction (Gate 1 follow-up)

### F1 The measurement

`wm size` override 1920×1080, `wm density` 320 → the app draws on a **960×540dp** canvas, not the
1920×1080 *points* the spec was authored against. Every spec point is therefore two dp here, and
passing the spec's numbers through unscaled doubled every label (Gate 1: "nav bar as if we took a
phone nav bar and increased its size to the fullest of the screen"). The diagnosis in the P2 report
was confirmed by the requester; the earlier assumption that the size was intended is withdrawn.

### F2 Tokens (`TvTypography`, `TvDimens`)

Type is halved, tracking halved with it so optical density holds:

| style | was | now | | style | was | now |
|---|---|---|---|---|---|---|
| displayHero | 76 | 38 | | caption | 20 | 10 |
| screenTitle | 56 | 28 | | button | 24 | 12 |
| sectionTitle | 34 | 17 | | tab | 22 | **13** |
| cardTitle | 28 | 14 | | npTitle | 48 | 24 |
| cardSubtitle | 22 | 11 | | npArtist | 30 | 15 |
| body | 24 | 12 | | lyricsActive/Inactive | 44/36 | 22/18 |

`tab` is the one deliberate exception: 13sp, not the arithmetic 11sp (requester's call — 11sp under
an icon is unreadable from a sofa).

Sizes reviewed case by case rather than halved blind:

| token | was | now | why |
|---|---|---|---|
| screenMarginHorizontal | 64 | 48 | 5% overscan of 960dp |
| screenMarginTop/Bottom | 48 | 32 | frames the smaller type |
| railGap / railTitleGap | 32/20 | 24/16 | follows type |
| tabBarHeight | 96 | 64 | requester: 56–64, 48 too tight with icon+label+dot |
| tabBarTopInset | 24 | 16 | |
| tabBarItemGap | 40 | 20 | requester |
| tabBarSidePadding | — | 120 | new; bar never closer than 120dp to an edge |
| tabIconSize / tabDotSize | — / 3 | 20 / 3 | new tokens |
| tileSize / artistTileSize | 220/200 | 160/150 | 5 tiles fit 864dp of content |
| buttonHeight / iconButtonSize | 56 | 44 | 12sp label + padding |
| iconInline/Control/Transport | 28/32/40 | 20/24/32 | **not** the halved 14/16/20 — too fine for a D-pad target at 3m |
| settingsRowHeight / trackRowHeight | 76/72 | 56/56 | fits 14sp + 32dp artwork |
| detailArtworkLarge / Collapsed | 360/120 | 260/96 | |
| heroHeight / heroTextWidth | 720 / 1280 | 540 / 560 | hero = canvas height; text column capped |
| focusGlowWidth / focusLift / focusParallax | 8/8/4 | 6/6/3 | scales with the tiles |
| focusRingWidth | 3 | 3 | legibility cue, not scale |
| emptyStateMaxWidth / menuWidth / menuMaxHeight | — | 520 / 400 / 400 | new; the menu's old `heightIn(max = 640)` exceeded the 540dp canvas |
| spacing ramp 4…64 | — | unchanged | rhythm, not scale |

### F3 Tab bar

Icons above labels (`TvTab.iconRes`; new `rounded_apps_24` for Browse, existing glyphs for the other
four), 13sp labels, 64dp tall, 20dp item gaps, 120dp side gutter. The gutter is applied on a wrapper
`Box` in `TvAppScaffold`, not as padding on the bar, so it caps the width and centres the measured
bar instead of adding hollow space inside the glass pill. Icon tint: accent when active, white when
focused, secondary otherwise; label + red dot stay as in §5.1.

### F4 Hardcoded-size audit (the grep the requester intended to run)

Files: `ui/tv`, `ui/elements/tv/apple`, `ui/screens/tv` — literals outside the token files.

Fixed here (they were duplicates of a token or would break at the new scale):

- `TvDetailHeader`: `lerp(56.sp, 34.sp)` → interpolates `screenTitle`→`sectionTitle`; the mini
  header's `sectionTitle.copy(fontSize = 28.sp)` override deleted (token only, per instruction);
  artwork shape `RoundedCornerShape(16.dp)`→`shapes.tile`, `8.dp`→`shapes.chip`; header padding
  64→48dp; quantise step 60→40dp over 96–260.
- `TvContextMenu`: shape 24dp→`shapes.panel`; `width(480)`/`heightIn(max=640)`→tokens (**the 640
  exceeded the canvas**).
- `TvEmptyState`: `widthIn(max = 720)`→`emptyStateMaxWidth`.
- `TvTopTabBar`: `size(3.dp)` dot→`tabDotSize`.
- `TvLibraryScreen`: 440×220 tile→380×170, shape 20→`shapes.tile`, icon 40→`iconControl`.
- `TvHomeScreen`: hero 540/900→tokens, skeleton title 280×32→160×24.
- `TvBrowseScreen`, `TvLibraryScreen`, `TvAlbumScreen`, `TvArtistScreen`, `TvSongsScreen`: skeleton
  widths 260/280→`tileSize`.
- Detail screens: `collapseDistance 360.dp`→`detailArtworkLarge` (three files).

Left alone, with reasons:

- `border(1.dp)` hairlines and `lift = 0.dp` — not sizes.
- `TvSkeleton`'s default `RoundedCornerShape(16.dp)` — equals `TvShapes.tile`; radii are unaffected
  by the canvas mismatch.
- `TvEqualizerGlyph` defaults 20/3dp — callers pass explicit values.
- Drawer-era `TvAlbumCard`/`TvArtistCard`/`TvPlaylistCard`/`TvSongCard` (128dp card, 52dp art, 16dp
  paddings) — only reachable from `TvSearchScreen`; flagged below rather than restyled.
- Pre-existing settings paddings (16/24/32dp) — spacing-ramp consistent.

### F5 Coupled fixes the halve forced

- **Grid tiles overflowed their cell.** `TvMediaGrid`/`TvHomeListsScreen` are 5-column grids of
  ~154dp cells; `TvMediaTile` was a fixed 220dp wide box. Added `fillWidth` (grid: fill the cell and
  stay square; rails keep a fixed `size`) and set it at the four grid call sites.

### F6 Flags for Gate 2 (not changed, deliberately)

1. `caption` is 10sp. Spec-faithful and it is only used for metadata lines, but it is the smallest
   text in the app — check it on the device before deciding.
2. `trackRowHeight`/`settingsRowHeight` 56dp with a 32dp thumbnail: fine for one 14sp line, tight if
   a row ever carries two.
3. Mini header is 48dp with a 48dp artwork — no vertical slack; it is an overlay, so it cannot grow.
4. Hero is exactly one canvas tall (540dp) and scrolls with the list; with the type halved the
   artwork dominates it. Keeping the hero was the requester's call.
5. Legacy search cards (`TvAlbumCard` etc.) now sit next to halved type; they were not restyled.

### F7 Commit

| Commit | Content |
|---|---|
| `d9cfe93` | type-scale halve, bar rework with icons, coupled size fixes, grid tile fill |

---

## Appendix G — P3 (Now Playing, lyrics, queue) execution log

Status: **implemented; compiled and packaged locally (`:app:assembleFossDebug`), not device-tested.**
This is the first phase in this project that was compiled inside the agent environment: a userland
JDK 21.0.13 + Android SDK platform 37.0 + build-tools 37.0.0 were installed under `$HOME/tc`, and
`local.properties` points at them (gitignored). No Gradle cache, SDK or toolchain is committed.

### G1 Files

New — `ui/elements/tv/apple/`:

| File | Content |
|---|---|
| `TvProgressBar.kt` | 2dp track/red fill, elapsed + remaining captions, ±10 s D-pad seek while focused |
| `TvLyricsPanel.kt` | right-hand `panel` glass, synced/word-synced/plain lyrics, empty state |
| `TvQueuePanel.kt` | Up Next list, context menu, reorder, remove, clear-after-current, debounced provider sync |

Rewritten — `ui/playing/tv/TvNowPlaying.kt` (full-bleed background, artwork/title block, displaced
panels, glass transport, focus/auto-hide rules). Modified — `ui/playing/NowPlaying.kt` (early TV
branch, C2), `ui/theme/tv/TvShapes.kt` (`TvDimens` now-playing tokens),
`res/values/strings_screens.xml` (19 `np_*` strings), `res/values/strings_actions.xml`
(`action_cancel`).

### G2 Decisions and deviations

1. **C2 landed as a true early branch.** `NowPlayingContent` returns into `TvNowPlaying` before any
   palette/DataStore collection, so the phone background no longer paints under the TV layout and no
   phone state is observed on TV. The phone branch below is unchanged.
2. **The lyrics panel does not reuse `LyricsView`.** That composable is phone Material3, reads the
   phone alignment/blur/bounce settings and is built around pull-to-refresh. `LyricsState` /
   `LyricsRepository` are reused unchanged; only rendering is new. Word sync colours words as the
   position passes them; plain lyrics are one scrollable block.
3. **No ±3-line blur/fade falloff in the panel.** The spec ties falloff to the lyrics-mode setting
   that P4 removes on TV (blur fixed to preset). Adding a blur distance now would be dead behaviour
   one phase later, so the panel uses opacity/weight only.
4. **Auto-hide cannot take focus away.** The 5 s timer hides controls only when no transport control
   holds focus; when it hides, focus moves to a focusable screen root, whose first key press reveals
   the controls and re-focuses play/pause. Reduce motion keeps the controls permanently visible, as
   the spec requires.
5. **Panel layout is wrap-width, fill-height.** The first draft put the panels in a full-screen
   container, which aligned them to the *left*; the container now wraps the panel width and is
   aligned `CenterEnd`.
6. **Provider queue sync is effectively a no-op today, by provider state, not by UI.** `savePlayQueue`
   is `TODO()` in `SubsonicMediaProvider` (inherited by Navidrome) and in `LocalMediaProvider`; the
   debounced 1 s write is wrapped in `runCatching` and logs only the exception class, so playback is
   never blocked. There is no capability flag on `MediaProvider` to test, so success is treated as
   best-effort exactly as the plan allows.
7. **"Go to Artist" appears only when the item carries an `artistId` extra**; "Go to Album" uses the
   `albumId` extra that song items already carry. Both navigate with
   `popUpTo<Screen.NowPlayingLandscape> { inclusive = true }`, so the detail screen does not stack on
   top of Now Playing.
8. **OLED protection mapping:** `LYRICS_ONLY` opens the lyrics panel and dims the wash (0.88), `MINIMAL`
   drops the wash to pure black, and the red progress fill is drawn at 80% in both. This preserves
   the two behaviours the old TV screen had, restated in the new layout.
9. **Gate fix outside P3 (one line):** `TvSettingsAppearance` read `Build.VERSION.SDK_INT` to enable
   the lyrics-blur switch. It now reads `LocalGlassTier.current.supportsBlur` (cross-cutting rule 2),
   and the commented-out refresh-ripple row that also matched the gate was deleted. The
   `SDK_INT|VERSION_CODES` grep is otherwise clean outside `TvGlass.kt`.
10. **Tokens, not literals:** artwork 280dp, info column 360dp, transport bar 360×48dp, play disc
    36dp, panels 40%/44% capped at 360/400dp, queue row 40dp with 22dp artwork, progress 2dp inside
    a 24dp focus target, wash drift ±6dp over 20 s, displacement 18%/0.94 — all in `TvDimens`.

### G3 Verification actually performed

```
cd Chora
JAVA_HOME=~/tc/jdk-21.0.13+9 ./gradlew :app:assembleFossDebug   # BUILD SUCCESSFUL, 2m 04s
# apk: app/build/outputs/apk/foss/debug/app-foss-debug.apk (20 MB)
```

- `:app:compileFossDebugKotlin` passes; `assembleFossDebug` produces the APK (resources, R class,
  string merge and dexing all exercised).
- Static: `TvNowPlaying(` has exactly two references (its declaration and the TV branch); the
  `SDK_INT|VERSION_CODES` grep is clean outside `TvGlass.kt`; new files contain no raw `fontSize`
  and no `R.font` references.
- **Not** built: `assembleSfproDebug` (licensed SF Pro files are not in this workspace, by design).
- **Not** done: any device/emulator run, screenshot, D-pad traversal, audio playback, gfxinfo capture,
  or queue-sync round trip. Those remain Gate 3 evidence for the requester.

### G4 Gate 3 checklist for the requester

```bash
./gradlew :app:installFossDebug     # or :app:installSfproDebug on a machine holding the OTFs
adb shell am start -n com.craftworks.music/.MainActivity
```

Now Playing: black base with a blurred wash, 280dp artwork, title/artist/album line, glass transport
with a 2dp red progress bar (left/right on the focused bar seeks ±10 s), controls fading after 5 s
and returning on any D-pad press; with reduce motion on, controls never fade and the wash does not
drift.

Lyrics: "Lyrics" opens a docked glass panel, artwork shifts left and scales down, the active line has
a red leading bar and auto-centres, word-synced tracks colour words as they play, unknown tracks show
"Lyrics not available for this track" with a refresh action; D-pad Left and Back both return focus to
the Lyrics pill.

Queue: "Up Next" lists the real controller queue with the current row tinted and equalized;
long-press or `⋯` gives Play Next / Move Up / Move Down / Remove / Go to Album(/Artist); Clear asks
for confirmation and removes everything after the current track; an empty queue offers "Add from
Library" and lands on the Library tab. Reordering with a Subsonic/Navidrome provider logs
`TV-QUEUE: Queue sync skipped (NotImplementedError)` — expected today.

Screenshots/perf still owed at this gate: now playing, + lyrics, + queue, OLED variant, reduce-motion
variant, gfxinfo for a track change and a panel animation.


## Appendix H — Phase 4 log (settings, dialogs, provider setup, release)

### What shipped

| Area | Result |
| --- | --- |
| Settings hub (`TvSettingScreen.kt`) | Grouped list instead of three wide buttons: Account (username, opens `NameDialog`), Playback, Media providers, Lyrics providers, Appearance, About (`BuildConfig.VERSION_NAME`). `TvSettingsSection` headers, `TvListRow` rows, `focusRestorer` on the list. |
| TV appearance (`TvSettingsAppearance.kt`) | Pruned to OLED mode, screen standby, Reduce Motion, Large Text, provider logo, lyrics autoscroll / recenter / speed. Theme, background style, nav/home ordering, alignment, blur, more-info, refresh animation and track numbers are gone from TV. |
| Large Text | `TvTypeScale.scaled(factor)` + `LocalTvLargeText`; `TvTheme` multiplies the ramp by `min(1.15, 1.3 / systemFontScale)`, `TvMediaGrid` drops 5 → 4 columns, `TvListRow`/`TvTrackRow` use `heightIn(min = …)` so rows grow. |
| Dialog shell (`TvDialogSurface.kt`) | One overlay component: scrim, `overlay` fill + hairline, 24dp radius, title/subtitle, trapped focus, panel swallows taps so the window underneath keeps focus, Back = dismiss. Plus `TvDialogOptionRow` (red dot for current value), `TvDialogTextField`, `TvDialogActions`. |
| Settings rows (`TvSettingsRows.kt`) | `TvSwitchRow`, `TvSettingsSliderRow` (labelled − / +, no Material slider), `TvSettingsSection`. Switch geometry (32×18/14 knob) comes from `TvDimens`. |
| Converted dialogs | `GenericListDialog`/`GenericCheckDialog` (covers every appearance and playback picker plus OLED/bitrate/format), `NameDialog`, `SelectProviderType`, `ModifyLrcLibProviderDialog`, `PlaylistDeletionConfirmationDialog`, `OnboardingDialog`. Provider cards now use `surface.1` and the TV type ramp. |
| Provider setup | Wizard entry, LRCLIB editor and the two creation sheets use the TV canvas and screen title; the creation sheets keep their existing focus-chain internals. |
| Release | `app/build.gradle.kts` reads `CHORA_KEYSTORE_FILE` / `CHORA_KEY_ALIAS` / `CHORA_KEYSTORE_PASSWORD` / `CHORA_KEY_PASSWORD` and signs release with one key when present, falling back to the debug key otherwise. CI builds `assembleFossRelease` and publishes the raw `app-foss-release.apk` as a GitHub Release asset (artifacts are always zipped) on tags or a manual run with `publish`. |

### Verification

- `:app:compileFossDebugKotlin` green after each group of edits.
- `:app:assembleFossDebug` **and** `:app:assembleFossRelease` green locally (release R8 minification included), so the CI job exercises the same task it publishes.
- Phone sources untouched: every file in this phase is under `ui/screens/tv`, `ui/elements/dialogs/tv`, `ui/elements/tv`, `ui/theme/tv`, plus `app/build.gradle.kts` and the workflow.

### Full-screen editors (why they are not 360dp sheets)

- `SongDialog`, `AddRadioDialog`/`ModifyRadioDialog` and the two provider creation forms are multi-column editors. They now share the TV palette and ramp — `bgCanvas` sheet, accent cursor/focus border, `labelPrimary`/`labelSecondary` text, `destructive` errors, `screenTitle`/`cardTitle`/`body` styles — but keep a full-bleed sheet instead of the 360dp dialog, because their field stacks need the canvas width. Their focus chains are unchanged.

### CI secrets

- `CHORA_KEYSTORE_BASE64` — base64 of the single release keystore.
- `CHORA_KEYSTORE_PASSWORD`, `CHORA_KEY_ALIAS`, `CHORA_KEY_PASSWORD` — matching credentials.
- Without them the workflow still passes and publishes a debug-key-signed APK with a notice.


## Appendix I — Phase 5 log (focus traversal, motion tokens, cleanup gates)

### The bug that started this phase

On the emulator, DOWN never left the focused tab bar into screen content: the tab stayed the only
focusable node with focus, on Library, Radios and Listen Now alike. Two causes, both now fixed in
code:

| Cause | Fix |
| --- | --- |
| The tab bar `Row` carried `focusGroup()`, which makes the bar a single focus area and swallowed the exit before 2D search could look outside it. | Removed. |
| Traversal was left entirely to two-dimensional search, which has to solve "nearest focusable below a floating 76dp pill that overlaps a full-screen lazy list". | Wired explicitly: `TvAppScaffold` attaches a `FocusRequester` to the content slot's focus group and hands it to `TvTopTabBar`, and every tab sets `focusProperties { down = contentFocusRequester }`. DOWN now enters the group, i.e. the first focusable child of the screen, whatever the screen's layout is. |

### Phase 5 task status

| Task | Status |
| --- | --- |
| P5-1 motion tuning | Done in code. `TvNowPlaying` no longer hardcodes specs: artwork crossfade, panel slide, panel tint, transport fade and the reduced-motion fades come from `TvMotion`. New tokens `stepChange` / `stepSlide` (onboarding step transitions, 350 ms cubic in-out), `panelSlide` (IntOffset flavour of `panelIn`) and `reducedFade` (the 120 ms fade that is still allowed under reduce motion). The TV onboarding transition also consults `LocalReduceMotion` now, which it did not before. |
| P5-4 a11y / focus order | Focus order fixed by the traversal change above. TalkBack pass and 1.3× Large Text still need a device. |
| P5-5 fallback pass | Code path unchanged and compiling (`GlassTier.Solid` / `BlurOnly` in `TvGlass.kt`); forcing a tier needs a device or a temporary debug override, which P5-6 removed. |
| P5-6 cleanup | `rg` over `ui/tv`, `ui/screens/tv`, `ui/elements/tv`, `ui/theme/tv`: no `TvSideNavigation`, no probe/debug tier override, no `SDK_INT` outside `TvGlass.kt`, no raw `fontSize` (the two hits derive from `TvTypography`), unused `focusGroup` import dropped. |
| P5-2 glass tuning, P5-3 perf numbers, P5-7 final QA | **Owed.** All three need a real device. `gfxinfo` on a software-rendered emulator measures the emulator, not the app. |

### Verification actually performed

- `:app:compileFossDebugKotlin` green after each edit group; `:app:assembleFossDebug` green
  (APK 20,521,480 B). Phone sources untouched — the six changed files are `ui/tv/TvAppScaffold.kt`,
  `ui/tv/TvTopTabBar.kt`, `ui/theme/tv/TvMotion.kt`, `ui/playing/tv/TvNowPlaying.kt`,
  `ui/elements/dialogs/tv/OnboardingDialog.kt` and the new `onboarding_setup_later` string.
- Earlier in P4 the build was installed and driven on an API 36 TV AVD: onboarding overview →
  provider step → "Set up later" → home, tab-to-tab LEFT/RIGHT, Library 2×2 tiles, Browse and
  Radios rendering. That is what stands behind "the onboarding is testable without a Navidrome
  server": the skip is an ordinary button, not a debug-only branch (`BuildConfig.DEBUG` is
  hard-wired `false` in the generated `fossDebug` config, so a debug gate would have compiled out).
- **The DOWN fix itself is compile-verified only.** This host has no KVM (uid not in the `kvm`
  group, `/dev/kvm` is `root:kvm 660`), so the AVD runs under TCG: boot 200 s, `pm install` 3 min,
  the app ANR-killed or silently killed before its first frame on most launches, `uiautomator dump`
  timing out on `UiAutomation`, and two runner restarts mid-run. Re-check on real hardware with:

```bash
adb install -r app-foss-debug.apk
adb shell am start -n com.craftworks.music/.MainActivity
adb shell input keyevent KEYCODE_DPAD_RIGHT   # walk to Library, CENTER to select
adb shell input keyevent KEYCODE_BACK         # a tab root sends focus into the bar
adb shell input keyevent KEYCODE_DPAD_DOWN    # expect the first content item to light up
```
