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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.ReleaseNotesCard
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.UpdateChannel
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.rememberPreference
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val showChangelog = com.metrolist.music.LocalChangelogState.current
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

    val settingsSearchEntries = buildList {
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.appearance),
                description = "Theme, colours, layout and visual preferences",
                keywords = listOf("theme", "colour", "color", "dark mode", "dynamic colour", "dynamic color"),
                icon = R.drawable.palette,
                onClick = { navController.navigate("settings/appearance") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.player_and_audio),
                description = "Equalizer, playback, queue, crossfade and audio quality",
                keywords = listOf("equalizer", "eq", "crossfade", "sleep timer", "speed", "pitch", "queue", "normalization"),
                icon = R.drawable.play,
                onClick = { navController.navigate("settings/player") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = "Playback Health",
                description = "Stream client, failures and repair tools",
                keywords = listOf("diagnostics", "stream", "troubleshooting", "errors", "repair"),
                icon = R.drawable.info,
                onClick = { navController.navigate("settings/playback_health") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.content),
                description = "Home feed, search, recommendations and content filters",
                keywords = listOf("home", "youtube", "spotify home", "recommendations", "explicit", "shorts"),
                icon = R.drawable.language,
                onClick = { navController.navigate("settings/content") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.ai_lyrics_translation),
                description = "AI provider, model and lyrics translation",
                keywords = listOf("ai", "lyrics", "translation", "gemini", "openrouter", "prompt"),
                icon = R.drawable.translate,
                onClick = { navController.navigate("settings/ai") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.integrations),
                description = "Spotify, Discord, Last.fm and Listen Together",
                keywords = listOf("connect", "account", "spotify", "discord", "lastfm", "last.fm", "together"),
                icon = R.drawable.spotify,
                onClick = { navController.navigate("settings/integrations") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.spotify_integration),
                description = "Spotify account, Home, Search, Canvas and sync",
                keywords = listOf("spotify home", "spotify only", "spotify search", "canvas", "spotify login"),
                icon = R.drawable.spotify,
                onClick = { navController.navigate("settings/integrations/spotify") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.discord_integration),
                description = "Discord integration and account",
                keywords = listOf("discord", "rich presence"),
                icon = R.drawable.discord,
                onClick = { navController.navigate("settings/integrations/discord") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.lastfm_integration),
                description = "Last.fm scrobbling and account",
                keywords = listOf("lastfm", "last.fm", "scrobble"),
                icon = R.drawable.music_note,
                onClick = { navController.navigate("settings/integrations/lastfm") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = "Listen Together",
                description = "Shared listening and room preferences",
                keywords = listOf("together", "room", "host", "guest", "sync playback"),
                icon = R.drawable.group_outlined,
                onClick = { navController.navigate("settings/integrations/listen_together") },
            ),
        )
        if (hasAndroidAuto) {
            add(
                SettingsSearchEntry(
                    title = stringResource(R.string.android_auto),
                    description = "Android Auto playback and pinned playlists",
                    keywords = listOf("car", "automotive", "android auto"),
                    icon = R.drawable.ic_android_auto,
                    onClick = { navController.navigate("settings/android_auto") },
                ),
            )
        }
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.privacy),
                description = "Privacy, security and hidden content",
                keywords = listOf("hidden home", "hidden library", "privacy", "security", "tracking"),
                icon = R.drawable.security,
                onClick = { navController.navigate("settings/privacy") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.storage),
                description = "Downloads, cache and local storage",
                keywords = listOf("downloads", "cache", "files", "storage"),
                icon = R.drawable.storage,
                onClick = { navController.navigate("settings/storage") },
            ),
        )
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.backup_restore),
                description = "Back up and restore Metrofy data",
                keywords = listOf("backup", "restore", "export", "import", "database"),
                icon = R.drawable.restore,
                onClick = { navController.navigate("settings/backup_restore") },
            ),
        )
        if (BuildConfig.UPDATER_AVAILABLE) {
            add(
                SettingsSearchEntry(
                    title = stringResource(R.string.updater),
                    description = "App updates and release channels",
                    keywords = listOf("update", "version", "preview", "stable", "release"),
                    icon = R.drawable.update,
                    onClick = { navController.navigate("settings/updater") },
                ),
            )
        }
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.changelog),
                description = "See what changed in Metrofy",
                keywords = listOf("changes", "release notes", "what's new"),
                icon = R.drawable.newspaper,
                onClick = { showChangelog.value = true },
            ),
        )
        add(
            SettingsSearchEntry(
                title = stringResource(R.string.about),
                description = "App version, credits and project information",
                keywords = listOf("version", "credits", "licence", "license", "project"),
                icon = R.drawable.info,
                onClick = { navController.navigate("settings/about") },
            ),
        )
    }
    val searchTokens = searchQuery.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    val searchResults = settingsSearchEntries.filter { entry ->
        val searchable = (listOf(entry.title, entry.description) + entry.keywords)
            .joinToString(" ")
            .lowercase()
        searchTokens.all(searchable::contains)
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

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CircleShape,
            placeholder = { Text(stringResource(R.string.settings_search_placeholder)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.clear),
                        )
                    }
                }
            } else {
                null
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (searchQuery.isNotBlank()) {
            Material3SettingsGroup(
                title = stringResource(R.string.settings_search_results, searchResults.size),
                items = searchResults.map { entry ->
                    Material3SettingsItem(
                        icon = painterResource(entry.icon),
                        title = { Text(entry.title) },
                        description = { Text(entry.description) },
                        onClick = entry.onClick,
                    )
                },
            )
            if (searchResults.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_search_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp),
                )
            }
        } else {
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

        Material3SettingsGroup(
            title = stringResource(R.string.integrations),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.spotify),
                    title = { Text(stringResource(R.string.integrations)) },
                    description = { Text("Spotify, Discord, Last.fm and Listen Together") },
                    onClick = { navController.navigate("settings/integrations") },
                ),
            ),
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
        } // End searchable page list / normal settings groups

    if (searchQuery.isBlank() && BuildConfig.UPDATER_AVAILABLE && Updater.isUpdateAvailable(BuildConfig.VERSION_NAME, latestVersionName)) {
            Spacer(modifier = Modifier.height(16.dp))
            ReleaseNotesCard()
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

private data class SettingsSearchEntry(
    val title: String,
    val description: String,
    val keywords: List<String>,
    val icon: Int,
    val onClick: () -> Unit,
)
