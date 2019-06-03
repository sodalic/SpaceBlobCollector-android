package io.sodalic.blob.sharedui;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;

import org.beiwe.app.CrashHandler;
import org.beiwe.app.ui.utils.AlertsManager;
import io.sodalic.blob.R;
import io.sodalic.blob.net.ServerException;
import io.sodalic.blob.utils.StringUtils;
import io.sodalic.blob.utils.UserFriendlyError;
import io.sodalic.blob.utils.Utils;

/**
 * Base class for UI-based async operations that access the server.
 */
public abstract class HttpUIAsync<Res> extends BaseHttpAsync<Res> {

    protected final BlobActivity activity;
    private final String customErrorTitle;

    private View progressView;
    private ProgressDialog progressDialog = null; // either progressView or progressDialog is null
    private Button submitButton;

    protected HttpUIAsync(@NonNull BlobActivity activity) {
        this(activity, null);
    }

    protected HttpUIAsync(@NonNull BlobActivity activity, String customErrorTitle) {
        super("Async." + Utils.getLogTag(activity.getClass()), activity.getBlobContext());
//        Objects.requireNonNull(activity, "activity");
        this.activity = activity;
        this.customErrorTitle = customErrorTitle;
    }

    @Override
    protected final void updateUiBefore() {
        // if the original task is started not from the UI thread like in the case
        // of CaptureFaceActivity, onPreExecute is run on that thread rather than
        // UI thread so we need an explicit switch here.
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUiBeforeImpl();
            }
        });
    }

    private void updateUiBeforeImpl() {
        // If there's a progress spinner, make it appear
        progressView = activity.findViewById(R.id.progressBar);
        if (progressView != null) {
            progressView.setVisibility(View.VISIBLE);
        } else {
            // if there is no explicit progressBar, use ProgressDialog
            progressDialog = ProgressDialog.show(activity,
                    null, activity.getString(R.string.async_progress_label),
                    true, false);
        }

        // If there's a submit button, disable it so the user can't submit twice
        submitButton = activity.findViewById(R.id.submitButton);
        if (submitButton != null) submitButton.setEnabled(false);
    }

    @Override
    protected final void updateUiAfter() {
        // Hide the progress spinner
        if (progressView != null) progressView.setVisibility(View.GONE);

        if (progressDialog != null) progressDialog.dismiss();

        // Re-enable the submit button
        if (submitButton != null) submitButton.setEnabled(true);
    }


    private String getErrorTitle(String defaultErrorTitle) {
        return StringUtils.isEmpty(customErrorTitle) ? defaultErrorTitle : customErrorTitle;
    }

    @Override
    protected void handleError(@NonNull Exception ex) {
        if (ex instanceof ServerException) {
            int responseCode = ((ServerException) ex).getResponseCode();
            String serverErrorTitle = getErrorTitle(activity.getString(R.string.server_error_title));
            if (responseCode != 0) {
                AlertsManager.showAlert(responseCode, serverErrorTitle, activity);
            } else {
                AlertsManager.showAlert(activity.getString(R.string.http_message_unknown_response_code), serverErrorTitle, activity);
            }
        } else {
            CrashHandler.writeCrashlog(ex, activity);
            String errorTitle = getErrorTitle(activity.getString(R.string.error_title));
            String errorMessage = activity.getString(R.string.http_message_unknown_response_code);
            if (ex instanceof UserFriendlyError) {
                errorMessage = ex.getMessage();
            }
            AlertsManager.showAlert(errorMessage, errorTitle, activity);
        }
    }


}
