package com.team21.questify.utils;

import android.text.TextUtils;

import java.util.regex.Pattern;

public class AuthValidation {
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static boolean isValidEmail(String s) {
        return notEmpty(s) && EMAIL_REGEX.matcher(s).matches();
    }

    public static boolean isStrongPassword(String s) {
        return notEmpty(s) && s.length() >= 6;
    }

    public static boolean passwordsMatch(String a, String b) {
        return TextUtils.equals(a, b);
    }

    public static boolean isValidUsername(String s) {
        return notEmpty(s) && s.length() >= 3 && !s.contains(" ");
    }
}
