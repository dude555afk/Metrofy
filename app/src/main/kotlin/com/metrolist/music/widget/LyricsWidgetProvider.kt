/**
 * Adapted from Pear Music's home-screen lyrics AppWidgetProvider.
 * Original project: https://github.com/dragonzords/Pear-Music
 * GPL-3.0; Metrofy adaptation dated 2026-11-19.
 */
package com.metrolist.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.metrolist.music.MainActivity
import com.metrolist.music.playback.MusicService

class LyricsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        LyricsWidgetState.installedCache = true
        LyricsWidgetState.push(context)
        requestRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        requestRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_PLAY) {
            runCatching {
                context.startService(
                    Intent(context, MusicService::class.java)
                        .setAction(MusicWidgetReceiver.ACTION_LYRICS_PLAY_PAUSE)
                )
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        LyricsWidgetState.installedCache = null
    }

    private fun requestRefresh(context: Context) {
        if (MusicService.isRunning) {
            runCatching {
                context.startService(
                    Intent(context, MusicService::class.java)
                        .setAction(MusicWidgetReceiver.ACTION_UPDATE_LYRICS_WIDGET)
                )
            }
        }
    }

    companion object {
        const val ACTION_PLAY = "com.metrolist.music.widget.LYRICS_PLAY"
    }
}
