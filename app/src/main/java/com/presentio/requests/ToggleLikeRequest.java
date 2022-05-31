package com.presentio.requests;

import com.presentio.js2p.structs.JsonFpost;
import com.presentio.http.Api;

import java.util.concurrent.Callable;

import okhttp3.Response;

public class ToggleLikeRequest implements Callable<Integer> {
    private Api postsApi;
    private JsonFpost post;

    public ToggleLikeRequest(Api postsApi, JsonFpost post) {
        this.postsApi = postsApi;
        this.post = post;
    }

    @Override
    public Integer call() throws Exception {
        Response response = postsApi.requestForce(
                "/v0/likes/" + post.id,
                post.liked.id == 0 ? "POST" : "DELETE",
                Api.createEmptyBody());

        return response.code();
    }
}
