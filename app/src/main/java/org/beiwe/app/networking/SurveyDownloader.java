package org.beiwe.app.networking;

import android.content.Context;
import android.util.Log;
import io.sodalic.blob.context.BlobContext;
import io.sodalic.blob.sharedui.HttpBgAsync;
import io.sodalic.blob.utils.Utils;
import org.beiwe.app.BackgroundService;
import org.beiwe.app.CrashHandler;
import org.beiwe.app.JSONUtils;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.survey.SurveyScheduler;
import org.beiwe.app.ui.utils.SurveyNotifications;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SurveyDownloader {
    private final static String TAG = Utils.getLogTag(SurveyDownloader.class);

    public static void downloadSurveys(BlobContext blobContext) {
        doDownload(blobContext);
    }

    private static void doDownload(BlobContext blobContext) {
        new HttpBgAsync(TAG, blobContext) {
            @Override
            protected void doTaskImpl(BlobContext blobContext, Context appContext) throws Exception {
                String jsonResponseString = blobContext.getServerApi().downloadSurveys();
                int res = updateSurveys(appContext, jsonResponseString);
                Log.i(TAG, "Update survey res = " + res);
            }
        }.execute();
    }

    //Returns an appropriate return code for the httpAsync error parsing.  -1 if something goes wrong, 200 if it works.
    private static int updateSurveys(Context appContext, String jsonString) {
        if (jsonString == null) {
            Log.e("Survey Downloader", "jsonString is null, probably have no network connection. squashing.");
            return -1;
        }

        List<String> surveys;
        try {
            surveys = JSONUtils.jsonArrayToStringList(new JSONArray(jsonString));
        } catch (JSONException e) {
//			CrashHandler.writeCrashlog(e, appContext); // this crash report has causes problems.
            Log.e("Survey Downloader", "JSON PARSING FAIL FAIL FAIL");
            return -1;
        }

        JSONObject surveyJSON;
        List<String> oldSurveyIds = PersistentData.getSurveyIds();
        ArrayList<String> newSurveyIds = new ArrayList<String>();
        String surveyId;
        String surveyType;
        String jsonQuestionsString;
        String jsonTimingsString;
        String jsonSettingsString;

        for (String surveyString : surveys) {
            try {
                surveyJSON = new JSONObject(surveyString);
            } catch (JSONException e) {
                CrashHandler.writeCrashlog(e, appContext);
                Log.e("Survey Downloader", "JSON fail 1");
                return -1;
            }
//			Log.d("debugging survey update", "whole thing: " + surveyJSON.toString());

            try {
                surveyId = surveyJSON.getString("_id");
            } catch (JSONException e) {
                CrashHandler.writeCrashlog(e, appContext);
                Log.e("Survey Downloader", "JSON fail 2");
                return -1;
            }
//			Log.d("debugging survey update", "id: " + surveyId.toString());

            try {
                surveyType = surveyJSON.getString("survey_type");
            } catch (JSONException e) {
                CrashHandler.writeCrashlog(e, appContext);
                Log.e("Survey Downloader", "JSON fail 2.5");
                return -1;
            }
//			Log.d("debugging survey update", "type: " + surveyType.toString());

            try {
                jsonQuestionsString = surveyJSON.getString("content");
            } catch (JSONException e) {
                CrashHandler.writeCrashlog(e, appContext);
                Log.e("Survey Downloader", "JSON fail 3");
                return -1;
            }
//			Log.d("debugging survey update", "questions: " + jsonQuestionsString);

            try {
                jsonTimingsString = surveyJSON.getString("timings");
            } catch (JSONException e) {
                CrashHandler.writeCrashlog(e, appContext);
                Log.e("Survey Downloader", "JSON fail 4");
                return -1;
            }
//			Log.d("debugging survey update", "timings: " + jsonTimingsString);

            try {
                jsonSettingsString = surveyJSON.getString("settings");
            } catch (JSONException e) {
                CrashHandler.writeCrashlog(e, appContext);
                Log.e("Survey Downloader", "JSON settings not present");
                return -1;
            }
//			Log.d("debugging survey update", "settings: " + jsonSettingsString);

            if (oldSurveyIds.contains(surveyId)) { //if surveyId already exists, check for changes, add to list of new survey ids.
                // Log.d("debugging survey update", "checking for changes");
                PersistentData.setSurveyContent(surveyId, jsonQuestionsString);
                PersistentData.setSurveyType(surveyId, surveyType);
                PersistentData.setSurveySettings(surveyId, jsonSettingsString);
//				Log.d("debugging survey update", "A is incoming, B is current.");
//				Log.d("debugging survey update", "A) " + jsonTimingsString);
//				Log.d("debugging survey update", "B) " + PersistentData.getSurveyTimes(surveyId) );
                if (!PersistentData.getSurveyTimes(surveyId).equals(jsonTimingsString)) {
//					Log.i("SurveyDownloader.java", "The survey times, they are a changin!");
                    BackgroundService.cancelSurveyAlarm(surveyId);
                    PersistentData.setSurveyTimes(surveyId, jsonTimingsString);
                    SurveyScheduler.scheduleSurvey(surveyId);
                }
                newSurveyIds.add(surveyId);
            } else { //if survey is new, create new survey entry.
                // Log.d("debugging survey update", "CREATE A SURVEY");
                PersistentData.addSurveyId(surveyId);
                PersistentData.createSurveyData(surveyId, jsonQuestionsString, jsonTimingsString, surveyType, jsonSettingsString);
                BackgroundService.registerTimers(appContext); // We need to register the surveyId before we can schedule it
                SurveyScheduler.scheduleSurvey(surveyId);
                SurveyScheduler.checkImmediateTriggerSurvey(appContext, surveyId);
            }
        }

        for (String oldSurveyId : oldSurveyIds) { //for each old survey id
            if (!newSurveyIds.contains(oldSurveyId)) { //check if it is still a valid survey (it the list of new survey ids.)
                // Log.d("survey downloader", "deleting survey " + oldSurveyId);
                PersistentData.deleteSurvey(oldSurveyId);
                //It is almost definitely not worth the effort to cancel any ongoing alarms for a survey. They are one-time, and there is de minimus value to actually cancelling it.
                // also, that requires accessing the background service, which means using ugly hacks like we do with the survey scheduler (though it would be okay because this code can only actually run if the background service is already instantiated.
                SurveyNotifications.dismissNotification(appContext, oldSurveyId);
                BackgroundService.registerTimers(appContext);
            }
        }
        return 200;
    }
}
