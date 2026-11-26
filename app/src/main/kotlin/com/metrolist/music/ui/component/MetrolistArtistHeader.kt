/**
 * Android adaptation of Metrolist's responsive ArtistHeaderImage and ArtistHeaderDetails.
 * Metrofy subscription and YouTube queue callbacks are injected by the existing screen.
 * Licensed under GPL-3.0.
 */
package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.music.R

@Composable
fun MetrolistArtistHeader(
    artistName: String,
    imageUrl: String?,
    imageOffsetY: Int = 0,
    subscriberCount: String? = null,
    monthlyListeners: String? = null,
    isSubscribed: Boolean,
    showSubscribe: Boolean,
    showRadio: Boolean,
    showShuffle: Boolean,
    onSubscribe: () -> Unit,
    onRadio: () -> Unit,
    onShuffle: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isSquareHeader = maxWidth < 600.dp
        val headerHorizontalPadding = if (maxWidth >= 960.dp) 32.dp else 20.dp
        val imageHeight =
            when {
                isSquareHeader -> maxWidth
                maxWidth >= 960.dp -> 280.dp
                else -> 320.dp
            }

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .offset { IntOffset(0, imageOffsetY) },
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.20f),
                                0.48f to Color.Transparent,
                                0.76f to MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                1f to MaterialTheme.colorScheme.surface,
                            ),
                        ),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp)
                    .padding(horizontal = headerHorizontalPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subscriberCount?.takeIf(String::isNotBlank)?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    monthlyListeners?.takeIf(String::isNotBlank)?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showSubscribe) {
                        FilledTonalButton(
                            onClick = onSubscribe,
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSubscribed) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                contentColor = if (isSubscribed) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                            ),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Text(
                                text = stringResource(if (isSubscribed) R.string.subscribed else R.string.subscribe),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showRadio) {
                            FilledTonalButton(
                                onClick = onRadio,
                                shape = CircleShape,
                                modifier = Modifier.height(44.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.radio),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.radio), maxLines = 1)
                            }
                        }

                        if (showShuffle) {
                            IconButton(
                                onClick = onShuffle,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = stringResource(R.string.shuffle),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
