package dissonance.cunninglinguist.fragmentry

import android.app.Application
import dissonance.cunninglinguist.fragmentry.core.di.AppContainer

class FragmentryApplication : Application() {
    /**
     * Dependency container for managing system lifetimes.
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.startEmbeddingBackfill()
    }
}
