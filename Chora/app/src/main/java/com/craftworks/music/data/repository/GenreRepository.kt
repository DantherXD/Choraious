package com.craftworks.music.data.repository

import android.util.Log
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.GenreListSort
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.managers.MediaProviderManager
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Genre access for Browse (plan P2-B1/B3).
 *
 * **Every provider currently stubs `getGenreList` with `TODO(...)`**, which throws
 * `NotImplementedError` rather than returning an empty list. Those calls are therefore wrapped:
 * an unsupported provider yields `null`, Browse hides its genre row, and the screen degrades to
 * curated rails instead of crashing. When a provider implements the endpoint, the list simply
 * starts arriving — no browser change needed.
 *
 * Genre filtering passes the genre *name* in `genreIds`, because `MediaModel.Genre` exposes no id
 * and Subsonic's album query already treats that field as a genre name.
 */
@Singleton
class GenreRepository @Inject constructor() {

    /** `null` means the provider cannot list genres; an empty list means it found none. */
    suspend fun getGenres(limit: Int = 60): List<MediaModel.Genre>? = coroutineScope {
        val provider = MediaProviderManager.currentProvider.value ?: return@coroutineScope null

        try {
            provider.getGenreList(
                MediaQuery.GenreListQuery(
                    sortBy = GenreListSort.NAME,
                    sortOrder = SortOrder.ASC,
                    limit = limit,
                    startIndex = 0,
                )
            )
        } catch (e: NotImplementedError) {
            Log.d(TAG, "${provider.javaClass.simpleName} does not implement getGenreList")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Genre list failed: ${e.message}")
            null
        }
    }

    suspend fun getAlbumsForGenre(genreName: String, limit: Int = 40): List<MediaItem> =
        coroutineScope {
            val provider = MediaProviderManager.currentProvider.value ?: return@coroutineScope emptyList()

            try {
                provider.getAlbumList(
                    MediaQuery.AlbumListQuery(
                        sortBy = AlbumListSort.RANDOM,
                        sortOrder = SortOrder.ASC,
                        genreIds = listOf(genreName),
                        limit = limit,
                        startIndex = 0,
                    )
                )?.map { it.toMediaItem() } ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "Genre album query failed: ${e.message}")
                emptyList()
            }
        }

    private companion object {
        const val TAG = "GenreRepository"
    }
}
