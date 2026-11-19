@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongHelper {
    companion object{
        suspend fun play(mediaItems: List<MediaItem>, index: Int, mediaController: MediaController?) {
            if (mediaItems.isEmpty())
                return

            withContext(Dispatchers.Main) {
                // Called from click handlers' coroutines, where an exception is not caught by
                // anything: a controller that has gone away mid-click used to take the whole app
                // down instead of surfacing as a player error.
                runCatching {
                    mediaController?.setMediaItems(mediaItems, index, 0)
                    mediaController?.prepare()
                    mediaController?.play()
                }.onFailure {
                    Log.e("SONG-HELPER", "Could not start playback", it)
                }
            }
        }
        fun enqueue(mediaItems: List<MediaItem>, mediaController: MediaController?) {
            runCatching { mediaController?.addMediaItems(mediaItems) }
                .onFailure { Log.e("SONG-HELPER", "Could not enqueue items", it) }
        }
        fun playNext(mediaItems: List<MediaItem>, mediaController: MediaController?) {
            runCatching {
                mediaController?.addMediaItems(
                    mediaController.currentMediaItemIndex + 1,
                    mediaItems
                )
            }.onFailure { Log.e("SONG-HELPER", "Could not queue items next", it) }
        }
    }
}
