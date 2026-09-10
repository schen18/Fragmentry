package dissonance.cunninglinguist.fragmentry.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/**
 * Shared atmospheric UI components to maintain visual consistency.
 */
@Composable
fun AmbientAffordance(
    text: String,
    modifier: Modifier = Modifier,
    alpha: Float = 0.6f,
    shadow: Shadow? = null,
    description: String? = null,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier
            .semantics {
                role = Role.Button
                contentDescription = description ?: text
            }
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Text(
        text = text,
        modifier = modifier.then(clickableModifier),
        style = TextStyle(
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 2.sp,
            shadow = shadow
        )
    )
}
