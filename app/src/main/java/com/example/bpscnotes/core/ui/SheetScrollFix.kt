package com.example.bpscnotes.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

// ─────────────────────────────────────────────────────────────
// Bottom-sheet flicker fix (QA 04-Jul issue 12)
//
// When a ModalBottomSheet holds full-height scrollable content, a light
// downward drag while the content sits at scroll position 0 is handled by
// BOTH the sheet's drag-to-dismiss and the content's own scroll/overscroll —
// each nudges the layout a few px in opposite phases, which reads as a
// vertical "flicker" when closing the sheet. Absorbing that boundary case
// (at top + dragging down) lets only the sheet's own drag/settle animate.
// Same pattern the marketplace course sheet already shipped with; these
// helpers exist so every other sheet uses one shared implementation.
// ─────────────────────────────────────────────────────────────

/** For sheet content using Modifier.verticalScroll(scrollState). Place BEFORE verticalScroll in the chain. */
@Composable
fun Modifier.sheetFlickerFix(scrollState: ScrollState): Modifier {
    val connection = remember(scrollState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (scrollState.value == 0 && available.y > 0f) available else Offset.Zero
        }
    }
    return this.nestedScroll(connection)
}

/** For sheet content using LazyColumn(state = listState). Place on the LazyColumn's modifier. */
@Composable
fun Modifier.sheetFlickerFix(listState: LazyListState): Modifier {
    val connection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val atTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                return if (atTop && available.y > 0f) available else Offset.Zero
            }
        }
    }
    return this.nestedScroll(connection)
}
