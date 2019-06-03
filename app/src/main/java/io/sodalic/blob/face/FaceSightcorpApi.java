package io.sodalic.blob.face;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import android.support.annotation.NonNull;
import android.util.Log;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.sodalic.blob.BuildConfig;
import io.sodalic.blob.net.DebugLoggingInterceptor;
import io.sodalic.blob.net.ServerApi;
import io.sodalic.blob.storage.UserMood;
import io.sodalic.blob.utils.StringUtils;
import io.sodalic.blob.utils.Utils;

/**
 * This class provides an access to the face-api.sightcorp.com API.
 */
public class FaceSightcorpApi {
    private static final String TAG = Utils.getLogTag(FaceSightcorpApi.class);

    private static final String API_URL = "https://api-face.sightcorp.com/api/detect/";
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.get("image/jpeg");

    private final OkHttpClient okHttpClient;
    private final String appKey;

    public FaceSightcorpApi(@NonNull String apiKey) {
        Objects.requireNonNull(apiKey);
        this.appKey = apiKey;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // enabling logging effectively means we are sharing it with the whole world
        if (BuildConfig.APP_IS_DEV && BuildConfig.ALLOW_INSECURE_CONNECTION) {
            builder.addInterceptor(new DebugLoggingInterceptor(TAG, false, true));
//            builder.addNetworkInterceptor(new DebugLoggingInterceptor(true, true));
        }
        // extend timeouts as default ones seem to be not enough for pictures
        builder.writeTimeout(60, TimeUnit.SECONDS);
        builder.readTimeout(60, TimeUnit.SECONDS);
        okHttpClient = builder.build();
    }

    /**
     * This is an object representation of the main data returned by the API
     */
    public static class PersonFaceData {
        public final long moment;
        public final int age;
        public final int gender; //-100 to 100
        public final int mood; // 0 to 100

        // all 0 to 100
        public final int happiness;
        public final int surprise;
        public final int anger;
        public final int disgust;
        public final int fear;
        public final int sadness;

        PersonFaceData(JSONObject personResponse) throws JSONException {
            this.moment = System.currentTimeMillis();
            this.age = personResponse.getInt("age");
            this.gender = personResponse.getInt("gender");
            this.mood = personResponse.getInt("mood");

            JSONObject emotions = personResponse.getJSONObject("emotions");
            happiness = emotions.getInt("happiness");
            surprise = emotions.getInt("surprise");
            anger = emotions.getInt("anger");
            disgust = emotions.getInt("disgust");
            fear = emotions.getInt("fear");
            sadness = emotions.getInt("sadness");
        }

        @Override
        public String toString() {
            return "PersonFaceData{" +
                    "age=" + age +
                    ", gender=" + gender +
                    ", mood=" + mood +
                    ", happiness=" + happiness +
                    ", surprise=" + surprise +
                    ", anger=" + anger +
                    ", disgust=" + disgust +
                    ", fear=" + fear +
                    ", sadness=" + sadness +
                    ", moment =" + new Date(moment).toString() +
                    '}';
        }

        @NonNull
        public UserMood toUserMood() {
            // this is what we use now, all the rest is ignored
            return new UserMood(moment, mood, happiness, surprise, anger, fear, sadness);
        }
    }

    @NonNull
    public PersonFaceData analyzeImage(@NonNull File imageFile) throws FaceApiException {
        Log.i(TAG, StringUtils.formatEn("Sending image '%s' of size %d for a face analysis", imageFile, imageFile.length()));

        String jsonResponseStr = sendApiRequest(imageFile);
        PersonFaceData faceData = parseApiResponse(jsonResponseStr);
        Log.i(TAG, "Face API data = " + faceData);
        return faceData;

    }

    @NonNull
    private String sendApiRequest(@NonNull File imageFile) throws FaceApiException {
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("app_key", appKey)
                .addFormDataPart("img", "image.jpeg", RequestBody.create(MEDIA_TYPE_JPEG, imageFile))
                .build();

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            final ResponseBody responseBody = response.body();
            final String responseBodyText = (responseBody != null) ? responseBody.string() : null;
            if (!response.isSuccessful()) {
                int code = response.code();
                Log.w(TAG, StringUtils.formatEn("Bad response code = %d", code));
                Log.w(TAG, StringUtils.formatEn("Bad response body = '%s'", responseBody));
                if (responseBody != null)
                    Log.w(TAG, StringUtils.formatEn("Bad response body = '%s'", responseBody.string()));
                //TODO SG: user-friendly text
                throw new FaceApiException(String.format("FaceApi returned a bad response code = '%d'", code));
            }
            if (responseBody == null) {
                // not an expected case
                Log.w(TAG, StringUtils.formatEn("Response code = %d, body is empty", response.code()));
                //TODO SG: user-friendly text
                throw new FaceApiException("FaceApi returned an empty response");
            } else {
                String bodyForLog = StringUtils.truncate(responseBodyText, ServerApi.MAX_LOG_BODY_LEN);
                Log.i(TAG, "Response = '" + bodyForLog + "'");
                return responseBodyText;
            }
        } catch (IOException e) {
            throw new FaceApiException(e);
        }
    }


    @NonNull
    private PersonFaceData parseApiResponse(@NonNull String jsonResponseStr) throws FaceApiException {
        try {
            JSONObject responseRoot = new JSONObject(jsonResponseStr);
            int errorCode = responseRoot.getInt("error_code");
            String description = responseRoot.getString("description");
            if (errorCode != 0) {
                Log.w(TAG, StringUtils.formatEn("Face api Response code = %d, desc = '%s'", errorCode, description));
                //TODO SG: user-friendly text
                throw new FaceApiException(String.format("Face API returned a bad error code %d", errorCode));
            }
            JSONArray people = responseRoot.getJSONArray("people");
            Log.i(TAG, "People length = " + people.length());
            if (people.length() == 0) {
                //TODO SG: user-friendly text
                throw new FaceApiException("No people found on the image");
            }
            JSONObject firstPerson = people.getJSONObject(0);
            Log.i(TAG, "First person: " + firstPerson.toString());
            return new PersonFaceData(firstPerson);

        } catch (JSONException e) {
            throw new FaceApiException(e);
        }
    }
}
