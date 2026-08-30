package dissonance.cunninglinguist.fragmentry.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import dissonance.cunninglinguist.fragmentry.core.ui.components.AtmosphericShard
import dissonance.cunninglinguist.fragmentry.core.ui.components.atmosphericJitter
import dissonance.cunninglinguist.fragmentry.core.ui.theme.Atmosphere

/**
 * The Collector is an ambient shelf for pinned fragments, supporting poem assembly.
 */
@Composable
fun TheCollector(
    pinnedFragments: List<FragmentEntity>,
    onFragmentClick: (FragmentEntity) -> Unit,
    onFragmentLongClick: (FragmentEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = pinnedFragments.isNotEmpty(),
        enter = slideInVertically(tween(800)) { it } + fadeIn(Atmosphere.MediumFade),
        exit = slideOutVertically(tween(400)) { it } + fadeOut(Atmosphere.FastFade),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "collected shards",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = pinnedFragments,
                    key = { it.id }
                ) { fragment ->
                    AtmosphericShard(
                        fragment = fragment,
                        modifier = Modifier.widthIn(max = 200.dp),
                        alpha = 0.6f,
                        isLuminous = true,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        jitterMaxOffset = 4,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        onClick = { onFragmentClick(fragment) },
                        onLongClick = { onFragmentLongClick(fragment) }
                    )
                }
            }
        }
    }
}
