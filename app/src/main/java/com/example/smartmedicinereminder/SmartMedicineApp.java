package com.example.smartmedicinereminder;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.smartmedicinereminder.utils.NotificationHelper;

public class SmartMedicineApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this);
        
        // Apply theme preference on app startup
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
