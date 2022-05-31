package com.presentio.requests;

import com.presentio.http.Api;
import com.presentio.js2p.structs.JsonUserInfo;

import java.util.concurrent.Callable;

import okhttp3.Response;

public class ToggleFollowRequest implements Callable<Integer> {
    private final Api usersApi;
    private final JsonUserInfo user;

    public ToggleFollowRequest(Api usersApi, JsonUserInfo user) {
        this.usersApi = usersApi;
        this.user = user;
    }

    @Override
    public Integer call() throws Exception {
        Response response = usersApi.request(
                "/v0/follow/" + user.user.id,
                user.user.follow.id == 0 ? "POST" : "DELETE",
                Api.createEmptyBody());

        return response.code();
    }
}
