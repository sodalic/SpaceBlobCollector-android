package io.sodalic.blob.utils;

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
}
