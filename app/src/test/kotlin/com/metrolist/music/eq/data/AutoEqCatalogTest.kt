package com.metrolist.music.eq.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoEqCatalogTest {

    @Test
    fun `index parser keeps source rig and earphone form`() {
        val entries = parseAutoEqIndex(
            """
            - [1Custom SA02](./crinacle/711%20in-ear/1Custom%20SA02) by crinacle on 711
            - [1MORE ComfoBuds Mini](./DHRME/in-ear/1MORE%20ComfoBuds%20Mini) by DHRME
            """.trimIndent(),
        )

        assertEquals(2, entries.size)
        assertEquals("crinacle", entries[0].source)
        assertEquals("711", entries[0].rig)
        assertEquals("in-ear", entries[0].form)
        assertEquals("DHRME", entries[1].source)
        assertEquals("in-ear", entries[1].form)
    }

    @Test
    fun `search matches model names without case sensitivity`() {
        val entries = parseAutoEqIndex(
            """
            - [Sony WH-1000XM4](./oratory1990/over-ear/Sony%20WH-1000XM4) by oratory1990
            - [Sennheiser HD 560S](./oratory1990/over-ear/Sennheiser%20HD%20560S) by oratory1990
            """.trimIndent(),
        )
        val models = groupAutoEqModels(entries)

        assertEquals(
            listOf("Sony WH-1000XM4"),
            searchAutoEqModels(models, "sony wh-1000").map { it.name },
        )
    }
}
