package dissonance.cunninglinguist.fragmentry.core.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the exploratory backstack with persistence across process death.
 */
class NavigationViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private companion object {
        const val KEY_STACK = "nav_stack"
    }

    private val _screenStack = MutableStateFlow(restoreStack())
    val screenStack: StateFlow<List<Screen>> = _screenStack.asStateFlow()

    val currentScreen: Screen
        get() = _screenStack.value.last()

    fun navigateTo(screen: Screen) {
        if (currentScreen == screen) return

        _screenStack.update { current ->
            if (screen == Screen.Field) {
                listOf(Screen.Field)
            } else {
                current + screen
            }
        }
        persist()
    }

    fun navigateBack(): Boolean {
        if (_screenStack.value.size > 1) {
            _screenStack.update { it.dropLast(1) }
            persist()
            return true
        }
        return false
    }

    /**
     * Names survive enum reordering between releases; entries saved by a
     * future version are dropped instead of crashing on restore.
     */
    private fun restoreStack(): List<Screen> {
        val saved = savedStateHandle.get<List<String>>(KEY_STACK) ?: return listOf(Screen.Field)
        return saved.mapNotNull { name -> Screen.entries.firstOrNull { it.name == name } }
            .ifEmpty { listOf(Screen.Field) }
    }

    private fun persist() {
        savedStateHandle[KEY_STACK] = _screenStack.value.map { it.name }
    }
}
