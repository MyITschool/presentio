package com.presentio.util;

public class AccessTokenUtil {
    private static String accessToken = "";

    public static void setToken(String token) {
        synchronized (accessToken) {
            accessToken = token;
        }
    }

    public static String getToken() {
        return accessToken;
    }
}
