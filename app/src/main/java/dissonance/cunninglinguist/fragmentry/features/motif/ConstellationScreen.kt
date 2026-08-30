package dissonance.cunninglinguist.fragmentry.features.motif

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import dissonance.cunninglinguist.fragmentry.core.ui.components.AtmosphericShard
import dissonance.cunninglinguist.fragmentry.core.ui.theme.AmberPatina
import dissonance.cunninglinguist.fragmentry.core.ui.theme.Atmosphere

import dissonance.cunninglinguist.fragmentry.core.ui.components.AmbientAffordance

/**
 * ConstellationScreen provides a filtered view of fragments sharing a specific motif.
 */
@Composable
fun ConstellationScreen(
    motif: String,
    fragments: List<FragmentEntity>,
    scrollState: LazyListState = rememberLazyListState(),
    onFragmentClick: (FragmentEntity) -> Unit,
    onFragmentLongClick: (FragmentEntity) -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, start = 32.dp, end = 32.dp)
        ) {
            // The Thread Header
            Text(
                text = "the $motif thread",
                style = TextStyle(
                    color = AmberPatina.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 64.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = scrollState,
                contentPadding = PaddingValues(bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(64.dp),
            ) {
                items(
                    items = fragments,
                    key = { it.id },
                ) { fragment ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AtmosphericShard(
                            fragment = fragment,
                            alpha = 1.0f, // Full luminosity for the thread members
                            isLuminous = fragment.isPinned,
                            jitterValue = fragment.embedding?.getOrNull(0),
                            textAlign = TextAlign.Center,
                            onClick = { onFragmentClick(fragment) },
                            onLongClick = { onFragmentLongClick(fragment) },
                        )
                    }
                }
            }
        }

        // Return affordance (Aligned with header top)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, start = 32.dp),
            contentAlignment = Alignment.TopStart
        ) {
            AmbientAffordance(
                text = "← field",
                alpha = 0.45f,
                onClick = onBack
            )
        }
    }
}
