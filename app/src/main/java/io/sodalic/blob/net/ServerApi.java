package io.sodalic.blob.net;

import android.content.Context;
import android.util.Log;
import io.sodalic.blob.utils.StringUtils;
import okhttp3.*;
import io.sodalic.blob.BuildConfig;
import io.sodalic.blob.utils.Utils;

import org.beiwe.app.CrashHandler;
import org.beiwe.app.DeviceInfo;
import org.beiwe.app.networking.PostRequest;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.storage.SetDeviceSettings;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;


/**
 * Class that exposes the API provided by the server-side
 */
public class ServerApi {
    private static final String TAG = Utils.getLogTag(ServerApi.class);

    private static final int MAX_LOG_BODY_LEN = 1000;

    private final OkHttpClient client = new OkHttpClient();
    private final Context androidContext;
    private final String baseServerUrl;

    public ServerApi(Context androidContext, String baseServerUrl) {
        Objects.requireNonNull(androidContext);
        Objects.requireNonNull(baseServerUrl);
        this.androidContext = androidContext;
        this.baseServerUrl = fixUrl(baseServerUrl);
        Log.i(TAG, String.format("Init ServerApi for '%s' => '%s'", baseServerUrl, this.baseServerUrl));
    }

    static String fixUrl(String serverUrl) {
        if (serverUrl.startsWith("https://")) {
            return serverUrl;
        } else if (serverUrl.startsWith("http://")) {
            if (BuildConfig.ALLOW_INSECURE_CONNECTION)
                return serverUrl;
            else
                return "https://" + serverUrl.substring(7);
        } else {
            return "https://" + serverUrl;
        }

    }

    private String sendSimplePost(String methodUrl, RequestBody requestBody) throws ServerException {
        final String fullUrl = baseServerUrl + methodUrl;
        Log.i(TAG, String.format("Sending request to '%s'", fullUrl));
        Request request = new Request.Builder()
                .url(fullUrl)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                Log.w(TAG, String.format("Bad response code = %d", code));
                Log.w(TAG, String.format("Bad response body = '%s'", response.body()));
                throw new ServerException(code);
            }

//            Headers responseHeaders = response.headers();
//            for (int i = 0; i < responseHeaders.size(); i++) {
//                System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
//            }

            String responseBody = response.body().string();
            String bodyForLog = StringUtils.truncate(responseBody, MAX_LOG_BODY_LEN);
            Log.i(TAG, "Response = '" + bodyForLog + "'");
            return responseBody;
        } catch (IOException e) {
            throw new ServerException(e);
        }
    }


    private static void addSecurityParameters(FormBody.Builder formBodyBuilder, String newPassword) {
        String patientId = PersistentData.getPatientID();
        String deviceId = DeviceInfo.getAndroidID();
        String password = PersistentData.getPassword();
        if (newPassword != null) password = newPassword;

        formBodyBuilder.add("patient_id", patientId);
        formBodyBuilder.add("password", password);
        formBodyBuilder.add("device_id", deviceId);
    }

    private static void addSecurityParameters(FormBody.Builder formBodyBuilder) {
        addSecurityParameters(formBodyBuilder, null);
    }

    //////////////////////////////////////////////////
    //// public API

    private static void addDeviceInfoParams(FormBody.Builder formBodyBuilder) {
        // The logic of hashing various parameters is affected by PersistentData.getUseAnonymizedHashing()
        formBodyBuilder.add("device_os", "Android");
        formBodyBuilder.add("product", DeviceInfo.getProduct());
        formBodyBuilder.add("app_version", DeviceInfo.getAppVersion());

        //TODO SG: don't collect it on the serer-side at all
        //formBodyBuilder.add("phone_number", getPhoneNumber(activity));
        formBodyBuilder.add("phone_number", "null");

        formBodyBuilder.add("bluetooth_id", DeviceInfo.getBluetoothMAC());
        formBodyBuilder.add("device_id", DeviceInfo.getAndroidID());
        formBodyBuilder.add("os_version", DeviceInfo.getAndroidVersion());
        formBodyBuilder.add("hardware_id", DeviceInfo.getHardwareId());
        formBodyBuilder.add("brand", DeviceInfo.getBrand());
        formBodyBuilder.add("manufacturer", DeviceInfo.getManufacturer());
        formBodyBuilder.add("model", DeviceInfo.getModel());
    }

    /**
     * Request to do full client-based user registration
     */
    public void sendRegisterFull(String userName, String password, String studyId) throws ServerException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder()
                .add("userName", userName)
                .add("password", password)
                .add("studyId", studyId);

        addDeviceInfoParams(formBodyBuilder);

        String responseBody = sendSimplePost("/register_user_full", formBodyBuilder.build());
        try {
            JSONObject responseJSON = new JSONObject(responseBody);
            String key = responseJSON.getString("client_public_key");
            int code = PostRequest.writeKey(key, 0);
            if (code != 0)
                throw new ServerException(code, "Server returned bad key");
            JSONObject deviceSettings = responseJSON.getJSONObject("device_settings");
            SetDeviceSettings.writeDeviceSettings(deviceSettings);
            String patient_id = responseJSON.getString("patient_id");
            Log.i(TAG, "patient_id = '" + patient_id + "'");
            PersistentData.setLoginCredentials(patient_id, password);
        } catch (JSONException e) {
            CrashHandler.writeCrashlog(e, androidContext);
            throw new ServerException(e);
        }
    }


    /**
     * This request is used to update register information if we are using non anonymized hashing
     */
    public void sendRegisterAgain(String password) throws ServerException {
        // Yes, currently 'register_user' expects also "new_password"
        FormBody.Builder formBodyBuilder = new FormBody.Builder()
                .add("new_password", password);

        addSecurityParameters(formBodyBuilder);
        addDeviceInfoParams(formBodyBuilder);

        String responseBody = sendSimplePost("/register_user", formBodyBuilder.build());
    }

    public String downloadSurveys() throws ServerException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        addSecurityParameters(formBodyBuilder);

        String responseBody = sendSimplePost("/download_surveys", formBodyBuilder.build());

        return responseBody;
    }

}
