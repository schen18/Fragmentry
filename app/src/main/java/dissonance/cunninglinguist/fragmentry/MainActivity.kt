package dissonance.cunninglinguist.fragmentry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import dissonance.cunninglinguist.fragmentry.core.ui.components.FragmentryNav
import dissonance.cunninglinguist.fragmentry.core.ui.components.TheCollector
import dissonance.cunninglinguist.fragmentry.core.ui.navigation.NavigationViewModel
import dissonance.cunninglinguist.fragmentry.core.ui.navigation.Screen
import dissonance.cunninglinguist.fragmentry.core.ui.theme.AmberPatina
import dissonance.cunninglinguist.fragmentry.core.ui.theme.Atmosphere
import dissonance.cunninglinguist.fragmentry.core.ui.theme.FragmentryTheme
import dissonance.cunninglinguist.fragmentry.features.capture.CaptureViewModel
import dissonance.cunninglinguist.fragmentry.features.capture.SparkScreen
import dissonance.cunninglinguist.fragmentry.features.explore.ExploreViewModel
import dissonance.cunninglinguist.fragmentry.features.explore.FieldScreen
import dissonance.cunninglinguist.fragmentry.features.explore.EchoScreen
import dissonance.cunninglinguist.fragmentry.features.assembly.CollectorScreen
import dissonance.cunninglinguist.fragmentry.features.maintenance.VeilScreen
import dissonance.cunninglinguist.fragmentry.features.motif.ConstellationScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var container: dissonance.cunninglinguist.fragmentry.core.di.AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        container = (application as FragmentryApplication).container

        setContent {
            FragmentryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navVM: NavigationViewModel = viewModel()
                    val screenStack by navVM.screenStack.collectAsState()
                    val currentScreen = navVM.currentScreen
                    val snackbarHostState = remember { SnackbarHostState() }

                    val fieldScrollState = rememberLazyListState()
                    
                    val exploreVM: ExploreViewModel = viewModel(initializer = {
                        ExploreViewModel(
                            container.repository,
                            container.textEmbedder,
                            motifGrouper = container.motifGrouper,
                            backupManager = container.backupManager,
                            preferences = container.preferences,
                            savedStateHandle = createSavedStateHandle(),
                        )
                    })
                    val captureVM: CaptureViewModel = viewModel(initializer = {
                        CaptureViewModel(container.repository, container.textEmbedder)
                    })

                    fun navigateBack() {
                        val exiting = navVM.currentScreen
                        if (navVM.navigateBack()) {
                            if (exiting == Screen.Echo) exploreVM.clearFocus()
                            if (exiting == Screen.Constellation) exploreVM.clearMotif()
                        }
                    }

                    BackHandler(enabled = screenStack.size > 1 && currentScreen != Screen.Spark) {
                        navigateBack()
                    }

                    val pinnedFragments by exploreVM.pinnedFragments.collectAsState()
                    val pinnedMotifs by exploreVM.pinnedMotifs.collectAsState()
                    val activeMotif by exploreVM.activeMotif.collectAsState()
                    val constellationFragments by exploreVM.constellationFragments.collectAsState()

                    // Backup/Restore Launchers
                    val exportLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("application/json"),
                    ) { uri ->
                        uri?.let {
                            lifecycleScope.launch {
                                runCatching {
                                    val stream = contentResolver.openOutputStream(it)
                                        ?: error("destination unavailable")
                                    container.backupManager.export(stream)
                                }.onSuccess {
                                    snackbarHostState.showSnackbar("memories preserved")
                                }.onFailure {
                                    snackbarHostState.showSnackbar("preservation failed")
                                }
                            }
                        }
                    }

                    val importLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                    ) { uri ->
                        uri?.let {
                            lifecycleScope.launch {
                                runCatching {
                                    val stream = contentResolver.openInputStream(it)
                                        ?: error("source unavailable")
                                    container.backupManager.restore(stream)
                                }.onSuccess {
                                    snackbarHostState.showSnackbar("memories summoned")
                                }.onFailure {
                                    snackbarHostState.showSnackbar("summoning failed")
                                }
                            }
                        }
                    }

                    val assemblyExportLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("text/plain"),
                    ) { uri ->
                        uri?.let {
                            lifecycleScope.launch {
                                val text = pinnedFragments.joinToString("\n\n") { it.text }
                                runCatching {
                                    val stream = contentResolver.openOutputStream(it)
                                        ?: error("destination unavailable")
                                    withContext(Dispatchers.IO) {
                                        stream.use { s -> s.write(text.toByteArray()) }
                                    }
                                }.onSuccess {
                                    snackbarHostState.showSnackbar("assembly preserved")
                                }.onFailure {
                                    snackbarHostState.showSnackbar("preservation failed")
                                }
                            }
                        }
                    }

                    val migrateLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                    ) { uri ->
                        uri?.let {
                            lifecycleScope.launch {
                                val stream = contentResolver.openInputStream(it)
                                if (stream == null) {
                                    snackbarHostState.showSnackbar("migration failed")
                                    return@launch
                                }
                                exploreVM.migrateFragments(stream) { imported ->
                                    lifecycleScope.launch {
                                        if (imported >= 0) {
                                            snackbarHostState.showSnackbar("$imported fragments summoned")
                                        } else {
                                            snackbarHostState.showSnackbar("migration failed")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dissolve is quiet but not irreversible: offer a recall.
                    LaunchedEffect(Unit) {
                        exploreVM.dissolvedFragments.collect { fragment ->
                            val result = snackbarHostState.showSnackbar(
                                "dissolved into the void",
                                actionLabel = "recall",
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                exploreVM.restoreFragment(fragment)
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        @OptIn(ExperimentalAnimationApi::class)
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { Atmosphere.spectralTransition() },
                            label = "spectral_transition"
                        ) { screen ->
                            when (screen) {
                                Screen.Field -> FieldScreen(
                                    viewModel = exploreVM,
                                    scrollState = fieldScrollState,
                                ) { fragment ->
                                    exploreVM.setFocus(fragment)
                                    navVM.navigateTo(Screen.Echo)
                                }
                                Screen.Spark -> SparkScreen(
                                    viewModel = captureVM,
                                    onDismiss = { navigateBack() },
                                )
                                Screen.Echo -> EchoScreen(
                                    viewModel = exploreVM,
                                    onRelease = {
                                        navigateBack()
                                    },
                                    onMotifClick = { motif ->
                                        exploreVM.setMotif(motif)
                                        navVM.navigateTo(Screen.Constellation)
                                    },
                                )
                                Screen.Collected -> CollectorScreen(
                                    pinnedFragments = pinnedFragments,
                                    pinnedMotifs = pinnedMotifs,
                                    onFragmentClick = { fragment ->
                                        exploreVM.setFocus(fragment)
                                        navVM.navigateTo(Screen.Echo)
                                    },
                                    onFragmentLongClick = { fragment ->
                                        exploreVM.togglePin(fragment) // Uncollect
                                    },
                                    onMotifClick = { motif ->
                                        exploreVM.setMotif(motif)
                                        navVM.navigateTo(Screen.Constellation)
                                    },
                                    onExport = {
                                        assemblyExportLauncher.launch("fragmentry_assembly_${System.currentTimeMillis()}.txt")
                                    }
                                )
                                Screen.Veil -> VeilScreen(
                                    viewModel = exploreVM,
                                    onBackup = { exportLauncher.launch("fragmentry_backup_${System.currentTimeMillis()}.json") },
                                    onRestore = { importLauncher.launch(arrayOf("application/json")) },
                                    onMigrate = { migrateLauncher.launch(arrayOf("text/plain")) },
                                )
                                Screen.Constellation -> activeMotif?.let { motif ->
                                    val (savedIndex, savedOffset) = exploreVM.getScrollPosition(motif)
                                    val scrollState = rememberLazyListState(
                                        initialFirstVisibleItemIndex = savedIndex,
                                        initialFirstVisibleItemScrollOffset = savedOffset
                                    )
                                    
                                    // Update persistence when scrolling stops
                                    LaunchedEffect(scrollState.isScrollInProgress) {
                                        if (!scrollState.isScrollInProgress) {
                                            exploreVM.updateScrollPosition(
                                                motif,
                                                scrollState.firstVisibleItemIndex,
                                                scrollState.firstVisibleItemScrollOffset
                                            )
                                        }
                                    }

                                    ConstellationScreen(
                                        motif = motif,
                                        fragments = constellationFragments,
                                        scrollState = scrollState,
                                        onFragmentClick = { fragment ->
                                            exploreVM.setFocus(fragment)
                                            navVM.navigateTo(Screen.Echo)
                                        },
                                        onFragmentLongClick = { fragment ->
                                            exploreVM.dissolveFragment(fragment)
                                        },
                                        onBack = {
                                            navigateBack()
                                        },
                                    )
                                }
                            }
                        }

                        // Persistent Navigation (Hidden during Spark capture void)
                        if (currentScreen != Screen.Spark) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                // The Shelf: ambient collected shards at the
                                // bottom of the Field.
                                if (currentScreen == Screen.Field) {
                                    TheCollector(
                                        pinnedFragments = pinnedFragments,
                                        onFragmentClick = { fragment ->
                                            exploreVM.setFocus(fragment)
                                            navVM.navigateTo(Screen.Echo)
                                        },
                                        onFragmentLongClick = { fragment ->
                                            exploreVM.togglePin(fragment)
                                        },
                                    )
                                }
                                FragmentryNav(
                                    activeTab = when (currentScreen) {
                                        Screen.Field, Screen.Constellation -> "field"
                                        Screen.Echo -> "field"
                                        Screen.Collected -> "collected"
                                        Screen.Veil -> "veil"
                                        Screen.Spark -> "field"
                                    },
                                    onNavigate = { tab ->
                                        val target = when (tab) {
                                            "field" -> Screen.Field
                                            "spark" -> Screen.Spark
                                            "veil" -> Screen.Veil
                                            "collected" -> Screen.Collected
                                            else -> Screen.Field
                                        }
                                        navVM.navigateTo(target)
                                    }
                                )
                            }
                        }

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 80.dp),
                        ) { data ->
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = data.visuals.message,
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        letterSpacing = 1.sp
                                    )
                                )
                                data.visuals.actionLabel?.let { label ->
                                    Text(
                                        text = label,
                                        modifier = Modifier
                                            .padding(start = 16.dp)
                                            .clickable { data.performAction() },
                                        style = TextStyle(
                                            color = AmberPatina.copy(alpha = 0.9f),
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // onDestroy fires on every configuration change; only tear down the
        // embedder when the task is genuinely finishing, or the lazy singleton
        // would hold a closed interpreter for the rest of the process lifetime.
        if (isFinishing) {
            container.shutdown()
        }
    }
}
