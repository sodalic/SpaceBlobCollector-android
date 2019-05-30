package io.sodalic.blob.net;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import android.content.Context;
import android.util.Log;

import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import org.beiwe.app.CrashHandler;
import org.beiwe.app.DeviceInfo;
import org.beiwe.app.networking.PostRequest;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.storage.SetDeviceSettings;
import io.sodalic.blob.BuildConfig;
import io.sodalic.blob.utils.StringUtils;
import io.sodalic.blob.utils.Utils;


/**
 * Class that exposes the API provided by the server-side
 */
public class ServerApi {
    private static final String TAG = Utils.getLogTag(ServerApi.class);

    private static final int MAX_LOG_BODY_LEN = 1000;

    // This is the media type used to describe our uploaded files
    // Let's use some fair description
    private static final MediaType MEDIA_TYPE_FILE_UPLOAD = MediaType.get("text/x.csv-encrypted");

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

    private static String fixUrl(String serverUrl) {
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

    /**
     * This the main method that actually does the job of sending requests
     */
    private String sendSimplePost(String methodUrl, RequestBody requestBody) throws ServerException {
        final String fullUrl = baseServerUrl + methodUrl;
        Log.i(TAG, String.format("Sending request to '%s'", fullUrl));
        Request request = new Request.Builder()
                .url(fullUrl)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            final ResponseBody responseBody = response.body();
            if (!response.isSuccessful()) {
                int code = response.code();
                Log.w(TAG, String.format("Bad response code = %d", code));
                Log.w(TAG, String.format("Bad response body = '%s'", responseBody));
                if (responseBody != null)
                    Log.w(TAG, String.format("Bad response body = '%s'", responseBody.string()));
                throw new ServerException(code);
            }
            if (responseBody == null) {
                // not an expected case
                Log.w(TAG, String.format("Response code = %d, body is empty", response.code()));
                return null;
            } else {
                String responseBodyText = responseBody.string();
                String bodyForLog = StringUtils.truncate(responseBodyText, MAX_LOG_BODY_LEN);
                Log.i(TAG, "Response = '" + bodyForLog + "'");
                return responseBodyText;
            }
        } catch (IOException e) {
            throw new ServerException(e);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //// legacy authentication
    private static Map<String, String> buildSecurityParameters(String newPassword) {
        String patientId = PersistentData.getPatientID();
        String deviceId = DeviceInfo.getAndroidID();
        String password = PersistentData.getPassword();
        if (newPassword != null) password = newPassword;

        HashMap<String, String> params = new HashMap<>();
        params.put("patient_id", patientId);
        params.put("password", password);
        params.put("device_id", deviceId);
        return params;
    }

    private static void addSecurityParameters(FormBody.Builder formBodyBuilder, String newPassword) {
        Map<String, String> params = buildSecurityParameters(newPassword);
        for (Map.Entry<String, String> e : params.entrySet()) {
            formBodyBuilder.add(e.getKey(), e.getValue());
        }
    }

    private static void addSecurityParameters(FormBody.Builder formBodyBuilder) {
        addSecurityParameters(formBodyBuilder, null);
    }

    private static void addSecurityParameters(MultipartBody.Builder multipartBodyBuilder) {
        Map<String, String> params = buildSecurityParameters(null);
        for (Map.Entry<String, String> e : params.entrySet()) {
            multipartBodyBuilder.addFormDataPart(e.getKey(), e.getValue());
        }
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

    /**
     * This request is used to download JSON with information about relevant surveys
     */
    public String downloadSurveys() throws ServerException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        addSecurityParameters(formBodyBuilder);

        String responseBody = sendSimplePost("/download_surveys", formBodyBuilder.build());
        return responseBody;
    }

    /**
     * This is the method that uploads {@code file} to the server for further processing.
     * The {@code file} is expected to be one of the known files provided by {@link org.beiwe.app.storage.TextFileManager}
     *
     * @param file file to upload onto the server
     */
    public void uploadFile(File file) throws ServerException {
        Log.i(TAG, String.format("Uploading file '%s'", file.getName()));

        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        addSecurityParameters(multipartBuilder);
        multipartBuilder
                .addFormDataPart("file_name", file.getName())
                .addFormDataPart("file", file.getName(), RequestBody.create(MEDIA_TYPE_FILE_UPLOAD, file));
        sendSimplePost("/upload", multipartBuilder.build());
        //TODO SG: remove file after successful upload
    }

}
