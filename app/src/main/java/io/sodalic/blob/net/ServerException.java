package io.sodalic.blob.net;

/**
 * Created by SergGr on 28.05.2019.
 */
public class ServerException extends Exception {
    private final int responseCode;

    public ServerException(String message) {
        super(message);
        this.responseCode = 0;
    }

    public ServerException(int responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    public ServerException(int responseCode) {
        this(responseCode, String.format("Response code = %d", responseCode));
    }

    public ServerException(Throwable cause) {
        super(cause);
        this.responseCode = 0;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
