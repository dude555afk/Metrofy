/**
 * Android source adaptation of Metrolist's CollectionHeader and CollectionInfoCard.
 * The supplied artwork/content/actions slots retain Metrofy's database and playback integrations.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MetrolistCollectionHeader(
    isWide: Boolean,
    isMedium: Boolean,
    modifier: Modifier = Modifier,
    mediumArtworkSize: Dp = 176.dp,
    artwork: @Composable (Modifier, Dp) -> Unit,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val headerModifier =
        when {
            isWide -> Modifier.fillMaxWidth()
            isMedium -> Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
            else -> Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(vertical = 8.dp)
        }

    Column(modifier.then(headerModifier)) {
        if (isMedium && !isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                artwork(Modifier.size(mediumArtworkSize), 16.dp)
                MetrolistCollectionInfoCard(Modifier.weight(1f), content)
            }
        } else {
            artwork(Modifier.fillMaxWidth().aspectRatio(1f), if (isWide) 20.dp else 28.dp)
            Spacer(Modifier.height(16.dp))
            MetrolistCollectionInfoCard(content = content)
        }
        actions?.let {
            Spacer(Modifier.height(16.dp))
            it()
        }
    }
}

@Composable
fun MetrolistCollectionInfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
