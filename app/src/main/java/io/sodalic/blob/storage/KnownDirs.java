package io.sodalic.blob.storage;

import java.io.File;

import android.Manifest;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import io.sodalic.blob.BuildConfig;
import io.sodalic.blob.utils.ImageUtils;
import io.sodalic.blob.utils.StringUtils;
import io.sodalic.blob.utils.Utils;

import static android.os.Environment.DIRECTORY_PICTURES;

/**
 * This is a helper class that contains a list of known files and folders
 * used to store various data.
 */
public final class KnownDirs {
    private static final String TAG = Utils.getLogTag(ImageUtils.class);

    private KnownDirs() {
        throw new RuntimeException("This is a static class");
    }

    private static void ensureDirectoryExists(@NonNull File dir, @NonNull String dirDescription) {
        if (!dir.exists() && !dir.mkdirs()) {
            // should never happen in the real life
            Log.e(TAG, StringUtils.formatEn("Failed to create the %s directory '%s'", dirDescription, dir));
            throw new RuntimeException(StringUtils.formatEn("Failed to create the %s directory '%s'", dirDescription, dir));
        }
    }

    /**
     * @param context      - any Android {@link Context}
     * @param ensureExists - if {@code true}, then the directory will also be created. Note that it means that
     *                     it expects that {@link Manifest.permission#WRITE_EXTERNAL_STORAGE} permission
     *                     has already been granted.
     * @return Returns a {@link File} pointing to a directory to put temporary images such as selfie images saved
     * for further processing.
     * Note that in the {@link BuildConfig#APP_IS_DEV} this is a publicly available folder
     * and otherwise it is a usual application-data folder
     */
    @NonNull
    public static File getTempImageDir(@NonNull Context context, boolean ensureExists) {
        File dir;
        if (BuildConfig.APP_IS_DEV) {
            File baseDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES);
            dir = new File(baseDir, StringUtils.getApplicationName(context));
        } else {
            File baseDir = context.getExternalFilesDir(null);
            dir = new File(baseDir, "face");
        }
        if (ensureExists)
            ensureDirectoryExists(dir, "images");
        return dir;
    }


    /**
     * The same as {@link #getTempImageDir(Context, boolean)} with {@code ensureExists = true}.
     * It means this call expects that {@link Manifest.permission#WRITE_EXTERNAL_STORAGE} permission
     * has already been granted.
     */
    public static File getTempImageDir(@NonNull Context context) {
        return getTempImageDir(context, true);
    }

    public static File getCrashLogDir(@NonNull Context context, boolean ensureExists) {
        File baseDir = context.getExternalFilesDir(null);
        File dir = new File(baseDir, "crashLog");
        if (ensureExists)
            ensureDirectoryExists(dir, "crash logs");
        return dir;
    }

    public static File getTrackingFilesDir(@NonNull Context context, boolean ensureExists) {
        // TODO SG: move all the writing to use getTrackingFilesDir
        // instead of just appContext.openFileOutput
        /*
        File baseDir = context.getFilesDir();
        File dir = new File(baseDir, "tracking");
        if (ensureExists)
            ensureDirectoryExists(dir, "tracking");
        return dir;
        //*/
        return context.getFilesDir();
    }
}
