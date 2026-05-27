package com.example.smartmedicinereminder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartmedicinereminder.models.Medicine;
import com.example.smartmedicinereminder.models.UserProfile;
import com.example.smartmedicinereminder.ui.ProfileActivity;
import com.example.smartmedicinereminder.ui.SettingsActivity;
import com.example.smartmedicinereminder.ui.auth.LoginActivity;
import com.example.smartmedicinereminder.ui.medicine.AddMedicineActivity;
import com.example.smartmedicinereminder.ui.medicine.MedicineAdapter;
import com.example.smartmedicinereminder.ui.medicine.MedicineListActivity;
import com.example.smartmedicinereminder.utils.AlarmHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private TextView tvGreetingLabel, tvUserNameDisplay, tvNoUpcoming, tvCurrentDate, tvCurrentPeriodTitle;
    private RecyclerView rvUpcomingMeds;
    private MedicineAdapter adapter;
    private List<Medicine> upcomingMedsList;

    private View cardStockTracker, cvCaregiverStatus;
    private TextView tvMedConsumptionStatus;
    private ImageView ivStatusIcon;
    private View btnEmergency;

    private DatabaseReference mDatabase;
    private DatabaseReference mUserDatabase;
    private FirebaseAuth mAuth;
    private BroadcastReceiver autoUpdateReceiver;
    private SharedPreferences appPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("medicines");
        mUserDatabase = FirebaseDatabase.getInstance().getReference("users");
        appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        initViews();
        setupNavigationDrawer();
        setupRecyclerView();
        setupListeners();
        setupAutoUpdate();
        
        loadProfileAndHeader();
        checkAndResetDailyStats();
        checkNotificationPermission();
        
        AlarmHelper.rescheduleAllAlarms(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
            }
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        tvGreetingLabel = findViewById(R.id.tvGreetingLabel);
        tvUserNameDisplay = findViewById(R.id.tvUserNameDisplay);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvCurrentPeriodTitle = findViewById(R.id.tvCurrentPeriodTitle);
        
        rvUpcomingMeds = findViewById(R.id.rvUpcomingMeds);
        tvNoUpcoming = findViewById(R.id.tvNoUpcoming);

        cardStockTracker = findViewById(R.id.cardStockTracker);
        cvCaregiverStatus = findViewById(R.id.cvCaregiverStatus);
        tvMedConsumptionStatus = findViewById(R.id.tvMedConsumptionStatus);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        btnEmergency = findViewById(R.id.btnEmergency);

        updateDateAndGreeting();
    }

    private void updateRoleUI() {
        String role = appPrefs.getString("user_role", "user");
        if (role.equals("caregiver")) {
            if (cardStockTracker != null) cardStockTracker.setVisibility(View.VISIBLE);
            if (cvCaregiverStatus != null) cvCaregiverStatus.setVisibility(View.VISIBLE);
            if (btnEmergency != null) btnEmergency.setVisibility(View.GONE);
        } else {
            if (cardStockTracker != null) cardStockTracker.setVisibility(View.GONE);
            if (cvCaregiverStatus != null) cvCaregiverStatus.setVisibility(View.GONE);
            if (btnEmergency != null) btnEmergency.setVisibility(View.VISIBLE);
        }
        if (adapter != null) {
            adapter.setUserRole(role);
        }
    }

    private void updateDateAndGreeting() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        if (tvCurrentDate != null) tvCurrentDate.setText(sdf.format(cal.getTime()));
    }

    private void setupAutoUpdate() {
        autoUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadDashboardData();
                updateDateAndGreeting();
                if (Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {
                    checkAndResetDailyStats();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(autoUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(autoUpdateReceiver, filter);
        }
    }

    private void loadProfileAndHeader() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        mUserDatabase.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserProfile profile = snapshot.getValue(UserProfile.class);
                if (profile != null) {
                    String name = profile.getFullName();
                    if (name != null && !name.isEmpty() && tvUserNameDisplay != null) {
                        tvUserNameDisplay.setText(name + " 👋");
                    }
                    
                    View headerView = navigationView.getHeaderView(0);
                    if (headerView != null) {
                        TextView navName = headerView.findViewById(R.id.navHeaderName);
                        TextView navEmail = headerView.findViewById(R.id.navHeaderEmail);
                        if (navName != null) navName.setText(profile.getFullName());
                        if (navEmail != null) navEmail.setText(user.getEmail());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkAndResetDailyStats() {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) < 5) cal.add(Calendar.DAY_OF_MONTH, -1);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        String lastResetDate = appPrefs.getString("last_reset_date", "");

        if (!today.equals(lastResetDate) && mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            mDatabase.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        postSnapshot.getRef().child("takenStatus").setValue(false);
                    }
                    appPrefs.edit().putString("last_reset_date", today).apply();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRoleUI();
        loadDashboardData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoUpdateReceiver != null) unregisterReceiver(autoUpdateReceiver);
    }

    private void setupRecyclerView() {
        upcomingMedsList = new ArrayList<>();
        if (rvUpcomingMeds != null) {
            rvUpcomingMeds.setLayoutManager(new LinearLayoutManager(this));
            adapter = new MedicineAdapter(upcomingMedsList, new MedicineAdapter.OnMedicineClickListener() {
                @Override
                public void onDeleteClick(Medicine medicine) {}
                @Override
                public void onItemClick(Medicine medicine) {
                    loadDashboardData();
                }
            });
            rvUpcomingMeds.setAdapter(adapter);
        }
    }

    private void loadDashboardData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        final String currentPeriod = getCurrentPeriod();
        String userId = user.getUid();

        if (tvCurrentPeriodTitle != null) {
            tvCurrentPeriodTitle.setText(currentPeriod.toUpperCase() + " MEDICINES");
        }

        mDatabase.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                upcomingMedsList.clear();
                boolean allTaken = true;
                boolean hasAnyInPeriod = false;

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Medicine medicine = postSnapshot.getValue(Medicine.class);
                    if (medicine != null && medicine.getPeriod() != null && medicine.getPeriod().contains(currentPeriod)) {
                        hasAnyInPeriod = true;
                        upcomingMedsList.add(medicine);
                        if (!medicine.isTakenStatus()) allTaken = false;
                    }
                }
                
                if (hasAnyInPeriod) {
                    if (tvGreetingLabel != null) {
                        tvGreetingLabel.setText(allTaken ? "All " + currentPeriod.toLowerCase() + " medicines taken!" : "Time to take your " + currentPeriod.toLowerCase() + " medicines");
                    }
                    if (tvNoUpcoming != null) tvNoUpcoming.setVisibility(View.GONE);
                } else {
                    if (tvGreetingLabel != null) tvGreetingLabel.setText("No medicines scheduled for now");
                    if (tvNoUpcoming != null) tvNoUpcoming.setVisibility(View.VISIBLE);
                }

                if (adapter != null) adapter.notifyDataSetChanged();
                
                if (appPrefs.getString("user_role", "user").equals("caregiver")) {
                    updateCaregiverStatus(allTaken, hasAnyInPeriod);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateCaregiverStatus(boolean allTaken, boolean hasAny) {
        if (tvMedConsumptionStatus == null || ivStatusIcon == null) return;
        
        if (!hasAny) {
            tvMedConsumptionStatus.setText("No medicines scheduled for this period.");
            ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info);
            ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_grey));
        } else if (allTaken) {
            tvMedConsumptionStatus.setText(getString(R.string.all_meds_taken));
            ivStatusIcon.setImageResource(android.R.drawable.checkbox_on_background);
            ivStatusIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvMedConsumptionStatus.setText(getString(R.string.meds_missed));
            ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert);
            ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_red));
        }
    }

    private String getCurrentPeriod() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "Morning";
        if (hour >= 12 && hour < 18) return "Afternoon";
        return "Night";
    }

    private void setupNavigationDrawer() {
        findViewById(R.id.btnMenu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupListeners() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;
                if (id == R.id.nav_medicines) {
                    startActivity(new Intent(this, MedicineListActivity.class));
                    return true;
                }
                if (id == R.id.nav_add) {
                    startActivity(new Intent(this, AddMedicineActivity.class));
                    return true;
                }
                if (id == R.id.nav_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                }
                return false;
            });
        }
        
        if (btnEmergency != null) {
            btnEmergency.setOnClickListener(v -> makeEmergencyCall());
        }
        
        if (cardStockTracker != null) {
            cardStockTracker.setOnClickListener(v -> {
                Intent intent = new Intent(this, MedicineListActivity.class);
                intent.putExtra("MODE", "STOCK_TRACKER");
                startActivity(intent);
            });
        }
    }

    private void makeEmergencyCall() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        mUserDatabase.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserProfile profile = snapshot.getValue(UserProfile.class);
                if (profile != null && profile.getEmergencyContactPhone() != null && !profile.getEmergencyContactPhone().isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + profile.getEmergencyContactPhone())));
                } else {
                    Toast.makeText(MainActivity.this, "Set emergency contact in profile", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
        else if (id == R.id.nav_add_medicine) startActivity(new Intent(this, AddMedicineActivity.class));
        else if (id == R.id.nav_medicine_list) startActivity(new Intent(this, MedicineListActivity.class));
        else if (id == R.id.nav_settings) startActivity(new Intent(this, SettingsActivity.class));
        else if (id == R.id.nav_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
