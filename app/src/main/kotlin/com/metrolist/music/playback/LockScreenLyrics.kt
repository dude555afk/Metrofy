/**
 * Adapted from Pear Music's Media3 lock-screen lyrics metadata player.
 * Original project: https://github.com/dragonzords/Pear-Music
 * GPL-3.0; Metrofy adaptation dated 2026-11-19.
 */
package com.metrolist.music.playback

import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import java.util.concurrent.CopyOnWriteArraySet

object LockScreenLyrics {
    @Volatile internal var player: LyricsMetadataPlayer? = null
        private set
    private val main = Handler(Looper.getMainLooper())
    private var last: String? = null

    fun attach(player: LyricsMetadataPlayer?) {
        this.player = player
        last = null
    }

    fun publish(line: String?) {
        if (line == last) return
        last = line
        main.post { player?.setLyric(line) }
    }
}

@OptIn(UnstableApi::class)
class LyricsMetadataPlayer(player: Player) : ForwardingPlayer(player) {
    private val listeners = CopyOnWriteArraySet<Player.Listener>()
    private var lyric: String? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                lyric = null
            }
        })
    }

    override fun addListener(listener: Player.Listener) {
        listeners += listener
        super.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        listeners -= listener
        super.removeListener(listener)
    }

    override fun getMediaMetadata(): MediaMetadata {
        val base = super.getMediaMetadata()
        val line = lyric ?: return base
        return base.buildUpon().setArtist(line).setSubtitle(line).build()
    }

    fun setLyric(line: String?) {
        if (line == lyric) return
        lyric = line
        val metadata = mediaMetadata
        val events = Player.Events(FlagSet.Builder().add(Player.EVENT_MEDIA_METADATA_CHANGED).build())
        listeners.forEach {
            it.onMediaMetadataChanged(metadata)
            it.onEvents(this, events)
        }
    }
}
