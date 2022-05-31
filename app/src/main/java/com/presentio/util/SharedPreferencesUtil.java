package com.presentio.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {
    public static final String PRESENTIO_PREFERENCES = "Presentio-Preferences";

    public static final String PRESENTIO_REFRESH_TOKEN = "Presentio-Refresh-Token";

    public static String getRefreshToken(SharedPreferences preferences) {
        return preferences.getString(PRESENTIO_REFRESH_TOKEN, null);
    }

    public static void setRefreshToken(SharedPreferences preferences, String token) {
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(PRESENTIO_REFRESH_TOKEN, token);

        editor.apply();
    }

    public static void removeRefreshToken(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();

        editor.remove(PRESENTIO_REFRESH_TOKEN);

        editor.apply();
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PRESENTIO_PREFERENCES, Context.MODE_PRIVATE);
    }
}
