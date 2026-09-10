package dissonance.cunninglinguist.fragmentry.features.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import dissonance.cunninglinguist.fragmentry.features.explore.ExploreViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.ui.window.Dialog
import androidx.compose.material3.LinearProgressIndicator

/**
 * The Veil is the quiet space for system maintenance (Backup/Restore).
 */
@Composable
fun VeilScreen(
    viewModel: ExploreViewModel,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onMigrate: () -> Unit,
) {
    val isMLReady by viewModel.isMLReady.collectAsState()
    val migrationState by viewModel.migrationState.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 48.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header (Non-centered, fixed at top of scrollable area)
            Text(
                text = "the veil",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 4.sp,
                ),
                modifier = Modifier.padding(bottom = 64.dp)
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                MaintenanceAction(
                    title = "preserve",
                    subtitle = "export your memory field to a file",
                    onClick = onBackup,
                )

                MaintenanceAction(
                    title = "summon",
                    subtitle = "restore memories from a backup",
                    onClick = onRestore,
                )

                MaintenanceAction(
                    title = "migrate",
                    subtitle = "import fragments from a text file",
                    onClick = onMigrate,
                )
            }

            Text(
                text = if (isMLReady) "the resonance is clear" else "the resonance is settling",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp,
                ),
                modifier = Modifier.padding(top = 64.dp),
            )
        }

        // Migration Progress Modal
        if (migrationState.isMigrating) {
            Dialog(onDismissRequest = {}) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "migrating fragments",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 2.sp
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val progress = if (migrationState.total > 0) {
                            migrationState.current.toFloat() / migrationState.total
                        } else 0f
                        
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = dissonance.cunninglinguist.fragmentry.core.ui.theme.AmberPatina.copy(alpha = 0.9f),
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "${migrationState.current} / ${migrationState.total}",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick),
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
            ),
        )
        Text(
            text = subtitle,
            style = TextStyle(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
