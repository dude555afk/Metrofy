/**
 * Parses Metrofy's bundled changelog.md sections into release history.
 * The release workflow uses the same ---vX.Y.Z delimiters for GitHub notes.
 */
package com.metrolist.music.utils

data class ChangelogEntry(
    val version: String,
    val notes: String,
)

fun parseChangelogEntries(text: String): List<ChangelogEntry> {
    val sections = mutableListOf<ChangelogEntry>()
    var version: String? = null
    val notes = StringBuilder()

    fun appendSection() {
        val currentVersion = version ?: return
        val description = notes.toString().trim()
        if (description.isNotEmpty()) {
            sections += ChangelogEntry(version = currentVersion, notes = description)
        }
        notes.clear()
    }

    text.lineSequence().forEach { line ->
        val nextVersion = line.trim().takeIf { it.matches(Regex("---v[^\\s]+")) }
            ?.removePrefix("---v")
        if (nextVersion != null) {
            appendSection()
            version = nextVersion
        } else if (version != null) {
            notes.appendLine(line)
        }
    }
    appendSection()
    return sections
}
