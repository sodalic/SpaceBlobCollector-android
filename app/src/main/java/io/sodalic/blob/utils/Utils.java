package io.sodalic.blob.utils;

import android.util.Log;

import java.util.ArrayList;

/**
 * A class with some generic utils methods
 */
public final class Utils {
    private static final String TAG = getLogTag(Utils.class);


    private Utils() {
        throw new RuntimeException("This class is static-only");
    }

    private static String shortenClassName(String className) {
        return className
                .replace("Activity", "Act")
                .replace("Fragment", "Frag")
                .replace("Manager", "Man")
                .replace("Listener", "Lis");
    }


    public static String getLogTag(Class clazz) {
        String simpleName = clazz.getName()
                .replace("io.sodalic.blob.", "isb.")
                .replace("org.beiwe.app.", "oba.");
        String[] parts = simpleName.split("[\\.]");
        ArrayList<String> shortParts = new ArrayList<String>();
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            String shortPart = (part.length() > 3) ? part.substring(0, 2) : part;
            shortParts.add(shortPart);
        }
        // add full class name
        shortParts.add(shortenClassName(parts[parts.length - 1]));

        return StringUtils.join(".", shortParts);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted sleep", e);
        }
    }
}
