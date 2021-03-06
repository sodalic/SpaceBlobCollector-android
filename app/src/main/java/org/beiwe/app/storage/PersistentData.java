package org.beiwe.app.storage;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import io.sodalic.blob.BuildConfig;
import org.beiwe.app.JSONUtils;
import io.sodalic.blob.R;
import org.json.JSONArray;
import org.json.JSONException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

/**A class for managing patient login sessions.
 * Uses SharedPreferences in order to save username-password combinations.
 * @author Dor Samet, Eli Jones, Josh Zagorsky */
public class PersistentData {
	public static String NULL_ID = "NULLID";
	private static final long MAX_LONG = 9223372036854775807L;

	private static int PRIVATE_MODE = 0;
	private static boolean isInitialized = false;

	// Private things that are encapsulated using functions in this class 
	private static SharedPreferences pref; 
	private static Editor editor;
	private static Context appContext;
	
	/**  Editor key-strings */
	private static final String PREF_NAME = "BeiwePref";
	private static final String SERVER_URL_KEY = "serverUrl";
	private static final String KEY_ID = "uid";
	private static final String KEY_PASSWORD_HASH = "password";
	private static final String IS_REGISTERED = "IsRegistered";
	private static final String LOGIN_EXPIRATION = "loginExpirationTimestamp";
	private static final String PCP_PHONE_KEY = "primary_care";
	private static final String PASSWORD_RESET_NUMBER_KEY = "reset_number";

	private static final String ACCELEROMETER = "accelerometer";
	private static final String GPS = "gps";
	private static final String CALLS = "calls";
	private static final String TEXTS = "texts";
	private static final String WIFI = "wifi";
	private static final String BLUETOOTH = "bluetooth";
	private static final String POWER_STATE = "power_state";
	private static final String ALLOW_UPLOAD_OVER_CELLULAR_DATA = "allow_upload_over_cellular_data";

	private static final String ACCELEROMETER_OFF_DURATION_SECONDS = "accelerometer_off_duration_seconds";
	private static final String ACCELEROMETER_ON_DURATION_SECONDS = "accelerometer_on_duration_seconds";
	private static final String BLUETOOTH_ON_DURATION_SECONDS = "bluetooth_on_duration_seconds";
	private static final String BLUETOOTH_TOTAL_DURATION_SECONDS = "bluetooth_total_duration_seconds";
	private static final String BLUETOOTH_GLOBAL_OFFSET_SECONDS = "bluetooth_global_offset_seconds";
	private static final String CHECK_FOR_NEW_SURVEYS_FREQUENCY_SECONDS = "check_for_new_surveys_frequency_seconds";
	private static final String CREATE_NEW_DATA_FILES_FREQUENCY_SECONDS = "create_new_data_files_frequency_seconds";
	private static final String GPS_OFF_DURATION_SECONDS = "gps_off_duration_seconds";
	private static final String GPS_ON_DURATION_SECONDS = "gps_on_duration_seconds";
	private static final String SECONDS_BEFORE_AUTO_LOGOUT = "seconds_before_auto_logout";
	private static final String UPLOAD_DATA_FILES_FREQUENCY_SECONDS = "upload_data_files_frequency_seconds";
	private static final String VOICE_RECORDING_MAX_TIME_LENGTH_SECONDS = "voice_recording_max_time_length_seconds";
	private static final String WIFI_LOG_FREQUENCY_SECONDS = "wifi_log_frequency_seconds";
	private static final String SURVEY_IDS = "survey_ids";
//	private static final String SURVEY_QUESTION_IDS = "question_ids";

	/*#####################################################################################
	################################### Initializing ######################################
	#####################################################################################*/

	/**The publicly accessible initializing function for the LoginManager, initializes the internal variables.
	 * @param context */
	public static void initialize( Context context ) {
		if ( isInitialized ) { return; }
		appContext = context;
		pref = appContext.getSharedPreferences(PREF_NAME, PRIVATE_MODE); //sets Shared Preferences private mode
		editor = pref.edit();
		editor.commit();
		isInitialized = true;
	} 

	/*#####################################################################################
	##################################### User State ######################################
	#####################################################################################*/

	/** Quick check for login. **/
	public static boolean isLoggedIn(){
		if (pref == null) Log.w("LoginManager", "FAILED AT ISLOGGEDIN");
		// If the current time is earlier than the expiration time, return TRUE; else FALSE
		return (System.currentTimeMillis() < pref.getLong(LOGIN_EXPIRATION, 0)); }

	/** Set the login session to expire a fixed amount of time in the future */
	public static void loginOrRefreshLogin() {
		editor.putLong(LOGIN_EXPIRATION, System.currentTimeMillis() + getMillisecondsBeforeAutoLogout());
		editor.commit(); }

	/** Set the login session to "expired" */
	public static void logout() {
		editor.putLong(LOGIN_EXPIRATION, 0);
		editor.commit(); }

	/**Getter for the IS_REGISTERED value. */
	public static boolean isRegistered() { 
		if (pref == null) Log.w("LoginManager", "FAILED AT ISREGISTERED");
		return pref.getBoolean(IS_REGISTERED, false); }

	/**Setter for the IS_REGISTERED value.
	 * @param value */
	public static void setRegistered(boolean value) { 
		editor.putBoolean(IS_REGISTERED, value);
		editor.commit(); }

	/*######################################################################################
	##################################### Passwords ########################################
	######################################################################################*/

	/**Checks that an input matches valid password requirements. (this only checks length)
	 * Throws up an alert notifying the user if the password is not valid.
	 * @param password
	 * @return true or false based on password requirements.*/
	public static boolean passwordMeetsRequirements(String password) {
		return (password.length() >= minPasswordLength());
	}

	public static int minPasswordLength() {
		if (BuildConfig.APP_IS_BETA) {
			return 1;
		} else {
			return 6;
		}
	}

 	/**Takes an input string and returns a boolean value stating whether the input matches the current password.
	 * @param input
	 * @param input
	 * @return */
	public static boolean checkPassword(String input){ return ( getPasswordHash().equals( EncryptionEngine.safeHash(input) ) ); }

	/**
	 * Hashes the password with {@link EncryptionEngine#safeHash(String)} and saves it to the local storage
	 */
	public static void savePasswordAsHash(String password){
        editor.putString(KEY_PASSWORD_HASH, EncryptionEngine.safeHash(password));
        editor.commit();
    }

	/*#####################################################################################
	################################# Listener Settings ###################################
	#####################################################################################*/

	public static boolean getAccelerometerEnabled(){ return pref.getBoolean(ACCELEROMETER, false); }
	public static boolean getGpsEnabled(){ return pref.getBoolean(GPS, false); }
	public static boolean getCallsEnabled(){ return pref.getBoolean(CALLS, false); }
	public static boolean getTextsEnabled(){ return pref.getBoolean(TEXTS, false); }
	public static boolean getWifiEnabled(){ return pref.getBoolean(WIFI, false); }
	public static boolean getBluetoothEnabled(){ return pref.getBoolean(BLUETOOTH, false); }
	public static boolean getPowerStateEnabled(){ return pref.getBoolean(POWER_STATE, false); }
	public static boolean getAllowUploadOverCellularData(){ return pref.getBoolean(ALLOW_UPLOAD_OVER_CELLULAR_DATA, false); }
	
	public static void setAccelerometerEnabled(boolean enabled) {
		editor.putBoolean(ACCELEROMETER, enabled);
		editor.commit(); }
	public static void setGpsEnabled(boolean enabled) {
		editor.putBoolean(GPS, enabled);
		editor.commit(); }
	public static void setCallsEnabled(boolean enabled) {
		editor.putBoolean(CALLS, enabled);
		editor.commit(); }
	public static void setTextsEnabled(boolean enabled) {
		editor.putBoolean(TEXTS, enabled);
		editor.commit(); }
	public static void setWifiEnabled(boolean enabled) {
		editor.putBoolean(WIFI, enabled);
		editor.commit(); }
	public static void setBluetoothEnabled(boolean enabled) {
		editor.putBoolean(BLUETOOTH, enabled);
		editor.commit(); }
	public static void setPowerStateEnabled(boolean enabled) {
		editor.putBoolean(POWER_STATE, enabled);
		editor.commit(); }
	public static void setAllowUploadOverCellularData(boolean enabled) {
		editor.putBoolean(ALLOW_UPLOAD_OVER_CELLULAR_DATA, enabled);
		editor.commit(); }
	
	/*#####################################################################################
	################################## Timer Settings #####################################
	#####################################################################################*/

	// Default timings (only used if app doesn't download custom timings)
	private static final long DEFAULT_ACCELEROMETER_OFF_MINIMUM_DURATION = 10;
	private static final long DEFAULT_ACCELEROMETER_ON_DURATION = 10 * 60;
	private static final long DEFAULT_BLUETOOTH_ON_DURATION = 1 * 60;
	private static final long DEFAULT_BLUETOOTH_TOTAL_DURATION = 5 * 60;
	private static final long DEFAULT_BLUETOOTH_GLOBAL_OFFSET = 0 * 60;
	private static final long DEFAULT_CHECK_FOR_NEW_SURVEYS_PERIOD = 24 * 60 * 60;
	private static final long DEFAULT_CREATE_NEW_DATA_FILES_PERIOD = 15 * 60;
	private static final long DEFAULT_GPS_OFF_MINIMUM_DURATION = 5 * 60;
	private static final long DEFAULT_GPS_ON_DURATION = 5 * 60;
	private static final long DEFAULT_SECONDS_BEFORE_AUTO_LOGOUT = 5 * 60;
	private static final long DEFAULT_UPLOAD_DATA_FILES_PERIOD = 60;
	private static final long DEFAULT_VOICE_RECORDING_MAX_TIME_LENGTH = 4 * 60;
	private static final long DEFAULT_WIFI_LOG_FREQUENCY = 5 * 60;
	
	public static long getAccelerometerOffDurationMilliseconds() { return 1000L * pref.getLong(ACCELEROMETER_OFF_DURATION_SECONDS, DEFAULT_ACCELEROMETER_OFF_MINIMUM_DURATION); }
	public static long getAccelerometerOnDurationMilliseconds() { return 1000L * pref.getLong(ACCELEROMETER_ON_DURATION_SECONDS, DEFAULT_ACCELEROMETER_ON_DURATION); }
	public static long getBluetoothOnDurationMilliseconds() { return 1000L * pref.getLong(BLUETOOTH_ON_DURATION_SECONDS, DEFAULT_BLUETOOTH_ON_DURATION); }
	public static long getBluetoothTotalDurationMilliseconds() { return 1000L * pref.getLong(BLUETOOTH_TOTAL_DURATION_SECONDS, DEFAULT_BLUETOOTH_TOTAL_DURATION); }
	public static long getBluetoothGlobalOffsetMilliseconds() { return 1000L * pref.getLong(BLUETOOTH_GLOBAL_OFFSET_SECONDS, DEFAULT_BLUETOOTH_GLOBAL_OFFSET); }
	public static long getCheckForNewSurveysFrequencyMilliseconds() { return 1000L * pref.getLong(CHECK_FOR_NEW_SURVEYS_FREQUENCY_SECONDS, DEFAULT_CHECK_FOR_NEW_SURVEYS_PERIOD); }
	public static long getCreateNewDataFilesFrequencyMilliseconds() { return 1000L * pref.getLong(CREATE_NEW_DATA_FILES_FREQUENCY_SECONDS, DEFAULT_CREATE_NEW_DATA_FILES_PERIOD); }
	public static long getGpsOffDurationMilliseconds() { return 1000L * pref.getLong(GPS_OFF_DURATION_SECONDS, DEFAULT_GPS_OFF_MINIMUM_DURATION); }
	public static long getGpsOnDurationMilliseconds() { return 1000L * pref.getLong(GPS_ON_DURATION_SECONDS, DEFAULT_GPS_ON_DURATION); }
	public static long getMillisecondsBeforeAutoLogout() { return 1000L * pref.getLong(SECONDS_BEFORE_AUTO_LOGOUT, DEFAULT_SECONDS_BEFORE_AUTO_LOGOUT); }
	public static long getUploadDataFilesFrequencyMilliseconds() { return 1000L * pref.getLong(UPLOAD_DATA_FILES_FREQUENCY_SECONDS, DEFAULT_UPLOAD_DATA_FILES_PERIOD); }
	public static long getVoiceRecordingMaxTimeLengthMilliseconds() { return 1000L * pref.getLong(VOICE_RECORDING_MAX_TIME_LENGTH_SECONDS, DEFAULT_VOICE_RECORDING_MAX_TIME_LENGTH); }
	public static long getWifiLogFrequencyMilliseconds() { return 1000L * pref.getLong(WIFI_LOG_FREQUENCY_SECONDS, DEFAULT_WIFI_LOG_FREQUENCY); }

	public static void setAccelerometerOffDurationSeconds(long seconds) {
		editor.putLong(ACCELEROMETER_OFF_DURATION_SECONDS, seconds);
		editor.commit(); }
	public static void setAccelerometerOnDurationSeconds(long seconds) {
		editor.putLong(ACCELEROMETER_ON_DURATION_SECONDS, seconds);
		editor.commit(); }
	public static void setBluetoothOnDurationSeconds(long seconds) {
		editor.putLong(BLUETOOTH_ON_DURATION_SECONDS, seconds);
		editor.commit(); }
	public static void setBluetoothTotalDurationSeconds(long seconds) {
		editor.putLong(BLUETOOTH_TOTAL_DURATION_SECONDS, seconds);
		editor.commit(); }
	public static void setBluetoothGlobalOffsetSeconds(long seconds) {
		editor.putLong(BLUETOOTH_GLOBAL_OFFSET_SECONDS, seconds);
		editor.commit(); }
	public static void setCheckForNewSurveysFrequencySeconds(long seconds) {
		editor.putLong(CHECK_FOR_NEW_SURVEYS_FREQUENCY_SECONDS, seconds);
		editor.commit(); }
	public static void setCreateNewDataFilesFrequencySeconds(long seconds) {
		editor.putLong(CREATE_NEW_DATA_FILES_FREQUENCY_SECONDS, seconds);
		editor.commit(); }
	public static void setGpsOffDurationSeconds(long seconds) {
		editor.putLong(GPS_OFF_DURATION_SECONDS, seconds);
		editor.commit(); }
	public static void setGpsOnDurationSeconds(long seconds) {
		editor.putLong(GPS_ON_DURATION_SECONDS, seconds);
		editor.commit(); }
	public static void setSecondsBeforeAutoLogout(long seconds) {
		editor.putLong(SECONDS_BEFORE_AUTO_LOGOUT, seconds);
		editor.commit(); }
	public static void setUploadDataFilesFrequencySeconds(long seconds) {
		editor.putLong(UPLOAD_DATA_FILES_FREQUENCY_SECONDS, seconds);
		editor.commit(); }
	public static void setVoiceRecordingMaxTimeLengthSeconds(long seconds) {
		editor.putLong(VOICE_RECORDING_MAX_TIME_LENGTH_SECONDS, seconds);
		editor.commit(); }
	public static void setWifiLogFrequencySeconds(long seconds) {
		editor.putLong(WIFI_LOG_FREQUENCY_SECONDS, seconds);
		editor.commit(); }

	
	//accelerometer, bluetooth, new surveys, create data files, gps, logout,upload, wifilog (not voice recording, that doesn't apply
	public static void setMostRecentAlarmTime(String identifier, long time) {
		editor.putLong(identifier + "-prior_alarm", time);
		editor.commit(); }
	public static long getMostRecentAlarmTime(String identifier) { return pref.getLong( identifier + "-prior_alarm", 0); }
	//we want default to be 0 so that checks "is this value less than the current expected value" (eg "did this timer event pass already")
	
	/*###########################################################################################
	################################### Text Strings ############################################
	###########################################################################################*/

	private static final String ABOUT_PAGE_TEXT_KEY = "about_page_text";
	private static final String CALL_CLINICIAN_BUTTON_TEXT_KEY = "call_clinician_button_text";
	private static final String CONSENT_FORM_TEXT_KEY = "consent_form_text";
	private static final String SURVEY_SUBMIT_SUCCESS_TOAST_TEXT_KEY = "survey_submit_success_toast_text";
	
	public static String getAboutPageText() {
		String defaultText = appContext.getString(R.string.default_about_page_text);
		return pref.getString(ABOUT_PAGE_TEXT_KEY, defaultText); }
	public static String getCallClinicianButtonText() {
		String defaultText = appContext.getString(R.string.default_call_clinician_text);
		return pref.getString(CALL_CLINICIAN_BUTTON_TEXT_KEY, defaultText); }
	public static String getConsentFormText() {
		String defaultText = appContext.getString(R.string.default_consent_form_text);
		return pref.getString(CONSENT_FORM_TEXT_KEY, defaultText); }
	public static String getSurveySubmitSuccessToastText() {
		String defaultText = appContext.getString(R.string.default_survey_submit_success_message);
		return pref.getString(SURVEY_SUBMIT_SUCCESS_TOAST_TEXT_KEY, defaultText); }
	
	public static void setAboutPageText(String text) {
		editor.putString(ABOUT_PAGE_TEXT_KEY, text);
		editor.commit(); }
	public static void setCallClinicianButtonText(String text) {
		editor.putString(CALL_CLINICIAN_BUTTON_TEXT_KEY, text);
		editor.commit(); }
	public static void setConsentFormText(String text) {
		editor.putString(CONSENT_FORM_TEXT_KEY, text);
		editor.commit(); }
	public static void setSurveySubmitSuccessToastText(String text) {
		editor.putString(SURVEY_SUBMIT_SUCCESS_TOAST_TEXT_KEY, text);
		editor.commit(); }

	/*###########################################################################################
	################################### User Credentials ########################################
	###########################################################################################*/

	public static void setServerUrl(String serverUrl) {
		if (editor == null) Log.e("LoginManager.java", "editor is null in setServerUrl()");
		editor.putString(SERVER_URL_KEY, serverUrl);
		editor.commit(); }

	public static String getServerUrl() { return pref.getString(SERVER_URL_KEY, null); }

	public static void setLoginCredentials( String userID, String password ) {
		if (editor == null) Log.e("LoginManager.java", "editor is null in setLoginCredentials()");
		editor.putString(KEY_ID, userID);
        savePasswordAsHash(password);
		editor.commit(); }

	public static String getPasswordHash() { return pref.getString(KEY_PASSWORD_HASH, null ); }
	public static String getPatientID() { return pref.getString(KEY_ID, NULL_ID); }


	/*###########################################################################################
	#################################### Contact Numbers ########################################
	###########################################################################################*/

	public static String getPrimaryCareNumber() { return pref.getString(PCP_PHONE_KEY, ""); }
	public static void setPrimaryCareNumber( String phoneNumber) {
		editor.putString(PCP_PHONE_KEY, phoneNumber );
		editor.commit(); }

	public static String getPasswordResetNumber() { return pref.getString(PASSWORD_RESET_NUMBER_KEY, ""); }
	public static void setPasswordResetNumber( String phoneNumber ){
		editor.putString(PASSWORD_RESET_NUMBER_KEY, phoneNumber );
		editor.commit(); }

	/*###########################################################################################
	###################################### Survey Info ##########################################
	###########################################################################################*/
	
	public static List<String> getSurveyIds() { return JSONUtils.jsonArrayToStringList(getSurveyIdsJsonArray()); }
	public static List<String> getSurveyQuestionMemory(String surveyId) { return JSONUtils.jsonArrayToStringList(getSurveyQuestionMemoryJsonArray(surveyId)); }
	public static String getSurveyTimes(String surveyId){ return pref.getString(surveyId + "-times", null); }
	public static String getSurveyContent(String surveyId){ return pref.getString(surveyId + "-content", null); }
	public static String getSurveyType(String surveyId){ return pref.getString(surveyId + "-type", null); }
	public static String getSurveySettings(String surveyId){ return pref.getString(surveyId + "-settings", null); }
	public static Boolean getSurveyNotificationState( String surveyId) { return pref.getBoolean(surveyId + "-notificationState", false ); }
	public static long getMostRecentSurveyAlarmTime(String surveyId) { return pref.getLong( surveyId + "-prior_alarm", MAX_LONG); }
	
	public static void createSurveyData(String surveyId, String content, String timings, String type, String settings){
		setSurveyContent(surveyId,  content);
		setSurveyTimes(surveyId, timings);
		setSurveyType(surveyId, type);
		setSurveySettings(surveyId, settings);
	}
	//individual setters
	public static void setSurveyContent(String surveyId, String content){
		editor.putString(surveyId + "-content", content);
		editor.commit(); }
	public static void setSurveyTimes(String surveyId, String times){
		editor.putString(surveyId + "-times", times);
		editor.commit(); }
	public static void setSurveyType(String surveyId, String type){
		editor.putString(surveyId + "-type", type);
		editor.commit(); }
	public static void setSurveySettings(String surveyId, String settings){
//		Log.d("presistent data", "setting survey settings: " + settings);
		editor.putString(surveyId + "-settings", settings);
		editor.commit();
	}
	
	//survey state storage
	public static void setSurveyNotificationState(String surveyId, Boolean bool ) {
		editor.putBoolean(surveyId + "-notificationState", bool );
		editor.commit(); }
	public static void setMostRecentSurveyAlarmTime(String surveyId, long time) {
		editor.putLong(surveyId + "-prior_alarm", time);
		editor.commit(); }
	
	
	public static void deleteSurvey(String surveyId) {
		editor.remove(surveyId + "-content");
		editor.remove(surveyId + "-times");
		editor.remove(surveyId + "-type");
		editor.remove(surveyId + "-notificationState");
		editor.remove(surveyId + "-settings");
		editor.remove(surveyId + "-questionIds");
		editor.commit();
		removeSurveyId(surveyId);
	}
	
	//array style storage and removal for surveyIds and questionIds	
	private static JSONArray getSurveyIdsJsonArray() {
		String jsonString = pref.getString(SURVEY_IDS, "0");
		// Log.d("persistant data", "getting ids: " + jsonString);
		if (jsonString == "0") { return new JSONArray(); } //return empty if the list is empty
		try { return new JSONArray(jsonString); }
		catch (JSONException e) { throw new NullPointerException("getSurveyIds failed, json string was: " + jsonString ); }
	}
		
	public static void addSurveyId(String surveyId) {
		List<String> list = JSONUtils.jsonArrayToStringList( getSurveyIdsJsonArray() );
		if ( !list.contains(surveyId) ) {
			list.add(surveyId);
			editor.putString(SURVEY_IDS, new JSONArray(list).toString() );
			editor.commit();
		}
		else { throw new NullPointerException("duplicate survey id added: " + surveyId); } //we ensure uniqueness in the downloader, this should be unreachable.
	}
	
	private static void removeSurveyId(String surveyId) {
		List<String> list = JSONUtils.jsonArrayToStringList( getSurveyIdsJsonArray() );
		if ( list.contains(surveyId) ) {
			list.remove(surveyId);
			editor.putString(SURVEY_IDS, new JSONArray(list).toString() );
			editor.commit();
		}
		else { throw new NullPointerException("survey id does not exist: " + surveyId); } //we ensure uniqueness in the downloader, this should be unreachable.
	}
	

	private static JSONArray getSurveyQuestionMemoryJsonArray( String surveyId ) {
		String jsonString = pref.getString(surveyId + "-questionIds", "0");
		if (jsonString == "0") { return new JSONArray(); } //return empty if the list is empty
		try { return new JSONArray(jsonString); }
		catch (JSONException e) { throw new NullPointerException("getSurveyIds failed, json string was: " + jsonString ); }
	}
		
	public static void addSurveyQuestionMemory(String surveyId, String questionId) {
		List<String> list = getSurveyQuestionMemory(surveyId);
		// Log.d("persistent data", "adding questionId: " + questionId);
		if ( !list.contains(questionId) ) {
			list.add(questionId);
			editor.putString(surveyId + "-questionIds", new JSONArray(list).toString() );
			editor.commit();
		}
		else { throw new NullPointerException("duplicate question id added: " + questionId); } //we ensure uniqueness in the downloader, this should be unreachable.
	}
	
	public static void clearSurveyQuestionMemory(String surveyId) {
		editor.putString(surveyId + "-questionIds", new JSONArray().toString() );
		editor.commit();
	}

	/*###########################################################################################
	###################################### Encryption ###########################################
	###########################################################################################*/

	private static final String HASH_SALT_KEY = "hash_salt_key";
	private static final String HASH_ITERATIONS_KEY = "hash_iterations_key";
	private static final String USE_ANONYMIZED_HASHING_KEY = "use_anonymized_hashing";

	// Get salt for pbkdf2 hashing
	public static byte[] getHashSalt() {
		String saltString = pref.getString(HASH_SALT_KEY, null);
		if(saltString == null) { // create salt if it does not exist
			byte[] newSalt = SecureRandom.getSeed(64);
			editor.putString(HASH_SALT_KEY, new String(newSalt));
			editor.commit();
			return newSalt;
		}
		else {
			return saltString.getBytes();
		}
	}

	// Get iterations for pbkdf2 hashing
	public static int getHashIterations() {
		int iterations = pref.getInt(HASH_ITERATIONS_KEY, 0);
		if(iterations == 0) { // create iterations if it does not exist
			// create random iteration count from 900 to 1100
			int newIterations = 1100 - new Random().nextInt(200);
			editor.putInt(HASH_ITERATIONS_KEY, newIterations);
			editor.commit();
			return newIterations;
		}
		else {
			return iterations;
		}
	}

	public static void setUseAnonymizedHashing(boolean useAnonymizedHashing) {
		editor.putBoolean(USE_ANONYMIZED_HASHING_KEY, useAnonymizedHashing);
		editor.commit();
	}
	public static boolean getUseAnonymizedHashing() {
		return pref.getBoolean(USE_ANONYMIZED_HASHING_KEY, true); //If not present, default to safe hashing
	}

	/*###########################################################################################
	###################################### FUZZY GPS ############################################
	###########################################################################################*/

	private static final String USE_GPS_FUZZING_KEY = "gps_fuzzing_key";
	private static final String LATITUDE_OFFSET_KEY = "latitude_offset_key";
	private static final String LONGITUDE_OFFSET_KEY = "longitude_offset_key";

	public static double getLatitudeOffset() {
		float latitudeOffset = pref.getFloat(LATITUDE_OFFSET_KEY, 0.0f);
		if(latitudeOffset == 0.0f && getUseGpsFuzzing()) { //create latitude offset if it does not exist
			float newLatitudeOffset = (float)(.2 + Math.random()*1.6); // create random latitude offset between (-1, -.2) or (.2, 1)
			if(newLatitudeOffset > 1) {
				newLatitudeOffset = (newLatitudeOffset-.8f) * -1;
			}
			editor.putFloat(LATITUDE_OFFSET_KEY, newLatitudeOffset);
			editor.commit();
			return newLatitudeOffset;
		}
		else {
			return latitudeOffset;
		}
	}

	public static float getLongitudeOffset() {
		float longitudeOffset = pref.getFloat(LONGITUDE_OFFSET_KEY, 0.0f);
		if(longitudeOffset == 0.0f && getUseGpsFuzzing()) { //create longitude offset if it does not exist
			float newLongitudeOffset = (float)(10 + Math.random()*340); // create random longitude offset between (-180, -10) or (10, 180)
			if(newLongitudeOffset > 180) {
				newLongitudeOffset = (newLongitudeOffset-170) * -1;
			}
			editor.putFloat(LONGITUDE_OFFSET_KEY, newLongitudeOffset);
			editor.commit();
			return newLongitudeOffset;
		}
		else {
			return longitudeOffset;
		}
	}

	public static void setUseGpsFuzzing(boolean useFuzzyGps) {
		editor.putBoolean(USE_GPS_FUZZING_KEY, useFuzzyGps);
		editor.commit();
	}
	private static boolean getUseGpsFuzzing() {
		return pref.getBoolean(USE_GPS_FUZZING_KEY, false);
	}

	/*###########################################################################################
	###################################### Call Buttons #########################################
	###########################################################################################*/

	private static final String CALL_CLINICIAN_BUTTON_ENABLED_KEY = "call_clinician_button_enabled";
	private static final String CALL_RESEARCH_ASSISTANT_BUTTON_ENABLED_KEY = "call_research_assistant_button_enabled";

	public static boolean getCallClinicianButtonEnabled() {
		return pref.getBoolean(CALL_CLINICIAN_BUTTON_ENABLED_KEY, false);
	}

	public static void setCallClinicianButtonEnabled(boolean enabled) {
		editor.putBoolean(CALL_CLINICIAN_BUTTON_ENABLED_KEY, enabled);
	}

	public static boolean getCallResearchAssistantButtonEnabled() {
		return pref.getBoolean(CALL_RESEARCH_ASSISTANT_BUTTON_ENABLED_KEY, false);
	}

	public static void setCallResearchAssistantButtonEnabled(boolean enabled) {
		editor.putBoolean(CALL_RESEARCH_ASSISTANT_BUTTON_ENABLED_KEY, enabled);
	}
}