package org.better.urn.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific PDF rendering surface component.
 * Downloads/caches PDF from [url] and renders the target page matching [state].
 */
@Composable
expect fun PdfRenderSurface(
    url: String,
    state: PdfViewerState,
    onLoadingStateChanged: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier
)
