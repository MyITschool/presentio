package com.presentio.requests;

import com.google.gson.Gson;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.http.Api;

import java.util.concurrent.Callable;

import okhttp3.Response;

public class GetPostRequest implements Callable<JsonFpost> {
    private DataHandler handler;
    private Api postsApi;
    private long postId;

    public GetPostRequest(DataHandler handler, Api postsApi, long postId) {
        this.handler = handler;
        this.postsApi = postsApi;
        this.postId = postId;
    }

    @Override
    public JsonFpost call() throws Exception {
        Response response = handler.getResponse(postsApi, "/v0/posts/" + postId);

        Gson gson = new Gson();

        return gson.fromJson(response.body().charStream(), JsonFpost.class);
    }
}
