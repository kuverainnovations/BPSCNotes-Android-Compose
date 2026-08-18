package com.example.bpscnotes.core.ui

import android.view.ViewGroup
import android.webkit.WebView

/**
 * Tear a WebView down when its Compose node leaves the tree.
 *
 * `AndroidView` never destroys the views its `factory` created, and a live
 * WebView keeps its renderer process, its Activity context and any playing
 * media alive. Left undestroyed it leaks the whole hosting screen and — for
 * pages with audio or video, like a live class — keeps playing after the user
 * has navigated away.
 *
 * Order matters. Stop the page first so media and JS timers halt, detach from
 * the parent next (destroy() on a still-attached WebView throws), then destroy.
 */
fun WebView.destroyWebView() {
    stopLoading()
    // Drops the current page: audio/video stops and JS timers die.
    loadUrl("about:blank")
    clearHistory()
    (parent as? ViewGroup)?.removeView(this)
    destroy()
}
