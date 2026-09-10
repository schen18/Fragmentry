package dissonance.cunninglinguist.fragmentry.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dissonance.cunninglinguist.fragmentry.core.ui.theme.AmberPatina

/**
 * Atmospheric navigation bar for moving between the core functions.
 */
@Composable
fun FragmentryNav(
    onNavigate: (String) -> Unit,
    activeTab: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem(text = "field", isActive = activeTab == "field") { onNavigate("field") }
        NavItem(text = "collected", isActive = activeTab == "collected") { onNavigate("collected") }
        NavItem(text = "spark", isActive = activeTab == "spark") { onNavigate("spark") }
        NavItem(text = "veil", isActive = activeTab == "veil") { onNavigate("veil") }
    }
}

@Composable
private fun NavItem(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val alpha by if (isActive) {
        val transition = rememberInfiniteTransition(label = "nav_pulse")
        transition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse_alpha",
        )
    } else {
        remember { mutableFloatStateOf(0.65f) }
    }

    val color = if (isActive) AmberPatina.copy(alpha = alpha) else MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)
    
    Text(
        text = text,
        modifier = Modifier
            .semantics {
                role = Role.Tab
                contentDescription = if (isActive) "active $text" else "go to $text"
            }
            .clickable(onClick = onClick),
        style = TextStyle(
            color = color,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp,
        ),
    )
}
