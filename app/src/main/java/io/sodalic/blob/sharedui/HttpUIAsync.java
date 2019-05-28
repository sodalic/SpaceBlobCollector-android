package io.sodalic.blob.sharedui;

import android.view.View;
import android.widget.Button;
import io.sodalic.blob.R;
import io.sodalic.blob.net.ServerException;
import io.sodalic.blob.utils.StringUtils;
import io.sodalic.blob.utils.Utils;
import org.beiwe.app.CrashHandler;
import org.beiwe.app.ui.utils.AlertsManager;

/**
 * Base class for UI-baseed async operations that access the server.
 */
public abstract class HttpUIAsync<Res> extends BaseHttpAsync<Res> {

    protected final BlobActivity activity;

    private final String customErrorTitle;

    private View alertSpinner;
    private Button submitButton;


    protected HttpUIAsync(BlobActivity activity) {
        this(activity, null);
    }

    protected HttpUIAsync(BlobActivity activity, String customErrorTitle) {
        super("Async." + Utils.getLogTag(activity.getClass()), activity.getBlobContext());
//        Objects.requireNonNull(activity, "activity");
        this.activity = activity;
        this.customErrorTitle = customErrorTitle;
    }

    /**
     * You may want to override the onPreExecute function (your pre-logic should occur outside
     * the instantiation of the HTTPAsync instance), if you do you should call super.onPreExecute()
     * as the first line in your custom logic. This is when the spinner will appear.
     */
    @Override
    protected final void onPreExecute() {
        // If there's a progress spinner, make it appear
        alertSpinner = activity.findViewById(R.id.progressBar);
        if (alertSpinner != null) alertSpinner.setVisibility(View.VISIBLE);

        // If there's a submit button, disable it so the user can't submit twice
        submitButton = activity.findViewById(R.id.submitButton);
        if (submitButton != null) submitButton.setEnabled(false);
    }

    @Override
    protected void updateUiAfter() {
        // Hide the progress spinner
        if (alertSpinner != null) alertSpinner.setVisibility(View.GONE);

        // Re-enable the submit button
        if (submitButton != null) submitButton.setEnabled(true);
    }


    private String getErrorTitle(String defaultErrorTitle) {
        return StringUtils.isEmpty(customErrorTitle) ? defaultErrorTitle : customErrorTitle;
    }

    @Override
    protected void handleError(Exception ex) {
        if (!(ex instanceof ServerException)) {
            CrashHandler.writeCrashlog(ex, activity);
            String errorTitle = getErrorTitle(activity.getString(R.string.error_title));
            AlertsManager.showAlert(activity.getString(R.string.http_message_unknown_response_code), errorTitle, activity);
        } else {
            int responseCode = ((ServerException) ex).getResponseCode();
            String serverErrorTitle = getErrorTitle(activity.getString(R.string.server_error_title));
            if (responseCode != 0) {
                AlertsManager.showAlert(responseCode, serverErrorTitle, activity);
            } else {
                AlertsManager.showAlert(activity.getString(R.string.http_message_unknown_response_code), serverErrorTitle, activity);
            }
        }
    }


}
