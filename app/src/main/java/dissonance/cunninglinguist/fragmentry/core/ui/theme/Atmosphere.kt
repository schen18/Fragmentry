package dissonance.cunninglinguist.fragmentry.core.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Text

/**
 * Shared atmospheric transition constants for Fragmentry.
 */
object Atmosphere {
    val SlowFade = tween<Float>(durationMillis = 1200)
    val MediumFade = tween<Float>(durationMillis = 800)
    val FastFade = tween<Float>(durationMillis = 400)

    @OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
    fun spectralTransition(): ContentTransform {
        return (fadeIn(MediumFade) + scaleIn(tween(1000), initialScale = 0.98f))
            .togetherWith(fadeOut(FastFade) + scaleOut(tween(600), targetScale = 1.02f))
    }

    /**
     * A tag cloud of motifs that scales based on frequency.
     */
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun DriftingMotifs(
        motifs: Map<String, Int>,
        modifier: Modifier = Modifier,
        onMotifClick: (String) -> Unit = {}
    ) {
        val maxCount = motifs.values.maxOrNull() ?: 1
        
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            motifs.toList().sortedByDescending { it.second }.forEach { (motif, count) ->
                // Sizing logic: Base size is 12sp. If motifs have varying counts, 
                // they scale up to 24sp. If all are 1, they stay at 14sp.
                val sizeRatio = if (maxCount > 1) {
                    (count.toFloat() - 1) / (maxCount - 1)
                } else {
                    0f // Default minimum scale when all motifs have frequency 1
                }
                
                val fontSize = (14 + (sizeRatio * 10)).sp
                val alpha = (0.4f + (sizeRatio * 0.4f))
                
                Text(
                    text = "#$motif",
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .clickable { onMotifClick(motif) },
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                        fontSize = fontSize,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    }

    /**
     * A subtle grain overlay to provide textural depth to the Void.
     */
    @Composable
    fun VoidGrain(modifier: Modifier = Modifier) {
        val colorScheme = MaterialTheme.colorScheme
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawWithCache {
                    val random = java.util.Random(42)
                    val density = 0.0006f // Slightly higher density
                    val count = (size.width * size.height * density).toInt()
                    val radius = 1.1f
                    val grainColor = colorScheme.onBackground.copy(alpha = 0.015f)

                    val points = List(size = count) {
                        Offset(
                            x = random.nextFloat() * size.width,
                            y = random.nextFloat() * size.height,
                        )
                    }

                    onDrawBehind {
                        for (point in points) {
                            drawCircle(
                                color = grainColor,
                                radius = radius,
                                center = point,
                            )
                        }
                    }
                },
        )
    }
}
