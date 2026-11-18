package com.craftworks.music.ui.tv

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen

/**
 * The five top-level TV tabs (spec §5.1/§5.2), in fixed order.
 *
 * [route] is where the tab navigates to. [destination] -> tab is the inverse mapping used to
 * paint the active dot: every route that belongs to a section must appear in one branch of
 * [toTvTab], or the bar loses its active state while the user is inside that section.
 */
enum class TvTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val route: Screen,
) {
    ListenNow(R.string.nav_listen_now, R.drawable.round_play_arrow_24, Screen.Home),
    Browse(R.string.nav_browse, R.drawable.rounded_apps_24, Screen.Browse),
    Radio(R.string.nav_radios, R.drawable.rounded_radio, Screen.Radios),
    Library(R.string.nav_library, R.drawable.rounded_library_music_24, Screen.Library),
    Search(R.string.nav_search, R.drawable.round_search_24, Screen.Search),
}

/**
 * Every destination that belongs to the Library section: the hub itself, the four list/graph
 * roots, their detail screens, the songs list and the whole settings graph.
 */
private val libraryDestinations = listOf(
    Screen.Library::class,
    Screen.Albums::class,
    Screen.AlbumList::class,
    Screen.AlbumDetails::class,
    Screen.Artists::class,
    Screen.ArtistsList::class,
    Screen.ArtistDetails::class,
    Screen.Playlists::class,
    Screen.PlaylistList::class,
    Screen.PlaylistDetails::class,
    Screen.Songs::class,
    Screen.Settings::class,
    Screen.SettingsList::class,
    Screen.S_Appearance::class,
    Screen.S_Media_Providers::class,
    Screen.S_Lyrics_Providers::class,
    Screen.S_Playback::class,
    Screen.S_Misc::class,
)

/**
 * The tab that owns the current destination, or `null` for a destination outside the tab set
 * (Now Playing keeps the bar hidden, so it has no active tab).
 *
 * Matches against the full hierarchy, not the leaf destination: `AlbumDetails` lives inside the
 * `Albums` graph and `S_Appearance` inside the `Settings` graph, and both must light up Library.
 */
fun NavDestination?.toTvTab(): TvTab? {
    val hierarchy = this?.hierarchy ?: return null
    return when {
        hierarchy.any { it.hasRoute(Screen.Search::class) } -> TvTab.Search
        hierarchy.any { it.hasRoute(Screen.Browse::class) } -> TvTab.Browse
        hierarchy.any { it.hasRoute(Screen.Radios::class) } -> TvTab.Radio
        hierarchy.any {
            it.hasRoute(Screen.Home::class) || it.hasRoute(Screen.HomeLists::class)
        } -> TvTab.ListenNow

        hierarchy.any { destination -> libraryDestinations.any { destination.hasRoute(it) } } ->
            TvTab.Library

        else -> null
    }
}

/** True when the user is on the tab's own root route (not pushed inside its section). */
fun NavDestination?.isRootOf(tab: TvTab): Boolean =
    this?.hasRoute(tab.route::class) == true
