/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Sort selector adapted to Metrolist's expressive split-button pattern.
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.constants.PlaylistSongSortType

@Composable
inline fun <reified T : Enum<T>> SortHeader(
    sortType: T,
    sortDescending: Boolean,
    crossinline onSortTypeChange: (T) -> Unit,
    crossinline onSortDescendingChange: (Boolean) -> Unit,
    crossinline sortTypeText: (T) -> Int,
    modifier: Modifier = Modifier,
    showDescending: Boolean? = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 8.dp),
    ) {
        Box {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.TonalLeadingButton(
                        onClick = { menuExpanded = true },
                        shapes = SplitButtonDefaults.leadingButtonShapesFor(SplitButtonDefaults.SmallContainerHeight),
                        enabled = true,
                    ) {
                        Text(
                            text = stringResource(sortTypeText(sortType)),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                },
                trailingButton = {
                    if (sortType != PlaylistSongSortType.CUSTOM && showDescending == true) {
                        SplitButtonDefaults.TrailingButton(
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            elevation = ButtonDefaults.filledTonalButtonElevation(),
                            onClick = { onSortDescendingChange(!sortDescending) },
                            shapes = SplitButtonDefaults.trailingButtonShapesFor(SplitButtonDefaults.SmallContainerHeight),
                            modifier = Modifier.size(44.dp, SplitButtonDefaults.SmallContainerHeight),
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (sortDescending) R.drawable.arrow_downward else R.drawable.arrow_upward,
                                ),
                                contentDescription = stringResource(
                                    if (sortDescending) R.string.sort_descending else R.string.sort_ascending,
                                ),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    } else {
                        SplitButtonDefaults.TrailingButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.size(12.dp, SplitButtonDefaults.SmallContainerHeight),
                        ) {}
                    }
                },
            )

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                enumValues<T>().forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(sortTypeText(type)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(
                                    if (sortType == type) {
                                        R.drawable.radio_button_checked
                                    } else {
                                        R.drawable.radio_button_unchecked
                                    },
                                ),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            onSortTypeChange(type)
                            menuExpanded = false
                        },
                    )
                }
            }
        }
    }
}
