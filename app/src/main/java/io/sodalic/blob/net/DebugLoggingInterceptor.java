package io.sodalic.blob.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

import okhttp3.*;
import okio.Buffer;

import io.sodalic.blob.utils.StringUtils;

/**
 * OkHttp interceptor that logs (almost) every request and response.
 * It should only be used in dev builds.
 */
public class DebugLoggingInterceptor implements Interceptor {
    private final String TAG;
    private final boolean isNetwork;
    private final boolean shouldLogBody;

    public DebugLoggingInterceptor(String baseTag, boolean isNetwork, boolean logBody) {
        this.isNetwork = isNetwork;
        TAG = baseTag + ((isNetwork) ? "_Net" : "_App");
        this.shouldLogBody = logBody;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long t1 = System.nanoTime();

        Log.i(TAG, StringUtils.formatEn("Sending request %s on %s%n%s",
                request.url(), chain.connection(), request.headers()));
        RequestBody requestBody = request.body();
        // don't log file upload anyway!
        if (shouldLogBody && (requestBody != null) && !(requestBody instanceof MultipartBody)) {
            // Currently Buffer.close does nothing but who knows about the future
            try (Buffer buffer = new Buffer()) {
                BufferedReader reader;
                requestBody.writeTo(buffer);
                buffer.flush();
                reader = new BufferedReader(new InputStreamReader(buffer.inputStream()));
                String line = null;
                while (null != (line = reader.readLine())) {
                    Log.i(TAG, line);
                }
            }
            Log.i(TAG, "---------------------------------");
        }

        Response response = chain.proceed(request);

        long t2 = System.nanoTime();
        Log.i(TAG, StringUtils.formatEn("Received response for %s in %.1fms%n%s",
                response.request().url(), (t2 - t1) / 1e6d, response.headers()));

        return response;
    }
}
