package com.team21.questify.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.team21.questify.application.model.User;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SharedPrefs {
    private static final String PREF_NAME = "QuestifyPrefs";
    private static final String KEY_USER_UID = "user_uid";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_USERNAME = "user_username";
    private static final String KEY_USERNAME_LOCKED = "username_locked";
    private static final String KEY_AVATAR_LOCKED = "avatar_locked";

    private static final String KEY_FRIENDS_IDS = "friendsIds";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    private final SharedPreferences prefs;

    public SharedPrefs(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserSession(String uid, String email, String username) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_UID, uid);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_USERNAME, username);
        editor.apply();
    }
    public void saveFCMToken(String token) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
    }
    public void lockUsername() {
        prefs.edit().putBoolean(KEY_USERNAME_LOCKED, true).apply();
    }

    public void lockAvatar() {
        prefs.edit().putBoolean(KEY_AVATAR_LOCKED, true).apply();
    }

    public String getUserUid() {
        return prefs.getString(KEY_USER_UID, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }
    public String getUsername() {
        return prefs.getString(KEY_USER_USERNAME, null);
    }

    public boolean isUsernameLocked() {
        return prefs.getBoolean(KEY_USERNAME_LOCKED, false);
    }

    public boolean isAvatarLocked() {
        return prefs.getBoolean(KEY_AVATAR_LOCKED, false);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
