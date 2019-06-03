package io.sodalic.blob.sharedui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import io.sodalic.blob.context.BlobContext;

/**
 * This is a base class for background fire-and-forget async tasks
 */
public abstract class HttpBgAsync extends BaseHttpAsync<Void> {

    protected HttpBgAsync(@NonNull String baseTag, @NonNull BlobContext blobContext) {
        super("AsyncBg." + baseTag, blobContext);
    }

    @Override
    protected final void updateUiBefore() {
        // do nothing here
    }

    @Override
    protected final void updateUiAfter() {
        // do nothing here
    }

    @Override
    protected void handleError(@NonNull Exception ex) {
        Log.w(TAG, "handleError", ex);
    }


    @Override
    protected void handleSuccess(Void result) {
    }

    @Override
    protected final Void doTask(@NonNull BlobContext blobContext) throws Exception {
        // pass blobContext.getAppContext() as it is the only
        // safe choice in background task
        doTaskImpl(blobContext, blobContext.getAppContext());
        return null;
    }

    protected abstract void doTaskImpl(@NonNull BlobContext blobContext, @NonNull Context appContext) throws Exception;

}
