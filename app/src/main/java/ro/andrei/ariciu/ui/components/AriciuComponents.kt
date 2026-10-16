package ro.andrei.ariciu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

object AriciuIcons {
    private fun icon(name: String, draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = draw)
        }.build()
    val Place = icon("Place") {
        moveTo(19f, 10f); curveTo(19f, 15f, 12f, 22f, 12f, 22f)
        curveTo(12f, 22f, 5f, 15f, 5f, 10f); curveTo(5f, 0f, 19f, 0f, 19f, 10f); close()
        moveTo(15f, 10f); curveTo(15f, 14f, 9f, 14f, 9f, 10f); curveTo(9f, 6f, 15f, 6f, 15f, 10f); close()
    }
    val Reminders = icon("Reminders") {
        moveTo(6f, 3f); lineTo(18f, 3f); curveTo(19f, 3f, 20f, 4f, 20f, 5f); lineTo(20f, 19f)
        curveTo(20f, 20f, 19f, 21f, 18f, 21f); lineTo(6f, 21f); curveTo(5f, 21f, 4f, 20f, 4f, 19f)
        lineTo(4f, 5f); curveTo(4f, 4f, 5f, 3f, 6f, 3f); close()
        moveTo(8f, 8f); lineTo(16f, 8f); moveTo(8f, 12f); lineTo(16f, 12f); moveTo(8f, 16f); lineTo(13f, 16f)
    }
    val Settings = icon("Settings") {
        moveTo(4f, 6f); lineTo(20f, 6f); moveTo(4f, 12f); lineTo(20f, 12f); moveTo(4f, 18f); lineTo(20f, 18f)
        moveTo(8f, 3f); lineTo(8f, 9f); moveTo(16f, 9f); lineTo(16f, 15f); moveTo(10f, 15f); lineTo(10f, 21f)
    }
    val Back = icon("Back") { moveTo(14f, 5f); lineTo(7f, 12f); lineTo(14f, 19f) }
    val Arrow = icon("Arrow") { moveTo(5f, 12f); lineTo(19f, 12f); moveTo(13f, 6f); lineTo(19f, 12f); lineTo(13f, 18f) }
    val Check = icon("Check") { moveTo(5f, 12f); lineTo(10f, 17f); lineTo(19f, 7f) }
}

@Composable
fun ScreenHeading(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
}

@Composable
fun ChoiceCard(title: String, description: String? = null, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant)) {
        Row(Modifier.heightIn(min = 64.dp).padding(16.dp), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface)
                if (description != null) Text(description, style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

@Composable
fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled,
        shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
