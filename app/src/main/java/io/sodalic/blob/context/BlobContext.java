package io.sodalic.blob.context;

import android.content.Context;

import io.sodalic.blob.tracking.UploadManager;

import io.sodalic.blob.net.ServerApi;

/**
 * Custom context backed by the {@link io.sodalic.blob.BlobApp}
 * that is used to provide additional infrastructure with fewer global shared object
 *
 * @see BlobContextImpl actual implementaton
 * @see BlobContextProxy proxy used by activities and so on that locates {@link BlobContextImpl} and deleagtes calls there
 */
public interface BlobContext {
    /**
     * @return The Android {@link Context} of the underlying entity
     */
    Context getAndroidContext();

    /**
     * @return The root {@link Context} (i.e. not bound to UI). Effectively this is the same as {@code getAndroidContext().getApplicationContext()}
     */
    Context getAppContext();

    /**
     * @return Whether the context is already fully initialized and you can do whatever you want. If it is not
     * fully initialized yet, you should be careful as some services such as {@link #getServerApi()} might be
     * not available (i.e. {@code null}) and other like {@link #getUploadManager()} might not work properly.
     */
    boolean isFullyInitialized();

    ServerApi getServerApi();

    void initServerApi(String serverUrl);

    UploadManager getUploadManager();

}
