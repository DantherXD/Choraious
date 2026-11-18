package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.data.repository.GenreRepository
import com.craftworks.music.managers.DataRefreshManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Browse (spec §7): curated rails plus genre chips.
 *
 * Mirrors [HomeScreenViewModel] — same repositories, same refresh trigger, rails skipped when
 * empty — and owns the genre selection so switching a chip does not rebuild the screen.
 *
 * `genres` stays `null` while the provider has no genre endpoint (see [GenreRepository]), which
 * is what lets the screen hide the whole row instead of showing an empty state.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val genreRepository: GenreRepository,
) : ViewModel() {

    private val _newReleases = MutableStateFlow<List<MediaItem>>(emptyList())
    val newReleases: StateFlow<List<MediaItem>> = _newReleases.asStateFlow()

    private val _mostPlayed = MutableStateFlow<List<MediaItem>>(emptyList())
    val mostPlayed: StateFlow<List<MediaItem>> = _mostPlayed.asStateFlow()

    private val _randomPicks = MutableStateFlow<List<MediaItem>>(emptyList())
    val randomPicks: StateFlow<List<MediaItem>> = _randomPicks.asStateFlow()

    private val _genres = MutableStateFlow<List<MediaModel.Genre>?>(null)
    val genres: StateFlow<List<MediaModel.Genre>?> = _genres.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _genreAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val genreAlbums: StateFlow<List<MediaItem>> = _genreAlbums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    private val _isEmpty = MutableStateFlow(false)

    /** True when there is nothing to show and nothing is loading: the provider-absent state. */
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    init {
        loadBrowseData()

        viewModelScope.launch {
            DataRefreshManager.dataSourceChangedEvent.collect {
                loadBrowseData()
            }
        }
    }

    fun loadBrowseData() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadFailed.value = false

            val provider = com.craftworks.music.managers.MediaProviderManager.currentProvider.value
            if (provider == null) {
                _isLoading.value = false
                _loadFailed.value = true
                _isEmpty.value = true
                return@launch
            }

            try {
                coroutineScope {
                    val newReleasesDeferred = async {
                        albumRepository.getAlbums(
                            MediaQuery.AlbumListQuery(
                                AlbumListSort.RECENTLY_ADDED, SortOrder.DESC, limit = 20, startIndex = 0
                            )
                        )
                    }
                    val mostPlayedDeferred = async {
                        albumRepository.getAlbums(
                            MediaQuery.AlbumListQuery(
                                AlbumListSort.PLAY_COUNT, SortOrder.DESC, limit = 20, startIndex = 0
                            )
                        )
                    }
                    val randomDeferred = async {
                        albumRepository.getAlbums(
                            MediaQuery.AlbumListQuery(
                                AlbumListSort.RANDOM, SortOrder.ASC, limit = 20, startIndex = 0
                            )
                        )
                    }
                    val genresDeferred = async { genreRepository.getGenres() }

                    _newReleases.value = newReleasesDeferred.await()
                    _mostPlayed.value = mostPlayedDeferred.await()
                    _randomPicks.value = randomDeferred.await()
                    _genres.value = genresDeferred.await()
                }

                _isEmpty.value = _newReleases.value.isEmpty() &&
                    _mostPlayed.value.isEmpty() &&
                    _randomPicks.value.isEmpty()
            } catch (e: Exception) {
                _loadFailed.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Selecting the same genre again clears the filter, so a second OK is "show everything". */
    fun selectGenre(genreName: String) {
        if (_selectedGenre.value == genreName) {
            clearGenre()
            return
        }

        _selectedGenre.value = genreName
        viewModelScope.launch {
            _genreAlbums.value = genreRepository.getAlbumsForGenre(genreName)
        }
    }

    fun clearGenre() {
        _selectedGenre.value = null
        _genreAlbums.value = emptyList()
    }
}
