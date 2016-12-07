package org.beiwe.app.survey;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.beiwe.app.JSONUtils;
import org.beiwe.app.R;
import org.beiwe.app.session.SessionActivity;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.storage.TextFileManager;
import org.beiwe.app.ui.user.MainMenuActivity;
import org.beiwe.app.ui.utils.SurveyNotifications;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**The SurveyActivity displays to the user the survey that has been pushed to the device.
 * Layout in this activity is rendered, not static.
 * @author Josh Zagorsky, Eli Jones */

public class SurveyActivity extends SessionActivity implements
        QuestionFragment.OnGoToNextQuestionListener,
        SubmitButtonFragment.OnSubmitButtonClickedListener {
	private String surveyId;
    private JSONArray jsonQuestions;
	private List<QuestionData> answers;
    private int currentQuestionIndex;

    //FIXME: Save fragment state so that when someone hits back, their answers are preserved
    //FIXME: If the SubmitButtonFragment has too many unanswered questions, they fill the whole screen and block the Submit button.  Figure out how to get the list to scroll.
    //FIXME: Add a ScrollView around the question layout in the fragment
    //FIXME: Check if Checkbox question no answer is the same as before
    //TODO: Give open response questions autofocus

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_survey);
		Intent triggerIntent = getIntent();
		surveyId = triggerIntent.getStringExtra("surveyId");

		if (savedInstanceState == null) {
			Bundle extras = getIntent().getExtras();
			if (extras != null) {
				answers = new ArrayList<>();

				//FIXME: stick this in a less crash-prone part of survey activity, not inside of of onCreate.
				TextFileManager.getDebugLogFile().writeEncrypted(System.currentTimeMillis() + " opened survey " + surveyId + ".");
				displaySurvey(surveyId);
			}
		}
	}


    @Override
    public void goToNextQuestion(QuestionData dataFromOldQuestion) {
        // If it's not the first question, the question index is the same as the backstack
        if (currentQuestionIndex >= 0) {
            currentQuestionIndex = getFragmentManager().getBackStackEntryCount();
        }
        currentQuestionIndex += 1;

		// Log the data from the current question
		if (dataFromOldQuestion != null) {
            recordDataFromQuestion(dataFromOldQuestion);
		}

        // If you've run out of questions, display the Submit button
        if (currentQuestionIndex >= jsonQuestions.length()) {
            displaySubmitButtonFragment();
        } else {
            // If you haven't run out of questions, display the next question
            try {
                JSONObject question1 = jsonQuestions.getJSONObject(currentQuestionIndex);
                Boolean isFirstQuestion = (currentQuestionIndex == 0);
                displaySurveyQuestionFragment(question1, isFirstQuestion);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    /**Called when the user presses "Submit" at the end of the survey,
     * saves the answers, and takes the user back to the main page. */
    @Override
    public void submitButtonClicked() {
        Log.i("SURVEYTIMINGSRECORDER**", "SUBMIT button clicked");
        SurveyTimingsRecorder.recordSubmit(getApplicationContext());
        recordAnswersAndClose();
    }


    private void displaySurvey(String surveyId) {
        jsonQuestions = getQuestionsArray(surveyId);
        currentQuestionIndex = -1;  // Start questionIndex at -1 so you can increase it by 1
        goToNextQuestion(null);

        // Record the time that the survey was first visible to the user
        //FIXME: stick this in a less crash-prone part of survey activity, not inside of of onCreate.
        SurveyTimingsRecorder.recordSurveyFirstDisplayed(surveyId);
    }


    private void recordDataFromQuestion(QuestionData questionData) {
        String questionId = questionData.getId();
        int answersIndex = getAnswersIndexForQuestionId(questionId);
        if (answersIndex == -1) {
            // If the question isn't in answers yet, append it to the end of answers
            answers.add(questionData);
        } else {
            // If the question is already in answers, replace the previous entry for this question
            answers.set(answersIndex, questionData);
        }
    }
    /**If a question with the same questionId is already in the answers ArrayList, return that
     * questionId. Otherwise, return -1. */
    private int getAnswersIndexForQuestionId(String questionId) {
        for (int i = 0; i < answers.size(); i++) {
            QuestionData question = answers.get(i);
            if (question.getId() == questionId) {
                return i;
            }
        }
        return -1;
    }


    private void displaySurveyQuestionFragment(JSONObject jsonQuestion, Boolean isFirstQuestion) {
		// Create a question fragment with the attributes of the question
		QuestionFragment questionFragment = new QuestionFragment();
		questionFragment.setArguments(QuestionJSONParser.getQuestionArgsFromJSONString(jsonQuestion));

		// Put the fragment into the view
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if (isFirstQuestion) {
            fragmentTransaction.add(R.id.questionFragmentGoesHere, questionFragment);
        } else {
            fragmentTransaction.replace(R.id.questionFragmentGoesHere, questionFragment);
            fragmentTransaction.addToBackStack(null);
        }
		fragmentTransaction.commit();
	}


    private void displaySubmitButtonFragment() {
		ArrayList<String> unansweredQuestions = getUnansweredQuestionsList();
		Bundle args = new Bundle();
		args.putStringArrayList("unansweredQuestions", unansweredQuestions);

		SubmitButtonFragment submitFragment = new SubmitButtonFragment();
		submitFragment.setArguments(args);

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.questionFragmentGoesHere, submitFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }


	private ArrayList<String> getUnansweredQuestionsList() {
		ArrayList<String> unansweredQuestions = new ArrayList<>();
		for (int i = 0; i < answers.size(); i++) {
			QuestionData questionData = answers.get(i);
			if (questionData.getAnswerString() == null) {
				unansweredQuestions.add("Question " + (i + 1) + ": " + questionData.getText());
			}
		}
		return unansweredQuestions;
	}


	private JSONArray getQuestionsArray(String surveyId) {
		// Get survey settings
		Boolean randomizeWithMemory = false;
		Boolean randomize = false;
		int numberQuestions = 0;
		try {
			JSONObject surveySettings = new JSONObject(PersistentData.getSurveySettings(surveyId));
			randomizeWithMemory = surveySettings.optBoolean(getString(R.string.randomizeWithMemory), false);
			randomize = surveySettings.optBoolean(getString(R.string.randomize), false);
			numberQuestions = surveySettings.optInt(getString(R.string.numberQuestions), 0);
		} catch (JSONException e) {
			Log.e("Survey Activity", "There was an error parsing survey settings");
			e.printStackTrace();
		}

		// Get survey content as an array of questions; each question is a JSON object
		JSONArray jsonQuestions = new JSONArray();
		try {
			jsonQuestions = new JSONArray(PersistentData.getSurveyContent(surveyId));
			// If randomizing the question order, reshuffle the questions in the JSONArray
			if (randomize && !randomizeWithMemory) { jsonQuestions = JSONUtils.shuffleJSONArray(jsonQuestions, numberQuestions); }
			if (randomize && randomizeWithMemory) { jsonQuestions = JSONUtils.shuffleJSONArrayWithMemory(jsonQuestions, numberQuestions, surveyId); }
		} catch (JSONException e) { e.printStackTrace(); }

		return jsonQuestions;
	}

	
	/**Write the Survey answers to a new SurveyAnswers.csv file, and show a Toast reporting either success or failure*/
	private void recordAnswersAndClose() {
		// Write the data to a SurveyAnswers file
        SurveyAnswersRecorder answersRecorder = new SurveyAnswersRecorder();
		// Show a Toast telling the user either "Thanks, success!" or "Oops, there was an error"
		String toastMsg = null;
		if (answersRecorder.writeLinesToFile(surveyId, answers)) {
			toastMsg = PersistentData.getSurveySubmitSuccessToastText();
		} else {
			toastMsg = getApplicationContext().getResources().getString(R.string.survey_submit_error_message);
		}
		Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();

		// Close the Activity
		startActivity(new Intent(getApplicationContext(), MainMenuActivity.class));
		PersistentData.setSurveyNotificationState(surveyId, false);		
		SurveyNotifications.dismissNotification(getApplicationContext(), surveyId);
		finish();
	}
}