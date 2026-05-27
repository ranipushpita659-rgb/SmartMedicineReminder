package com.example.smartmedicinereminder.ui.medicine;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartmedicinereminder.R;
import com.example.smartmedicinereminder.models.Medicine;
import com.example.smartmedicinereminder.utils.AlarmHelper;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MedicineListActivity extends AppCompatActivity {

    private RecyclerView rvMedicines;
    private MedicineAdapter adapter;
    private final List<Medicine> medicineList = new ArrayList<>();
    private LinearLayout llEmpty;
    private ExtendedFloatingActionButton fabAdd;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private ValueEventListener mListener;
    private Query mQuery;
    private boolean isStockTrackerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_list);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("medicines");

        if (getIntent() != null && "STOCK_TRACKER".equals(getIntent().getStringExtra("MODE"))) {
            isStockTrackerMode = true;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
    }

    private void initViews() {
        rvMedicines = findViewById(R.id.rvMedicines);
        llEmpty = findViewById(R.id.tvEmpty);
        fabAdd = findViewById(R.id.fabAdd);

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(MedicineListActivity.this, AddMedicineActivity.class);
                startActivity(intent);
            });
            if (isStockTrackerMode) fabAdd.setVisibility(View.GONE);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(isStockTrackerMode ? "Medicine Stock Tracker" : getString(R.string.medicine_list));
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        if (rvMedicines != null) {
            rvMedicines.setLayoutManager(new LinearLayoutManager(this));
            adapter = new MedicineAdapter(medicineList, new MedicineAdapter.OnMedicineClickListener() {
                @Override
                public void onDeleteClick(Medicine medicine) {
                    if (!isStockTrackerMode) deleteMedicine(medicine);
                }

                @Override
                public void onItemClick(Medicine medicine) {
                }
            });
            adapter.setStockTrackerMode(isStockTrackerMode);
            rvMedicines.setAdapter(adapter);
        }
    }

    private void fetchMedicines() {
        if (mAuth.getCurrentUser() == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        
        if (mListener != null && mQuery != null) {
            mQuery.removeEventListener(mListener);
        }

        mQuery = mDatabase.orderByChild("userId").equalTo(userId);
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                medicineList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    try {
                        Medicine medicine = postSnapshot.getValue(Medicine.class);
                        if (medicine != null) {
                            medicineList.add(medicine);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isFinishing()) {
                    Toast.makeText(MedicineListActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        mQuery.addValueEventListener(mListener);
    }

    private void updateUI() {
        if (llEmpty != null) {
            llEmpty.setVisibility(medicineList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (rvMedicines != null) {
            rvMedicines.setVisibility(medicineList.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void deleteMedicine(Medicine medicine) {
        if (medicine == null || medicine.getId() == null) return;
        
        mDatabase.child(medicine.getId()).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AlarmHelper.cancelAlarm(this, medicine);
                if (medicine.getImageUrl() != null && !medicine.getImageUrl().isEmpty()) {
                    try {
                        File file = new File(medicine.getImageUrl());
                        if (file.exists()) file.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Toast.makeText(MedicineListActivity.this, "Medicine Deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MedicineListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchMedicines();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mListener != null && mQuery != null) {
            mQuery.removeEventListener(mListener);
        }
    }
}
