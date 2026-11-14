package com.metrolist.music.eq.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class AutoEqEntry(
    val name: String,
    val source: String,
    val rig: String?,
    val form: String,
    val path: String,
)

data class AutoEqModel(
    val name: String,
    val entries: List<AutoEqEntry>,
)

/**
 * Cached client for the public AutoEq results catalogue.
 *
 * Network work runs on Dispatchers.IO. Profiles are parsed into Metrofy's existing
 * ParametricEQ format and applied through the existing equalizer service.
 */
object AutoEqCatalog {
    private const val RESULTS_URL = "https://raw.githubusercontent.com/jaakkopasanen/AutoEq/master/results"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 20_000

    private val mutex = Mutex()
    @Volatile
    private var cachedModels: List<AutoEqModel>? = null

    suspend fun models(): List<AutoEqModel> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedModels ?: groupAutoEqModels(parseAutoEqIndex(fetch("$RESULTS_URL/INDEX.md")))
                .also { cachedModels = it }
        }
    }

    suspend fun load(entry: AutoEqEntry): ParametricEQ = withContext(Dispatchers.IO) {
        val fileName = entry.path.substringAfterLast('/') + "%20ParametricEQ.txt"
        val text = fetch("$RESULTS_URL/" + entry.path + "/$fileName")
        val profile = ParametricEQParser.parseText(text)
        val errors = ParametricEQParser.validate(profile)
        require(errors.isEmpty()) {
            errors.firstOrNull() ?: "Invalid AutoEq profile"
        }
        profile
    }

    private fun fetch(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Metrofy-AutoEQ/1.0")

        return try {
            val responseCode = connection.responseCode
            check(responseCode in 200..299) {
                "AutoEQ request failed with HTTP $responseCode"
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

private val autoEqIndexLine = Regex("""^- \[(.+)]\(\./(.+)\) by (.+?)(?: on (.+))?$""")

fun parseAutoEqIndex(markdown: String): List<AutoEqEntry> =
    markdown.lineSequence().mapNotNull { line ->
        val match = autoEqIndexLine.matchEntire(line.trim()) ?: return@mapNotNull null
        val (name, path, source, rig) = match.destructured
        val formDirectory = path.split('/').getOrNull(1) ?: path.split('/').firstOrNull().orEmpty()
        val form = formDirectory.substringAfterLast("%20")
        if (name.isBlank() || path.isBlank() || source.isBlank()) {
            null
        } else {
            AutoEqEntry(
                name = name,
                source = source,
                rig = rig.ifBlank { null },
                form = form,
                path = path,
            )
        }
    }.toList()

fun groupAutoEqModels(entries: List<AutoEqEntry>): List<AutoEqModel> =
    entries.groupBy { entry ->
        entry.name.replace(Regex("""\s*\([^)]*\)"""), "").trim()
    }.map { (name, modelEntries) ->
        AutoEqModel(name = name, entries = modelEntries)
    }.sortedBy { it.name.lowercase() }

fun searchAutoEqModels(
    models: List<AutoEqModel>,
    query: String,
): List<AutoEqModel> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return models

    val words = needle.split(Regex("\\s+"))
    return models.filter { model ->
        words.all { word -> model.name.lowercase().contains(word) }
    }.sortedBy { model ->
        val name = model.name.lowercase()
        when {
            name == needle -> 0
            name.startsWith(needle) -> 1
            name.split(' ', '-').any { it.startsWith(words.first()) } -> 2
            else -> 3
        }
    }
}
