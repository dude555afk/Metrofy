/**
 * Connected list/grid toggle adapted from Metrolist's LibraryContent ButtonGroup.
 * Uses Metrofy's existing LibraryViewType state and callbacks.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MetrolistLibraryViewToggle(
    isGrid: Boolean,
    onListClick: () -> Unit,
    onGridClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ToggleButton(
            checked = !isGrid,
            onCheckedChange = { checked -> if (checked) onListClick() },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.list),
                contentDescription = stringResource(R.string.switch_to_grid_view),
                modifier = Modifier.size(20.dp),
            )
        }
        ToggleButton(
            checked = isGrid,
            onCheckedChange = { checked -> if (checked) onGridClick() },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.grid_view),
                contentDescription = stringResource(R.string.switch_to_list_view),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
