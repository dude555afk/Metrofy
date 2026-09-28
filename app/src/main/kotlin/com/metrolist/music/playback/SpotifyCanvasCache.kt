/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.Context
import android.util.LruCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.metrolist.spotify.Spotify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal object SpotifyCanvasCache {
    private const val CACHE_DIRECTORY = "spotify_canvas"
    private const val MAX_CACHE_SIZE_BYTES = 256L * 1024L * 1024L
    private const val NO_CANVAS = ""

    private val urlEntries = LruCache<String, String>(128)
    private val cacheLock = Any()

    @Volatile
    private var videoCache: SimpleCache? = null

    suspend fun getUrl(trackUri: String): String? {
        urlEntries.get(trackUri)?.let { return it.ifEmpty { null } }
        val url = Spotify.canvasUrl(trackUri).getOrThrow()
        urlEntries.put(trackUri, url ?: NO_CANVAS)
        return url
    }

    fun dataSourceFactory(context: Context): DataSource.Factory =
        CacheDataSource.Factory()
            .setCache(getVideoCache(context))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context.applicationContext))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    suspend fun prepare(context: Context) {
        withContext(Dispatchers.IO) {
            getVideoCache(context)
        }
    }

    fun size(context: Context): Long = getVideoCache(context).cacheSpace

    fun clear(context: Context) {
        urlEntries.evictAll()
        val cache = getVideoCache(context)
        cache.keys.toList().forEach { key ->
            runCatching { cache.removeResource(key) }
                .onFailure { Timber.tag("SpotifyCanvas").w(it, "Failed to remove cached Canvas resource") }
        }
    }

    private fun getVideoCache(context: Context): SimpleCache =
        videoCache ?: synchronized(cacheLock) {
            videoCache ?: SimpleCache(
                context.applicationContext.cacheDir.resolve(CACHE_DIRECTORY),
                LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES),
                StandaloneDatabaseProvider(context.applicationContext),
            ).also { videoCache = it }
        }
}
