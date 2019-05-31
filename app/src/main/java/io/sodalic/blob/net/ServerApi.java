package io.sodalic.blob.net;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import android.content.Context;
import android.util.Log;

import okhttp3.*;
import okio.Buffer;
import org.json.JSONException;
import org.json.JSONObject;

import org.beiwe.app.CrashHandler;
import org.beiwe.app.DeviceInfo;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.storage.SetDeviceSettings;
import org.beiwe.app.storage.TextFileManager;
import io.sodalic.blob.BuildConfig;
import io.sodalic.blob.utils.StringUtils;
import io.sodalic.blob.utils.Utils;


/**
 * Class that exposes the API provided by the server-side
 */
public class ServerApi {
    static final String TAG = Utils.getLogTag(ServerApi.class);

    private static final int MAX_LOG_BODY_LEN = 1000;

    // This is the media type used to describe our uploaded files
    // Let's use some fair description
    private static final MediaType MEDIA_TYPE_FILE_UPLOAD = MediaType.get("text/x.csv-encrypted");

    private final Context androidContext;
    private final String baseServerUrl;

    private final OkHttpClient client;


    public ServerApi(Context androidContext, String baseServerUrl) {
        Objects.requireNonNull(androidContext);
        Objects.requireNonNull(baseServerUrl);
        this.androidContext = androidContext;
        this.baseServerUrl = fixUrl(baseServerUrl);
        Log.i(TAG, String.format("Init ServerApi for '%s' => '%s'", baseServerUrl, this.baseServerUrl));

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // enabling logging effectively means we are sharing it with the whole world
        if (BuildConfig.APP_IS_DEV && BuildConfig.ALLOW_INSECURE_CONNECTION) {
            builder.addInterceptor(new DebugLoggingInterceptor(false, true));
//            builder.addNetworkInterceptor(new DebugLoggingInterceptor(true, true));
        }
        client = builder.build();
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

    /**
     * @param explicitPasswordHash an optional parameter to explicitly override the stored hash with some user-provided value
     */
    private static Map<String, String> buildSecurityParameters(String explicitPasswordHash) {
        // TODO SG: refactor to make this logic more clear, see also:
        //  - sendRegisterFull
        //  - sendRegisterAgain
        //  - sendSetPassword
        //  - EncryptionEngine.safeHash()
        // The actual user password is only sent to the server when the password is changed.
        // For the authentication password hash is saved and sent to the server.
        // On the server the original password is hashed 2 times:
        // 1) the same way as it is done on the client to save in the local storage
        // 2) this hashed password is treated as the real password and then it is hashed with
        //    a salt and that salted hash is stored in the DB.
        //
        // Originally the field sent for authentication was also called "password" which
        // caused some confusion. Now "password" and "password_hash" use different names.
        String passwordHash = (explicitPasswordHash != null) ? explicitPasswordHash : PersistentData.getPasswordHash();

        HashMap<String, String> params = new HashMap<>();
        params.put("patient_id", PersistentData.getPatientID());
        params.put("password_hash", passwordHash);
        params.put("device_id", DeviceInfo.getAndroidID());
        return params;
    }

    private static void addSecurityParameters(FormBody.Builder formBodyBuilder, String explicitPasswordHash) {
        Map<String, String> params = buildSecurityParameters(explicitPasswordHash);
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

    public String getBaseServerUrl() {
        return baseServerUrl;
    }

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
            savePublicKey(key);
            JSONObject deviceSettings = responseJSON.getJSONObject("device_settings");
            SetDeviceSettings.writeDeviceSettings(deviceSettings);
            String patientId = responseJSON.getString("patient_id");
            Log.i(TAG, "patient_id = '" + patientId + "'");


            // TODO SG: We need to do it here because of the re-register scenario
            // that requires the authentication data
            PersistentData.setLoginCredentials(patientId, password);
            PersistentData.setUserName(userName);

        } catch (JSONException e) {
            CrashHandler.writeCrashlog(e, androidContext);
            throw new ServerException(e);
        }
    }

    private static void savePublicKey(String key) throws ServerException {
        if (!key.startsWith("MIIBI")) {
            Log.w(TAG, "Bad key '" + key + "'");
            //TODO SG: 2 is the code originally used here. It is handled in AlertsManager
            throw new ServerException(2, "Server returned bad key");
        }
        TextFileManager.getKeyFile().deleteSafely();
        TextFileManager.getKeyFile().safeWritePlaintext(key);
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

        sendSimplePost("/register_user", formBodyBuilder.build());
    }

    /**
     * This is the request to change user's password
     *
     * @param currentPasswordHash Hash of the old password generated with {@link org.beiwe.app.storage.EncryptionEngine#safeHash(String)}.
     *                            Old password is passed to the server to check there, rather than doing just client-side check.
     * @param newPassword         New password as entered by the user
     */
    public void sendSetPassword(String currentPasswordHash, String newPassword) throws ServerException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder()
                .add("new_password", newPassword);

        addSecurityParameters(formBodyBuilder, currentPasswordHash);

        sendSimplePost("/set_password", formBodyBuilder.build());
    }

    /**
     * This request is used to download JSON with information about relevant surveys
     */
    public String downloadSurveys() throws ServerException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        addSecurityParameters(formBodyBuilder);
        return sendSimplePost("/download_surveys", formBodyBuilder.build());
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
    }

    public static final class UrlPostData {
        public final String url;
        public final byte[] postData;

        public UrlPostData(String url, byte[] postData) {
            this.url = url;
            this.postData = postData;
        }
    }

    public UrlPostData getSurveyGraphUrlAndData() {
        String url = baseServerUrl + "/graph";
//        Map<String, String> securityParameters = buildSecurityParameters(null);
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        addSecurityParameters(formBodyBuilder);
        FormBody formBody = formBodyBuilder.build();
        byte[] postData = getRequestBodyBytes(formBody);
        return new UrlPostData(url, postData);
    }

    private static byte[] getRequestBodyBytes(RequestBody body) {
        // Currently Buffer.close does nothing but who knows about the future
        try (Buffer buffer = new Buffer()) {
            body.writeTo(buffer);
            buffer.flush();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            buffer.copyTo(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Request body encoding has failed");
            throw new RuntimeException(e);
        }
    }
}
