/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.UpdateChannelKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.MetrolistSettingsSearchField
import com.metrolist.music.ui.component.MetrolistSettingsSearchResultRow
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.ReleaseNotesCard
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.UpdateChannel
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.rememberPreference
import androidx.compose.runtime.remember

private data class SettingsSearchDestination(
    val title: String,
    val description: String,
    val icon: Int,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val updateChannelName by rememberPreference(UpdateChannelKey, UpdateChannel.STABLE.name)
    val updateChannel =
        runCatching { UpdateChannel.valueOf(updateChannelName) }
            .getOrDefault(UpdateChannel.STABLE)
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val hasAndroidAuto = remember {
        try {
            context.packageManager.getPackageInfo(
                "com.google.android.projection.gearhead", 0
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val showChangelog = com.metrolist.music.LocalChangelogState.current

    val appearanceTitle = stringResource(R.string.appearance)
    val playerTitle = stringResource(R.string.player_and_audio)
    val contentTitle = stringResource(R.string.content)
    val aiLyricsTitle = stringResource(R.string.ai_lyrics_translation)
    val androidAutoTitle = stringResource(R.string.android_auto)
    val privacyTitle = stringResource(R.string.privacy)
    val storageTitle = stringResource(R.string.storage)
    val backupTitle = stringResource(R.string.backup_restore)
    val updaterTitle = stringResource(R.string.updater)
    val changelogTitle = stringResource(R.string.changelog)
    val aboutTitle = stringResource(R.string.about)

    val settingsSearchDestinations = buildList {
        add(SettingsSearchDestination(appearanceTitle, "Theme, colors and interface", R.drawable.palette) { navController.navigate("settings/appearance") })
        add(SettingsSearchDestination(playerTitle, "Playback, audio and player controls", R.drawable.play) { navController.navigate("settings/player") })
        add(SettingsSearchDestination("Playback Health", "Stream client, failures and repair tools", R.drawable.info) { navController.navigate("settings/playback_health") })
        add(SettingsSearchDestination(contentTitle, "Content preferences and filtering", R.drawable.language) { navController.navigate("settings/content") })
        add(SettingsSearchDestination(aiLyricsTitle, "AI lyrics and translation settings", R.drawable.translate) { navController.navigate("settings/ai") })
        if (hasAndroidAuto) {
            add(SettingsSearchDestination(androidAutoTitle, "Android Auto playback and controls", R.drawable.ic_android_auto) { navController.navigate("settings/android_auto") })
        }
        add(SettingsSearchDestination(privacyTitle, "Privacy and security", R.drawable.security) { navController.navigate("settings/privacy") })
        add(SettingsSearchDestination(storageTitle, "Cache, downloads and local storage", R.drawable.storage) { navController.navigate("settings/storage") })
        add(SettingsSearchDestination(backupTitle, "Back up and restore app data", R.drawable.restore) { navController.navigate("settings/backup_restore") })
        if (BuildConfig.UPDATER_AVAILABLE) {
            add(SettingsSearchDestination(updaterTitle, "Updates and release channels", R.drawable.update) { navController.navigate("settings/updater") })
        }
        add(SettingsSearchDestination(changelogTitle, "Recent changes and release notes", R.drawable.newspaper) { showChangelog.value = true })
        add(SettingsSearchDestination(aboutTitle, "App version, contributors and licenses", R.drawable.info) { navController.navigate("settings/about") })
    }
    val filteredSettings = settingsSearchDestinations.filter { destination ->
        searchQuery.isBlank() ||
            destination.title.contains(searchQuery.trim(), ignoreCase = true) ||
            destination.description.contains(searchQuery.trim(), ignoreCase = true)
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        MetrolistSettingsSearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )

        if (searchQuery.isBlank()) {
            // User Interface Section
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_ui),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.appearance)) },
                        onClick = { navController.navigate("settings/appearance") }
                    )
                )
            )
    
            Spacer(modifier = Modifier.height(16.dp))
    
            // Player & Content Section (moved up and combined with content)
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_player_content),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.play),
                        title = { Text(stringResource(R.string.player_and_audio)) },
                        onClick = { navController.navigate("settings/player") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.info),
                        title = { Text("Playback Health") },
                        description = { Text("Stream client, failures and repair tools") },
                        onClick = { navController.navigate("settings/playback_health") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.language),
                        title = { Text(stringResource(R.string.content)) },
                        onClick = { navController.navigate("settings/content") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.translate),
                        title = { Text(stringResource(R.string.ai_lyrics_translation)) },
                        onClick = { navController.navigate("settings/ai") }
                    )
                )
            )
    
            Spacer(modifier = Modifier.height(16.dp))
    
            // Android Auto Section — only shown if Android Auto is installed
            if (hasAndroidAuto) {
                Material3SettingsGroup(
                    title = "Android Auto",
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.ic_android_auto),
                            title = { Text(stringResource(R.string.android_auto)) },
                            onClick = { navController.navigate("settings/android_auto") }
                        )
                    )
                )
    
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Privacy & Security Section
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_privacy),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.security),
                        title = { Text(stringResource(R.string.privacy)) },
                        onClick = { navController.navigate("settings/privacy") }
                    )
                )
            )
    
            Spacer(modifier = Modifier.height(16.dp))
    
            // Storage & Data Section
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_storage),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.storage),
                        title = { Text(stringResource(R.string.storage)) },
                        onClick = { navController.navigate("settings/storage") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.restore),
                        title = { Text(stringResource(R.string.backup_restore)) },
                        onClick = { navController.navigate("settings/backup_restore") }
                    )
                )
            )
    
            Spacer(modifier = Modifier.height(16.dp))
    
            // System & About Section
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_system),
                items = buildList {
                    if (isAndroid12OrLater) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.link),
                                title = { Text(stringResource(R.string.default_links)) },
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                            "package:${context.packageName}".toUri()
                                        )
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        when (e) {
                                            is ActivityNotFoundException -> {
                                                Toast.makeText(
                                                    context,
                                                    R.string.open_app_settings_error,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
    
                                            is SecurityException -> {
                                                Toast.makeText(
                                                    context,
                                                    R.string.open_app_settings_error,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
    
                                            else -> {
                                                Toast.makeText(
                                                    context,
                                                    R.string.open_app_settings_error,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            )
                        )
                    }
                    if (BuildConfig.UPDATER_AVAILABLE) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.update),
                                title = { Text(stringResource(R.string.updater)) },
                                onClick = { navController.navigate("settings/updater") }
                            )
                        )
                    }
                    val showChangelog = com.metrolist.music.LocalChangelogState.current
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.newspaper),
                            title = { Text(stringResource(R.string.changelog)) },
                            onClick = { showChangelog.value = true }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.info),
                            title = { Text(stringResource(R.string.about)) },
                            onClick = { navController.navigate("settings/about") }
                        )
                    )
                    if (
                        BuildConfig.UPDATER_AVAILABLE &&
                        Updater.isUpdateAvailable(BuildConfig.VERSION_NAME, latestVersionName)
                    ) {
                        val releaseInfo = Updater.getCachedLatestRelease(updateChannel)
                        val hasCompatibleAsset =
                            releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) } != null
    
                        if (hasCompatibleAsset) {
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.update),
                                    title = {
                                        Text(
                                            text = stringResource(R.string.new_version_available),
                                        )
                                    },
                                    description = {
                                        Text(
                                            text = latestVersionName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    showBadge = true,
                                    onClick = { navController.navigate("settings/updater") }
                                )
                            )
                        }
                    }
                }
            )
        if (BuildConfig.UPDATER_AVAILABLE && Updater.isUpdateAvailable(BuildConfig.VERSION_NAME, latestVersionName)) {
                Spacer(modifier = Modifier.height(16.dp))
                ReleaseNotesCard()
            }
    
    
        } else if (filteredSettings.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                filteredSettings.forEachIndexed { index, destination ->
                    MetrolistSettingsSearchResultRow(
                        title = destination.title,
                        supportingText = destination.description,
                        iconRes = destination.icon,
                        query = searchQuery,
                        isFirst = index == 0,
                        isLast = index == filteredSettings.lastIndex,
                        onClick = destination.onClick,
                    )
                }
            }
        } else {
            Text(
                text = "No matching settings",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}
