package dissonance.cunninglinguist.fragmentry.features.explore

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import dissonance.cunninglinguist.fragmentry.core.ui.components.AtmosphericShard
import dissonance.cunninglinguist.fragmentry.core.ui.components.atmosphericJitter
import dissonance.cunninglinguist.fragmentry.core.ui.theme.Atmosphere
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * The primary browsing surface. 
 * A quiet, non-linear landscape of fragments.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FieldScreen(
    viewModel: ExploreViewModel,
    scrollState: LazyListState = rememberLazyListState(),
    onFragmentClick: (FragmentEntity) -> Unit,
) {
    val state by viewModel.fieldState.collectAsState()
    var isSearching by remember { mutableStateOf(value = false) }
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val adaptiveJitter = (screenWidth / 60).coerceIn(8, 24)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Atmospheric Search Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isSearching) {
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    text = "seek...",
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 2.sp,
                                    ),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                
                Text(
                    text = "close",
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { 
                            isSearching = false
                            viewModel.setSearchQuery("")
                        },
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp,
                    ),
                )
            } else {
                Text(
                    text = "the field",
                    modifier = Modifier.clickable { isSearching = true },
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 4.sp,
                    ),
                )
            }
        }

        // Organic transition for data entry
        AnimatedVisibility(
            visible = state.fragments.isNotEmpty() || state.groupedFragments.isNotEmpty(),
            enter = fadeIn(Atmosphere.SlowFade),
            exit = fadeOut(Atmosphere.MediumFade),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = scrollState,
                contentPadding = PaddingValues(top = 100.dp, bottom = 300.dp, start = 32.dp, end = 32.dp),
            ) {
                state.groupedFragments.forEach { (motif, fragments) ->
                    val isExpanded = state.expandedMotifs.contains(motif)
                    
                    val tagJitterValue = fragments.mapNotNull { it.embedding?.getOrNull(0) }
                        .let { if (it.isNotEmpty()) it.average().toFloat() else null }

                    if (motif != null) {
                        stickyHeader(key = "nebula_$motif") {
                            NebulaHeader(
                                motif = motif,
                                isCollapsed = !isExpanded,
                                jitterValue = tagJitterValue,
                                onClick = { viewModel.toggleMotifExpansion(motif) }
                            )
                        }
                    } else if (state.groupedFragments.size > 1) {
                        item(key = "nebula_primal") {
                            NebulaHeader(
                                motif = "primal field", 
                                isSubtle = true,
                                isCollapsed = !isExpanded,
                                jitterValue = tagJitterValue,
                                onClick = { viewModel.toggleMotifExpansion(null) }
                            )
                        }
                    }

                    if (isExpanded) {
                        itemsIndexed(
                            items = fragments,
                            key = { _, fragment -> fragment.id },
                        ) { index, fragment ->
                            // Saveable so the staggered entrance plays once per
                            // fragment, not again on every scroll-back.
                            var isVisible by rememberSaveable { mutableStateOf(value = false) }
                            LaunchedEffect(Unit) {
                                if (!isVisible) {
                                    val delayMillis = minOf(index, 10) * 100
                                    delay(delayMillis.milliseconds)
                                    isVisible = true
                                }
                            }

                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(Atmosphere.SlowFade) + slideInVertically(tween(1200)) { 20 },
                                exit = fadeOut(Atmosphere.MediumFade),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AtmosphericShard(
                                        fragment = fragment,
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        alpha = if (fragment.isPinned) 1.0f else 0.75f,
                                        isLuminous = fragment.isPinned,
                                        jitterValue = fragment.embedding?.getOrNull(0),
                                        textAlign = TextAlign.Center,
                                        onClick = { onFragmentClick(fragment) },
                                        onLongClick = { viewModel.dissolveFragment(fragment) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.fragments.isEmpty() && !state.isLoading) {
            EmptyField(isSeeking = state.searchQuery.isNotBlank())
        }
    }
}

@Composable
private fun NebulaHeader(
    motif: String,
    isSubtle: Boolean = false,
    isCollapsed: Boolean = false,
    jitterValue: Float? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    0.0f to MaterialTheme.colorScheme.background,
                    0.8f to MaterialTheme.colorScheme.background,
                    1.0f to Color.Transparent
                )
            )
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center // Changed from CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .atmosphericJitter(jitterValue = jitterValue)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            Text(
                text = if (isSubtle) motif else "#$motif",
                style = TextStyle(
                    color = if (isSubtle) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    } else {
                        dissonance.cunninglinguist.fragmentry.core.ui.theme.AmberPatina.copy(alpha = 0.6f)
                    },
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 6.sp,
                )
            )
            
            if (isCollapsed) {
                Text(
                    text = " ...",
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyField(isSeeking: Boolean) {
    val messages = remember(isSeeking) {
        if (isSeeking) listOf(
            "nothing resonates",
            "the seek dissolves into silence",
        ) else listOf(
            "the field is quiet",
            "awaiting a spark",
            "the void remains",
            "silence resonates",
        )
    }
    val message = remember(isSeeking) { messages.random() }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                letterSpacing = 2.sp
            )
        )
    }
}
