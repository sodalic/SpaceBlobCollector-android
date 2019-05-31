package org.beiwe.app.session;

import java.util.Objects;

import android.content.Context;
import android.widget.Toast;

import org.beiwe.app.storage.EncryptionEngine;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.ui.utils.AlertsManager;
import io.sodalic.blob.R;
import io.sodalic.blob.context.BlobContext;
import io.sodalic.blob.net.ServerException;
import io.sodalic.blob.sharedui.BlobActivity;
import io.sodalic.blob.sharedui.HttpUIAsync;

/**
 * Code designed to be used in two very similar Activities: ResetPassword and ForgotPassword.
 * Both Activities have three text-input fields (EditTexts): current password, new password, and
 * confirm new password.
 * This code checks the new password and confirm new password; if they're good, it sends the
 * current password to the server.
 * This differs from other authentication calls in the rest of the app in that current password
 * comes from the text-input field, NOT from the app's storage.  This means that an admin can reset
 * the password on the server, and the user can use that new password to reset the device's
 * password, regardless of what password is currently saved on the device.
 *
 * @author Josh Zagorsky, Dor Samet
 */
public class ResetPassword {

    private final BlobActivity currentActivity;
    private final Context appContext;

    public ResetPassword(BlobActivity currentActivity) {
        Objects.requireNonNull(currentActivity);
        this.currentActivity = currentActivity;
        this.appContext = currentActivity.getApplicationContext();
    }

    public void checkInputsAndTryToResetPassword(String currentPassword, String newPassword, String confirmNewPassword) {
        String currentPasswordHash = EncryptionEngine.safeHash(currentPassword);
        // If the new password has too few characters, pop up an alert, and do nothing else
        if (!PersistentData.passwordMeetsRequirements(newPassword)) {
            String alertMessage = String.format(appContext.getString(R.string.password_too_short), PersistentData.minPasswordLength());
            AlertsManager.showAlert(alertMessage, appContext.getString(R.string.reset_password_error_alert_title), currentActivity);
            return;
        }
        // If the new passwords don't match, pop up an alert, and do nothing else
        if (!newPassword.equals(confirmNewPassword)) {
            AlertsManager.showAlert(appContext.getString(R.string.password_mismatch),
                    appContext.getString(R.string.reset_password_error_alert_title),
                    currentActivity);
            return;
        }
        // If new password and confirm new password are valid, try resetting them on the server
        new ResetPasswordTask(currentActivity, currentPasswordHash, newPassword).execute();
    }

    /**
     * Runs the network operation to reset the password on the server.
     */
    static class ResetPasswordTask extends HttpUIAsync<Void> {
        private final String currentPasswordHash;
        private final String newPassword;

        ResetPasswordTask(BlobActivity activity, String currentPasswordHash, String newPassword) {
            super(activity, activity.getString(R.string.reset_password_error_alert_title));
            this.currentPasswordHash = currentPasswordHash;
            this.newPassword = newPassword;
        }

        @Override
        protected Void doTask(BlobContext blobContext) throws ServerException {
            blobContext.getServerApi().sendSetPassword(currentPasswordHash, newPassword);
            return null;
        }

        @Override
        protected void handleSuccess(Void result) {
            // Set the password on the device to the new permanent password
            PersistentData.savePasswordHashDirectly(newPassword);
            // Set the user to "logged in"
            PersistentData.loginOrRefreshLogin();
            // Show a Toast with a "Success!" message
            String message = activity.getString(R.string.pass_reset_complete);
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            // Kill the activity
            activity.finish();
        }
    }


}
