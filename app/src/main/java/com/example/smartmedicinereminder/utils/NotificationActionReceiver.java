package com.example.smartmedicinereminder.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.smartmedicinereminder.models.Medicine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationAction";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String period = intent.getStringExtra("period");

        if ("ACTION_MARK_TAKEN".equals(action) && period != null) {
            markSlotAsTaken(context, period);
        }
    }

    private void markSlotAsTaken(Context context, String period) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("medicines");
        mDatabase.orderByChild("userId").equalTo(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Medicine medicine = postSnapshot.getValue(Medicine.class);
                            if (medicine != null && medicine.getPeriod() != null && medicine.getPeriod().contains(period)) {
                                if (!medicine.isTakenStatus()) {
                                    postSnapshot.getRef().child("takenStatus").setValue(true);
                                }
                            }
                        }
                        
                        // Use the consolidated stop method
                        AlarmHelper.stopAlarmAndService(context, period);
                        
                        Toast.makeText(context, "All " + period + " medicines marked as taken", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: " + error.getMessage());
                    }
                });
    }
}
