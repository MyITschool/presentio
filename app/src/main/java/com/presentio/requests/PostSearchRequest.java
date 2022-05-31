package com.presentio.requests;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.http.Api;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import okhttp3.Response;

public class PostSearchRequest implements Callable<ArrayList<JsonFpost>> {
    private final Api postsApi;
    private final int page;
    private final String query;

    public PostSearchRequest(Api postsApi, int page, String query) {
        this.postsApi = postsApi;
        this.page = page;
        this.query = query;
    }

    @Override
    public ArrayList<JsonFpost> call() throws Exception {
        Response response = postsApi.requestForce("/v0/posts/search/" + page + '?' + query);

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<JsonFpost>>() {}.getType();

        return gson.fromJson(response.body().charStream(), type);
    }
}
