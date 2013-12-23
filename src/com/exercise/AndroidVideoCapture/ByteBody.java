package com.exercise.AndroidVideoCapture;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.parser.StringParser;

public class ByteBody implements AsyncHttpRequestBody<String> {
    public ByteBody() {
    }

    byte[] mBodyBytes;
    String string;
    public ByteBody(byte[] data) {
        this();
        this.mBodyBytes = data;
    }

    @Override
    public void parse(DataEmitter emitter, final CompletedCallback completed) {
        new StringParser().parse(emitter).setCallback(new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                string = result;
                completed.onCompleted(e);
            }
        });
    }

    public static final String CONTENT_TYPE = "application/octet-stream";

    @Override
    public void write(AsyncHttpRequest request, DataSink sink, final CompletedCallback completed) {
        Util.writeAll(sink, mBodyBytes, completed);
    }

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        return mBodyBytes.length;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public String get() {
        return toString();
    }
}