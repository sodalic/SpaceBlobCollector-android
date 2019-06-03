package io.sodalic.blob.face;

/**
 * Created by SergGr on 01.06.2019.
 */
public class FaceApiException extends Exception {
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
