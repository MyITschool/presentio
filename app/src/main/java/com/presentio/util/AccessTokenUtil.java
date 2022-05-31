package com.presentio.util;

public class AccessTokenUtil {
    private static String accessToken = null;

    public static void setToken(String token) {
        accessToken = token;
    }

    public static String getToken() {
        return accessToken;
    }
}
