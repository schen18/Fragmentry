package dissonance.cunninglinguist.fragmentry.features.assembly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import dissonance.cunninglinguist.fragmentry.core.ui.components.AtmosphericShard

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import dissonance.cunninglinguist.fragmentry.core.ui.components.AmbientAffordance

/**
 * The Collector Screen handles the assembly of collected fragments in a semantically aligned field.
 */
@Composable
fun CollectorScreen(
    pinnedFragments: List<FragmentEntity>,
    pinnedMotifs: Map<String, Int>,
    onFragmentClick: (FragmentEntity) -> Unit,
    onFragmentLongClick: (FragmentEntity) -> Unit,
    onMotifClick: (String) -> Unit,
    onExport: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
    ) {
        if (pinnedFragments.isEmpty()) {
            Text(
                text = "nothing collected yet",
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 2.sp
                )
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp, start = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "assembly",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 4.sp
                            )
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AmbientAffordance(
                                text = "weave",
                                alpha = 0.65f,
                                description = "copy all to clipboard",
                                onClick = {
                                    val poem = pinnedFragments.joinToString("\n\n") { it.text }
                                    clipboardManager.setText(AnnotatedString(poem))
                                }
                            )
                            AmbientAffordance(
                                text = "preserve",
                                alpha = 0.65f,
                                description = "export as text file",
                                onClick = onExport
                            )
                        }
                    }
                }

                // Motif Cloud
                if (pinnedMotifs.isNotEmpty()) {
                    item {
                        dissonance.cunninglinguist.fragmentry.core.ui.theme.Atmosphere.DriftingMotifs(
                            motifs = pinnedMotifs,
                            onMotifClick = onMotifClick,
                            modifier = Modifier.padding(bottom = 48.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                }

                items(
                    items = pinnedFragments,
                    key = { it.id }
                ) { fragment ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AtmosphericShard(
                            fragment = fragment,
                            modifier = Modifier.padding(vertical = 12.dp),
                            alpha = 0.85f,
                            isLuminous = true,
                            jitterValue = fragment.embedding?.getOrNull(0),
                            fontSize = 17.sp,
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Center,
                            onClick = { onFragmentClick(fragment) },
                            onLongClick = { onFragmentLongClick(fragment) }
                        )
                    }
                }
            }
        }
    }
}
