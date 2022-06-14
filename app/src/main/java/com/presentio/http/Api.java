package com.presentio.http;

import android.content.Context;
import android.content.SharedPreferences;


import com.google.gson.Gson;
import com.presentio.js2p.JsonRefresh;
import com.presentio.util.AccessTokenUtil;
import com.presentio.util.SharedPreferencesUtil;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Api {
    public abstract static class ApiException extends Exception {
        protected ApiException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends ApiException {
        public InvalidCredentialsException() {
            super("Unable to do request because of invalid credentials");
        }
    }

    public static class ServerUnavailableException extends ApiException {
        public ServerUnavailableException() {
            super("Server was unable to process the request");
        }
    }

    public interface LogoutHandler {
        void onLogout();
    }

    public static final String HOST_USER_SERVICE = "http://188.225.10.136:8080";
    public static final String HOST_POSTS_SERVICE = "http://188.225.10.136:8081";

    private static final String REFRESH_HOST = HOST_USER_SERVICE;

    private static LogoutHandler handler = null;

    private final Context context;
    private final OkHttpClient client;
    private final String host;
    
    public Api(Context context, OkHttpClient client, String host) {
        this.context = context;
        this.client = client;
        this.host = host;
    }
    
    public Response request(
            String path,
            String method,
            RequestBody body,
            CacheControl cacheControl
    ) throws IOException, ApiException {
        Request request = createRequest(path, method, body, cacheControl);

        Response response = getResponse(request);

        if (response.code() >= 500 && response.code() < 600) {
            throw new ServerUnavailableException();
        }

        return response;
    }
    
    public Response request(String path) throws IOException, ApiException {
        return request(path, "GET", null);
    }
    
    public Response request(String path, String method, RequestBody body) throws IOException, ApiException {
        return request(path, method, body, null);
    }
    
    public Response requestForce(String path) throws IOException, ApiException {
        return requestForce(path, "GET", null);
    }
    
    public Response requestForce(String path, String method, RequestBody body) throws IOException, ApiException {
        return request(path, method, body, CacheControl.FORCE_NETWORK);
    }

    public String getNewToken() throws IOException, InvalidCredentialsException {
        SharedPreferences preferences = SharedPreferencesUtil.getSharedPreferences(context);

        String refreshToken = SharedPreferencesUtil.getRefreshToken(preferences);

        Request request = new Request.Builder()
                .url(REFRESH_HOST + "/v0/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken)
                .method("GET", null)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build();

        Response response = client.newCall(request).execute();

        if (response.code() != 200) {
            throw new InvalidCredentialsException();
        }

        Gson gson = new Gson();

        return gson.fromJson(response.body().charStream(), JsonRefresh.class).accessToken;
    }

    private Request createRequest(
            String path,
            String method,
            RequestBody body,
            CacheControl cacheControl
    ) {
        Request.Builder builder = new Request.Builder()
                .url(host + path)
                .method(method, body)
                .header("Authorization", "Bearer " + AccessTokenUtil.getToken());

        if (cacheControl != null) {
            builder = builder.cacheControl(cacheControl);
        }

        return builder.build();
    }

    private Response getResponse(Request request) throws IOException, InvalidCredentialsException {
        Response response = client.newCall(request).execute();

        int code = response.code();

        while (code == 406 || code == 408 || code == 400 || code == 403) {
            AccessTokenUtil.setToken(getNewToken());

            request = request
                    .newBuilder()
                    .header("Authorization", "Bearer " + AccessTokenUtil.getToken())
                    .build();

            response = client.newCall(request).execute();
            code = response.code();
        }

        return response;
    }

    public static RequestBody createEmptyBody() {
        return RequestBody.create(null, new byte[0]);
    }

    public static void setHandler(LogoutHandler handler) {
        Api.handler = handler;
    }

    public static void logout() {
        if (handler != null) {
            handler.onLogout();
        }
    }
}
