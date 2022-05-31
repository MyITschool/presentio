package com.presentio.requests;

import com.google.gson.Gson;
import com.presentio.http.Api;
import com.presentio.params.CreatePostParams;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewPostRequest implements Callable<Integer> {
    private final Api postsApi;
    private final String text;
    private final ArrayList<String> tags;
    private final String[] attachments;
    private final float photoRatio;

    public NewPostRequest(
            Api postsApi,
            String text,
            ArrayList<String> tags,
            String[] attachments,
            float photoRatio
    ) {
        this.postsApi = postsApi;
        this.text = text;
        this.tags = tags;
        this.attachments = attachments;
        this.photoRatio = photoRatio;
    }

    @Override
    public Integer call() throws Exception {
        Gson gson = new Gson();

        String json = gson.toJson(new CreatePostParams(text, tags, attachments, photoRatio));

        RequestBody body = RequestBody.create(MediaType.parse("text/json"), json);
        Response response = postsApi.request("/v0/posts/", "POST", body);

        return response.code();
    }
}
