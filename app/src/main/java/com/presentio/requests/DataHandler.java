package com.presentio.requests;

import com.presentio.http.Api;

import java.io.IOException;

import okhttp3.Response;

public interface DataHandler extends Finisher {
    Response getResponse(Api api, String url) throws IOException, Api.ApiException;
}
