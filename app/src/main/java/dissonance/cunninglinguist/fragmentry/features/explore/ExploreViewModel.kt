package dissonance.cunninglinguist.fragmentry.features.explore

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import dissonance.cunninglinguist.fragmentry.core.domain.analysis.MotifAnalyzer
import dissonance.cunninglinguist.fragmentry.core.domain.analysis.SemanticMotifGrouper
import dissonance.cunninglinguist.fragmentry.core.domain.ml.TextEmbedder
import dissonance.cunninglinguist.fragmentry.core.domain.repository.FragmentRepository
import dissonance.cunninglinguist.fragmentry.core.domain.retrieval.ResonanceRanker
import dissonance.cunninglinguist.fragmentry.core.data.backup.BackupManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

data class MigrationState(
    val current: Int = 0,
    val total: Int = 0,
    val isMigrating: Boolean = false
)

class ExploreViewModel(
    private val repository: FragmentRepository,
    private val textEmbedder: TextEmbedder,
    private val ranker: ResonanceRanker = ResonanceRanker(),
    private val motifGrouper: SemanticMotifGrouper = SemanticMotifGrouper(textEmbedder),
    private val backupManager: BackupManager? = null,
    private val preferences: SharedPreferences? = null,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private companion object {
        const val KEY_EXPANDED = "expanded_motifs"
        const val KEY_FOCUS_ID = "focus_fragment_id"
        const val KEY_ACTIVE_MOTIF = "active_motif"
    }

    private val _expandedMotifs = MutableStateFlow<Set<String?>>(emptySet())

    fun toggleMotifExpansion(motif: String?) {
        _expandedMotifs.update { current ->
            val next = if (current.contains(motif)) current - motif else current + motif
            preferences?.edit()?.putStringSet(KEY_EXPANDED, next.filterNotNull().toSet())?.apply()
            // Special handling for null (Primal)
            if (motif == null) {
                preferences?.edit()?.putBoolean("expanded_primal", next.contains(null))?.apply()
            }
            next
        }
    }

    init {
        val saved = preferences?.getStringSet(KEY_EXPANDED, emptySet()) ?: emptySet()
        _expandedMotifs.value = saved.toSet()
        if (preferences?.getBoolean("expanded_primal", false) == true) {
            _expandedMotifs.update { it + null }
        }
    }

    private val _migrationState = MutableStateFlow(MigrationState())
    val migrationState = _migrationState.asStateFlow()

    /**
     * Imports fragments from a raw text stream. The migration state always
     * terminates; [onFinished] receives the imported count, or -1 on failure.
     */
    fun migrateFragments(inputStream: InputStream, onFinished: (importedCount: Int) -> Unit = {}) {
        val manager = backupManager ?: run {
            onFinished(0)
            return
        }
        viewModelScope.launch {
            _migrationState.value = MigrationState(isMigrating = true)
            val imported = try {
                manager.migrate(inputStream, textEmbedder) { current, total ->
                    _migrationState.value = MigrationState(current, total, true)
                }
            } catch (e: Exception) {
                Log.e("ExploreViewModel", "Migration failed", e)
                -1
            } finally {
                _migrationState.value = MigrationState(isMigrating = false)
            }
            onFinished(imported)
        }
    }

    // Scroll persistence for constellations (limited to avoid Binder bloat)
    private val _scrollIndices = mutableMapOf<String, Int>()
    private val _scrollOffsets = mutableMapOf<String, Int>()

    fun getScrollPosition(motif: String): Pair<Int, Int> {
        return (_scrollIndices[motif] ?: 0) to (_scrollOffsets[motif] ?: 0)
    }

    fun updateScrollPosition(motif: String, index: Int, offset: Int) {
        // Limit total cached positions to prevent memory leaks or bloat
        if (_scrollIndices.size > 20) {
            _scrollIndices.clear()
            _scrollOffsets.clear()
        }
        _scrollIndices[motif] = index
        _scrollOffsets[motif] = offset
    }

    private val _rawFragments = MutableStateFlow<List<FragmentEntity>>(emptyList())

    // True once the repository has emitted at least once; distinguishes
    // "still loading" from "the library is genuinely empty".
    private val _isLoaded = MutableStateFlow(false)

    // Literal motif-tag frequencies across the library; only a changing tag
    // set re-triggers semantic grouping.
    private val motifFrequencies = _rawFragments
        .map { fragments ->
            fragments.flatMap { MotifAnalyzer.deserializeTags(it.tags) }
                .groupingBy { it }
                .eachCount()
        }
        .distinctUntilChanged()
        .flowOn(defaultDispatcher)

    // Tag -> representative tag. Empty until the embedder settles; every
    // consumer falls back to identity until then.
    private val _canonicalMotifs = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _searchQuery = MutableStateFlow("")

    val fieldState = combine(_rawFragments, _searchQuery, _expandedMotifs, _isLoaded, _canonicalMotifs) { fragments, query, expanded, isLoaded, canonical ->
        val filtered = if (query.isBlank()) fragments
                      else fragments.filter { fragment ->
                          // Motifs match by substring, mirroring text search:
                          // seeking "star" must find the [stars] constellation.
                          val motifMatch = MotifAnalyzer.deserializeTags(fragment.tags).any { motif ->
                              motif.contains(query, ignoreCase = true)
                          }
                          fragment.text.contains(query, ignoreCase = true) || motifMatch
                      }

        // Grouping logic for Nebula clustering
        val grouped: Map<String?, List<FragmentEntity>> = if (query.isNotBlank()) {
            mapOf(null to filtered) // No grouping during search
        } else {
            groupFragmentsByMotif(filtered, canonical)
        }

        FieldState(
            fragments = filtered,
            groupedFragments = grouped,
            expandedMotifs = expanded,
            searchQuery = query,
            isLoading = !isLoaded,
        )
    }
        .flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FieldState())

    private fun groupFragmentsByMotif(
        fragments: List<FragmentEntity>,
        canonical: Map<String, String>
    ): Map<String?, List<FragmentEntity>> {
        if (fragments.isEmpty()) return emptyMap()

        fun canonicalTags(fragment: FragmentEntity): Set<String> =
            MotifAnalyzer.deserializeTags(fragment.tags)
                .mapTo(mutableSetOf()) { canonical[it] ?: it }

        val result = mutableMapOf<String?, MutableList<FragmentEntity>>()

        // 1. Cluster frequency: merge counts of tags that resonate (a
        //    fragment carrying both [dream] and [dreams] counts once).
        val clusterCounts = mutableMapOf<String, Int>()
        fragments.forEach { fragment ->
            canonicalTags(fragment).forEach { cluster ->
                clusterCounts[cluster] = (clusterCounts[cluster] ?: 0) + 1
            }
        }
        val motifCounts = clusterCounts.entries
            .sortedByDescending { it.value }
            .take(12) // Top 12 motifs become nebula anchors
            .map { it.key }

        val assignedIds = mutableSetOf<Long>()

        // 2. Assign fragments to their most prominent anchor
        motifCounts.forEach { motif ->
            val cluster = fragments.filter {
                it.id !in assignedIds && motif in canonicalTags(it)
            }
            if (cluster.isNotEmpty()) {
                result[motif] = cluster.toMutableList()
                assignedIds.addAll(cluster.map { it.id })
            }
        }

        // 3. Collect orphans into the "Primal" field
        val primal = fragments.filter { it.id !in assignedIds }
        if (primal.isNotEmpty()) {
            result[null] = primal.toMutableList()
        }

        return result
    }

    private val _focusFragment = MutableStateFlow<FragmentEntity?>(null)
    val focusFragment = _focusFragment.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val resonances = combine(_focusFragment, _rawFragments) { focus, all ->
        if (focus == null || focus.embedding == null) {
            emptyList()
        } else {
            val query = focus.embedding
            val candidates = all.filter { it.id != focus.id }
            ranker.rank(query, candidates).take(3)
        }
    }
    .flowOn(defaultDispatcher)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // Words in the focused text whose canonical motif is shared with the
    // library — "dreams" highlights when the shared cluster is "dream".
    val motifs = combine(_focusFragment, _rawFragments, _canonicalMotifs) { focus, all, canonical ->
        if (focus == null) {
            emptySet()
        } else {
            val shared = MotifAnalyzer.findSharedMotifs(focus, all)
                .mapTo(mutableSetOf()) { canonical[it] ?: it }
            if (shared.isEmpty()) {
                emptySet()
            } else {
                focus.text.lowercase()
                    .split(Regex("[\\s\\p{Punct}]+"))
                    .asSequence()
                    .map { word -> word.filter { it.isLetter() } }
                    .filter { it.length > 3 }
                    .filterTo(mutableSetOf()) { word -> (canonical[word] ?: word) in shared }
            }
        }
    }
    .flowOn(defaultDispatcher)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())

    private val _activeMotif = MutableStateFlow(savedStateHandle.get<String>(KEY_ACTIVE_MOTIF))
    val activeMotif = _activeMotif.asStateFlow()

    // A thread gathers every fragment whose tags resonate with the active
    // motif's cluster, regardless of surface form.
    val constellationFragments = combine(activeMotif, _rawFragments, _canonicalMotifs) { motif, fragments, canonical ->
        if (motif == null) {
            emptyList()
        } else {
            val anchor = canonical[motif] ?: motif
            fragments.filter { fragment ->
                MotifAnalyzer.deserializeTags(fragment.tags).any { (canonical[it] ?: it) == anchor }
            }
        }
    }
    .flowOn(defaultDispatcher)
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pinnedFragments = _rawFragments.map { fragments ->
        fragments.filter { it.isPinned }
    }
    .flowOn(defaultDispatcher)
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pinnedMotifs = combine(pinnedFragments, _canonicalMotifs) { fragments, canonical ->
        fragments.flatMap { MotifAnalyzer.deserializeTags(it.tags) }
            .groupingBy { canonical[it] ?: it }
            .eachCount()
    }
    .flowOn(defaultDispatcher)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    // Re-checked until the model settles; a failed load is retried with quiet
    // exponential backoff rather than leaving "the resonance is settling"
    // forever.
    val isMLReady = flow {
        var attempt = 0
        while (true) {
            val ready = textEmbedder.isReady()
            emit(ready)
            if (ready) break
            delay(1_000L shl attempt.coerceAtMost(6)) // 1s..64s
            attempt++
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        repository.getFragments()
            .onEach { fragments ->
                // lastViewedAt is bookkeeping; normalizing it away lets the
                // StateFlow's equality check suppress library-wide recompute
                // cascades for view-timestamp-only writes.
                _rawFragments.value = fragments.map { it.copy(lastViewedAt = 0L) }
                _isLoaded.value = true

                // Restore the focused fragment after process death once the
                // library is available.
                if (_focusFragment.value == null) {
                    savedStateHandle.get<Long>(KEY_FOCUS_ID)?.let { id ->
                        fragments.find { it.id == id }?.let { _focusFragment.value = it }
                    }
                }

                _focusFragment.value?.let { currentFocus ->
                    fragments.find { it.id == currentFocus.id }?.let { updated ->
                        if (updated.text != currentFocus.text || updated.isPinned != currentFocus.isPinned) {
                            _focusFragment.value = updated
                        }
                    }
                }
            }
            .launchIn(viewModelScope)

        // Semantic motif grouping: rebuild when the library's tag set changes,
        // or once resonance becomes available after startup.
        viewModelScope.launch {
            combine(motifFrequencies, isMLReady) { frequencies, ready ->
                if (ready) frequencies else emptyMap()
            }
                .distinctUntilChanged()
                .collectLatest { frequencies ->
                    _canonicalMotifs.value = motifGrouper.buildCanonicalMap(frequencies)
                }
        }
    }

    fun setFocus(fragment: FragmentEntity) {
        _focusFragment.value = fragment
        savedStateHandle[KEY_FOCUS_ID] = fragment.id
        // Focusing the Echo is the view event the lastViewedAt column records.
        viewModelScope.launch { repository.touchLastViewed(fragment.id) }
    }

    fun clearFocus() {
        _focusFragment.value = null
        savedStateHandle.remove<Long>(KEY_FOCUS_ID)
    }

    fun setMotif(motif: String) {
        _activeMotif.value = motif
        savedStateHandle[KEY_ACTIVE_MOTIF] = motif
    }

    fun clearMotif() {
        _activeMotif.value = null
        savedStateHandle.remove<String>(KEY_ACTIVE_MOTIF)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePin(fragment: FragmentEntity) {
        viewModelScope.launch {
            // Targeted update: a full-entity write would carry the
            // normalized lastViewedAt from UI state back into the database.
            repository.setPinned(fragment.id, !fragment.isPinned)
            if (_focusFragment.value?.id == fragment.id) {
                _focusFragment.value = _focusFragment.value?.copy(isPinned = !fragment.isPinned)
            }
        }
    }

    private val _dissolvedFragments = MutableSharedFlow<FragmentEntity>(extraBufferCapacity = 8)
    val dissolvedFragments = _dissolvedFragments.asSharedFlow()

    fun dissolveFragment(fragment: FragmentEntity) {
        viewModelScope.launch {
            // Re-read so the emitted entity carries real database values and
            // a recall restores the fragment losslessly.
            val fresh = repository.getFragment(fragment.id) ?: return@launch
            repository.deleteFragment(fresh)
            if (_focusFragment.value?.id == fragment.id) {
                _focusFragment.value = null
                savedStateHandle.remove<Long>(KEY_FOCUS_ID)
            }
            _dissolvedFragments.emit(fresh)
        }
    }

    fun restoreFragment(fragment: FragmentEntity) {
        viewModelScope.launch {
            repository.saveFragment(fragment)
        }
    }

    fun updateFragmentText(target: FragmentEntity, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            val fresh = repository.getFragment(target.id) ?: return@launch
            if (fresh.text == newText) return@launch
            val embedding = textEmbedder.embed(newText).getOrNull()
            val updated = fresh.copy(
                text = newText,
                // Keep the stale vector for ranking in the meantime, but drop
                // the fingerprint so the backfill pass knows to re-embed.
                embedding = embedding ?: fresh.embedding,
                modelFingerprint = if (embedding != null) textEmbedder.getModelFingerprint() else null,
            )
            repository.updateFragment(updated)
            if (_focusFragment.value?.id == updated.id) {
                _focusFragment.value = updated
            }
        }
    }
}
