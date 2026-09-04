package org.better.urn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Multiplatform system back event handler.
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

/**
 * Modifier handling desktop-specific key and pointer back events.
 */
expect fun Modifier.handleDesktopBack(dispatcher: BackDispatcher): Modifier

class BackDispatcher {
    private class HandlerEntry(
        val enabled: () -> Boolean,
        val onBack: () -> Unit,
    )

    private val handlers = mutableStateListOf<HandlerEntry>()

    fun register(enabled: () -> Boolean, onBack: () -> Unit): () -> Unit {
        val entry = HandlerEntry(enabled, onBack)
        handlers.add(entry)
        return {
            handlers.remove(entry)
        }
    }

    fun handleBack(): Boolean {
        for (i in handlers.indices.reversed()) {
            val entry = handlers[i]
            if (entry.enabled()) {
                entry.onBack()
                return true
            }
        }
        return false
    }
}

val LocalBackDispatcher = compositionLocalOf { BackDispatcher() }

@Composable
fun ProvideBackDispatcher(
    dispatcher: BackDispatcher = remember { BackDispatcher() },
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBackDispatcher provides dispatcher) {
        content()
    }
}
