package com.metrolist.music.utils

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
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import com.metrolist.innertubex.models.YouTubeLocale
import com.metrolist.music.constants.AudioQuality
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Playback-only bridge to InnerTubeX.
 *
 * Metrofy keeps its existing YouTube metadata/browse stack so Spotify Canvas, lyrics and
 * mappings remain untouched. Only audio stream extraction is delegated to the resolver used by
 * current Metrolist, including the v0.7.4 client/fallback fixes.
 */
object InnerTubeXPlaybackResolver {
    private const val TAG = "InnerTubeXPlayback"
    private const val DEFAULT_STREAM_TTL_SECONDS = 5 * 60

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
        }

    private val innerTube = InnerTube(httpClient)
    private val configStore =
        RemotePlayerConfigStore(
            httpClient = httpClient,
            repository = PlayerConfigRepository.disabled(),
        )
    private val cipherService = YouTubeCipherService(httpClient, configStore)
    private val extractor =
        InnerTubeExtractor(
            configParser =
                YtConfigParserImpl(
                    httpClient = httpClient,
                    innerTube = innerTube,
                    configStore = configStore,
                    cipherService = cipherService,
                ),
            cipherService = cipherService,
            innerTube = innerTube,
        )

    suspend fun resolve(
        videoId: String,
        playlistId: String?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<YTPlayerUtils.PlaybackData> =
        runCatching {
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
                        audioQuality = audioQuality.toInnerTubeX(connectivityManager),
                        clientPlaybackNonce = generateClientPlaybackNonce(),
                    ),
                ) { "InnerTubeX returned no playable stream" }

            check(stream.sabrBootstrap == null) {
                "InnerTubeX selected SABR even though Metrofy's playback path requested direct audio"
            }

            Timber
                .tag(TAG)
                .i(
                    "Resolved %s with client=%s itag=%d mime=%s",
                    videoId,
                    stream.clientName,
                    stream.itag,
                    stream.mimeType,
                )

            stream.toPlaybackData()
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

    private fun com.metrolist.innertubex.extraction.ExtractedStream.toPlaybackData(): YTPlayerUtils.PlaybackData {
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
        )
    }
}
