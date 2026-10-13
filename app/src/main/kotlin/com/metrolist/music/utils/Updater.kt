/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.metrolist.music.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val description: String,
    val releaseDate: String,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val architecture: String,
    val variant: String // "foss" or "gms"
)

enum class UpdateChannel {
    STABLE,
    PREVIEW,
}

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set
    
    private val cachedReleaseInfo = mutableMapOf<UpdateChannel, ReleaseInfo>()
    private var cachedAllReleases: List<Pair<ReleaseInfo, Boolean>> = emptyList()
    
    private const val CHECK_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L // 2 hours
    private const val GITHUB_API_BASE = "https://api.github.com/repos/dude555afk/Metrofy"

    /**
     * SemVer-ish comparison used by stable and preview builds.
     * Stable 0.8.9.4 correctly sorts after 0.8.9.4-preview.1.
     */
    fun compareVersions(v1: String, v2: String): Int {
        fun parse(version: String): Pair<List<Int>, List<String>?> {
            val normalized = version.removePrefix("v").substringBefore("+")
            val core = normalized.substringBefore("-")
                .split(".")
                .map { it.toIntOrNull() ?: 0 }
            val prerelease = normalized.substringAfter("-", "")
                .takeIf { it.isNotBlank() }
                ?.split(".")
            return core to prerelease
        }

        val (core1, pre1) = parse(v1)
        val (core2, pre2) = parse(v2)
        val maxCore = maxOf(core1.size, core2.size)
        for (i in 0 until maxCore) {
            val a = core1.getOrNull(i) ?: 0
            val b = core2.getOrNull(i) ?: 0
            if (a != b) return a.compareTo(b)
        }

        if (pre1 == null && pre2 == null) return 0
        if (pre1 == null) return 1
        if (pre2 == null) return -1

        val maxPre = maxOf(pre1.size, pre2.size)
        for (i in 0 until maxPre) {
            val a = pre1.getOrNull(i) ?: return -1
            val b = pre2.getOrNull(i) ?: return 1
            val an = a.toIntOrNull()
            val bn = b.toIntOrNull()
            val cmp = when {
                an != null && bn != null -> an.compareTo(bn)
                an != null -> -1
                bn != null -> 1
                else -> a.compareTo(b, ignoreCase = true)
            }
            if (cmp != 0) return cmp
        }
        return 0
    }

    /**
     * Checks if the latest version is newer than the current version.
     * Returns true if an update is available (latestVersion > currentVersion)
     */
    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        return compareVersions(latestVersion, currentVersion) > 0
    }

    /**
     * Get the current app's architecture and variant
     */
    private fun getCurrentAppVariant(): Pair<String, String> {
        val architecture = BuildConfig.ARCHITECTURE
        val variant = if (BuildConfig.CAST_AVAILABLE) "gms" else "foss"
        return architecture to variant
    }

    /**
     * Parse release assets from GitHub API response
     */
    private fun parseAssets(assetsArray: JSONArray): List<ReleaseAsset> {
        val assets = mutableListOf<ReleaseAsset>()
        
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            val name = asset.getString("name")
            
            // Skip non-APK files
            if (!name.endsWith(".apk")) continue
            
            val downloadUrl = asset.getString("browser_download_url")
            val size = asset.getLong("size")
            
            // Parse architecture and variant from filename
            val (arch, variant) = when {
                name == "Metrofy.apk" || name == "Metrofy-preview.apk" -> "universal" to "foss"
                name == "Metrofy-with-Google-Cast.apk" ||
                    name == "Metrofy-preview-with-Google-Cast.apk" -> "universal" to "gms"
                name.startsWith("app-") && name.endsWith("-release.apk") -> {
                    val arch = name.removePrefix("app-").removeSuffix("-release.apk")
                    arch to "foss"
                }
                name.startsWith("app-") && name.endsWith("-with-Google-Cast.apk") -> {
                    val arch = name.removePrefix("app-").removeSuffix("-with-Google-Cast.apk")
                    arch to "gms"
                }
                else -> null to null
            }
            
            if (arch != null && variant != null) {
                assets.add(ReleaseAsset(name, downloadUrl, size, arch, variant))
            }
        }
        
        return assets
    }

    /**
     * Fetch latest release from GitHub API
     */
    suspend fun getLatestRelease(
        forceRefresh: Boolean = false,
        channel: UpdateChannel = UpdateChannel.STABLE,
    ): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                cachedReleaseInfo[channel]?.takeIf { !forceRefresh }?.let {
                    return@runCatching it
                }

                val releases = getAllReleases(forceRefresh = forceRefresh).getOrThrow()
                val candidates =
                    releases
                        .filter { (info, prerelease) ->
                            when (channel) {
                                UpdateChannel.STABLE -> !prerelease
                                UpdateChannel.PREVIEW -> true
                            }
                        }
                        .map { it.first }
                        .filter { it.assets.isNotEmpty() }

                val releaseInfo =
                    candidates.maxWithOrNull { a, b ->
                        compareVersions(a.versionName, b.versionName)
                    } ?: error("No compatible releases found for $channel")

                cachedReleaseInfo[channel] = releaseInfo
                lastCheckTime = System.currentTimeMillis()
                releaseInfo
            }
        }

    /**
     * Fetch all releases from GitHub API (paginated)
     */
    suspend fun getAllReleases(
        forceRefresh: Boolean = false,
    ): Result<List<Pair<ReleaseInfo, Boolean>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (cachedAllReleases.isNotEmpty() && !forceRefresh) {
                    return@runCatching cachedAllReleases
                }

                val releases = mutableListOf<Pair<ReleaseInfo, Boolean>>()
                var page = 1
                var hasMore = true

                while (hasMore && page <= 10) {
                    val response =
                        client.get("$GITHUB_API_BASE/releases?page=$page&per_page=30")
                            .bodyAsText()
                    val json = JSONArray(response)

                    if (json.length() == 0) {
                        hasMore = false
                        break
                    }

                    for (i in 0 until json.length()) {
                        val releaseObj = json.getJSONObject(i)
                        if (releaseObj.optBoolean("draft", false)) continue

                        val tag = releaseObj.getString("tag_name")
                        val version =
                            releaseObj.optString("name")
                                .takeIf { it.isNotBlank() }
                                ?: tag.removePrefix("v")

                        releases +=
                            ReleaseInfo(
                                tagName = tag,
                                versionName = version,
                                description = releaseObj.optString("body"),
                                releaseDate = releaseObj.optString("published_at"),
                                assets = parseAssets(releaseObj.getJSONArray("assets")),
                            ) to releaseObj.optBoolean("prerelease", false)
                    }

                    hasMore = json.length() == 30
                    page++
                }

                cachedAllReleases = releases
                releases
            }
        }

    suspend fun getPreviousRelease(
        channel: UpdateChannel = UpdateChannel.STABLE,
        currentVersion: String = BuildConfig.VERSION_NAME,
        forceRefresh: Boolean = false,
    ): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                getAllReleases(forceRefresh)
                    .getOrThrow()
                    .asSequence()
                    .filter { (_, prerelease) ->
                        channel == UpdateChannel.PREVIEW || !prerelease
                    }
                    .map { it.first }
                    .filter { it.assets.isNotEmpty() }
                    .filter { compareVersions(it.versionName, currentVersion) < 0 }
                    .maxWithOrNull { a, b -> compareVersions(a.versionName, b.versionName) }
                    ?: error("No previous compatible release found")
            }
        }

    /**
     * Get the download URL for the correct app variant
     */
    fun getDownloadUrlForCurrentVariant(releaseInfo: ReleaseInfo): String? {
        val (currentArch, currentVariant) = getCurrentAppVariant()
        
        return releaseInfo.assets
            .find { it.architecture == currentArch && it.variant == currentVariant }
            ?.downloadUrl
    }

    /**
     * Get all available download URLs for a release
     */
    fun getAllDownloadUrls(releaseInfo: ReleaseInfo): Map<String, String> {
        return releaseInfo.assets.associate { "${it.architecture}-${it.variant}" to it.downloadUrl }
    }

    /**
     * Check if update is needed (respects 2-hour cache)
     */
    suspend fun checkForUpdate(
        forceRefresh: Boolean = false,
        channel: UpdateChannel = UpdateChannel.STABLE,
        skippedVersion: String? = null,
    ): Result<Pair<ReleaseInfo?, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cached = cachedReleaseInfo[channel]
                val shouldFetch =
                    forceRefresh ||
                        cached == null ||
                        (System.currentTimeMillis() - lastCheckTime) > CHECK_INTERVAL_MILLIS

                val releaseInfo =
                    if (shouldFetch) {
                        getLatestRelease(forceRefresh = forceRefresh, channel = channel).getOrThrow()
                    } else {
                        cached
                    }

                val hasUpdate =
                    releaseInfo != null &&
                        releaseInfo.versionName != skippedVersion &&
                        isUpdateAvailable(BuildConfig.VERSION_NAME, releaseInfo.versionName)

                releaseInfo to hasUpdate
            }
        }

    /**
     * Get the download URL for the correct app variant
     * Returns null if no matching asset is found
     */
    fun getLatestDownloadUrl(
        channel: UpdateChannel = UpdateChannel.STABLE,
    ): String? =
        cachedReleaseInfo[channel]?.let { getDownloadUrlForCurrentVariant(it) }

    /**
     * Get the latest release info (cached)
     */
    fun getCachedLatestRelease(
        channel: UpdateChannel = UpdateChannel.STABLE,
    ): ReleaseInfo? = cachedReleaseInfo[channel]

    /**
     * Downloads the correct APK for this build directly into Metrofy's private cache.
     * The browser and public Downloads folder are deliberately bypassed.
     */
    suspend fun downloadUpdate(
        context: Context,
        releaseInfo: ReleaseInfo,
        onProgress: (Int) -> Unit = {},
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val (arch, variant) = getCurrentAppVariant()
            val asset = releaseInfo.assets.firstOrNull {
                it.architecture == arch && it.variant == variant
            } ?: error("No compatible APK found for this Metrofy build")

            val updateDir = File(context.cacheDir, UPDATE_CACHE_DIR).apply { mkdirs() }
            updateDir.listFiles()?.forEach { stale ->
                if (stale.name.endsWith(".apk") || stale.name.endsWith(".part")) stale.delete()
            }

            val safeVersion = releaseInfo.tagName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val target = File(updateDir, "Metrofy-$safeVersion.apk")
            val partial = File(updateDir, target.name + ".part")

            val connection = (URL(asset.downloadUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.android.package-archive")
                setRequestProperty("User-Agent", "Metrofy/${BuildConfig.VERSION_NAME}")
            }

            try {
                connection.connect()
                if (connection.responseCode !in 200..299) {
                    error("APK download failed: HTTP ${connection.responseCode}")
                }

                val total = asset.size.takeIf { it > 0 } ?: connection.contentLengthLong
                var copied = 0L
                var lastProgress = -1

                connection.inputStream.use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (total > 0) {
                                val progress = ((copied * 100L) / total).toInt().coerceIn(0, 100)
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    onProgress(progress)
                                }
                            }
                        }
                        output.fd.sync()
                    }
                }

                check(copied > 0L) { "Downloaded APK is empty" }
                if (asset.size > 0L) {
                    check(copied == asset.size) {
                        "Downloaded APK size mismatch (expected ${asset.size}, got $copied)"
                    }
                }

                if (target.exists()) target.delete()
                check(partial.renameTo(target)) { "Could not finalize downloaded APK" }
                onProgress(100)
                target
            } finally {
                connection.disconnect()
                if (partial.exists()) partial.delete()
            }
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.startActivity(
            installPermissionIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun installUpdate(context: Context, apk: File): Result<Unit> =
        runCatching {
            check(apk.exists() && apk.length() > 0L) { "Downloaded APK is missing" }
            check(canRequestPackageInstalls(context)) { "Install permission is required" }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                apk,
            )
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, APK_MIME_TYPE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }

    /** Removes APKs left behind after a completed, cancelled, or superseded update. */
    fun cleanupCachedUpdates(context: Context) {
        runCatching {
            File(context.cacheDir, UPDATE_CACHE_DIR)
                .listFiles()
                ?.forEach(File::delete)
        }
    }

    private const val UPDATE_CACHE_DIR = "updates"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
}
