package io.sodalic.blob.context;

import android.content.Context;
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

    ServerApi getServerApi();

    void initServerApi(String serverUrl);

}
