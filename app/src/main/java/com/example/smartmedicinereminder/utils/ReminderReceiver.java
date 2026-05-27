package com.example.smartmedicinereminder.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smartmedicinereminder.models.Medicine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static MediaPlayer mediaPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        String period = intent.getStringExtra("period");
        if (period == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        SharedPreferences appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String role = appPrefs.getString("user_role", "user");

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("medicines");
        mDatabase.orderByChild("userId").equalTo(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasUntaken = false;
                        StringBuilder medList = new StringBuilder();

                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Medicine medicine = postSnapshot.getValue(Medicine.class);
                            if (medicine != null && medicine.getPeriod() != null && medicine.getPeriod().contains(period)) {
                                if (!medicine.isTakenStatus()) {
                                    hasUntaken = true;
                                    if (medList.length() > 0) medList.append(", ");
                                    medList.append(medicine.getMedicineName());
                                }
                            }
                        }

                        if (hasUntaken) {
                            if (role.equals("caregiver")) {
                                NotificationHelper.showNotification(context, 
                                        "Medicine Missed Alert", 
                                        "Senior citizen missed their " + period + " medicines: " + medList.toString(), 
                                        period.hashCode(), period, "");
                            } else {
                                startAlarmSound(context);
                                String title = "Time to take your " + period + " medicines";
                                String message = "Medicines: " + medList.toString();
                                NotificationHelper.showNotification(context, title, message, period.hashCode(), period, "");
                                AlarmHelper.scheduleSnooze(context, period);
                            }
                        } else {
                            stopAlarmSound(context);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: " + error.getMessage());
                    }
                });
    }

    private void startAlarmSound(Context context) {
        if (mediaPlayer != null) {
            stopAlarmSound(context);
        }

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, alarmUri);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mediaPlayer.setAudioAttributes(audioAttributes);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "Error playing alarm sound", e);
        }
    }

    public static void stopAlarmSound(Context context) {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping alarm sound", e);
            } finally {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }
}
