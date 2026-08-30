package dissonance.cunninglinguist.fragmentry.features.capture

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import dissonance.cunninglinguist.fragmentry.core.domain.ml.TextEmbedder
import dissonance.cunninglinguist.fragmentry.core.domain.repository.FragmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages the "Spark" capture process, handling persistence and silent embedding.
 */
class CaptureViewModel(
    private val repository: FragmentRepository,
    private val textEmbedder: TextEmbedder
) : ViewModel() {

    private val _uiState = MutableStateFlow(SparkState())
    val uiState = _uiState.asStateFlow()

    fun onTextChanged(newText: String) {
        _uiState.update { it.copy(text = newText) }
    }

    /**
     * Captures the fragment and preserves it in the void.
     * Optionally triggers a callback (e.g., to exit the screen).
     */
    fun sparkFragment(onSuccess: (() -> Unit)? = null) {
        val currentText = _uiState.value.text.trim()
        if (currentText.isEmpty()) {
            onSuccess?.invoke()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            try {
                // Silent embedding generation
                val embeddingResult = textEmbedder.embed(currentText)
                val embedding = embeddingResult.getOrNull()
                
                val fragment = FragmentEntity(
                    text = currentText,
                    embedding = embedding,
                    modelFingerprint = if (embedding != null) textEmbedder.getModelFingerprint() else null
                )
                
                repository.saveFragment(fragment)
                _uiState.update { SparkState() } // Reset to clear screen for next fragment
                onSuccess?.invoke()
            } catch (e: Exception) {
                Log.e("CaptureViewModel", "Failed to spark fragment", e)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
