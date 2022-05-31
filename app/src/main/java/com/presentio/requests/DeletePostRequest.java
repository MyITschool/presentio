package com.presentio.requests;

import com.presentio.http.Api;

import java.util.concurrent.Callable;

import okhttp3.Response;

public class DeletePostRequest implements Callable<Integer> {
    private Api postsApi;
    private long postId;

    public DeletePostRequest(Api postsApi, long postId) {
        this.postsApi = postsApi;
        this.postId = postId;
    }

    @Override
    public Integer call() throws Exception {
        Response response = postsApi.request("/v0/posts/" + postId, "DELETE", null);

        return response.code();
    }
}
