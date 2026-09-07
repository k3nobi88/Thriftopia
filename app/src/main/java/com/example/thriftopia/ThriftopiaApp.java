package com.example.thriftopia;

import android.app.Application;

public class ThriftopiaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ThemeManager.applySavedTheme(this);
    }
}