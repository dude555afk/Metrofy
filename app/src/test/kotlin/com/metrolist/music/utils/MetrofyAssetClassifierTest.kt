package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetrofyAssetClassifierTest {
    @Test
    fun `recognizes current official release assets`() {
        assertEquals("universal" to "foss", classifyMetrofyApk("Metrofy.apk"))
        assertEquals("universal" to "gms", classifyMetrofyApk("Metrofy-with-Google-Cast.apk"))
    }

    @Test
    fun `recognizes capitalized preview assets and older lowercase preview aliases`() {
        assertEquals("universal" to "foss", classifyMetrofyApk("Metrofy-Preview.apk"))
        assertEquals("universal" to "gms", classifyMetrofyApk("Metrofy-Preview-with-Google-Cast.apk"))
        assertEquals("universal" to "foss", classifyMetrofyApk("Metrofy-preview.apk"))
        assertEquals("universal" to "gms", classifyMetrofyApk("Metrofy-preview-with-Google-Cast.apk"))
    }

    @Test
    fun `does not classify unrelated assets`() {
        assertNull(classifyMetrofyApk("notes.txt"))
        assertNull(classifyMetrofyApk("Metrofy-izzy.apk"))
        assertNull(classifyMetrofyApk("some-other-app.apk"))
    }
}
