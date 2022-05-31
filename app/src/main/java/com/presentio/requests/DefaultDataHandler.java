package com.presentio.requests;

import com.presentio.http.Api;

import java.io.IOException;

import okhttp3.Response;

public class DefaultDataHandler implements DataHandler {
    @Override
    public Response getResponse(Api api, String url) throws IOException, Api.ApiException {
        return api.request(url);
    }

    @Override
    public void finish(boolean success) {
        // by default finish does nothing
    }
}
