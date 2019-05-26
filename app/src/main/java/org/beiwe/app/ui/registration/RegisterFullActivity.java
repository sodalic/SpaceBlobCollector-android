package org.beiwe.app.ui.registration;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.*;
import org.beiwe.app.*;
import org.beiwe.app.networking.HTTPUIAsync;
import org.beiwe.app.networking.PostRequest;
import org.beiwe.app.storage.EncryptionEngine;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.survey.TextFieldKeyboard;
import org.beiwe.app.ui.utils.AlertsManager;

import static org.beiwe.app.networking.PostRequest.addWebsitePrefix;


/**
 * This is the activity to perform the registration of the user in the default study.
 *
 * @author SergGr
 */

@SuppressLint("ShowToast")
public class RegisterFullActivity extends RunningBackgroundServiceActivity {
    private EditText userNameInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    private LinearLayout advancedFieldsContainer;
    private EditText serverUrlInput;
    private EditText studyIdUrlInput;

    private final static int PERMISSION_CALLBACK = 0; //This callback value can be anything, we are not really using it
    private final static int REQUEST_PERMISSIONS_IDENTIFIER = 1500;

    /**
     * Users will go into this activity first to register information on the phone and on the server.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_full);


        userNameInput = findViewById(R.id.registerUserNameInput);
        passwordInput = findViewById(R.id.registerNewPasswordInput);
        confirmPasswordInput = findViewById(R.id.registerConfirmNewPasswordInput);

        CheckBox showAdvancedCheckbox = findViewById(R.id.registerActivityShowAdvancedFields);
        advancedFieldsContainer = findViewById(R.id.registerActivityAdvancedFieldsContainer);
        serverUrlInput = findViewById(R.id.serverUrlInput);
        studyIdUrlInput = findViewById(R.id.studyIdInput);

        TextFieldKeyboard textFieldKeyboard = new TextFieldKeyboard(getApplicationContext());
        textFieldKeyboard.makeKeyboardBehave(userNameInput);
        textFieldKeyboard.makeKeyboardBehave(passwordInput);
        textFieldKeyboard.makeKeyboardBehave(confirmPasswordInput);
        textFieldKeyboard.makeKeyboardBehave(serverUrlInput);
        textFieldKeyboard.makeKeyboardBehave(studyIdUrlInput);


        if (!BuildConfig.CUSTOMIZABLE_SERVER_URL) {
            TextView serverUrlCaption = findViewById(R.id.serverUrlCaption);
            serverUrlCaption.setVisibility(View.GONE);
            serverUrlInput.setVisibility(View.GONE);
        } else {
            serverUrlInput.setText(BuildConfig.DEFAULT_SERVER_URL);
        }
        // TODO: get rid altogether?
        // For now this is the default study on the dev server
        studyIdUrlInput.setText(BuildConfig.DEFAULT_STUDY_ID);

        passwordInput.setHint(String.format(getString(R.string.registration_replacement_password_hint), PersistentData.minPasswordLength()));
        confirmPasswordInput.setHint(String.format(getString(R.string.registration_replacement_password_hint), PersistentData.minPasswordLength()));

        updateAdvancedFieldsVisibility(false);
        showAdvancedCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateAdvancedFieldsVisibility(buttonView.isChecked());
            }
        });
    }

    private void updateAdvancedFieldsVisibility(boolean show) {
        advancedFieldsContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    /**
     * Registration sequence begins here, called when the submit button is pressed.
     *
     * @param view
     */
    public synchronized void registerButtonPressed(View view) {
        String serverUrl = serverUrlInput.getText().toString();
        String userName = userNameInput.getText().toString();
        String password = passwordInput.getText().toString();
        String confirmNewPassword = confirmPasswordInput.getText().toString();
        String studyId = studyIdUrlInput.getText().toString();

        if (studyId.length() == 0) {
            AlertsManager.showAlert(getString(R.string.invalid_study_id), getString(R.string.couldnt_register), this);
            return;
        } else if ((serverUrl.length() == 0) && (BuildConfig.CUSTOMIZABLE_SERVER_URL)) {
            // If the study URL is empty, alert the user
            AlertsManager.showAlert(getString(R.string.url_too_short), getString(R.string.couldnt_register), this);
            return;
        } else if (userName.length() == 0) {
            // If the user id length is too short, alert the user
            AlertsManager.showAlert(getString(R.string.invalid_user_id), getString(R.string.couldnt_register), this);
            return;
        } else if (!PersistentData.passwordMeetsRequirements(password)) {
            // If the new password has too few characters
            String alertMessage = String.format(getString(R.string.password_too_short), PersistentData.minPasswordLength());
            AlertsManager.showAlert(alertMessage, getString(R.string.couldnt_register), this);
            return;
        } else if (!password.equals(confirmNewPassword)) {
            // If the new password doesn't match the confirm new password
            AlertsManager.showAlert(getString(R.string.password_mismatch), getString(R.string.couldnt_register), this);
            return;
        } else {
            if (BuildConfig.CUSTOMIZABLE_SERVER_URL) {
                PersistentData.setServerUrl(serverUrl);
            }
            tryToRegisterWithTheServer(this,
                    addWebsitePrefix(getApplicationContext().getString(R.string.register_full_url)),
                    addWebsitePrefix(getApplicationContext().getString(R.string.register_url)),
                    userName,
                    password,
                    studyId);
        }
    }


    /**
     * Implements the server request logic for user, device registration.
     *
     * @param registerFullUrl the URL for device registration
     */
    static private void tryToRegisterWithTheServer(final Activity currentActivity, final String registerFullUrl, final String reRegisterUrl,
                                                   final String userName, final String password, final String studyId) {
        new HTTPUIAsync(registerFullUrl, currentActivity) {

            @Override
            protected Void doInBackground(Void... arg0) {
                DeviceInfo.initialize(currentActivity.getApplicationContext());
                // Always use anonymized hashing when first registering the phone.
                parameters = PostRequest.makeParameter("userName", userName) + PostRequest.makeParameter("password", password) + PostRequest.makeParameter("studyId", studyId)
                        + buildDeviceInfoParams(currentActivity);
                responseCode = PostRequest.httpRegisterFull(parameters, url, password);

                // If we are not using anonymized hashing, resubmit the phone identifying information
                if (responseCode == 200 && !PersistentData.getUseAnonymizedHashing()) { // This short circuits so if the initial register fails, it won't try here
                    try {
                        //Sleep for one second so the backend does not receive information with overlapping timestamps
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // this is a re-regestering call with a different signature so "new_password"
                    parameters = PostRequest.makeParameter("new_password", password) + buildDeviceInfoParams(currentActivity);
                    int resp = PostRequest.httpRegisterAgain(parameters, reRegisterUrl);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void arg) {
                super.onPostExecute(arg);
                if (responseCode == 200) {
                    PersistentData.setUserName(userName);
                    PersistentData.setPassword(password); //TODO: now it is a partial duplicate of the PostRequest.doRegisterRequestEx logic

                    //TODO: does PhoneNumberEntryActivity still make sense in our app?
//                    if (PersistentData.getCallClinicianButtonEnabled() || PersistentData.getCallResearchAssistantButtonEnabled()) {
//                        activity.startActivity(new Intent(activity.getApplicationContext(), PhoneNumberEntryActivity.class));
//                    } else {
//                        activity.startActivity(new Intent(activity.getApplicationContext(), ConsentFormActivity.class));
//                    }
                    activity.startActivity(new Intent(activity.getApplicationContext(), ConsentFormActivity.class));
                    activity.finish();
                } else {
                    AlertsManager.showAlert(responseCode, currentActivity.getString(R.string.couldnt_register), currentActivity);
                }
            }
        };
    }


    static String buildDeviceInfoParams(Activity activity) {
        // The logic of hashing various parameters is affected by PersistentData.getUseAnonymizedHashing()
        return PostRequest.makeParameter("bluetooth_id", DeviceInfo.getBluetoothMAC()) +
                PostRequest.makeParameter("phone_number", getPhoneNumber(activity)) +
                PostRequest.makeParameter("device_id", DeviceInfo.getAndroidID()) +
                PostRequest.makeParameter("device_os", "Android") +
                PostRequest.makeParameter("os_version", DeviceInfo.getAndroidVersion()) +
                PostRequest.makeParameter("hardware_id", DeviceInfo.getHardwareId()) +
                PostRequest.makeParameter("brand", DeviceInfo.getBrand()) +
                PostRequest.makeParameter("manufacturer", DeviceInfo.getManufacturer()) +
                PostRequest.makeParameter("model", DeviceInfo.getModel()) +
                PostRequest.makeParameter("product", DeviceInfo.getProduct()) +
                PostRequest.makeParameter("app_version", DeviceInfo.getBeiweVersion());
    }

    /**
     * This is the function that requires SMS permissions.  We need to supply a (unique) identifier for phone numbers to the registration arguments.
     *
     * @return
     */
    private static String getPhoneNumber(Activity activity) {
        TelephonyManager phoneManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber;
        try {
            // If READ_TEXT_AND_CALL_LOGS is true, we should not be able to get here without having
            // asked for the SMS permission.  If it's false, we don't have permission to do this.
            phoneNumber = phoneManager.getLine1Number();
        } catch (SecurityException e) {
            phoneNumber = "";
        }
        if (phoneNumber == null) {
            return EncryptionEngine.hashPhoneNumber("");
        }
        return EncryptionEngine.hashPhoneNumber(phoneNumber);
    }
	
	
	/*####################################################################
	###################### Permission Prompting ##########################
	####################################################################*/

    private static Boolean prePromptActive = false;
    private static Boolean postPromptActive = false;
    private static Boolean thisResumeCausedByFalseActivityReturn = false;
    private static Boolean aboutToResetFalseActivityReturn = false;
    private static Boolean activityNotVisible = false;

    private void goToSettings() {
        // Log.i("reg", "goToSettings");
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(myAppSettings, REQUEST_PERMISSIONS_IDENTIFIER);
    }


    @Override
    protected void onResume() {
        // Log.i("reg", "onResume");
        super.onResume();
        activityNotVisible = false;

        // This used to be in an else block, its idempotent and we appear to have been having problems with it not having been run.
        DeviceInfo.initialize(getApplicationContext());

        if (aboutToResetFalseActivityReturn) {
            aboutToResetFalseActivityReturn = false;
            thisResumeCausedByFalseActivityReturn = false;
            return;
        }
        if (BuildConfig.READ_TEXT_AND_CALL_LOGS &&
                !PermissionHandler.checkAccessReadSms(getApplicationContext()) &&
                !thisResumeCausedByFalseActivityReturn) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)) {
                if (!prePromptActive && !postPromptActive) {
                    showPostPermissionAlert(this);
                }
            } else if (!prePromptActive && !postPromptActive) {
                showPrePermissionAlert(this);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityNotVisible = true;
    }

    ;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Log.i("reg", "onActivityResult. requestCode: " + requestCode + ", resultCode: " + resultCode );
        aboutToResetFalseActivityReturn = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Log.i("reg", "onRequestPermissionResult");
        if (activityNotVisible) return; //this is identical logical progression to the way it works in SessionActivity.
        for (int i = 0; i < grantResults.length; i++) {
            if (permissions[i].equals(Manifest.permission.READ_SMS)) {
//				Log.i("permiss", "permission return: " + permissions[i]);
                if (grantResults[i] == PermissionHandler.PERMISSION_GRANTED) {
                    break;
                }
                if (shouldShowRequestPermissionRationale(permissions[i])) {
                    showPostPermissionAlert(this);
                } //(shouldShow... "This method returns true if the app has requested this permission previously and the user denied the request.")
            }
//			else { Log.w("permiss", "permission return: " + permissions[i]); }
        }
    }

    /* Message Popping */

    public static void showPrePermissionAlert(final Activity activity) {
        // Log.i("reg", "showPreAlert");
        if (prePromptActive) {
            return;
        }
        prePromptActive = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Permissions Requirement:");
        builder.setMessage(R.string.permission_registration_read_sms_alert);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                activity.requestPermissions(new String[]{Manifest.permission.READ_SMS}, PERMISSION_CALLBACK);
                prePromptActive = false;
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
            }
        }); //Okay button
        builder.create().show();
    }

    public static void showPostPermissionAlert(final RegisterFullActivity activity) {
        // Log.i("reg", "showPostAlert");
        if (postPromptActive) {
            return;
        }
        postPromptActive = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Permissions Requirement:");
        builder.setMessage(R.string.permission_registration_actually_need_sms_alert);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                thisResumeCausedByFalseActivityReturn = true;
                activity.goToSettings();
                postPromptActive = false;
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
            }
        }); //Okay button
        builder.create().show();
    }
}
