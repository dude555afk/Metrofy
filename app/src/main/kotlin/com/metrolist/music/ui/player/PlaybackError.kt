/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackException
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.utils.InnerTubeXPlaybackResolver
import java.time.Instant

@Composable
fun PlaybackError(
    error: PlaybackException,
    isLoggedIn: Boolean,
    retry: () -> Unit,
    retryWithAnotherClient: (() -> Unit)? = null,
    diagnostics: InnerTubeXPlaybackResolver.PlaybackDiagnostics? = null,
    onOpenPlaybackHealth: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val causes = remember(error) { error.causeChain() }
    val reportedAt = remember(error) { Instant.now() }
    val errorReport = remember(error, diagnostics, reportedAt) {
        buildPlaybackErrorReport(error, diagnostics, reportedAt)
    }

    val rawErrorMessage = error.cause?.cause?.message 
        ?: error.cause?.message 
        ?: error.message 
        ?: stringResource(R.string.error_unknown)
    
    val isRestricted = rawErrorMessage.contains("age", ignoreCase = true) ||
            rawErrorMessage.contains("Sign in to confirm your age", ignoreCase = true) ||
            rawErrorMessage.contains("LOGIN_REQUIRED", ignoreCase = true) ||
            rawErrorMessage.contains("confirm your age", ignoreCase = true) ||
            rawErrorMessage.contains("AGE_CHECK_REQUIRED", ignoreCase = true) ||
            rawErrorMessage.contains("AGE_VERIFICATION_REQUIRED", ignoreCase = true) ||
            rawErrorMessage.contains("CONTENT_CHECK_REQUIRED", ignoreCase = true) ||
            rawErrorMessage.contains("country", ignoreCase = true) ||
            error.errorCode == PlaybackException.ERROR_CODE_REMOTE_ERROR
    
    val errorMessage = when {
        isRestricted && !isLoggedIn -> stringResource(R.string.error_restricted_not_logged_in)
        isRestricted && isLoggedIn -> stringResource(R.string.error_restricted_logged_in)
        else -> rawErrorMessage
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Error icon
        Icon(
            painter = painterResource(R.drawable.error),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Main error message
        Text(
            text = stringResource(R.string.error_playback_failed),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Error details
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Error code
        Text(
            text = "Code: ${getErrorCodeName(error.errorCode)} (${error.errorCode})",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        diagnostics?.activeClient?.takeIf { it.isNotBlank() }?.let { client ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.playback_stream_client, client),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        diagnostics?.failedClients?.takeIf { it.isNotEmpty() }?.let { failed ->
            Text(
                text = stringResource(
                    R.string.playback_failed_clients,
                    failed.sorted().joinToString(", "),
                ),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = retry,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.replay),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.retry))
            }

            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("Metrofy Playback Error", errorReport),
                    )
                },
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.content_copy),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.copy))
            }
        }

        if (retryWithAnotherClient != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = retryWithAnotherClient,
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(text = stringResource(R.string.try_another_stream_client))
            }
        }

        if (onOpenPlaybackHealth != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenPlaybackHealth,
                shape = RoundedCornerShape(20.dp),
            ) {
                Text("Playback Health & fixes")
            }
        }
    }
}

internal fun Throwable.causeChain(): List<Throwable> =
    generateSequence(this) { it.cause }.take(8).toList()

private fun buildPlaybackErrorReport(
    error: PlaybackException,
    diagnostics: InnerTubeXPlaybackResolver.PlaybackDiagnostics?,
    reportedAt: Instant,
): String =
    buildString {
        appendLine("Metrofy Playback Error Report")
        appendLine("============================")
        appendLine("Time: $reportedAt")
        appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Architecture: ${BuildConfig.ARCHITECTURE}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine()
        appendLine("Playback")
        appendLine("--------")
        appendLine("Video ID: ${diagnostics?.videoId ?: "Unknown"}")
        appendLine("Stream client: ${diagnostics?.activeClient ?: "Not resolved"}")
        appendLine(
            "Rejected clients: " +
                diagnostics?.failedClients?.sorted()?.joinToString(", ").orEmpty().ifBlank { "None" },
        )
        diagnostics?.lastResolvedAtMs?.let { appendLine("Last stream resolution: ${Instant.ofEpochMilli(it)}") }
        appendLine()
        appendLine("Error")
        appendLine("-----")
        appendLine("Code: ${getErrorCodeName(error.errorCode)} (${error.errorCode})")
        error.causeChain().forEachIndexed { index, cause ->
            appendLine("[$index] ${cause.javaClass.name}: ${cause.message ?: "(no message)"}")
        }
        appendLine()
        appendLine("Stack trace")
        appendLine("-----------")
        append(error.stackTraceToString())
    }

/**
 * Get human-readable error code name from PlaybackException error code
 */
private fun getErrorCodeName(errorCode: Int): String {
    return when (errorCode) {
        PlaybackException.ERROR_CODE_UNSPECIFIED -> "UNSPECIFIED"
        PlaybackException.ERROR_CODE_REMOTE_ERROR -> "REMOTE_ERROR"
        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "BEHIND_LIVE_WINDOW"
        PlaybackException.ERROR_CODE_TIMEOUT -> "TIMEOUT"
        PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> "FAILED_RUNTIME_CHECK"
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "IO_UNSPECIFIED"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "IO_NETWORK_CONNECTION_FAILED"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "IO_NETWORK_CONNECTION_TIMEOUT"
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "IO_INVALID_HTTP_CONTENT_TYPE"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "IO_BAD_HTTP_STATUS"
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "IO_FILE_NOT_FOUND"
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "IO_NO_PERMISSION"
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "IO_CLEARTEXT_NOT_PERMITTED"
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> "IO_READ_POSITION_OUT_OF_RANGE"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "PARSING_CONTAINER_MALFORMED"
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "PARSING_MANIFEST_MALFORMED"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "PARSING_CONTAINER_UNSUPPORTED"
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> "PARSING_MANIFEST_UNSUPPORTED"
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "DECODER_INIT_FAILED"
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "DECODER_QUERY_FAILED"
        PlaybackException.ERROR_CODE_DECODING_FAILED -> "DECODING_FAILED"
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> "DECODING_FORMAT_EXCEEDS_CAPABILITIES"
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "DECODING_FORMAT_UNSUPPORTED"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "AUDIO_TRACK_INIT_FAILED"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> "AUDIO_TRACK_WRITE_FAILED"
        PlaybackException.ERROR_CODE_DRM_UNSPECIFIED -> "DRM_UNSPECIFIED"
        PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED -> "DRM_SCHEME_UNSUPPORTED"
        PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> "DRM_PROVISIONING_FAILED"
        PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR -> "DRM_CONTENT_ERROR"
        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> "DRM_LICENSE_ACQUISITION_FAILED"
        PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION -> "DRM_DISALLOWED_OPERATION"
        PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR -> "DRM_SYSTEM_ERROR"
        PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED -> "DRM_DEVICE_REVOKED"
        PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED -> "DRM_LICENSE_EXPIRED"
        else -> "UNKNOWN_ERROR_$errorCode"
    }
}
