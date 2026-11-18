package com.example.bpscnotes.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Pinch-to-zoom for content that also needs to scroll.
 *
 * Gesture split is deliberate: zoom and pan are bound to MULTI-touch via
 * [transformable], so a one-finger drag is left alone and reaches whatever
 * scrollable is underneath. A `detectTransformGestures` block would consume
 * every pointer event and freeze the list it sits on — which is exactly the
 * situation in the PDF viewers, where pages live in a LazyColumn.
 *
 * Double-tap toggles between fit and `doubleTapScale` for the common
 * "enlarge this one diagram" case without any pinching.
 *
 * Panning is clamped to the scaled content's own bounds, so the content can
 * never be dragged off into empty space and lost.
 */
class ZoomState internal constructor(
    private val minScale: Float,
    private val maxScale: Float,
    private val doubleTapScale: Float,
) {
    var scale by mutableFloatStateOf(1f)
        private set
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set

    internal var layoutSize = IntSize.Zero

    /** True once the user has zoomed in at all — drives the reset affordance. */
    val isZoomed: Boolean get() = scale > 1.01f

    internal fun applyTransform(zoomChange: Float, panX: Float, panY: Float) {
        scale = (scale * zoomChange).coerceIn(minScale, maxScale)
        if (scale <= 1.001f) {
            // Back at fit — recentre, rather than leaving the content nudged
            // off to one side by the pan that came with the pinch out.
            reset()
            return
        }
        offsetX = clampPan(offsetX + panX, layoutSize.width)
        offsetY = clampPan(offsetY + panY, layoutSize.height)
    }

    /**
     * Keep the scaled content covering the viewport. At scale s the content
     * overhangs the viewport by (s-1)/2 on each side, which is exactly how
     * far it may travel before an empty edge would come into view.
     */
    private fun clampPan(value: Float, extent: Int): Float {
        if (extent == 0) return 0f
        val limit = (extent * (scale - 1f)) / 2f
        return value.coerceIn(-limit, limit)
    }

    internal fun toggleDoubleTap() {
        if (isZoomed) reset() else scale = doubleTapScale
    }

    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
}

@Composable
fun rememberZoomState(
    minScale: Float = 1f,
    maxScale: Float = 5f,
    doubleTapScale: Float = 2.5f,
): ZoomState = remember(minScale, maxScale, doubleTapScale) {
    ZoomState(minScale, maxScale, doubleTapScale)
}

/**
 * Applies [state] to this element: multi-touch pinch/pan plus double-tap.
 *
 * Apply to the CONTAINER of the scrollable content, not to each item — the
 * transform is one graphics-layer operation over the whole subtree.
 */
@Composable
fun Modifier.zoomable(state: ZoomState): Modifier {
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        state.applyTransform(zoomChange, panChange.x, panChange.y)
    }
    return this
        .onSizeChanged { state.layoutSize = it }
        .pointerInput(state) {
            detectTapGestures(onDoubleTap = { state.toggleDoubleTap() })
        }
        .transformable(state = transformState, lockRotationOnZoomPan = true)
        .graphicsLayer {
            scaleX = state.scale
            scaleY = state.scale
            translationX = state.offsetX
            translationY = state.offsetY
        }
}

/**
 * Convenience wrapper: a full-size Box whose contents zoom together.
 *
 * [onZoomChanged] fires when the zoom level crosses a whole step. The PDF
 * viewers use it to decide when to re-rasterise pages at a higher
 * resolution — scaling a bitmap alone only magnifies its pixels.
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    state: ZoomState = rememberZoomState(),
    onZoomChanged: ((Float) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    // Reported from an effect, never from composition — notifying a caller
    // mid-compose would have it mutate state during the compose pass.
    if (onZoomChanged != null) {
        val step = kotlin.math.round(state.scale * 2f) / 2f
        LaunchedEffect(step) { onZoomChanged(step) }
    }
    Box(modifier = modifier.fillMaxSize().zoomable(state), content = content)
}

/**
 * Full-screen, zoomable viewer for a single image.
 *
 * Built for handwritten answer photos: they are camera shots of a page, so
 * fit-width on a phone is almost never readable and the reviewer needs to
 * get right in. Double-tap or pinch; tap the close button to leave.
 */
@Composable
fun ZoomableImageDialog(
    url: String,
    onDismiss: () -> Unit,
    caption: String? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF202124)),
        ) {
            ZoomableBox {
                AsyncImage(
                    model = url,
                    contentDescription = caption,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
            }

            if (caption != null) {
                Text(
                    caption,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )
            }
        }
    }
}
