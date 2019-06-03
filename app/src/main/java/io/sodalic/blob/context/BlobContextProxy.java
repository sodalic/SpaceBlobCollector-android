package io.sodalic.blob.context;

import android.content.Context;
import io.sodalic.blob.BlobApp;
import io.sodalic.blob.face.FaceSightcorpApi;
import io.sodalic.blob.net.ServerApi;

import java.util.Objects;

import io.sodalic.blob.storage.UserStateData;
import io.sodalic.blob.tracking.UploadManager;

/**
 * Proxy to the {@link BlobContext} that is expected to be used in most of the cases.
 * It works by getting the {@link BlobApp} using {@link Context#getApplicationContext()}
 * but returns the original {@link Context} as {@link #getAndroidContext()}
 */
public final class BlobContextProxy implements BlobContext {
    private final Context context;
    private final BlobContext blobContext;

    public BlobContextProxy(Context context) {
        Objects.requireNonNull(context, "context");
        this.context = context; //preserve the original Context!
        this.blobContext = findBlobContext(context);
    }

    private static BlobContext findBlobContext(Context context) {
        Objects.requireNonNull(context, "context");
        if (context instanceof BlobApp)
            return ((BlobApp) context).getBlobContext();

        Context appContext = context.getApplicationContext();
        Objects.requireNonNull(appContext, "context.getApplicationContext()");
        return ((BlobApp) appContext).getBlobContext();
    }

    /**
     * @return The original {@link Context} used to initialize this proxy
     */
    @Override
    public Context getAndroidContext() {
        return context;
    }

    ////////////////////////////////////////////
    /// delegate the rest of the methods to the blobContext

    @Override
    public Context getAppContext() {
        return blobContext.getAppContext();
    }

    @Override
    public boolean isFullyInitialized() {
        return blobContext.isFullyInitialized();
    }

    @Override
    public ServerApi getServerApi() {
        return blobContext.getServerApi();
    }

    @Override
    public void initServerApi(String serverUrl) {
        blobContext.initServerApi(serverUrl);
    }

    @Override
    public UploadManager getUploadManager() {
        return blobContext.getUploadManager();
    }

    @Override
    public FaceSightcorpApi getFaceApi() {
        return blobContext.getFaceApi();
    }

    @Override
    public UserStateData getUserStateData() {
        return blobContext.getUserStateData();
    }
}
