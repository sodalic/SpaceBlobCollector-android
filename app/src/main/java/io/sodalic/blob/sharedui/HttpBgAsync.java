package io.sodalic.blob.sharedui;

import android.content.Context;
import android.util.Log;
import io.sodalic.blob.context.BlobContext;

/**
 * This is a base class for background fire-and-forget async tasks
 */
public abstract class HttpBgAsync extends BaseHttpAsync<Void> {

    protected HttpBgAsync(String baseTag, BlobContext blobContext) {
        super("AsyncBg." + baseTag, blobContext);
    }

    @Override
    protected final void updateUiAfter() {
        // do nothing here
    }

    @Override
    protected void handleError(Exception ex) {
        Log.w(logTag, "handleError", ex);
    }


    @Override
    protected void handleSuccess(Void result) {
    }

    @Override
    protected final Void doTask(BlobContext blobContext) throws Exception {
        // pass blobContext.getAppContext() as it is the only
        // safe choice in background task
        doTaskImpl(blobContext, blobContext.getAppContext());
        return null;
    }

    protected abstract void doTaskImpl(BlobContext blobContext, Context appContext) throws Exception;

}
