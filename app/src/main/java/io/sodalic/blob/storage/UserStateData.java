package io.sodalic.blob.storage;

import java.util.Objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import io.sodalic.blob.utils.Utils;

/**
 * Class that provides access to a part of the user state
 * stored locally
 */
public class UserStateData {
    private static final String TAG = Utils.getLogTag(UserStateData.class);

    private static final String PREFERENCES_NAME = "SpaceBlob-UserData";

    private final SharedPreferencesWrapper prefs;

    public UserStateData(@NonNull Context context) {
        Objects.requireNonNull(context, "context");
        this.prefs = SharedPreferencesWrapper.createPrivate(context, PREFERENCES_NAME);
    }

    private static final String USER_NAME_KEY = "userName";

    public String getUserName() {
        return prefs.getString(USER_NAME_KEY, null);
    }

    public void setUserName(@NonNull String userName) {
        prefs.putString(USER_NAME_KEY, userName);
    }

    private static final UserMood DEFAULT_MOOD = new UserMood(0L, 50, 50, 0, 0, 0, 0);
    private static final String MOOD_MOMENT_KEY = "mood_moment";
    private static final String MOOD_MOOD_KEY = "mood";
    private static final String MOOD_HAPPINESS_KEY = "happiness";
    private static final String MOOD_SURPRISE_KEY = "surprise";
    private static final String MOOD_ANGER_KEY = "anger";
    private static final String MOOD_FEAR_KEY = "fear";
    private static final String MOOD_SADNESS_KEY = "sadness";

    @NonNull
    public UserMood getLastMood() {
        long moment = prefs.getLong(MOOD_MOMENT_KEY, -1);
        if (moment == -1) {
            Log.i(TAG, "Returning the default mood");
            return DEFAULT_MOOD;
        }

        int mood = prefs.getInt(MOOD_MOOD_KEY, -1);
        int happiness = prefs.getInt(MOOD_HAPPINESS_KEY, -1);
        int surprise = prefs.getInt(MOOD_SURPRISE_KEY, -1);
        int anger = prefs.getInt(MOOD_ANGER_KEY, -1);
        int fear = prefs.getInt(MOOD_FEAR_KEY, -1);
        int sadness = prefs.getInt(MOOD_SADNESS_KEY, -1);
        return new UserMood(moment, mood, happiness, surprise, anger, fear, sadness);
    }

    public void setLastMood(@NonNull UserMood userMood) {
        Log.i(TAG, "Saving mood " + userMood);
        Objects.requireNonNull(userMood, "userMood");
        SharedPreferences.Editor massEditor = prefs.getEditor();

        massEditor.putLong(MOOD_MOMENT_KEY, userMood.moment);
        massEditor.putInt(MOOD_MOOD_KEY, userMood.mood);
        massEditor.putInt(MOOD_HAPPINESS_KEY, userMood.happiness);
        massEditor.putInt(MOOD_SURPRISE_KEY, userMood.surprise);
        massEditor.putInt(MOOD_ANGER_KEY, userMood.anger);
        massEditor.putInt(MOOD_FEAR_KEY, userMood.fear);
        massEditor.putInt(MOOD_SADNESS_KEY, userMood.sadness);

        massEditor.commit();
    }
}
