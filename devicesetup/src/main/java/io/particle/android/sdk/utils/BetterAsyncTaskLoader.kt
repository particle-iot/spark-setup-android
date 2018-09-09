package io.particle.android.sdk.utils

import android.content.Context
import android.support.v4.content.AsyncTaskLoader


abstract class BetterAsyncTaskLoader<T>(context: Context) : AsyncTaskLoader<T>(context) {

    abstract val loadedContent: T?

    abstract fun hasContent(): Boolean

    override fun onStartLoading() {
        // How is this not on AsyncTaskLoader already?
        if (hasContent()) {
            deliverResult(loadedContent)
        }
        if (takeContentChanged() || !hasContent()) {
            forceLoad()
        }
    }
}
