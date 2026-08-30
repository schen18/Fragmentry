package dissonance.cunninglinguist.fragmentry.core.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import kotlin.math.abs

/**
 * Applies a horizontal offset based on a value (usually semantic gravity or hash fallback)
 * to create a meaningful, non-linear layout.
 */
fun Modifier.atmosphericJitter(
    jitterValue: Float? = null,
    fallbackSeed: Int = 0,
    maxOffset: Int = 160, // Increased default for more visible "nebula" effect
    step: Int = 1
): Modifier {
    val finalJitter = if (jitterValue != null) {
        // Amplify the semantic value. Normalized components are usually small (~0.05).
        // We use a high multiplier to span more of the void.
        (jitterValue * 15f).coerceIn(-1.0f, 1.0f) * (maxOffset / 2f)
    } else {
        val absoluteSeed = abs(fallbackSeed)
        ((absoluteSeed % 40) - 20).toFloat() * (maxOffset / 40f)
    }

    return this.then(
        Modifier.offset(x = finalJitter.dp)
    )
}
