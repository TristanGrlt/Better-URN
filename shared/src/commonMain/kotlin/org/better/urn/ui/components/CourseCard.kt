package org.better.urn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.better.urn.data.Course
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import androidx.compose.ui.layout.ContentScale

@Composable
fun CourseCard(course: Course, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth().aspectRatio(1f) // Carré parfait
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val imageUrl = course.getImageUrl("4bc20954e6d1659237a16b2d567343fa") // Idéalement passé en paramètre depuis le HomeScreen

            if (imageUrl != null) {
                KamelImage(
                    resource = asyncPainterResource(data = imageUrl),
                    contentDescription = "Image du cours",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp) // Ou fillMaxWidth() si tu veux une grande image
                )
            } else {
                // Le CircleAvatar de secours avec l'icône
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    // Icon(...)
                }
            }

            Column {
                Text(
                    text = course.fullname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = course.shortname,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
