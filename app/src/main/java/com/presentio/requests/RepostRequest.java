package com.presentio.requests;

import com.google.gson.Gson;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.params.CreatePostParams;
import com.presentio.http.Api;

import java.util.concurrent.Callable;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RepostRequest implements Callable<Integer> {
    private Api postsApi;
    private String text;
    private JsonFpost post;

    public RepostRequest(Api postsApi, String text, JsonFpost post) {
        this.postsApi = postsApi;
        this.text = text;
        this.post = post;
    }

    @Override
    public Integer call() throws Exception {
        Gson gson = new Gson();

        String json = gson.toJson(new CreatePostParams(text, post.id, post.userId));

        RequestBody body = RequestBody.create(MediaType.parse("text/json"), json);
        Response response = postsApi.request("/v0/posts/", "POST", body);

        return response.code();
    }
}
