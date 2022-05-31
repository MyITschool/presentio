package com.presentio.requests;

import com.google.gson.Gson;
import com.presentio.js2p.structs.JsonUserInfo;
import com.presentio.http.Api;

import java.util.concurrent.Callable;

import okhttp3.Response;

public class GetUserInfoRequest implements Callable<JsonUserInfo> {
    private final DataHandler handler;
    private final Api usersApi;
    private final long userId;

    public GetUserInfoRequest(Api usersApi) {
        this(usersApi, -1);
    }

    public GetUserInfoRequest(Api usersApi, long userId) {
        this(new DefaultDataHandler(), usersApi, userId);
    }

    public GetUserInfoRequest(DataHandler handler, Api usersApi) {
        this(handler, usersApi, -1);
    }

    public GetUserInfoRequest(DataHandler handler, Api usersApi, long userId) {
        this.handler = handler;
        this.usersApi = usersApi;
        this.userId = userId;
    }

    @Override
    public JsonUserInfo call() throws Exception {
        String userPart = userId == -1 ? "self" : String.valueOf(userId);

        Response response = handler.getResponse(usersApi, "/v0/user/info/" + userPart);

        Gson gson = new Gson();

        return gson.fromJson(response.body().charStream(), JsonUserInfo.class);
    }
}
