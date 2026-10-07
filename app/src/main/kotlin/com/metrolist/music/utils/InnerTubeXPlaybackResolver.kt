package com.metrolist.music.utils

import android.content.Context
import android.net.ConnectivityManager
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.Thumbnail
import com.metrolist.innertube.models.Thumbnails
import com.metrolist.innertube.models.response.PlayerResponse
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality as InnerTubeXAudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.ExtractedStream
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.PoTokenResult
import com.metrolist.innertubex.extraction.StreamResolveException
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.TokenProviderCapabilities
import com.metrolist.innertubex.extraction.YtConfigParser
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import com.metrolist.innertubex.extraction.strategy.PoTokenProviderKind
import com.metrolist.innertubex.models.YouTubeLocale
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.utils.potoken.PoTokenGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Playback-only InnerTubeX bridge.
 *
 * This deliberately mirrors the reliability pieces used by upstream Metrolist:
 * persistent player-config caching, WebView PoToken support, client blacklisting after a rejected
 * stream, and propagation of the stream's bounded-range policy into Media3.
 */
object InnerTubeXPlaybackResolver {
    private const val TAG = "InnerTubeXPlayback"
    private const val DEFAULT_STREAM_TTL_SECONDS = 5 * 60
    private const val STREAM_CLIENT_FAILURE_TTL_MS = 5 * 60 * 1000L
    private const val PLAYER_CONFIG_URL =
        "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"

    @Volatile
    private var applicationContext: Context? = null

    private data class FailedStreamClients(
        val clientNames: Set<String>,
        val failedAtMs: Long,
    )

    private val streamClientFailures = ConcurrentHashMap<String, FailedStreamClients>()

    @Synchronized
    fun initialize(context: Context) {
        if (applicationContext == null) applicationContext = context.applicationContext
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val httpClient =
        HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    },
                )
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            engine {
                config {
                    retryOnConnectionFailure(true)
                    YouTube.proxy?.let(::proxy)
                    YouTube.proxyAuth?.let { auth ->
                        proxyAuthenticator { _, response ->
                            response.request
                                .newBuilder()
                                .header("Proxy-Authorization", auth)
                                .build()
                        }
                    }
                }
            }
        }

    private val innerTube = InnerTube(httpClient)

    private val configRepository: PlayerConfigRepository by lazy {
        AndroidPlayerConfigRepository(
            requireNotNull(applicationContext) { "InnerTubeXPlaybackResolver is not initialized" },
        )
    }

    private val configStore by lazy {
        RemotePlayerConfigStore(
            httpClient = httpClient,
            repository = configRepository,
        )
    }

    private val cipherService by lazy {
        YouTubeCipherService(httpClient, configStore)
    }

    private val poTokenGenerator by lazy { PoTokenGenerator() }

    private val tokenProvider =
        object : TokenProvider {
            override val capabilities =
                TokenProviderCapabilities(
                    providers = setOf(PoTokenProviderKind.WEB_BOTGUARD),
                    usesWebView = true,
                )

            override suspend fun getPoToken(
                videoId: String,
                visitorData: String,
                cookie: String?,
            ): PoTokenResult? =
                poTokenGenerator.getWebClientPoToken(videoId, visitorData)?.let { token ->
                    PoTokenResult(
                        playerRequestToken = token.playerRequestPoToken,
                        streamingDataToken = token.streamingDataPoToken,
                        visitorData = visitorData,
                    )
                }

            override suspend fun close() = Unit
        }

    private val extractor by lazy {
        InnerTubeExtractor(
            configParser =
                YtConfigParserImpl(
                    httpClient = httpClient,
                    innerTube = innerTube,
                    remotePlayerConfigStore = configStore,
                    cipherService = cipherService,
                ).withEmbeddedConfigFallback(),
            cipherService = cipherService,
            innerTube = innerTube,
            tokenProvider = tokenProvider,
        )
    }

    suspend fun prewarm() {
        syncSession()
        extractor.prewarm()
    }

    suspend fun resolve(
        videoId: String,
        playlistId: String?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<YTPlayerUtils.PlaybackData> =
        try {
            syncSession()

            val isUploaded =
                playlistId == "MLPT" ||
                    playlistId?.contains("MLPT") == true

            val hints =
                ContentHints(
                    isUploaded = isUploaded,
                    wantVideo = false,
                ).withStreamCapabilities(
                    allowHls = false,
                    allowSabr = false,
                    allowBoundedRange = true,
                )

            val stream =
                requireNotNull(
                    extractor.extract(
                        videoId = videoId,
                        hints = hints,
                        excludedClients = failedStreamClients(videoId),
                        audioQuality = audioQuality.toInnerTubeX(connectivityManager),
                        clientPlaybackNonce = generateClientPlaybackNonce(),
                    ),
                ) { "InnerTubeX returned no playable stream" }

            check(stream.sabrBootstrap == null) {
                "InnerTubeX selected SABR even though Metrofy requested direct audio"
            }

            Timber
                .tag(TAG)
                .i(
                    "Resolved %s with client=%s itag=%d mime=%s bounded=%s chunk=%d",
                    videoId,
                    stream.clientName,
                    stream.itag,
                    stream.mimeType,
                    stream.requireBoundedRange || stream.useRangeChunks,
                    stream.rangeChunkSizeBytes,
                )

            Result.success(stream.toPlaybackData())
        } catch (error: CancellationException) {
            throw error
        } catch (error: StreamResolveException) {
            val cause = error.cause
            Result.failure(
                if (error.reason == StreamResolveException.Reason.NETWORK && cause != null) {
                    cause
                } else {
                    error
                },
            )
        } catch (error: Exception) {
            Result.failure(error)
        }

    fun markStreamClientFailed(
        videoId: String,
        clientName: String,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        streamClientFailures.compute(videoId) { _, failures ->
            FailedStreamClients(failures?.clientNames.orEmpty() + clientName, nowMs)
        }
        Timber.tag(TAG).w("Blacklisted stream client=%s for video=%s", clientName, videoId)
    }

    fun clearStreamClientFailures() {
        streamClientFailures.clear()
    }

    suspend fun refreshAfterStreamRejection(): Boolean =
        cipherService.refreshAfterStreamRejection()

    private fun failedStreamClients(
        videoId: String,
        nowMs: Long = System.currentTimeMillis(),
    ): Set<String> {
        val failures = streamClientFailures[videoId] ?: return emptySet()
        if ((nowMs - failures.failedAtMs) !in 0 until STREAM_CLIENT_FAILURE_TTL_MS) {
            streamClientFailures.remove(videoId, failures)
            return emptySet()
        }
        return failures.clientNames
    }

    private fun syncSession() {
        innerTube.locale =
            YouTubeLocale(
                gl = YouTube.locale.gl,
                hl = YouTube.locale.hl,
            )
        innerTube.visitorData = YouTube.visitorData
        innerTube.dataSyncId = YouTube.dataSyncId
        innerTube.cookie = YouTube.cookie
    }

    private fun AudioQuality.toInnerTubeX(
        connectivityManager: ConnectivityManager,
    ): InnerTubeXAudioQuality =
        when (this) {
            AudioQuality.HIGH -> InnerTubeXAudioQuality.HIGH
            AudioQuality.LOW -> InnerTubeXAudioQuality.LOW
            AudioQuality.AUTO ->
                if (connectivityManager.isActiveNetworkMetered) {
                    InnerTubeXAudioQuality.LOW
                } else {
                    InnerTubeXAudioQuality.AUTO
                }
        }

    private fun ExtractedStream.toPlaybackData(): YTPlayerUtils.PlaybackData {
        val metadata = mediaMetadata
        val fullMimeType =
            if (codecs.isNullOrBlank()) {
                mimeType.orEmpty()
            } else {
                "${mimeType.orEmpty()}; codecs=\"$codecs\""
            }

        val localAudioConfig =
            if (loudnessDb != null || perceptualLoudnessDb != null) {
                PlayerResponse.PlayerConfig.AudioConfig(
                    loudnessDb = loudnessDb,
                    perceptualLoudnessDb = perceptualLoudnessDb,
                )
            } else {
                null
            }

        val localVideoDetails =
            metadata?.let {
                PlayerResponse.VideoDetails(
                    videoId = videoId,
                    title = it.title,
                    author = it.author,
                    channelId = it.channelId.orEmpty(),
                    lengthSeconds = it.durationSeconds?.toString().orEmpty(),
                    musicVideoType = it.musicVideoType,
                    viewCount = it.viewCount,
                    thumbnail =
                        Thumbnails(
                            it.thumbnails.map { thumbnail ->
                                Thumbnail(
                                    url = thumbnail.url,
                                    width = thumbnail.width,
                                    height = thumbnail.height,
                                )
                            },
                        ),
                )
            }

        val localTracking =
            playbackTracking?.let {
                PlayerResponse.PlaybackTracking(
                    videostatsPlaybackUrl =
                        it.playbackUrl?.let(PlayerResponse.PlaybackTracking::VideostatsPlaybackUrl),
                    videostatsWatchtimeUrl =
                        it.watchtimeUrl?.let(PlayerResponse.PlaybackTracking::VideostatsWatchtimeUrl),
                    atrUrl = null,
                )
            }

        val localFormat =
            PlayerResponse.StreamingData.Format(
                itag = itag,
                url = audioUrl,
                mimeType = fullMimeType,
                bitrate = bitrate ?: 0,
                width = null,
                height = null,
                contentLength = contentLengthBytes,
                quality = "",
                fps = null,
                qualityLabel = null,
                averageBitrate = bitrate,
                audioQuality = null,
                approxDurationMs = metadata?.durationSeconds?.times(1000)?.toString(),
                audioSampleRate = sampleRate,
                audioChannels = null,
                loudnessDb = loudnessDb,
                lastModified = null,
                signatureCipher = null,
                cipher = null,
                audioTrack = null,
            )

        val ttlSeconds =
            expiresAt
                ?.let {
                    ((it.toEpochMilliseconds() - System.currentTimeMillis()) / 1000L)
                        .toInt()
                        .coerceAtLeast(1)
                } ?: DEFAULT_STREAM_TTL_SECONDS

        return YTPlayerUtils.PlaybackData(
            audioConfig = localAudioConfig,
            videoDetails = localVideoDetails,
            playbackTracking = localTracking,
            format = localFormat,
            streamUrl = audioUrl,
            streamExpiresInSeconds = ttlSeconds,
            streamHeaders = headers,
            streamClient = clientName,
            requireBoundedRange = requireBoundedRange,
            rangeChunkSizeBytes = rangeChunkSizeBytes,
            useRangeChunks = useRangeChunks,
        )
    }

    private class AndroidPlayerConfigRepository(context: Context) : PlayerConfigRepository {
        private val preferences =
            context.getSharedPreferences("innertubex_player_config", Context.MODE_PRIVATE)

        override val enabled: Boolean = true
        override val sourceUrl: String = PLAYER_CONFIG_URL
        override val defaultSourceUrl: String = PLAYER_CONFIG_URL

        override var cachedJson: String
            get() = preferences.getString("json", "").orEmpty()
            set(value) = preferences.edit().putString("json", value).apply()

        override var cachedAtMs: Long
            get() = preferences.getLong("cached_at_ms", 0L)
            set(value) = preferences.edit().putLong("cached_at_ms", value).apply()

        override var cachedSourceUrl: String
            get() = preferences.getString("source_url", "").orEmpty()
            set(value) = preferences.edit().putString("source_url", value).apply()

        override var cachedEtag: String
            get() = preferences.getString("etag", "").orEmpty()
            set(value) = preferences.edit().putString("etag", value).apply()
    }

    private fun YtConfigParser.withEmbeddedConfigFallback(): YtConfigParser =
        object : YtConfigParser by this {
            override suspend fun fetchConfig(
                videoId: String,
                useLoginCookies: Boolean,
            ) =
                try {
                    this@withEmbeddedConfigFallback.fetchConfig(videoId, useLoginCookies)
                } catch (_: IllegalStateException) {
                    this@withEmbeddedConfigFallback.fetchEmbeddedConfig(
                        videoId,
                        useLoginCookies = false,
                    )
                }
        }
}
