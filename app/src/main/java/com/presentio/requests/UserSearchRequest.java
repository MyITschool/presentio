package com.presentio.requests;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.presentio.http.Api;
import com.presentio.js2p.structs.JsonUser;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import okhttp3.Response;

public class UserSearchRequest implements Callable<ArrayList<JsonUser>> {
    private final Api usersApi;
    private final int page;
    private final String query;

    public UserSearchRequest(Api usersApi, int page, String query) {
        this.usersApi = usersApi;
        this.page = page;
        this.query = query;
    }

    @Override
    public ArrayList<JsonUser> call() throws Exception {
        Response response = usersApi.requestForce("/v0/user/search/" + page + '?' + query);

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<JsonUser>>() {}.getType();

        return gson.fromJson(response.body().charStream(), type);
    }
}
