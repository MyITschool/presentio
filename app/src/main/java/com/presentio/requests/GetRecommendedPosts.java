package com.presentio.requests;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.presentio.http.Api;
import com.presentio.js2p.structs.JsonFpost;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import okhttp3.Response;

public class GetRecommendedPosts implements Callable<ArrayList<JsonFpost>> {
    private DataHandler handler;
    private Api postsApi;
    private int page;

    public GetRecommendedPosts(DataHandler handler, Api postsApi, int page) {
        this.handler = handler;
        this.postsApi = postsApi;
        this.page = page;
    }

    public GetRecommendedPosts(Api postsApi, int page) {
        this(new DefaultDataHandler() {
            @Override
            public Response getResponse(Api api, String url) throws IOException, Api.ApiException {
                return api.requestForce(url);
            }
        }, postsApi, page);
    }

    @Override
    public ArrayList<JsonFpost> call() throws Exception {
        Response response = handler.getResponse(postsApi, "/v0/posts/recommended/" + page);

        Gson gson = new Gson();

        Type type = new TypeToken<ArrayList<JsonFpost>>() {}.getType();
        return gson.fromJson(response.body().charStream(), type);
    }
}
