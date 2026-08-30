package dissonance.cunninglinguist.fragmentry.features.explore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dissonance.cunninglinguist.fragmentry.core.ui.components.AmbientAffordance
import dissonance.cunninglinguist.fragmentry.core.ui.components.AtmosphericShard
import dissonance.cunninglinguist.fragmentry.core.ui.components.atmosphericJitter
import dissonance.cunninglinguist.fragmentry.core.ui.theme.Atmosphere
import dissonance.cunninglinguist.fragmentry.core.ui.theme.AmberPatina
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.ui.graphics.Shadow
import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity

@Composable
fun EchoScreen(
    viewModel: ExploreViewModel,
    onRelease: () -> Unit,
    onMotifClick: (String) -> Unit,
) {
    val focus by viewModel.focusFragment.collectAsState()
    val resonances by viewModel.resonances.collectAsState()
    val motifs by viewModel.motifs.collectAsState()
    
    var isEditing by remember { mutableStateOf(value = false) }
    var editingTarget by remember { mutableStateOf<FragmentEntity?>(null) }
    var editedText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    fun beginEditing(fragment: FragmentEntity) {
        editingTarget = fragment
        editedText = fragment.text
        isEditing = true
    }

    // Saves are addressed to the fragment the edit session started on, never
    // to whatever is focused when the save eventually lands.
    fun commitEditing() {
        val target: FragmentEntity = editingTarget ?: return
        editingTarget = null
        isEditing = false
        if (editedText != target.text) {
            viewModel.updateFragmentText(target, editedText)
        }
    }

    fun cancelEditing() {
        editingTarget = null
        isEditing = false
    }

    BackHandler {
        if (isEditing) commitEditing() else onRelease()
    }

    LaunchedEffect(editedText) {
        val target: FragmentEntity = editingTarget ?: return@LaunchedEffect
        if (!isEditing || editedText == target.text) return@LaunchedEffect
        delay(2.seconds)
        if (editingTarget?.id == target.id && editedText != target.text) {
            viewModel.updateFragmentText(target, editedText)
            editingTarget = target.copy(text = editedText)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    ),
                ),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (isEditing) commitEditing() else onRelease()
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 40.dp, vertical = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The Soloist (Focus)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = tween(durationMillis = 800),
                    )
                    .drawBehind {
                        if (focus?.isPinned == true) {
                            // Organic circular glow behind the focused text
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        AmberPatina.copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.maxDimension * 0.7f
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isEditing) {
                    BasicTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)),
                    )
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                } else {
                    focus?.let { fragment ->
                        val annotatedText = remember(fragment.text, motifs) {
                            buildAnnotatedString {
                                val words = fragment.text.split(Regex("(?<=\\s)|(?=\\s)"))
                                words.forEach { word ->
                                    val cleanWord = word.lowercase().filter { it.isLetter() }
                                    if (motifs.contains(cleanWord)) {
                                        withLink(
                                            LinkAnnotation.Clickable(
                                                tag = "MOTIF",
                                                linkInteractionListener = { onMotifClick(cleanWord) },
                                                styles = TextLinkStyles(
                                                    style = SpanStyle(
                                                        color = AmberPatina.copy(alpha = 0.75f),
                                                        shadow = Shadow(
                                                            color = AmberPatina.copy(alpha = 0.4f),
                                                            blurRadius = 8f
                                                        )
                                                    ),
                                                ),
                                            ),
                                        ) {
                                            append(word)
                                        }
                                    } else {
                                        append(word)
                                    }
                                }
                            }
                        }
                        
                        Text(
                            text = annotatedText,
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Serif,
                                textAlign = TextAlign.Center,
                                lineHeight = 38.sp,
                                shadow = if (fragment.isPinned) {
                                    Shadow(
                                        color = AmberPatina.copy(alpha = 0.3f),
                                        blurRadius = 12f
                                    )
                                } else null
                            ),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                beginEditing(fragment)
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            // The Chorus (Resonances)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                AnimatedContent(
                    targetState = resonances,
                    transitionSpec = {
                        (fadeIn(tween(1200)) + slideInVertically(tween(1400, easing = LinearOutSlowInEasing)) { 40 })
                            .togetherWith(fadeOut(tween(800)))
                    },
                    label = "chorus_drift"
                ) { currentResonances ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(48.dp),
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "chorus_drift")
                        
                        currentResonances.forEachIndexed { index, resonance ->
                            val drift by infiniteTransition.animateFloat(
                                initialValue = -8f,
                                targetValue = 8f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = 3000 + (index * 500), 
                                        easing = LinearOutSlowInEasing
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "drift_$index"
                            )

                            AtmosphericShard(
                                fragment = resonance,
                                modifier = Modifier.graphicsLayer { translationX = drift },
                                alpha = if (resonance.isPinned) 0.6f else 0.35f,
                                isLuminous = resonance.isPinned,
                                jitterValue = resonance.embedding?.getOrNull(0),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                onClick = {
                                    commitEditing()
                                    viewModel.setFocus(resonance)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Return and Pin affordances (Ambient)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AmbientAffordance(
                    text = "← field",
                    alpha = 0.45f,
                    description = "return to field",
                    onClick = onRelease
                )

                focus?.let { fragment ->
                    AmbientAffordance(
                        text = if (fragment.isPinned) "collected" else "collect",
                        alpha = if (fragment.isPinned) 0.8f else 0.45f,
                        shadow = if (fragment.isPinned) {
                            Shadow(
                                color = AmberPatina.copy(alpha = 0.5f),
                                blurRadius = 10f
                            )
                        } else null,
                        description = if (fragment.isPinned) "fragment collected" else "collect fragment",
                    ) {
                        viewModel.togglePin(fragment)
                    }

                    AmbientAffordance(
                        text = "dissolve",
                        alpha = 0.45f,
                        description = "dissolve fragment into the void",
                    ) {
                        cancelEditing()
                        viewModel.dissolveFragment(fragment)
                        onRelease()
                    }
                }
            }
        }
    }
}
