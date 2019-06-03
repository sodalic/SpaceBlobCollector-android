package io.sodalic.blob.utils;

import java.util.Locale;

import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * Utility class with {@link String}-related methods
 */
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * Checks string for both {@code null} and {@link String#isEmpty()}
     */
    public static boolean isEmpty(String str) {
        return (str == null) || (str.isEmpty());
    }

    /**
     * Joins {@code parts} with {@code delim} into a new string.
     * <p>
     * This method should behave the same way as Java 8 {@link String#join(CharSequence, Iterable)}
     * which is not yet available on the older supported Android devices.
     */
    public static String join(String delim, Iterable<String> parts) {

        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (String part : parts) {
            if (first) {
                first = false;
            } else {
                sb.append(delim);
            }
            sb.append(part);
        }
        return sb.toString();
    }

    /**
     * Truncates {@link String} to a given {@code maxLen}. If {@code s} is {@code null}, it is returned as is.
     */
    public static String truncate(String s, int maxLen) {
        if (isEmpty(s))
            return s;
        if (s.length() > maxLen)
            return s.substring(0, maxLen);
        else
            return s;
    }

    private static final Locale DEFAULT_FORMAT_LOCALE = Locale.US;

    /**
     * This method is a simple wrapper for {@link String#format(String, Object...)}
     * that uses a fixed pre-defined {@link Locale} for formatting.
     * <p>
     * This method is expected to be used in various places where formatted strings
     * are not supposed to be shown to user such as logging and where consistent
     * string formatting is useful.
     */
    public static String formatEn(String formatString, Object... args) {
        return String.format(DEFAULT_FORMAT_LOCALE, formatString, args);
    }

    /**
     * This method attempts to get a user-friendly application name the will match
     * what the user sees from the outside.
     *
     * @param context Any Android {@link Context}
     * @return Application name String
     */
    public static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

}
