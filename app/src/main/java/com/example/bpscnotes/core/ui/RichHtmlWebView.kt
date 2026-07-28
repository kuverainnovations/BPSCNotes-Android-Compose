package com.example.bpscnotes.core.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders rich-text HTML (the same TipTap output as Current Affairs — headings,
 * bold, lists, tables, colours, images) inside a content-sized WebView, so it
 * can sit as one block in a scrolling Compose screen.
 *
 * Used for the Answer Writing model answer, which is now authored in the same
 * rich editor as Current Affairs. Height is WRAP_CONTENT: the WebView measures
 * the full content and the parent Compose scroll handles the rest — the WebView
 * itself never scrolls.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RichHtmlWebView(html: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val doc = remember(html) { wrapHtml(html) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                settings.apply {
                    javaScriptEnabled = false
                    domStorageEnabled = false
                    allowContentAccess = false
                    allowFileAccess = false
                    loadWithOverviewMode = true
                    useWideViewPort = false
                }
                // The parent Compose column scrolls; the WebView must not.
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isNestedScrollingEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url ?: return false
                        return try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                            true
                        } catch (_: Exception) { false }
                    }
                }
            }
        },
        update = { it.loadDataWithBaseURL(null, doc, "text/html", "UTF-8", null) },
    )
}

/** Wrap raw TipTap HTML in a lightweight styled document. Transparent bg so the
 *  host card colour shows through; dark text; indigo headings/list markers. */
private fun wrapHtml(inner: String): String = """
    <!DOCTYPE html><html><head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
      * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
      html, body { margin: 0; padding: 0; background: transparent; }
      body { font-family: -apple-system, Roboto, 'Segoe UI', sans-serif;
             font-size: 15px; line-height: 1.7; color: #1e293b; word-break: break-word; }
      p { margin: 0 0 12px; }
      h1 { font-size: 1.4em; font-weight: 800; margin: 18px 0 10px; color: #1A237E; }
      h2 { font-size: 1.22em; font-weight: 800; margin: 16px 0 8px; color: #1A237E; }
      h3 { font-size: 1.08em; font-weight: 700; margin: 14px 0 8px; color: #1A237E; }
      ul, ol { padding-left: 1.4em; margin: 0 0 12px; }
      li { margin-bottom: 6px; }
      li::marker { color: #3949AB; font-weight: 700; }
      a { color: #3949AB; text-decoration: underline; }
      blockquote { border-left: 3px solid #3949AB; padding: 6px 14px; margin: 14px 0;
                   background: rgba(57,73,171,0.06); border-radius: 0 8px 8px 0; font-style: italic; }
      table { border-collapse: collapse; width: 100%; margin: 14px 0; display: block; overflow-x: auto; }
      th, td { border: 1px solid #CBD5E1; padding: 8px 12px; text-align: left; min-width: 70px; }
      th { background: rgba(57,73,171,0.08); font-weight: 700; color: #1A237E; }
      img { max-width: 100%; height: auto; border-radius: 8px; display: block; margin: 12px auto; }
      mark { border-radius: 3px; padding: 1px 3px; }
      strong { font-weight: 700; } em { font-style: italic; }
      u { text-decoration: underline; } s { text-decoration: line-through; }
    </style></head><body>$inner</body></html>
""".trimIndent()
