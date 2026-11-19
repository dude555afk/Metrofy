/**
 * Adapted from Pear Music's live lyrics widget state machine.
 * Original project: https://github.com/dragonzords/Pear-Music
 * GPL-3.0; Metrofy adaptation dated 2026-11-19.
 */
package com.metrolist.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.toBitmap
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Pear's complete state/render engine for its two-page ViewFlipper lyrics widget.
 *
 * A newly sung line is filled into the hidden page, then the flipper changes page exactly
 * once. Word progress updates refresh the text without changing pages, so launcher
 * rebinds do not replay the line transition.
 */
object LyricsWidgetState {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    @Volatile var installedCache: Boolean? = null
    private var checkedAt = 0L

    private var title: String? = null
    private var artist: String? = null
    private var playing = false
    private var artUrl: String? = null
    private var art: Bitmap? = null
    private var backdrop: Bitmap? = null
    private var imageLoader: ImageLoader? = null

    private var lineKey: Any? = null
    private var prev = ""
    private var line = "Play a song to see its lyrics here"
    private var next = ""
    private var sungWords = -1
    private var page = 0
    private var needsFull = true
    private var lastWordPush = 0L

    private fun loader(context: Context): ImageLoader =
        imageLoader ?: synchronized(this) {
            imageLoader ?: SingletonImageLoader.get(context.applicationContext)
                .also { imageLoader = it }
        }

    fun installed(context: Context): Boolean {
        val now = System.currentTimeMillis()
        val cached = installedCache
        if (cached != null && now - checkedAt < 30_000) return cached
        checkedAt = now
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, LyricsWidget::class.java))
        return ids.isNotEmpty().also { installedCache = it }
    }

    /**
     * [lineKey] identifies a line transition. [progress] is 0..1 through the line;
     * null means no timing data, so the widget does not claim word-level sync.
     */
    fun update(
        context: Context,
        title: String?,
        artist: String?,
        artUrl: String?,
        lineKey: Any?,
        prev: String,
        line: String,
        next: String,
        progress: Float?,
        playing: Boolean,
    ) {
        val app = context.applicationContext
        val wordCount = line.split(' ').count { it.isNotBlank() }
        val sung = if (progress == null) wordCount
            else kotlin.math.ceil(progress * wordCount).toInt().coerceIn(0, wordCount)
        val newSong = title != this.title || artist != this.artist || artUrl != this.artUrl
        val newLine = lineKey != this.lineKey || line != this.line

        this.title = title
        this.artist = artist
        this.playing = playing.also { if (it != this.playing) needsFull = true }

        if (artUrl != this.artUrl) {
            this.artUrl = artUrl
            art = null
            backdrop = null
            if (artUrl != null) scope.launch {
                val imageLoader = loader(app)
                val small = imageLoader.execute(
                    ImageRequest.Builder(app)
                        .data(artUrl)
                        .size(160)
                        .allowHardware(false)
                        .build()
                )
                val blurred = imageLoader.execute(
                    ImageRequest.Builder(app)
                        .data(artUrl)
                        .size(256)
                        .allowHardware(false)
                        .transformations(SoftBlurTransformation(width = 48, radius = 5))
                        .build()
                )
                if (this@LyricsWidgetState.artUrl == artUrl) {
                    art = (small as? SuccessResult)?.image?.toBitmap()
                    backdrop = (blurred as? SuccessResult)?.image?.toBitmap()
                    push(app)
                }
            }
        }

        when {
            newSong || needsFull -> {
                this.lineKey = lineKey
                this.prev = prev
                this.line = line
                this.next = next
                sungWords = sung
                needsFull = false
                push(app)
            }
            newLine -> {
                this.lineKey = lineKey
                this.prev = prev
                this.line = line
                this.next = next
                sungWords = sung
                page = 1 - page
                lastWordPush = System.currentTimeMillis() + 300
                partial(app) { views ->
                    fillPage(views, page)
                    views.setDisplayedChild(R.id.lw_flipper, page)
                }
            }
            sung != sungWords && System.currentTimeMillis() - lastWordPush > 450 -> {
                lastWordPush = System.currentTimeMillis()
                sungWords = sung
                push(app)
            }
        }
    }

    private fun lineId(page: Int) = if (page == 0) R.id.lw_line_a else R.id.lw_line_b

    private fun fillPage(views: RemoteViews, page: Int) {
        views.setTextViewText(if (page == 0) R.id.lw_prev_a else R.id.lw_prev_b, prev)
        views.setTextViewText(lineId(page), karaoke(line, sungWords))
        views.setTextViewText(if (page == 0) R.id.lw_next_a else R.id.lw_next_b, next)
    }

    /** Dim only words not yet sung, retaining Pear's widget treatment. */
    private fun karaoke(text: String, sung: Int): CharSequence {
        if (sung <= 0) return android.text.SpannableString(text).apply {
            if (text.isNotEmpty()) {
                setSpan(android.text.style.ForegroundColorSpan(DIM), 0, text.length, 0)
            }
        }
        var seen = 0
        var cut = text.length
        for (match in Regex("\\S+").findAll(text)) {
            seen++
            if (seen == sung) {
                cut = match.range.last + 1
                break
            }
        }
        return android.text.SpannableString(text).apply {
            if (cut < text.length) {
                setSpan(android.text.style.ForegroundColorSpan(DIM), cut, text.length, 0)
            }
        }
    }

    private const val DIM = 0x73FFFFFF

    private fun partial(context: Context, build: (RemoteViews) -> Unit) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, LyricsWidget::class.java))
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_lyrics)
        build(views)
        manager.partiallyUpdateAppWidget(ids, views)
    }

    fun push(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, LyricsWidget::class.java))
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_lyrics).apply {
            setTextViewText(R.id.lw_title, title ?: "Not Playing")
            setTextViewText(R.id.lw_artist, artist ?: "Metrofy")
            fillPage(this, 0)
            fillPage(this, 1)
            art?.let { setImageViewBitmap(R.id.lw_art, it) }
                ?: setImageViewResource(R.id.lw_art, android.R.color.transparent)
            backdrop?.let { setImageViewBitmap(R.id.lw_backdrop, it) }
                ?: setImageViewResource(R.id.lw_backdrop, android.R.color.transparent)
            setImageViewResource(R.id.lw_play, if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play)
            setOnClickPendingIntent(
                R.id.lw_play,
                PendingIntent.getBroadcast(
                    context,
                    21,
                    Intent(context, LyricsWidget::class.java)
                        .setAction(LyricsWidget.ACTION_PLAY),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            setOnClickPendingIntent(
                R.id.lw_root,
                PendingIntent.getActivity(
                    context,
                    22,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        manager.updateAppWidget(ids, views)
    }
}
