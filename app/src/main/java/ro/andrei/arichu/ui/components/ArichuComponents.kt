package ro.andrei.arichu.ui.components

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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

object ArichuIcons {
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
    val Search = icon("Search") { moveTo(17f, 10f); curveTo(17f, 19f, 3f, 19f, 3f, 10f); curveTo(3f, 1f, 17f, 1f, 17f, 10f); moveTo(15f, 15f); lineTo(21f, 21f) }
    val Locate = icon("My location") { moveTo(19f, 12f); curveTo(19f, 21f, 5f, 21f, 5f, 12f); curveTo(5f, 3f, 19f, 3f, 19f, 12f); moveTo(12f, 2f); lineTo(12f, 7f); moveTo(12f, 17f); lineTo(12f, 22f); moveTo(2f, 12f); lineTo(7f, 12f); moveTo(17f, 12f); lineTo(22f, 12f) }
    val Back = icon("Back") { moveTo(14f, 5f); lineTo(7f, 12f); lineTo(14f, 19f) }
    val Arrow = icon("Arrow") { moveTo(5f, 12f); lineTo(19f, 12f); moveTo(13f, 6f); lineTo(19f, 12f); lineTo(13f, 18f) }
    val Check = icon("Check") { moveTo(5f, 12f); lineTo(10f, 17f); lineTo(19f, 7f) }

    private fun androidx.compose.ui.graphics.vector.PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy); arcTo(r, r, 0f, true, true, cx + r, cy); arcTo(r, r, 0f, true, true, cx - r, cy); close()
    }
    private fun androidx.compose.ui.graphics.vector.PathBuilder.bell() {
        moveTo(6f, 16f); lineTo(6f, 11f); arcTo(6f, 6f, 0f, false, true, 18f, 11f); lineTo(18f, 16f)
        lineTo(19.5f, 18f); lineTo(4.5f, 18f); close(); moveTo(10f, 21f); lineTo(14f, 21f)
    }
    val Store = icon("Shop") {
        moveTo(5.5f, 4f); lineTo(18.5f, 4f); lineTo(20f, 9f); lineTo(4f, 9f); close()
        moveTo(5f, 9f); lineTo(5f, 20f); lineTo(19f, 20f); lineTo(19f, 9f)
        moveTo(10f, 20f); lineTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 20f)
    }
    val Mall = icon("Shopping centre") {
        moveTo(5f, 8f); lineTo(19f, 8f); lineTo(20f, 20f); lineTo(4f, 20f); close()
        moveTo(9f, 8f); lineTo(9f, 6.5f); arcTo(3f, 3f, 0f, false, true, 15f, 6.5f); lineTo(15f, 8f)
    }
    val Town = icon("Town") {
        moveTo(3f, 20f); lineTo(3f, 12f); lineTo(8f, 8f); lineTo(13f, 12f); lineTo(13f, 20f); close()
        moveTo(13f, 20f); lineTo(13f, 10f); lineTo(17f, 6f); lineTo(21f, 10f); lineTo(21f, 20f)
        moveTo(6.5f, 20f); lineTo(6.5f, 15f); lineTo(9.5f, 15f); lineTo(9.5f, 20f)
    }
    val Pin = icon("Pin") {
        moveTo(12f, 21f); curveTo(12f, 21f, 5f, 15f, 5f, 9.5f); arcTo(7f, 7f, 0f, false, true, 19f, 9.5f)
        curveTo(19f, 15f, 12f, 21f, 12f, 21f); close(); circle(12f, 9.5f, 2.5f)
    }
    val Arrive = icon("On arrival") {
        moveTo(3f, 12f); lineTo(14f, 12f); moveTo(10f, 8f); lineTo(14f, 12f); lineTo(10f, 16f)
        moveTo(14f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(14f, 19f)
    }
    val Depart = icon("On departure") {
        moveTo(10f, 12f); lineTo(21f, 12f); moveTo(17f, 8f); lineTo(21f, 12f); lineTo(17f, 16f)
        moveTo(10f, 5f); lineTo(4f, 5f); lineTo(4f, 19f); lineTo(10f, 19f)
    }
    val BellNormal = icon("Normal alert") { bell() }
    val BellSilent = icon("Silent alert") { bell(); moveTo(4f, 4f); lineTo(20f, 20f) }
    val BellUrgent = icon("Urgent alert") { bell(); moveTo(12f, 8f); lineTo(12f, 12f); moveTo(12f, 14.5f); lineTo(12f, 14.6f) }
    val Clock = icon("Time") { circle(12f, 12f, 9f); moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15.5f, 14f) }
    val ZoomIn = icon("Zoom in") { moveTo(5f, 12f); lineTo(19f, 12f); moveTo(12f, 5f); lineTo(12f, 19f) }
    val ZoomOut = icon("Zoom out") { moveTo(5f, 12f); lineTo(19f, 12f) }
    val Compass = icon("Compass") {
        circle(12f, 12f, 9f)
        moveTo(15.5f, 8.5f); lineTo(13.5f, 13.5f); lineTo(8.5f, 15.5f); lineTo(10.5f, 10.5f); close()
    }
    val Pharmacy = icon("Pharmacy") { circle(12f, 12f, 9f); moveTo(12f, 8f); lineTo(12f, 16f); moveTo(8f, 12f); lineTo(16f, 12f) }
    val Health = icon("Hospital or clinic") {
        moveTo(5f, 21f); lineTo(5f, 5f); lineTo(19f, 5f); lineTo(19f, 21f); close()
        moveTo(12f, 9f); lineTo(12f, 15f); moveTo(9f, 12f); lineTo(15f, 12f)
    }
    val Fuel = icon("Fuel") {
        moveTo(5f, 21f); lineTo(5f, 4f); lineTo(14f, 4f); lineTo(14f, 21f); moveTo(4f, 21f); lineTo(15f, 21f)
        moveTo(5f, 9f); lineTo(14f, 9f); moveTo(14f, 8f); lineTo(17f, 8f); lineTo(19f, 10f); lineTo(19f, 17f)
    }
    val Bank = icon("Bank") {
        moveTo(3f, 9f); lineTo(12f, 4f); lineTo(21f, 9f); close()
        moveTo(6f, 11f); lineTo(6f, 17f); moveTo(10f, 11f); lineTo(10f, 17f); moveTo(14f, 11f); lineTo(14f, 17f); moveTo(18f, 11f); lineTo(18f, 17f)
        moveTo(4f, 20f); lineTo(20f, 20f)
    }
    val Post = icon("Post office") { moveTo(3f, 6f); lineTo(21f, 6f); lineTo(21f, 18f); lineTo(3f, 18f); close(); moveTo(3f, 6f); lineTo(12f, 13f); lineTo(21f, 6f) }
    val Food = icon("Food and cafes") {
        moveTo(5f, 8f); lineTo(16f, 8f); lineTo(16f, 13f); arcTo(4f, 4f, 0f, false, true, 12f, 17f); lineTo(9f, 17f); arcTo(4f, 4f, 0f, false, true, 5f, 13f); close()
        moveTo(16f, 10f); lineTo(17.5f, 10f); arcTo(2f, 2f, 0f, false, true, 17.5f, 14f); lineTo(16f, 14f)
        moveTo(4f, 21f); lineTo(17f, 21f); moveTo(9f, 3f); lineTo(9f, 5f); moveTo(12f, 3f); lineTo(12f, 5f)
    }
    val Hardware = icon("Hardware") { moveTo(14f, 4f); lineTo(20f, 10f); lineTo(17f, 13f); lineTo(11f, 7f); close(); moveTo(13.5f, 9.5f); lineTo(5f, 18f); lineTo(6f, 20f); lineTo(8f, 19f) }
    val Bakery = icon("Bakery") {
        moveTo(4f, 14f); arcTo(8f, 6f, 0f, false, true, 20f, 14f); lineTo(20f, 18f); arcTo(1f, 1f, 0f, false, true, 19f, 19f)
        lineTo(5f, 19f); arcTo(1f, 1f, 0f, false, true, 4f, 18f); close(); moveTo(9f, 10f); lineTo(10.5f, 13f); moveTo(13f, 10f); lineTo(14.5f, 13f)
    }
    val Download = icon("Download") { moveTo(12f, 4f); lineTo(12f, 15f); moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f); moveTo(5f, 19f); lineTo(19f, 19f) }
    val Close = icon("Close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) }
    val Layers = icon("Map layers") {
        moveTo(12f, 3f); lineTo(21f, 8f); lineTo(12f, 13f); lineTo(3f, 8f); close()
        moveTo(3f, 12f); lineTo(12f, 17f); lineTo(21f, 12f); moveTo(3f, 16f); lineTo(12f, 21f); lineTo(21f, 16f)
    }
}

@Composable
fun ScreenHeading(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
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
        Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled,
        shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Disclosure content stays inside the surface that owns its accessible toggle. */
@Composable
fun ExpandableSection(title: String, expanded: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleMedium)
            }
            if (expanded) content()
        }
    }
}
