package org.beiwe.app;

import java.io.*;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;

import org.beiwe.app.storage.PersistentData;
import io.sodalic.blob.BuildConfig;
import io.sodalic.blob.context.BlobContextProxy;
import io.sodalic.blob.storage.KnownDirs;
import io.sodalic.blob.utils.StringUtils;
import io.sodalic.blob.utils.Utils;

public class CrashHandler implements java.lang.Thread.UncaughtExceptionHandler {
    private static final String TAG = Utils.getLogTag(CrashHandler.class);

    private final Context errorHandlerContext;
    private int millisecondsUntilRestart = 500;

    public CrashHandler(Context context) {
        errorHandlerContext = context;
    }

    /**
     * This function is where any errors that occur in any Activity that inherits RunningBackgroundServiceActivity
     * will dump its errors.  We roll it up, stick it in a file, and try to restart the app after exiting it.
     * (using a new alarm like we do in the BackgroundService).
     */
    public void uncaughtException(Thread thread, Throwable exception) {

        Log.w("CrashHandler Raw", "start original stacktrace");
        exception.printStackTrace();
        Log.w("CrashHandler Raw", "end original stacktrace");

        //Write that log file
        Sentry.getContext().recordBreadcrumb(
                new BreadcrumbBuilder().setMessage("Attempting application restart").build()
        );
        // to track strange bug early in the app cycle, log all uncaughtExceptions locally as well
        logCrashLocally(exception, errorHandlerContext);
        writeCrashlog(exception, errorHandlerContext);

        String toastMsg = exception.getMessage();
        Toast.makeText(errorHandlerContext, toastMsg, Toast.LENGTH_LONG).show();

//		Log.i("inside crashlog", "does this line happen");  //keep this line for debugging crashes in the crash handler (yup.)
        //setup to restart service
        Intent restartServiceIntent = new Intent(errorHandlerContext, BackgroundService.class);
        restartServiceIntent.setPackage(errorHandlerContext.getPackageName());
        PendingIntent restartServicePendingIntent = PendingIntent.getService(errorHandlerContext, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) errorHandlerContext.getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + millisecondsUntilRestart, restartServicePendingIntent);

        saveLogCat(errorHandlerContext);

        //exit beiwe
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    private static File getLogCatFile(Context context, boolean createDir) {
        return new File(KnownDirs.getCrashLogDir(context, createDir), "logcat.txt");
    }

    public static File getLogCatFile(Context context) {
        return getLogCatFile(context, false);
    }

    private static void saveLogCat(Context context) {
        File outputFile = getLogCatFile(context, true);
        Log.i(TAG, StringUtils.formatEn("Saving logcat to '%s'", outputFile));

        try {
            Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            // ignore
            Log.w(TAG, "Failed to save logcat", e);
        }
    }

    public static void deleteCrashLog(Context context) {
        File logCatFile = getLogCatFile(context, false);
        Log.i(TAG, StringUtils.formatEn("Deleting logcat '%s', exists = %s", logCatFile, Boolean.toString(logCatFile.exists())));
        if (logCatFile.exists()) {
            if (!logCatFile.delete()) {
                Log.w(TAG, StringUtils.formatEn("Failed to delete logcat '%s'", logCatFile));
            }
        }
    }

    public static void trySendCrashLogEmail(Activity activity) {
        Log.i(TAG, "Try to send log e-mail");
        File logCatFile = getLogCatFile(activity);
        //send file using email
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // Set type to "email"
        emailIntent.setType("vnd.android.cursor.dir/email");
        String to[] = {BuildConfig.CRASH_LOG_EMAIL};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        // the attachment
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logCatFile));
        // the mail subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Sodalic Crash Log");
        emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(Intent.createChooser(emailIntent, "Send crash log email..."));
    }

    /**
     * Creates a crash log file that will be uploaded at the next upload event.
     * Also writes error to the error log so that it is visible in logcat.
     *
     * @param exception A Throwable (probably your error).
     * @param context   An android Context
     */
    public static void writeCrashlog(Throwable exception, Context context) {
        BlobContextProxy contextProxy = new BlobContextProxy(context);
        String serverUrl = contextProxy.isFullyInitialized() ? contextProxy.getServerApi().getBaseServerUrl() : PersistentData.getServerUrl();

        try {
            Sentry.getContext().addTag("user_id", PersistentData.getPatientID());
            Sentry.getContext().addTag("server_url", serverUrl);
            Sentry.capture(exception);
        } catch (Exception e1) {
            logCrashLocally(e1, context);
        }
    }

    private static void logCrashLocally(Throwable exception, Context context) {
        String exceptionInfo = System.currentTimeMillis() + "\n"
                + "Version:" + DeviceInfo.getAppVersion()
                + ", AndroidVersion:" + DeviceInfo.getAndroidVersion()
                + ", Product:" + DeviceInfo.getProduct()
                + ", Brand:" + DeviceInfo.getBrand()
                + ", HardwareId:" + DeviceInfo.getHardwareId()
                + ", Manufacturer:" + DeviceInfo.getManufacturer()
                + ", Model:" + DeviceInfo.getModel() + "\n\n";

        StringWriter buf = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(buf)) {
            exception.printStackTrace(printWriter);
        }
        exceptionInfo += buf.toString();


        //Print an error log if debug mode is active.
        if (BuildConfig.APP_IS_BETA) {
            Log.e(TAG, "Crash error details:\n" + exceptionInfo); //Log error...
        }

        FileOutputStream outStream; //write a file...
        try {
            outStream = context.openFileOutput("crashlog_" + System.currentTimeMillis(), Context.MODE_APPEND);
            outStream.write((exceptionInfo).getBytes());
            outStream.flush();
            outStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not write to file, file DNE.", e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "Could not write to file, IOException.", e);
            e.printStackTrace();
        }
    }
}