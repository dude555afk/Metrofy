/**
 * Android source port of Metrolist shared HomeContent.HomeGallery.
 *
 * Each pager page is a fixed-width column containing a vertical run of items.
 * Metrofy supplies the item renderer so existing database, playback and menu
 * behavior stays connected to Metrofy's own ViewModels and repositories.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <T> MetrolistHomeGallery(
    items: List<T>,
    rows: Int,
    itemWidth: Dp,
    rowHeight: Dp,
    pageSpacing: Dp,
    modifier: Modifier = Modifier,
    rowSpacing: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    key: (Int, T) -> Any = { index, item -> "${index}_${item.hashCode()}" },
    content: @Composable (index: Int, item: T, modifier: Modifier) -> Unit,
) {
    if (items.isEmpty() || rows <= 0) return

    val pageCount = (items.size + rows - 1) / rows
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val flingBehavior = PagerDefaults.flingBehavior(
        pagerState = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(pageCount),
    )

    HorizontalPager(
        state = pagerState,
        pageSize = PageSize.Fixed(itemWidth),
        pageSpacing = pageSpacing,
        flingBehavior = flingBehavior,
        key = { page -> key(page * rows, items[page * rows]) },
        verticalAlignment = androidx.compose.ui.Alignment.Top,
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight * rows + rowSpacing * (rows - 1))
            .clipToBounds(),
    ) { page ->
        Column(
            modifier = Modifier.width(itemWidth),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
        ) {
            repeat(rows) { row ->
                val index = page * rows + row
                items.getOrNull(index)?.let { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .clipToBounds(),
                    ) {
                        content(index, item, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
