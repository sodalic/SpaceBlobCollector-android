package io.sodalic.blob.face;

import io.sodalic.blob.utils.UserFriendlyError;

/**
 * Created by SergGr on 01.06.2019.
 */
public class FaceApiException extends Exception implements UserFriendlyError {
    public FaceApiException(String message) {
        super(message);
    }

    public FaceApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public FaceApiException(Throwable cause) {
        super(cause);
    }
}
