/**
 * Android adaptation of Metrolist PodcastContent.EpisodeRow.
 * Uses Metrofy's EpisodeItem model and ItemThumbnail playback indicator.
 * Episode actions and queue behavior remain owned by OnlinePodcastScreen.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.models.EpisodeItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetrolistPodcastEpisodeRow(
    episode: EpisodeItem,
    isFirst: Boolean,
    isLast: Boolean,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val outerRadius = 20.dp
    val innerRadius = 6.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) outerRadius else innerRadius,
        topEnd = if (isFirst) outerRadius else innerRadius,
        bottomStart = if (isLast) outerRadius else innerRadius,
        bottomEnd = if (isLast) outerRadius else innerRadius,
    )
    val metadata = listOfNotNull(
        episode.author?.name,
        episode.publishDateText?.takeIf(String::isNotBlank),
    ).joinToString(" • ")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemThumbnail(
                thumbnailUrl = episode.thumbnail,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(64.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metadata.isNotBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailingContent()
        }
    }
}
