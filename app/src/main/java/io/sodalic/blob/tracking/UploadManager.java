package io.sodalic.blob.tracking;

import java.io.File;
import java.util.Objects;

import android.content.Context;
import android.util.Log;

import org.beiwe.app.CrashHandler;
import org.beiwe.app.networking.NetworkUtility;
import org.beiwe.app.storage.TextFileManager;
import io.sodalic.blob.context.BlobContext;
import io.sodalic.blob.net.ServerApi;
import io.sodalic.blob.net.ServerException;
import io.sodalic.blob.storage.KnownDirs;
import io.sodalic.blob.utils.StringUtils;
import io.sodalic.blob.utils.Utils;

/**
 * This is the class that is responsible for uploading the captured data onto the server
 */
public class UploadManager {

    private static final String TAG = Utils.getLogTag(UploadManager.class);

    private final Object FILE_UPLOAD_LOCK = new Object(); //Our lock for file uploading
    private final BlobContext blobContext;

    public UploadManager(BlobContext blobContext) {
        Objects.requireNonNull(blobContext);
        this.blobContext = blobContext;
    }

    /**
     * Uploads all available files on a separate thread.
     */
    public void uploadAllFiles() {
        // determine if you are allowed to upload over WiFi or cellular data, return if not.
        if (!NetworkUtility.canUpload(blobContext.getAppContext())) {
            return;
        }

        Log.i(TAG, "Files upload was requested");
        if (!blobContext.isFullyInitialized()) {
            Log.w(TAG, "Trying to upload while context is not initialized yet", new RuntimeException("FakeForStackTrace"));
            return;
        }
        // Run the HTTP POST on a separate thread
        Thread uploaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                doUploadAllFiles();
            }
        }, "uploader_thread");
        uploaderThread.start();
    }

    /**
     * Uploads all files to the server.
     * Files get deleted as soon as a 200 OK code in received from the server.
     */
    private void doUploadAllFiles() {
        final Context appContext = blobContext.getAppContext();
        final ServerApi serverApi = blobContext.getServerApi();
        final File baseDir = KnownDirs.getTrackingFilesDir(appContext, false);

        final long startTime = System.currentTimeMillis();
        final long stopTime = startTime + 1000 * 60 * 60; //One hour to upload files

        synchronized (FILE_UPLOAD_LOCK) {
            // get files under the lock!
            final String[] files = TextFileManager.getAllUploadableFiles();
            Log.i(TAG, "uploading " + files.length + " files");

            for (int i = 0; i < files.length; i++) {
                String fileName = files[i];
                if (((i + 1) % 5 == 0) && (files.length > 10)) {
                    Log.i(TAG, StringUtils.formatEn("Uploading %d of %d", i + 1, files.length));
                }

                if (!NetworkUtility.canUpload(appContext)) {
                    Log.i(TAG, "Stop uploading because of no WiFi");
                    return;
                }

                File file = new File(baseDir + "/" + fileName);
                if (!file.exists()) {
                    Log.e(TAG, StringUtils.formatEn("Non-existent file '%s', full path '%s'", fileName, file));
                    continue;
                }
                try {
                    serverApi.uploadFile(file);
                    // delete file only if there is no error in serverApi.uploadFile
                    TextFileManager.delete(fileName);
                } catch (ServerException e) {
                    Log.w(TAG, "Failed to upload file '" + fileName + "'.", e);
                }

                if (stopTime < System.currentTimeMillis()) {
                    Log.w(TAG, "shutting down upload due to time limit, we should never reach this.");
                    TextFileManager.getDebugLogFile().writeEncrypted(
                            StringUtils.formatEn("%d upload time limit of 1 hr since %d is reached %d of %d files, there are likely files still on the phone that have not been uploaded.",
                                    System.currentTimeMillis(), startTime, i, files.length));
                    CrashHandler.writeCrashlog(new RuntimeException(StringUtils.formatEn("Upload took longer than 1 hour for %d of %d files", i, files.length)), appContext);
                    return;
                }
            }
            Log.i(TAG, StringUtils.formatEn("DONE WITH UPLOAD of %d files", files.length));
        }
    }


}
