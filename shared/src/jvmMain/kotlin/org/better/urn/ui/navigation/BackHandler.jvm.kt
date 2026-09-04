package org.better.urn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    val dispatcher = LocalBackDispatcher.current
    val currentOnBack by rememberUpdatedState(onBack)
    val currentEnabled by rememberUpdatedState(enabled)

    DisposableEffect(dispatcher) {
        val unregister = dispatcher.register(
            enabled = { currentEnabled },
            onBack = currentOnBack,
        )
        onDispose {
            unregister()
        }
    }
}

actual fun Modifier.handleDesktopBack(dispatcher: BackDispatcher): Modifier = this.composed {
    DisposableEffect(dispatcher) {
        val listener = AWTEventListener { event ->
            if ((event is KeyEvent) && (event.id == KeyEvent.KEY_RELEASED)) {
                val isEscape = event.keyCode == KeyEvent.VK_ESCAPE
                val isAltLeft = (event.keyCode == KeyEvent.VK_LEFT) && event.isAltDown
                val isBackKey = (event.keyCode == KeyEvent.VK_BACK_SPACE) && event.isAltDown
                if (isEscape || isAltLeft || isBackKey) {
                    dispatcher.handleBack()
                }
            } else if ((event is MouseEvent) && (event.id == MouseEvent.MOUSE_RELEASED)) {
                if ((event.button == 4) || (event.button == 8)) {
                    dispatcher.handleBack()
                }
            }
        }

        val mask = AWTEvent.KEY_EVENT_MASK or AWTEvent.MOUSE_EVENT_MASK
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(listener, mask)
        } catch (_: Throwable) {
        }

        onDispose {
            try {
                Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
            } catch (_: Throwable) {
            }
        }
    }
    this
}
