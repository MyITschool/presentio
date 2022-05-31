package com.presentio.requests;

import com.google.gson.Gson;
import com.presentio.js2p.structs.JsonComment;
import com.presentio.params.CreateCommentParams;
import com.presentio.http.Api;

import java.util.concurrent.Callable;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateCommentRequest implements Callable<JsonComment> {
    private final Api postsApi;
    private final String text;
    private final long postId;

    public CreateCommentRequest(Api postsApi, String text, long postId) {
        this.postsApi = postsApi;
        this.text = text;
        this.postId = postId;
    }

    @Override
    public JsonComment call() throws Exception {
        Gson gson = new Gson();

        String json = gson.toJson(new CreateCommentParams(text));

        RequestBody body = RequestBody.create(MediaType.parse("text/json"), json);
        Response response = postsApi.request("/v0/comments/" + postId, "POST", body);

        return gson.fromJson(response.body().charStream(), JsonComment.class);
    }
}
