package com.example.smartmedicinereminder.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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

import java.util.Calendar;

public class AlarmHelper {
    private static final String TAG = "AlarmHelper";

    public static void scheduleMedicineAlarms(Context context, Medicine medicine) {
        if (medicine == null || medicine.getMedicineTime() == null || medicine.getMedicineTime().isEmpty()) return;

        try {
            String[] timeParts = medicine.getMedicineTime().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            String period = getPeriodFromTime(hour);
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("period", period);
            
            int requestCode = medicine.getId().hashCode(); 

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }
            Log.d(TAG, "Alarm scheduled for " + medicine.getMedicineName() + " at " + hour + ":" + minute);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm: " + e.getMessage());
        }
    }

    private static String getPeriodFromTime(int hour) {
        if (hour >= 5 && hour < 12) return "Morning";
        if (hour >= 12 && hour < 18) return "Afternoon";
        return "Night";
    }

    public static void scheduleSnooze(Context context, String period) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5); 

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("period", period);
        int requestCode = period.hashCode() + 1000; 

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    public static void cancelPeriodAlarms(Context context, String period) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        int requestCode = period.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        // Also cancel snooze
        int snoozeRequestCode = period.hashCode() + 1000;
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                snoozeRequestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (snoozePendingIntent != null) {
            alarmManager.cancel(snoozePendingIntent);
            snoozePendingIntent.cancel();
        }
        
        NotificationHelper.cancelNotification(context, period.hashCode());
        ReminderReceiver.stopAlarmSound(context);
    }

    public static void stopAlarmAndService(Context context, String period) {
        Intent serviceIntent = new Intent(context, AlarmService.class);
        context.stopService(serviceIntent);
        cancelPeriodAlarms(context, period);
    }

    public static void cancelAlarm(Context context, Medicine medicine) {
        if (medicine == null || medicine.getId() == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        int requestCode = medicine.getId().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    public static void checkIfSlotCleared(Context context, String period) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("medicines");
        mDatabase.orderByChild("userId").equalTo(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean allTaken = true;
                        boolean hasInPeriod = false;
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Medicine medicine = postSnapshot.getValue(Medicine.class);
                            if (medicine != null && medicine.getPeriod() != null && medicine.getPeriod().contains(period)) {
                                hasInPeriod = true;
                                if (!medicine.isTakenStatus()) {
                                    allTaken = false;
                                    break;
                                }
                            }
                        }
                        if (hasInPeriod && allTaken) {
                            stopAlarmAndService(context, period);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public static void rescheduleAllAlarms(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("medicines");
        mDatabase.orderByChild("userId").equalTo(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Medicine medicine = postSnapshot.getValue(Medicine.class);
                            if (medicine != null) {
                                scheduleMedicineAlarms(context, medicine);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public static void cancelAllAlarms(Context context) {
        Intent serviceIntent = new Intent(context, AlarmService.class);
        context.stopService(serviceIntent);

        cancelPeriodAlarms(context, "Morning");
        cancelPeriodAlarms(context, "Afternoon");
        cancelPeriodAlarms(context, "Noon");
        cancelPeriodAlarms(context, "Night");
    }
}
