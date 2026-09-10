package dissonance.cunninglinguist.fragmentry.features.capture

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dissonance.cunninglinguist.fragmentry.core.ui.components.AmbientAffordance

@Composable
fun SparkScreen(
    viewModel: CaptureViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.isSaving) {
        if (!uiState.isSaving) {
            focusRequester.requestFocus()
        }
    }

    BackHandler {
        viewModel.sparkFragment(onDismiss)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Explicit Return Affordance
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(32.dp),
            contentAlignment = Alignment.TopStart
        ) {
            AmbientAffordance(
                text = "cancel",
                alpha = 0.55f,
                description = "cancel capture",
                onClick = onDismiss
            )
        }

        if (!uiState.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 120.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = uiState.text,
                    onValueChange = viewModel::onTextChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            Crossfade(
                                targetState = uiState.text.isEmpty(),
                                label = "placeholder_fade"
                            ) { isEmpty ->
                                if (isEmpty) {
                                    Text(
                                        text = "a spark...",
                                        style = TextStyle(
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            fontSize = 24.sp,
                                            fontFamily = FontFamily.Serif,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        // Explicit Save Affordance
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = uiState.text.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AmbientAffordance(
                    text = "capture",
                    alpha = 0.7f,
                    description = "capture fragment",
                ) {
                    viewModel.sparkFragment()
                }
            }
        }
    }
}
