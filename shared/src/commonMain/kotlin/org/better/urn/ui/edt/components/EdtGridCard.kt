package org.better.urn.ui.edt.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.better.urn.data.EdtEvent
import org.better.urn.data.allowBreakAnywhere
import org.better.urn.data.formatEventHours
import org.better.urn.data.parseHexColor

@Composable
fun EdtGridCard(
    event: EdtEvent,
    modifier: Modifier = Modifier
) {
    val rawColor = parseHexColor(event.colorHex)
    val backgroundColor = rawColor.copy(alpha = 0.2f)
    val formattedHours = remember(event.startMs, event.endMs) {
        formatEventHours(event.startMs, event.endMs)
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = backgroundColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(rawColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = event.title.allowBreakAnywhere(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                if (formattedHours.isNotBlank()) {
                    Text(
                        text = formattedHours.allowBreakAnywhere(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (event.location.isNotBlank()) {
                    Text(
                        text = event.location.allowBreakAnywhere(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
