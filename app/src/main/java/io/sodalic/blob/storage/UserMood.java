package io.sodalic.blob.storage;

import java.util.Date;

/**
 * Created by SergGr on 03.06.2019.
 */
public class UserMood {
    public final long moment;

    // all 0 to 100
    public final int mood;
    public final int happiness;
    public final int surprise;
    public final int anger;
    public final int fear;
    public final int sadness;

    public UserMood(long moment, int mood, int happiness, int surprise, int anger, int fear, int sadness) {
        this.moment = moment;
        this.mood = mood;
        this.happiness = happiness;
        this.surprise = surprise;
        this.anger = anger;
        this.fear = fear;
        this.sadness = sadness;
    }

    @Override
    public String toString() {
        return "UserMood{" +
                "moment=" + new Date(moment).toString() +
                ", mood=" + mood +
                ", happiness=" + happiness +
                ", surprise=" + surprise +
                ", anger=" + anger +
                ", fear=" + fear +
                ", sadness=" + sadness +
                '}';
    }
}
