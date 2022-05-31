package com.presentio.requests;

import com.presentio.http.Api;
import com.presentio.js2p.structs.JsonFpost;

import java.util.concurrent.Callable;

import okhttp3.Response;

public class ToggleFavoritePostRequest implements Callable<Integer> {
    private final Api postsApi;
    private final JsonFpost post;

    public ToggleFavoritePostRequest(Api postsApi, JsonFpost post) {
        this.postsApi = postsApi;
        this.post = post;
    }

    @Override
    public Integer call() throws Exception {
        Response response = postsApi.request(
                "/v0/favorites/" + post.id,
                post.favorite.id == 0 ? "POST" : "DELETE",
                Api.createEmptyBody()
        );

        return response.code();
    }
}
