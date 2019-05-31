package io.sodalic.blob.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * Created by SergGr on 31.05.2019.
 */
class DebugLoggingInterceptor implements Interceptor {
    private final String TAG;
    private final boolean isNetwork;
    private final boolean shouldLogBody;

    DebugLoggingInterceptor(boolean isNetwork, boolean logBody) {
        this.isNetwork = isNetwork;
        TAG = ServerApi.TAG + ((isNetwork) ? "_Net" : "_App");
        this.shouldLogBody = logBody;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long t1 = System.nanoTime();

        Log.i(ServerApi.TAG, String.format("Sending request %s on %s%n%s",
                request.url(), chain.connection(), request.headers()));
        if (shouldLogBody && (request.body() != null)) {
            RequestBody body = request.body();
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            buffer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(buffer.inputStream()));
            String line = null;
            while (null != (line = reader.readLine())) {
                Log.i(ServerApi.TAG, line);
            }
            Log.i(ServerApi.TAG, "---------------------------------");
        }

        Response response = chain.proceed(request);

        long t2 = System.nanoTime();
        Log.i(ServerApi.TAG, String.format("Received response for %s in %.1fms%n%s",
                response.request().url(), (t2 - t1) / 1e6d, response.headers()));

        return response;
    }
}
