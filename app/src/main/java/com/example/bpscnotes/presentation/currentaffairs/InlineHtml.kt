package com.example.bpscnotes.presentation.currentaffairs

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.Text

/**
 * Renders inline TipTap HTML as Compose styled text.
 *
 * Handles the exact tags TipTap's inline extension set produces:
 *   <strong>/<b>    → bold
 *   <em>/<i>        → italic
 *   <u>             → underline
 *   <s>/<del>       → strikethrough
 *   <span style="color:#hex">           → text color
 *   <span style="background-color:#hex">→ highlight (background)
 *   <mark style="background-color:#hex">→ highlight
 *   <a href="...">  → blue + underline (link style only, not clickable)
 *   <br>            → newline
 *   HTML entities   → decoded
 *
 * Block-level tags (h1, ul, table …) are intentionally not handled —
 * they never appear in Headline or Summary since those fields use the
 * 'inline'/'inlineSingleLine' TipTap mode which doesn't load those
 * extensions, and the server-side sanitizer (sanitizeCaInline) enforces
 * the same restriction.
 *
 * Drop-in replacement for Text() anywhere you want inline-rich HTML:
 *   RichHtmlText(article.headline, style = MaterialTheme.typography.titleMedium)
 */
@Composable
fun RichHtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val annotated = remember(html) { inlineHtmlToAnnotatedString(html) }
    Text(
        text      = annotated,
        modifier  = modifier,
        style     = style,
        color     = color,
        fontWeight = fontWeight,
        maxLines  = maxLines,
        overflow  = overflow,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Core parser — package-internal so tests can reach it without making it public
// API. The parsing strategy: recursive descent where each open tag immediately
// consumes everything up to its matching close tag and recurses into the inner
// content. This is correct for well-nested HTML (which TipTap always produces)
// and avoids a mutable stack entirely.
// ─────────────────────────────────────────────────────────────────────────────

internal fun inlineHtmlToAnnotatedString(html: String): AnnotatedString =
    buildAnnotatedString { walkNodes(this, html.trim()) }

private fun walkNodes(builder: AnnotatedString.Builder, html: String) {
    var i = 0
    while (i < html.length) {
        if (html[i] != '<') {
            // Text node — accumulate until next tag
            val end = html.indexOf('<', i).let { if (it < 0) html.length else it }
            builder.append(decodeEntities(html.substring(i, end)))
            i = end
            continue
        }

        val tagEnd = html.indexOf('>', i)
        if (tagEnd < 0) {
            // Malformed — treat rest as text
            builder.append(html.substring(i))
            break
        }

        val rawTag = html.substring(i + 1, tagEnd) // content between < and >
        i = tagEnd + 1

        when {
            rawTag.startsWith('/') -> {
                // Close tag encountered at the outer level — this means a
                // parent call already consumed the content and we've looped
                // past the matching close tag. Nothing to do; the recursive
                // call from the open-tag branch already handled it.
            }
            rawTag.lowercase().startsWith("br") -> builder.append('\n')
            rawTag.startsWith('!') -> { /* comment/doctype — skip */ }
            else -> {
                val name = tagNameOf(rawTag)
                val closeTag = "</$name>"
                val closeIdx = html.indexOf(closeTag, i, ignoreCase = true)
                if (closeIdx < 0) {
                    // Self-closing or unclosed tag — skip
                    continue
                }
                val inner = html.substring(i, closeIdx)
                i = closeIdx + closeTag.length

                val spanStyle = spanStyleFor(name, rawTag)
                if (spanStyle != null) {
                    builder.withStyle(spanStyle) { walkNodes(this, inner) }
                } else {
                    walkNodes(builder, inner)
                }
            }
        }
    }
}

/** Returns just the tag name from a raw tag string like `span style="..."`. */
private fun tagNameOf(raw: String): String =
    raw.trim().split(Regex("[ /\t\r\n]"), limit = 2)[0].lowercase()

/** Maps a tag + its attribute string to a SpanStyle, or null if the tag
 *  carries no style information relevant to inline rendering. */
private fun spanStyleFor(name: String, raw: String): SpanStyle? = when (name) {
    "strong", "b" -> SpanStyle(fontWeight = FontWeight.Bold)
    "em", "i"     -> SpanStyle(fontStyle  = FontStyle.Italic)
    "u"           -> SpanStyle(textDecoration = TextDecoration.Underline)
    "s", "del", "strike" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    "a"           -> SpanStyle(color = Color(0xFF2563EB), textDecoration = TextDecoration.Underline)
    "span"        -> {
        val color = extractInlineColor(raw)
        val bg    = extractInlineBg(raw)
        if (color == null && bg == null) null
        else SpanStyle(
            color      = color?.let { parseColor(it) } ?: Color.Unspecified,
            background = bg?.let   { parseColor(it) } ?: Color.Unspecified,
        )
    }
    "mark"        -> extractInlineBg(raw)?.let { SpanStyle(background = parseColor(it)) }
    else          -> null
}

// ── Attribute extraction ──────────────────────────────────────────────────────

/** Extracts `color` from inline style attribute, e.g. `style="color:#dc2626"`. */
private fun extractInlineColor(raw: String): String? {
    val style = styleAttr(raw) ?: return null
    return Regex("""(?:^|;)\s*color\s*:\s*(#[0-9a-fA-F]{3,8}|rgb\([^)]+\))""")
        .find(style)?.groupValues?.get(1)
}

/** Extracts `background-color` from inline style, e.g. `style="background-color:#fef08a"`. */
private fun extractInlineBg(raw: String): String? {
    val style = styleAttr(raw) ?: return null
    return Regex("""background-color\s*:\s*(#[0-9a-fA-F]{3,8}|rgb\([^)]+\))""")
        .find(style)?.groupValues?.get(1)
}

/** Returns the value of the `style="..."` attribute from a raw tag string, or null. */
private fun styleAttr(raw: String): String? =
    Regex("""style\s*=\s*["']([^"']*)["']""").find(raw)?.groupValues?.get(1)

// ── Color parsing ─────────────────────────────────────────────────────────────

private fun parseColor(s: String): Color {
    val v = s.trim()
    return try {
        when {
            v.startsWith('#') -> {
                // Expand 3-digit shorthand (#RGB → #RRGGBB) before parsing
                val hex = if (v.length == 4) "#${v[1]}${v[1]}${v[2]}${v[2]}${v[3]}${v[3]}" else v
                Color(android.graphics.Color.parseColor(hex))
            }
            v.startsWith("rgb(") -> {
                val nums = v.removePrefix("rgb(").removeSuffix(")")
                    .split(',').mapNotNull { it.trim().toIntOrNull() }
                if (nums.size >= 3) Color(nums[0], nums[1], nums[2]) else Color.Unspecified
            }
            else -> Color.Unspecified
        }
    } catch (_: Exception) { Color.Unspecified }
}

// ── Entity decoding ───────────────────────────────────────────────────────────

private fun decodeEntities(text: String): String = text
    .replace("&amp;",  "&")
    .replace("&lt;",   "<")
    .replace("&gt;",   ">")
    .replace("&nbsp;", "\u00A0")
    .replace("&quot;", "\"")
    .replace("&#39;",  "'")
    .replace(Regex("&#(\\d+);")) { mr ->
        mr.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: mr.value
    }
