package com.presentio.requests;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.http.Api;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import okhttp3.Response;

public class GetUserPostsRequest implements Callable<ArrayList<JsonFpost>> {
    private final DataHandler handler;
    private final Api postsApi;
    private final int page;
    private final long userId;

    public GetUserPostsRequest(Api postsApi, int page) {
        this(new DefaultDataHandler(), postsApi, page);
    }

    public GetUserPostsRequest(DataHandler handler, Api postsApi, int page) {
        this(handler, postsApi, page, -1);
    }

    public GetUserPostsRequest(Api postsApi, int page, long userId) {
        this(new DefaultDataHandler(), postsApi, page, userId);
    }

    public GetUserPostsRequest(DataHandler handler, Api postsApi, int page, long userId) {
        this.handler = handler;
        this.postsApi = postsApi;
        this.page = page;
        this.userId = userId;
    }

    @Override
    public ArrayList<JsonFpost> call() throws Exception {
        String userPart = userId == -1 ? "self" : String.valueOf(userId);

        Response response = handler.getResponse(postsApi, "/v0/posts/user/" + userPart + '/' + page);

        Gson gson = new Gson();

        Type type = new TypeToken<ArrayList<JsonFpost>>() {}.getType();
        return gson.fromJson(response.body().charStream(), type);
    }
}
