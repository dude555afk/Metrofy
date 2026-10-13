/**
 * Metrofy playback health center.
 */
package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackHealthScreen(
    navController: NavController,
) {
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    var diagnostics by remember { mutableStateOf(playerConnection?.playbackDiagnostics()) }
    var repairStatus by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(playerConnection) {
        val connection = playerConnection ?: return@LaunchedEffect
        while (true) {
            diagnostics = connection.playbackDiagnostics()
            delay(1_000)
        }
    }

    fun formatTime(value: Long?): String =
        value?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(it)) }
            ?: "Never"

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
            ),
        )
        Spacer(Modifier.height(8.dp))

        val d = diagnostics
        Material3SettingsGroup(
            title = "Current stream",
            items =
                listOf(
                    Material3SettingsItem(
                        title = { Text("Video ID") },
                        description = { Text(d?.videoId ?: "Nothing playing") },
                    ),
                    Material3SettingsItem(
                        title = { Text("Active InnerTube client") },
                        description = { Text(d?.activeClient ?: "Not resolved yet") },
                    ),
                    Material3SettingsItem(
                        title = { Text("Rejected clients") },
                        description = {
                            Text(
                                d?.failedClients
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.joinToString(", ")
                                    ?: "None",
                            )
                        },
                    ),
                    Material3SettingsItem(
                        title = { Text("Last successful resolution") },
                        description = { Text(formatTime(d?.lastResolvedAtMs)) },
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))

        Material3SettingsGroup(
            title = "Resolver health",
            items =
                listOf(
                    Material3SettingsItem(
                        title = { Text("Successful resolutions") },
                        description = { Text((d?.totalResolutions ?: 0L).toString()) },
                    ),
                    Material3SettingsItem(
                        title = { Text("Resolution failures") },
                        description = { Text((d?.totalFailures ?: 0L).toString()) },
                    ),
                    Material3SettingsItem(
                        title = { Text("Last failure") },
                        description = { Text(d?.lastFailureMessage ?: "None recorded") },
                    ),
                    Material3SettingsItem(
                        title = { Text("Last resolver refresh") },
                        description = { Text(formatTime(d?.lastRefreshAtMs)) },
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))

        Button(
            enabled = playerConnection != null && d?.videoId != null,
            onClick = {
                playerConnection?.retryWithAnotherStreamClient()
                repairStatus = "Retrying with another stream client…"
                diagnostics = playerConnection?.playbackDiagnostics()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Try another stream client")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            enabled = playerConnection != null,
            onClick = {
                playerConnection?.clearPlaybackClientFailures()
                diagnostics = playerConnection?.playbackDiagnostics()
                repairStatus = "Rejected-client list cleared"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Clear rejected clients")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            enabled = playerConnection != null && !refreshing,
            onClick = {
                val connection = playerConnection ?: return@Button
                scope.launch {
                    refreshing = true
                    repairStatus = "Refreshing player config and prewarming resolver…"
                    connection.refreshPlaybackEngine()
                        .onSuccess {
                            repairStatus =
                                if (it) {
                                    "Playback engine refreshed with a new player config"
                                } else {
                                    "Playback engine refreshed; current config is still valid"
                                }
                        }
                        .onFailure { repairStatus = it.message ?: "Playback refresh failed" }
                    diagnostics = connection.playbackDiagnostics()
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (refreshing) "Refreshing…" else "Refresh playback engine")
        }

        repairStatus?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    TopAppBar(
        title = { Text("Playback Health") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
