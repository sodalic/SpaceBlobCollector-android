package io.sodalic.blob.sharedui;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Objects;

import io.sodalic.blob.context.BlobContext;


/**
 * Base class for our specific {@link AsyncTask} to wrap {@link io.sodalic.blob.net.ServerApi} requests.
 * You want to use one of the subclasses:
 * @see HttpUIAsync for requests from UI (that UI waits for)
 * @see HttpBgAsync for background requests
 */
public abstract class BaseHttpAsync<Res> extends AsyncTask<Void, Void, BaseHttpAsync.ResultWrapper<Res>> {

    protected final String logTag;
    private final BlobContext blobContext;

    public BaseHttpAsync(String logTag, BlobContext blobContext) {
        Objects.requireNonNull(logTag, "logTag");
        Objects.requireNonNull(blobContext, "blobContext");
        this.logTag = logTag;
        this.blobContext = blobContext;
    }

    @Override
    protected final ResultWrapper<Res> doInBackground(Void... voids) {
        try {
            Res result = doTask(blobContext);
            return new ResultWrapper<>(result);
        } catch (Exception ex) {
            return new ResultWrapper<>(ex);
        }
    }

    /**
     * Your code should override the onPostExecute function, call super.onPostExecute(), and handle
     * any additional special response and user notification logic required by your code.
     */
    @Override
    protected final void onPostExecute(ResultWrapper<Res> resultWrapper) {
        super.onPostExecute(resultWrapper);

        updateUiAfter();

        if (resultWrapper.exception != null) {
            Log.w(logTag, "Async error", resultWrapper.exception);
            handleError(resultWrapper.exception);
        } else {
            handleSuccess(resultWrapper.result);
        }
    }


    protected abstract Res doTask(BlobContext blobContext) throws Exception;

    protected abstract void handleSuccess(Res result);

    protected abstract void handleError(Exception ex);

    protected abstract void updateUiAfter();

    static class ResultWrapper<R> {
        final R result;
        final Exception exception;

        ResultWrapper(R result) {
            this.result = result;
            this.exception = null;
        }

        ResultWrapper(Exception exception) {
            Objects.requireNonNull(exception, "exception");
            this.result = null;
            this.exception = exception;
        }
    }

}
