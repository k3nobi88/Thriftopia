package com.example.thriftopia;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static final String PREF_NAME = "thriftopia_theme_pref";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final String MODE_SYSTEM = "system";
    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";

    public static void applySavedTheme(Context context) {
        String mode = getSavedThemeMode(context);
        applyThemeMode(mode);
    }

    public static void saveThemeMode(Context context, String mode) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_THEME_MODE, mode).apply();

        applyThemeMode(mode);
    }

    public static String getSavedThemeMode(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_THEME_MODE, MODE_SYSTEM);
    }

    private static void applyThemeMode(String mode) {
        if (MODE_LIGHT.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (MODE_DARK.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}