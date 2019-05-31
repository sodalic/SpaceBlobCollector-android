package io.sodalic.blob.context;

import android.content.Context;
import android.util.Log;
import io.sentry.Sentry;
import io.sentry.android.AndroidSentryClientFactory;
import io.sentry.dsn.InvalidDsnException;
import tracking.UploadHelper;

import io.sodalic.blob.BuildConfig;
import io.sodalic.blob.net.ServerApi;
import io.sodalic.blob.utils.StringUtils;
import io.sodalic.blob.utils.Utils;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.storage.TextFileManager;
import org.beiwe.app.networking.PostRequest;
import org.beiwe.app.CrashHandler;

import java.util.Objects;

/**
 * The main and actual implementation of the {@link BlobContext} interface
 */
public class BlobContextImpl implements BlobContext {
    private static final String TAG = Utils.getLogTag(BlobContextImpl.class);

    private final Context rootContext;

    private ServerApi serverApi;
    private final UploadHelper uploadHelper;


    public BlobContextImpl(Context context) {
        Objects.requireNonNull(context, "context");
        rootContext = context;
        uploadHelper = new UploadHelper(this);
    }

    public void init() {
        Log.i(TAG, "Context.init");

        try {
            String sentryDsn = BuildConfig.SENTRY_DSN;
            Sentry.init(sentryDsn, new AndroidSentryClientFactory(rootContext));
        } catch (InvalidDsnException ie) {
            Sentry.init(new AndroidSentryClientFactory(rootContext));
        }
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(rootContext));

        PersistentData.initialize(rootContext);
        TextFileManager.initialize(rootContext);
        PostRequest.initialize(rootContext);

        if (PersistentData.isRegistered()) {
            String serverUrl = PersistentData.getServerUrl();
            if (!StringUtils.isEmpty(serverUrl)) {
                Log.i(TAG, String.format("Restoring ServerApi for '%s'", serverUrl));
                initServerApi(serverUrl);
            } else {
                Log.e(TAG, String.format("User is registered but serverUrl is empty. UserId = '%s'", PersistentData.getPatientID()));
            }
        }
    }

    @Override
    public Context getAndroidContext() {
        return rootContext;
    }

    @Override
    public Context getAppContext() {
        return rootContext;
    }

    @Override
    public boolean isFullyInitialized() {
        return (serverApi != null);
    }

    @Override
    public ServerApi getServerApi() {
        Objects.requireNonNull(serverApi, "serverApi");
        return serverApi;
    }

    @Override
    public void initServerApi(String serverUrl) {
        Objects.requireNonNull(serverUrl, "serverUrl");
        serverApi = new ServerApi(rootContext, serverUrl);
    }

    @Override
    public UploadHelper getUploadHelper() {
        return uploadHelper;
    }
}