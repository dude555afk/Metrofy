/**
 * Stream URL cache that keeps the request metadata returned by InnerTubeX.
 */
package com.metrolist.music.playback

import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec

internal data class CachedStreamUrl(
    val first: String,
    val second: Long,
    val requestHeaders: Map<String, String> = emptyMap(),
    val clientName: String? = null,
    val requireBoundedRange: Boolean = true,
    val rangeChunkSizeBytes: Long = DEFAULT_RANGE_CHUNK_BYTES,
    val useRangeChunks: Boolean = false,
) {
    companion object {
        const val DEFAULT_RANGE_CHUNK_BYTES = 512 * 1024L
    }
}

internal fun DataSpec.withResolvedStream(stream: CachedStreamUrl): DataSpec {
    val resolved =
        withUri(stream.first.toUri())
            .withRequestHeaders(httpRequestHeaders + stream.requestHeaders)

    if ((!stream.requireBoundedRange && !stream.useRangeChunks) || stream.rangeChunkSizeBytes <= 0L) {
        return resolved
    }

    val boundedLength =
        if (length == C.LENGTH_UNSET.toLong()) {
            stream.rangeChunkSizeBytes
        } else {
            minOf(length, stream.rangeChunkSizeBytes)
        }
    return resolved.subrange(0, boundedLength)
}

internal class StreamUrlCache(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val entries = LinkedHashMap<String, CachedStreamUrl>()

    operator fun get(mediaId: String): CachedStreamUrl? = synchronized(entries) { entries[mediaId] }

    operator fun set(mediaId: String, value: Pair<String, Long>) {
        synchronized(entries) {
            entries[mediaId] =
                CachedStreamUrl(
                    first = value.first,
                    second = value.second,
                )
        }
    }

    fun putResolved(
        mediaId: String,
        url: String,
        expiresInSeconds: Int,
        requestHeaders: Map<String, String>,
        clientName: String,
        requireBoundedRange: Boolean,
        rangeChunkSizeBytes: Long,
        useRangeChunks: Boolean,
    ): CachedStreamUrl {
        val safeTtlSeconds = (expiresInSeconds - 60).coerceAtLeast(1)
        val expiresAt =
            runCatching {
                Math.addExact(currentTimeMillis(), safeTtlSeconds.toLong() * 1000L)
            }.getOrDefault(Long.MAX_VALUE)

        val stream =
            CachedStreamUrl(
                first = url,
                second = expiresAt,
                requestHeaders = requestHeaders.toMap(),
                clientName = clientName,
                requireBoundedRange = requireBoundedRange,
                rangeChunkSizeBytes =
                    rangeChunkSizeBytes.takeIf { it > 0L }
                        ?: CachedStreamUrl.DEFAULT_RANGE_CHUNK_BYTES,
                useRangeChunks = useRangeChunks,
            )
        synchronized(entries) { entries[mediaId] = stream }
        return stream
    }

    fun remove(mediaId: String): CachedStreamUrl? = synchronized(entries) { entries.remove(mediaId) }

    fun clear() = synchronized(entries) { entries.clear() }
}
