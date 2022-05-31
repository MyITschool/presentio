package com.presentio.requests;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.presentio.http.Api;
import com.presentio.js2p.structs.JsonFpost;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import okhttp3.Response;

public class GetUserFavorites implements Callable<ArrayList<JsonFpost>> {
    private final Api postsApi;
    private final DataHandler handler;
    private final int page;

    public GetUserFavorites(Api postsApi, int page) {
        this(postsApi, new DefaultDataHandler(), page);
    }

    public GetUserFavorites(Api postsApi, DataHandler handler, int page) {
        this.postsApi = postsApi;
        this.handler = handler;
        this.page = page;
    }

    @Override
    public ArrayList<JsonFpost> call() throws Exception {
        Response response = handler.getResponse(postsApi, "/v0/favorites/" + page);

        Gson gson = new Gson();

        Type type = new TypeToken<ArrayList<JsonFpost>>() {}.getType();
        return gson.fromJson(response.body().charStream(), type);
    }
}
