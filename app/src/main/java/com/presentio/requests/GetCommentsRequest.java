package com.presentio.requests;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.presentio.js2p.structs.JsonComment;
import com.presentio.http.Api;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import okhttp3.Response;

public class GetCommentsRequest implements Callable<ArrayList<JsonComment>> {
    private DataHandler handler;
    private Api postsApi;
    private long postId;
    private int page;

    public GetCommentsRequest(DataHandler handler, Api postsApi, long postId, int page) {
        this.handler = handler;
        this.postsApi = postsApi;
        this.postId = postId;
        this.page = page;
    }

    public GetCommentsRequest(Api postsApi, long postId, int page) {
        this(new DefaultDataHandler(), postsApi, postId, page);
    }

    @Override
    public ArrayList<JsonComment> call() throws Exception {
        Response response = handler.getResponse(postsApi, "/v0/comments/" + postId + "/" + page);

        Gson gson = new Gson();

        Type type = new TypeToken<ArrayList<JsonComment>>() {}.getType();
        return gson.fromJson(response.body().charStream(), type);
    }
}
