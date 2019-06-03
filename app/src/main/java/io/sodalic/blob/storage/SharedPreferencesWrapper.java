package io.sodalic.blob.storage;

import java.util.Objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import io.sodalic.blob.utils.StringUtils;

/**
 * A helper class to simplify work with {@link android.content.SharedPreferences}
 */
public final class SharedPreferencesWrapper {
    private final String TAG;
    private final SharedPreferences preferences;

    public SharedPreferencesWrapper(@NonNull SharedPreferences preferences, @NonNull String tag) {
        Objects.requireNonNull(preferences, "preferences");
        this.preferences = preferences;
        TAG = "SPW-" + tag;
    }

    @NonNull
    public static SharedPreferencesWrapper createPrivate(@NonNull Context context, @NonNull String name) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(name, "name");
        SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        return new SharedPreferencesWrapper(prefs, name);
    }

    /**
     * Use it in case you want to modify several values at the same time
     *
     * @return {@link SharedPreferences.Editor} for the wrapped preferences
     */
    public SharedPreferences.Editor getEditor() {
        return preferences.edit();
    }


    public String getString(@NonNull String key, @Nullable String defValue) {
        return preferences.getString(key, defValue);
    }

    public void putString(@NonNull String key, @Nullable String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        if (!editor.commit()) {
            Log.e(TAG, StringUtils.formatEn("Failed to save key '%s', value = '%s'", key, value));
            throw new RuntimeException(String.format("Failed to save key '%s'", key));
        }
    }

    public int getInt(@NonNull String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    public void putInt(@NonNull String key, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        if (!editor.commit()) {
            Log.e(TAG, StringUtils.formatEn("Failed to save key '%s', value = '%d'", key, value));
            throw new RuntimeException(String.format("Failed to save key '%s'", key));
        }
    }

    public long getLong(@NonNull String key, long defValue) {
        return preferences.getLong(key, defValue);
    }

    public void putLong(@NonNull String key, long value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value);
        if (!editor.commit()) {
            Log.e(TAG, StringUtils.formatEn("Failed to save key '%s', value = '%d'", key, value));
            throw new RuntimeException(String.format("Failed to save key '%s'", key));
        }
    }

    public boolean getBoolean(@NonNull String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    public void putBoolean(@NonNull String key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        if (!editor.commit()) {
            Log.e(TAG, StringUtils.formatEn("Failed to save key '%s', value = '%s'", key, Boolean.toString(value)));
            throw new RuntimeException(String.format("Failed to save key '%s'", key));
        }
    }

    public float getFloat(@NonNull String key, float defValue) {
        return preferences.getFloat(key, defValue);
    }

    public void putFloat(@NonNull String key, float value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(key, value);
        if (!editor.commit()) {
            Log.e(TAG, StringUtils.formatEn("Failed to save key '%s', value = '%f'", key, value));
            throw new RuntimeException(String.format("Failed to save key '%s'", key));
        }
    }
}
