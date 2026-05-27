package com.example.smartmedicinereminder.utils;

import com.example.smartmedicinereminder.R;
import java.util.Calendar;
import java.util.Random;

public class QuoteHelper {

    public static int getGreetingResourceId() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 6 && hour < 12) {
            return R.string.morning_greeting;
        } else if (hour >= 12 && hour < 17) {
            return R.string.afternoon_greeting;
        } else if (hour >= 17 && hour < 21) {
            return R.string.evening_greeting;
        } else {
            return R.string.night_greeting;
        }
    }

    public static int getTimeBasedQuoteResourceId() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 6 && hour < 12) {
            return R.string.morning_quote;
        } else if (hour >= 21 || hour < 6) {
            return R.string.night_quote;
        } else {
            // Random general wish for afternoon and evening
            int[] wishes = {
                R.string.wish_stay_healthy,
                R.string.wish_take_care,
                R.string.wish_be_well
            };
            return wishes[new Random().nextInt(wishes.length)];
        }
    }
}