/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.utils

import android.content.Context
import timber.log.Timber

/**
 * Crash diagnostics stay on the device. No GitHub credentials are shipped in the APK,
 * and no crash payloads are uploaded in the background.
 *
 * The existing crash screen offers a user-initiated Share Logs action. Any future
 * automatic reporting must use a server-side authenticated endpoint and explicit consent.
 */
object CrashReporter {
    fun init(@Suppress("UNUSED_PARAMETER") context: Context) = Unit

    fun reportFatal(throwable: Throwable) {
        Timber.tag("CrashReporter").e(throwable, "Fatal crash (local only)")
    }

    fun reportNonFatal(throwable: Throwable) {
        var cause: Throwable? = throwable
        while (cause != null) {
            if (cause is java.util.concurrent.CancellationException) return
            cause = cause.cause
        }
        Timber.tag("CrashReporter").w(throwable, "Non-fatal exception (local only)")
    }

    fun reportAnr(mainThreadStack: Array<StackTraceElement>, blockedMs: Long) {
        Timber.tag("CrashReporter").w(
            "ANR: main thread blocked for %dms (local only)\n%s",
            blockedMs,
            mainThreadStack.joinToString("\n"),
        )
    }

    /**
     * Sanitizes crash logs before the user can inspect or explicitly share them.
     * This is best-effort, not a substitute for reviewing shared diagnostic data.
     */
    fun sanitize(input: String): String {
        var output = input
        output = Regex("""/storage/emulated/\d+(?:/[^\s"')\]]*)?""").replace(output, "<storage>")
        output = Regex("""/data/user/\d+/[^\s"')\]]*""").replace(output, "<data>")
        output = Regex("""/data/data/[^\s"')\]]*""").replace(output, "<data>")
        output = Regex("""https?://[^\s"')\]]+""").replace(output) {
            it.value.substringBefore('?').substringBefore('#')
        }
        output = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
            .replace(output, "<email>")
        output = Regex("""[A-Za-z0-9+/=_\-]{24,}""").replace(output) { match ->
            val value = match.value
            val mixed = value.any(Char::isUpperCase) && value.any(Char::isLowerCase)
            val alphanumeric = value.any(Char::isDigit) && value.any(Char::isLetter)
            if (mixed || alphanumeric) "<redacted>" else value
        }
        return output
    }
}
