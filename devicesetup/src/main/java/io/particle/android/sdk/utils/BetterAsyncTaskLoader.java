package io.particle.android.sdk.utils;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;


public abstract class BetterAsyncTaskLoader<T> extends AsyncTaskLoader<T> {

    public BetterAsyncTaskLoader(Context context) {
        super(context);
    }

    public abstract boolean hasContent();

    public abstract T getLoadedContent();

    @Override
    protected void onStartLoading() {
        // How is this not on AsyncTaskLoader already?
        if (hasContent()) {
            deliverResult(getLoadedContent());
        }
        if (takeContentChanged() || !hasContent()) {
            forceLoad();
        }
    }
}
