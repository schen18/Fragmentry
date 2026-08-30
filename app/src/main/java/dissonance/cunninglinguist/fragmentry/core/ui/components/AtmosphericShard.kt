package dissonance.cunninglinguist.fragmentry.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import dissonance.cunninglinguist.fragmentry.core.ui.theme.AmberPatina

/**
 * A unified component for displaying fragment "shards" across the application.
 * Supports organic jitter via a "Semantic Star" and custom luminous states.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AtmosphericShard(
    fragment: FragmentEntity,
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f,
    isLuminous: Boolean = false,
    jitterValue: Float? = null,
    fontSize: TextUnit = 18.sp,
    lineHeight: TextUnit = 26.sp,
    textAlign: TextAlign = TextAlign.Start,
    jitterMaxOffset: Int = 160,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val textAlpha = if (isLuminous) alpha.coerceAtLeast(0.9f) else alpha
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    
    val textStyle = TextStyle(
        color = if (isLuminous) AmberPatina.copy(alpha = textAlpha * 0.9f) else MaterialTheme.colorScheme.onBackground.copy(alpha = textAlpha),
        fontSize = fontSize,
        fontFamily = FontFamily.Serif,
        lineHeight = lineHeight,
        textAlign = textAlign,
        letterSpacing = 0.2.sp,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The Semantic Star (Drifting Particle)
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .atmosphericJitter(
                    jitterValue = jitterValue ?: fragment.embedding?.getOrNull(0),
                    fallbackSeed = fragment.text.hashCode(),
                    maxOffset = jitterMaxOffset
                )
                .size(8.dp) // Larger for visibility
                .clip(CircleShape)
                .background(
                    if (isLuminous) AmberPatina.copy(alpha = 0.8f) 
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                .drawBehind {
                    drawCircle(
                        color = (if (isLuminous) AmberPatina else onBackgroundColor)
                            .copy(alpha = 0.15f),
                        radius = size.maxDimension * 3f
                    )
                }
        )

        // The Fragment Text (Centered and Stable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (isLuminous) {
                        // Subtle organic circular glow behind the text
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AmberPatina.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.maxDimension * 0.8f
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fragment.text,
                style = textStyle,
                maxLines = maxLines,
                overflow = overflow,
            )
        }
    }
}
