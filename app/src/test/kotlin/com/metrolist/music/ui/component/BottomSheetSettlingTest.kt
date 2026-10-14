package com.metrolist.music.ui.component

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomSheetSettlingTest {
    private val dismissed = (-80).dp
    private val collapsed = 64.dp
    private val expanded = 800.dp

    @Test
    fun `dismiss threshold is the midpoint between the two absolute anchors`() {
        assertEquals(
            dismissedAnchor,
            calculateSheetSettlingAnchor((-9).dp, dismissed, collapsed, expanded, true),
        )
        assertEquals(
            collapsedAnchor,
            calculateSheetSettlingAnchor((-8).dp, dismissed, collapsed, expanded, true),
        )
    }

    @Test
    fun `expand threshold accounts for the collapsed anchor offset`() {
        assertEquals(
            collapsedAnchor,
            calculateSheetSettlingAnchor(431.dp, dismissed, collapsed, expanded, true),
        )
        assertEquals(
            expandedAnchor,
            calculateSheetSettlingAnchor(432.dp, dismissed, collapsed, expanded, true),
        )
    }

    @Test
    fun `cancelled drag cannot dismiss the sheet`() {
        assertEquals(
            collapsedAnchor,
            calculateSheetSettlingAnchor((-60).dp, dismissed, collapsed, expanded, false),
        )
    }

    @Test
    fun `dismissed and collapsed anchors at same point do not result in dismiss`() {
        assertEquals(
            collapsedAnchor,
            calculateSheetSettlingAnchor(64.dp, 64.dp, 64.dp, expanded, true),
        )
    }
}
