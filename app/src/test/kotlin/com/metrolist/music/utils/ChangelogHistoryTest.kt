package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogHistoryTest {
    @Test
    fun `reads every release and keeps newest first`() {
        val markdown = """
            ---v0.8.9.7
            # UI
            - Better animations.

            ---v0.8.9.6
            # Fixes
            - Improved playback.
            ---v13.4.0
            # Upstream changes
            - Kept history.
        """.trimIndent()

        val entries = parseChangelogEntries(markdown)

        assertEquals(listOf("0.8.9.7", "0.8.9.6", "13.4.0"), entries.map { it.version })
        assertTrue(entries[0].notes.contains("Better animations."))
        assertTrue(entries[1].notes.contains("Improved playback."))
        assertTrue(entries[2].notes.contains("Kept history."))
    }

    @Test
    fun `keeps section markdown without swallowing next heading`() {
        val entries = parseChangelogEntries("---v1.2.0\\n# Title\\n- Feature A\\n\\n---v1.1.0\\n- Fixed B")
        assertEquals("# Title\\n- Feature A", entries[0].notes)
        assertEquals("- Fixed B", entries[1].notes)
    }

    @Test
    fun `does not show orphaned, duplicate-empty or missing release sections`() {
        val entries = parseChangelogEntries("Orphan notes\\n---v1.0.0\\n  \\n---v0.9.0\\n- Actual changes")
        assertEquals(1, entries.size)
        assertEquals("0.9.0", entries.single().version)
    }
}
