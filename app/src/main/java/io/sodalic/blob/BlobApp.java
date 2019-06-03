package io.sodalic.blob;

import android.app.Application;
import android.util.Log;
import io.sodalic.blob.context.BlobContext;
import io.sodalic.blob.context.BlobContextImpl;
import io.sodalic.blob.utils.Utils;

/**
 * Created by SergGr on 28.05.2019.
 */
public class BlobApp extends Application {
    private static final String TAG = Utils.getLogTag(BlobApp.class);

    private BlobContextImpl blobContext;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        // create only here because until this moment getBaseContext() is not initialized yet
        blobContext = new BlobContextImpl(this);
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "onTerminate");
    }

    public BlobContext getBlobContext() {
        return blobContext;
    }

}
