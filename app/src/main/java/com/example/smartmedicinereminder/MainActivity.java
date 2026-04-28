package com.example.smartmedicinereminder;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartmedicinereminder.models.Medicine;
import com.example.smartmedicinereminder.ui.auth.LoginActivity;
import com.example.smartmedicinereminder.ui.medicine.AddMedicineActivity;
import com.example.smartmedicinereminder.ui.medicine.MedicineAdapter;
import com.example.smartmedicinereminder.ui.medicine.MedicineListActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * MainActivity serves as the Dashboard.
 * It now filters and displays "Upcoming Medicines" based on the current time of day.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvUserName, tvNoUpcoming, tvCurrentSlotLabel;
    private RecyclerView rvUpcomingMeds;
    private MedicineAdapter adapter;
    private List<Medicine> upcomingMedsList;
    private MaterialCardView cardAddMeds, cardMedsList;
    private com.google.android.material.button.MaterialButton btnEmergency;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("medicines");

        initViews();
        setupNavigationDrawer();
        setupRecyclerView();
        setupListeners();
        loadUpcomingMedicines();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        tvUserName = findViewById(R.id.tvUserName);
        tvNoUpcoming = findViewById(R.id.tvNoUpcoming);
        tvCurrentSlotLabel = findViewById(R.id.tvCurrentSlotLabel);
        rvUpcomingMeds = findViewById(R.id.rvUpcomingMeds);
        cardAddMeds = findViewById(R.id.cardAddMeds);
        cardMedsList = findViewById(R.id.cardMedsList);
        btnEmergency = findViewById(R.id.btnEmergency);

        tvUserName.setText("Senior Citizen");
    }

    private void setupRecyclerView() {
        upcomingMedsList = new ArrayList<>();
        rvUpcomingMeds.setLayoutManager(new LinearLayoutManager(this));
        // Using existing MedicineAdapter. We'll pass a dummy listener for dashboard view.
        adapter = new MedicineAdapter(upcomingMedsList, medicine -> {
            // Delete not allowed from dashboard, or we can implement it
        });
        rvUpcomingMeds.setAdapter(adapter);
    }

    private void loadUpcomingMedicines() {
        String currentSlot = getCurrentTimeSlot();
        tvCurrentSlotLabel.setText(currentSlot + " Medicines");

        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                upcomingMedsList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Medicine medicine = postSnapshot.getValue(Medicine.class);
                    if (medicine != null && medicine.getTimeSlot().contains(currentSlot)) {
                        upcomingMedsList.add(medicine);
                    }
                }

                if (upcomingMedsList.isEmpty()) {
                    tvNoUpcoming.setVisibility(View.VISIBLE);
                    rvUpcomingMeds.setVisibility(View.GONE);
                } else {
                    tvNoUpcoming.setVisibility(View.GONE);
                    rvUpcomingMeds.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getCurrentTimeSlot() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        double currentTime = hour + (minute / 60.0);

        if (currentTime >= 6.0 && currentTime <= 10.5) {
            return "Morning";
        } else if (currentTime >= 12.0 && currentTime <= 15.0) {
            return "Noon";
        } else if (currentTime >= 20.0 && currentTime <= 23.5) {
            return "Night";
        } else {
            // If outside slots, show the next upcoming one
            if (currentTime < 6.0) return "Morning";
            if (currentTime < 12.0) return "Noon";
            if (currentTime < 20.0) return "Night";
            return "Morning"; // For very late night, show tomorrow morning
        }
    }

    private void setupNavigationDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupListeners() {
        cardAddMeds.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddMedicineActivity.class)));
        cardMedsList.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MedicineListActivity.class)));
        btnEmergency.setOnClickListener(v -> Toast.makeText(this, "Emergency Call...", Toast.LENGTH_LONG).show());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile) {
            Toast.makeText(this, "Profile Clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_add_medicine) {
            startActivity(new Intent(MainActivity.this, AddMedicineActivity.class));
        } else if (id == R.id.nav_medicine_list) {
            startActivity(new Intent(MainActivity.this, MedicineListActivity.class));
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}