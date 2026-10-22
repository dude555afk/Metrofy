/**
 * SoundPod-style compact pill tabs for the isolated Metrofy Nova preview.
 * Metrofy continues to own page state, data loading, and playback behaviour.
 */
package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun <T> NovaFilterTabs(
    chips: List<Pair<T, String>>,
    currentValue: T?,
    onValueUpdate: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(items = chips, key = { it.second }) { option ->
            val selected = option.first == currentValue
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else Color.Transparent,
                    )
                    .clickable { onValueUpdate(option.first) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.second,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
