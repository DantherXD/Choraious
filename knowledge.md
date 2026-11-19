# Project knowledge — Chora

Chora is a lightweight Android music player (Kotlin + Jetpack Compose) that streams from
Subsonic / OpenSubsonic / Navidrome servers, local device storage, and internet radio.
Published on Google Play and F-Droid. Upstream: https://github.com/CraftWorksMC/Chora

The repository is cloned in `Chora/` (this workspace root is only a wrapper; all paths below
are relative to `Chora/`). Single Gradle module: `:app`, namespace and applicationId
`com.craftworks.music`.

The README states this is the author's first Kotlin project and the code is not well
organized — expect duplication, hardcoded values and dead scaffolding. Match what is there
rather than restructuring files you touch.

## Commands

Run from `Chora/`. Requires JDK 21 and an Android SDK with platform 37 (create
`local.properties` with `sdk.dir=...`; it is gitignored). Gradle comes from the wrapper
(9.6.0), AGP 9.4.1.

- Build debug APK: `./gradlew assembleFossDebug` (this is exactly what CI runs; there is no
  plain `assembleDebug` any more because of the `fonts` flavor dimension. `assembleSfproDebug`
  needs licensed SF Pro files that are gitignored and only exist on the owner's machine)
- Compile only: `./gradlew compileDebugKotlin`
- Install on device: `./gradlew installDebug`
- Lint: `./gradlew lintDebug`
- Tests: no `src/test` or `src/androidTest` sources exist yet. JUnit/Espresso deps are
  declared but there is nothing to run; do not claim test coverage.

CI: `.github/workflows/build.yml` **at the repository root** (GitHub ignores a workflow nested in
`Chora/`) builds `assembleFossRelease` on push/PR with JDK 21. Its run steps use
`working-directory: Chora`. The APK lands at
`Chora/app/build/outputs/apk/foss/release/app-foss-release.apk`, and three things make it
downloadable from the run page:

- a *Check the APK was produced* step that fails the job if the output path ever changes, and
  writes size + sha256 to the step summary;
- an `actions/upload-artifact@v4` step on **every** run (`chora-foss-release-apk`, 30 days).
  GitHub always hands artifacts out as a `.zip`;
- a `softprops/action-gh-release@v3` step that uploads the **raw** `.apk` to the rolling
  `latest-build` prerelease on every push to the default branch (asset overwritten in place, so
  the download URL is stable). A `v*` tag gets its own release, and a manual run with
  `publish=true` gets `build-<run_number>`.

A green run used to leave no downloadable APK at all: the build step ran, but the only publish
step was gated on a `v*` tag or `publish=true`, so a normal push threw the APK away with the
runner.

Version bumps: edit `versionCode` / `versionName` manually in `app/build.gradle.kts`
(currently 311 / 1.31.1).

## Architecture

- `data/providers/media/` — `MediaProvider`, a `@Serializable` abstract class implemented by
  `subsonic/SubsonicMediaProvider`, `navidrome/NavidromeMediaProvider`, `local/LocalMediaProvider`.
  Every provider method returns provider-agnostic model types from `data/model/`.
  Adding a provider subclass also requires registering it in `MediaProvider.serializerModule`.
- `managers/MediaProviderManager.kt` — process-wide `object` singleton holding the provider
  map and `currentProvider: StateFlow<MediaProvider?>`. Persists providers to an encrypted
  DataStore file `providers.pb` (`utils/EncryptedMediaProviderSerializer` + `utils/CryptoData`,
  Android Keystore AES). Initialized from `ChoraApplication.onCreate()`.
- `managers/` — `DataRefreshManager`, `MigrationManager`, `TranscodeManager`; settings live in
  `managers/settings/*SettingsManager` (Appearance, LocalData, MediaProvider, Misc, Playback),
  all DataStore-backed.
- `data/repository/` — `Song/Album/Artist/Playlist/Radio/Starred/LyricsRepository`
  (`@Singleton`, constructor-injected). They delegate to `MediaProviderManager.currentProvider`
  and map results to `androidx.media3.common.MediaItem`, often through `*.toMediaItem()` helpers.
- `player/MusicService.kt` — `MediaLibraryService` + ExoPlayer + MediaSession; serves Android
  Auto / media browsing, resolves stream URLs through `ResolvingDataSource` and
  `OkHttpDataSource` (for transcode + headers). Largest/most delicate file (~920 lines).
- `ui/` — Compose UI. `NavGraph.kt` + `data/model/Screen.kt` use type-safe Navigation
  (`composable<Screen.Home>`); add a route in that sealed interface. Screens in `ui/screens/`,
  TV variants in `ui/screens/tv/`, full-screen player in `ui/playing/` (incl. `playing/tv`),
  shared widgets/dialogs in `ui/elements/`, theme in `ui/theme/`.
- DI: Hilt. `@HiltAndroidApp` on `ChoraApplication`, `@AndroidEntryPoint` on `MainActivity` and
  `MusicService`, `@HiltViewModel` for view models, `data/di/DataSourceModule` provides the
  lyrics data sources.
- Lyrics: `data/providers/lyrics/{lrclib,lrcred,netease,unison}` with parsing in
  `utils/TtmlParser.kt`, `utils/LyricsUtils.kt`. Supports word/line synced and unsynced lyrics.

## Conventions and gotchas

- Phone, Android Auto, Android TV and F-Droid are all first-class. UI changes generally need a
  matching path under `ui/screens/tv/` (and TV dialogs under `ui/elements/dialogs/tv/`);
  the TV/phone split is chosen via `LocalConfiguration` (`UI_MODE_TYPE_TELEVISION`).
- All user-facing text goes through `stringResource(...)`; the strings are split across
  `res/values/strings.xml`, `strings_actions.xml`, `strings_dialogs.xml`, `strings_screens.xml`,
  `strings_settings.xml`. Add new strings to the matching file. Translations are managed on
  Crowdin and live in `res/values-<locale>/`; `generateLocaleConfig = true` builds the locale
  config automatically, so do not hand-write it.
- Release builds are minified + resource-shrunk (ProGuard) but signed with the *debug* signing
  config, so `assembleRelease` works locally without a keystore. `debug` is explicitly
  `isDebuggable = false` / `isProfileable = true` — attach the profiler rather than a debugger.
- `network_security_config.xml` allows cleartext HTTP and user-installed CA certs so LAN
  Subsonic/Navidrome servers on plain HTTP work. Do not tighten this without an explicit ask.
- Provider credentials are stored encrypted (Android Keystore); never log credentials, URLs with
  tokens, or provider data.
- Toolchain is ahead of stable: compileSdk/targetSdk 37, minSdk 24, Kotlin 2.4.21, Compose BOM
  2026.09.00, KSP for Hilt, Ktor/Ktorfit, Coil 3, kotlinx.serialization, Media3 1.11.x.
  Use versions from `gradle/libs.versions.toml`; do not add libraries ad hoc.
- Release signing, `versionCode` bumps and pushes are release work — only do them when asked.


## Phase 4 (TV settings, dialogs, release)

- TV settings/dialog primitives: `ui/elements/tv/apple/TvDialogSurface.kt` (`TvDialogSurface`, `TvDialogOptionRow`, `TvDialogTextField`, `TvDialogActions`) and `TvSettingsRows.kt` (`TvSwitchRow`, `TvSettingsSliderRow`, `TvSettingsSection`). New TV dialogs should use these instead of `AlertDialog`.
- Dialogs are overlay windows, so the TV preset is `overlay` tint + hairline (no backdrop sampling is possible there). The panel swallows taps so the window beneath keeps focus.
- `GenericListDialog` / `GenericCheckDialog` render through the new shell; their signatures are unchanged, so pickers need no edits.
- Large Text is a whole-ramp factor in `TvTheme` (`TvTypeScale.scaled`, capped against the system font scale) plus `LocalTvLargeText` for grids.
- Release APKs are signed from `CHORA_KEYSTORE_FILE`/`CHORA_KEY_ALIAS`/`CHORA_KEYSTORE_PASSWORD`/`CHORA_KEY_PASSWORD` when set, otherwise with the debug key. CI builds `assembleFossRelease` and attaches the raw APK to a GitHub Release (workflow_dispatch `publish: true` or a `v*` tag) — Actions artifacts are always zipped.
- `assembleFossRelease` (R8) takes over 5 minutes cold: run it detached with `nohup … > /tmp/build.log 2>&1 &` and poll the log; the sync shell limit is five minutes.

## Phase 5 (focus traversal, motion tokens)

- The tab bar is **not** a `focusGroup` — as a group it traps the exit and DOWN never reaches the
  screen. `TvAppScaffold` puts a `FocusRequester` on the content slot's focus group and passes it
  to `TvTopTabBar`, where every tab declares `focusProperties { down = contentFocusRequester }`.
  New shell-level navigation should be wired the same way rather than left to 2D search.
- Motion specs live in `TvMotion`: `stepChange`/`stepSlide` (onboarding steps), `panelSlide`
  (IntOffset flavour of `panelIn`), `reducedFade` (the only fade allowed under reduce motion).
  Do not hardcode `tween(...)` in TV code; slide specs need the `IntOffset` tokens.
- Testing TV UI on this host is effectively impossible: no KVM (uid not in the `kvm` group), so the
  AVD runs under TCG — 200 s boot, 3 min `pm install`, the app ANR-killed or silently killed before
  its first frame, and `uiautomator dump` timing out on `UiAutomation`. Use screenshots only, and do
  not claim a D-pad path was verified unless a screenshot shows the focus ring on the target.
