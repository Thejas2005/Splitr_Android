package com.splitr.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "splitr_session";
    private static final String KEY_USER_ID  = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_BASE_URL = "base_url";

    private static final String DEFAULT_URL = "http://10.0.2.2:8000/";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(int userId, String username) {
        prefs.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public boolean isLoggedIn() {
        return getUserId() != -1;
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    public void saveBaseUrl(String url) {
        prefs.edit().putString(KEY_BASE_URL, url).apply();
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, DEFAULT_URL);
    }
}
