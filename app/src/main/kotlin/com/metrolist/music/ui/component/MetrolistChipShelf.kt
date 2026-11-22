/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Android/Hilt-compatible port of Metrolist's shared ChipShelf.
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp

private val ChipShelfHeight = 48.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> MetrolistChipShelf(
    options: List<T>,
    selectedOption: T?,
    onSelectionChange: (T?) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    allLabel: String? = null,
    selectionRequired: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp),
    scrollState: ScrollState = rememberScrollState(),
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ChipShelfHeight)
                .clipToBounds()
                .focusGroup()
                .selectableGroup()
                .horizontalScroll(scrollState)
                .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        allLabel?.let { text ->
            ShelfChip(
                text = text,
                selected = selectedOption == null,
                onClick = { onSelectionChange(null) },
            )
        }

        options.forEach { option ->
            val isSelected = selectedOption == option
            ShelfChip(
                text = label(option),
                selected = isSelected,
                onClick = {
                    onSelectionChange(
                        if (isSelected && !selectionRequired) null else option,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShelfChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        shapes = ToggleButtonDefaults.shapesFor(ButtonDefaults.ExtraSmallContainerHeight),
        contentPadding = ButtonDefaults.ExtraSmallContentPadding,
        modifier =
            Modifier
                .height(ButtonDefaults.ExtraSmallContainerHeight)
                .semantics {
                    role = Role.Button
                    this.selected = selected
                },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}
