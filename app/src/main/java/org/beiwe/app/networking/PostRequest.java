package org.beiwe.app.networking;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import org.beiwe.app.CrashHandler;
import org.beiwe.app.DeviceInfo;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.storage.SetDeviceSettings;
import org.beiwe.app.storage.TextFileManager;
import io.sodalic.blob.BuildConfig;
import io.sodalic.blob.R;

/** PostRequest is our class for handling all HTTP operations we need; they are all in the form of HTTP post requests. 
 * All HTTP connections are HTTPS, and automatically include a password and identifying information. 
 * @author Josh, Eli, Dor */

//TODO: Low priority. Eli. clean this up and update docs. It does not adequately state that it puts into any request automatic security parameters, and it is not obvious why some of the functions exist (minimal http thing)
public class PostRequest {
	private static Context appContext;

	/**Uploads must be initialized with an appContext before they can access the wifi state or upload a _file_. */
	private PostRequest( Context applicationContext ) { appContext = applicationContext; }

	/** Simply runs the constructor, using the applcationContext to grab variables.  Idempotent. */
	public static void initialize(Context applicationContext) { new PostRequest(applicationContext); }


	/*##################################################################################
	 ##################### Publicly Accessible Functions ###############################
	 #################################################################################*/




	/**For use with Async tasks.
	 * Makes an HTTP post request with the provided URL and parameters, returns the server's response code from that request
	 * @param parameters HTTP parameters
	 * @return an int of the server's response code from the HTTP request */
	public static int httpRequestcode( String parameters, String url, String newPassword ) {
		try {
			return doPostRequestGetResponseCode( parameters, new URL(url), newPassword ); }
		catch (MalformedURLException e) {
			Log.e("PosteRequestFileUpload", "malformed URL");
			e.printStackTrace(); 
			return 0; }
		catch (IOException e) {
			Log.e("PostRequest","Unable to establish network connection");
			return 502; }
	}


	/*##################################################################################
	 ################################ Common Code ######################################
	 #################################################################################*/

	/**Creates an HTTP connection with minimal settings.  Some network funcitonality
	 * requires this minimal object.
	 * @param url a URL object
	 * @return a new HttpURLConnection with minimal settings applied
	 * @throws IOException This function can throw 2 kinds of IO exceptions: IOExeptions and ProtocolException*/
	private static HttpURLConnection minimalHTTP(URL url) throws IOException {
		// Create a new HttpURLConnection and set its parameters
		// SG: allow non-secure HTTP connections
		// see also PersistentData.setServerUrl
		Log.i("PostRequest", "Connecting to '" + url + "'");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setUseCaches(false);
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Connection", "Keep-Alive");
		connection.setRequestProperty("Cache-Control", "no-cache");
		connection.setConnectTimeout(3000);
		connection.setReadTimeout(5000);
		return connection;
	}


	/**For use with functionality that requires additional parameters be added to an HTTP operation.
	 * @param parameters a string that has been created using the makeParameters function
	 * @param url a URL object
	 * @return a new HttpURLConnection with common settings */
	private static HttpURLConnection setupHTTP( String parameters, URL url, String newPassword ) throws IOException {
		HttpURLConnection connection = minimalHTTP(url);

		DataOutputStream request = new DataOutputStream( connection.getOutputStream() );
		request.write( securityParameters(newPassword).getBytes() );
		request.write( parameters.getBytes() );
		request.flush();
		request.close();

		return connection;
	}

	/**Reads in the response data from an HttpURLConnection, returns it as a String.
	 * @param connection an HttpURLConnection
	 * @return a String containing return data
	 * @throws IOException */
	private static String readResponse(HttpURLConnection connection) throws IOException {
		Integer responseCode = connection.getResponseCode();
		if (responseCode == 200) {
			BufferedReader reader = new BufferedReader(new InputStreamReader( new DataInputStream( connection.getInputStream() ) ) );
			String line;
			StringBuilder response = new StringBuilder();
			while ( (line = reader.readLine() ) != null) { response.append(line); }
			return response.toString();
		}
		return responseCode.toString();
	}


	/*##################################################################################
	 ####################### Actual Post Request Functions #############################
	 #################################################################################*/
	
	private static String doPostRequestGetResponseString(String parameters, String urlString) throws IOException {
		HttpURLConnection connection = setupHTTP( parameters, new URL( urlString ), null );
		connection.connect();
		String data = readResponse(connection);
		connection.disconnect();
		return data;
	}


	private static int doPostRequestGetResponseCode(String parameters, URL url, String newPassword) throws IOException {
		HttpURLConnection connection = setupHTTP(parameters, url, newPassword);
		int response = connection.getResponseCode();
		connection.disconnect();
		return response;
	}


	private static int doRegisterRequest(String parameters, URL url) throws IOException {
	    return doRegisterRequestEx(parameters,url,null);
    }

    private static int doRegisterRequestEx(String parameters, URL url, String password) throws IOException {
		HttpURLConnection connection = setupHTTP(parameters, url, null);
		int response = connection.getResponseCode();
		if ( response == 200 ) {
			String responseBody = readResponse(connection);
			try {
				JSONObject responseJSON = new JSONObject(responseBody);
				String key = responseJSON.getString("client_public_key");
				writeKey(key, response);
				JSONObject deviceSettings = responseJSON.getJSONObject("device_settings");
				SetDeviceSettings.writeDeviceSettings(deviceSettings);
                Log.i("RegisterFull", "Response = '"+responseBody + "'");
                if (password != null) {
                    String patient_id = responseJSON.getString("patient_id");
                    PersistentData.setLoginCredentials(patient_id, password);
                    Log.i("RegisterFull", "patient_id = '"+patient_id + "'");
                }
            } catch (JSONException e) {
                CrashHandler.writeCrashlog(e, appContext);
            }
		}
		connection.disconnect();
		return response;
	}

	// Simple registration that does not parse response data.
	// This is used for resubmitting non anonymized identifier data during registration
	private static int doRegisterRequestSimple(String parameters, URL url) throws IOException {
		HttpURLConnection connection = setupHTTP(parameters, url, null);
		int response = connection.getResponseCode();
		String responseBody = readResponse(connection);
		connection.disconnect();
		return response;
	}
	
	public static int writeKey(String key, int httpResponse) {
		if ( !key.startsWith("MIIBI") ) {
			Log.e("PostRequest - register", " Received an invalid encryption key from server: " + key );
			return 2; }
		// Log.d( "PostRequest", "Received a key: " + key );
		TextFileManager.getKeyFile().deleteSafely();
		TextFileManager.getKeyFile().safeWritePlaintext( key );
		return httpResponse;
	}
	


	//#######################################################################################
	//############################### UTILITY FUNCTIONS #####################################
	//#######################################################################################

	public static String makeParameter(String key, String value) { return key + "=" + value + "&"; }

	/** Create the 3 standard security parameters for POST request authentication.
	 *  @param newPassword If this is a Forgot Password request, pass in a newPassword string from
	 *  a text input field instead of from the device storage.
	 *  @return a String of the securityParameters to append to the POST request */
	public static String securityParameters(String newPassword) {
		String patientId = PersistentData.getPatientID();
		String deviceId = DeviceInfo.getAndroidID();
		String password = PersistentData.getPassword();
		if (newPassword != null) password = newPassword;

		return makeParameter("patient_id", patientId) +
				makeParameter("password", password) +
				makeParameter("device_id", deviceId);
	}

	public static String addWebsitePrefix(String URL){
		String serverUrl = PersistentData.getServerUrl();
		if ((BuildConfig.CUSTOMIZABLE_SERVER_URL) && (serverUrl != null)) {
			return serverUrl + URL;
		} else {
			// If serverUrl == null, this should be an old version of the app that didn't let the
			// user specify the URL during registration, so assume the URL is either
			// studies.beiwe.org or staging.beiwe.org.
			if (BuildConfig.APP_IS_BETA) return appContext.getResources().getString(R.string.staging_website) + URL;
			else return appContext.getResources().getString(R.string.production_website) + URL;
		}
	}

}