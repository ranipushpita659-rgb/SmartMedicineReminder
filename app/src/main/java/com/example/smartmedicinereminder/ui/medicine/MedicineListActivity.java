package com.example.smartmedicinereminder.ui.medicine;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartmedicinereminder.R;
import com.example.smartmedicinereminder.models.Medicine;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MedicineListActivity extends AppCompatActivity {

    private RecyclerView rvMedicines;
    private MedicineAdapter adapter;
    private List<Medicine> medicineList;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_list);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("medicines");

        initViews();
        setupToolbar();
        setupRecyclerView();
        fetchMedicines();
    }

    private void initViews() {
        rvMedicines = findViewById(R.id.rvMedicines);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAdd = findViewById(R.id.fabAdd);

        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(MedicineListActivity.this, AddMedicineActivity.class));
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        medicineList = new ArrayList<>();
        rvMedicines.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicineAdapter(medicineList, medicine -> deleteMedicine(medicine));
        rvMedicines.setAdapter(adapter);
    }

    private void fetchMedicines() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                medicineList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Medicine medicine = postSnapshot.getValue(Medicine.class);
                    if (medicine != null) {
                        medicineList.add(medicine);
                    }
                }
                
                if (medicineList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvMedicines.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvMedicines.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MedicineListActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMedicine(Medicine medicine) {
        mDatabase.child(medicine.getId()).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Delete local image file if it exists
                if (medicine.getImageUrl() != null && !medicine.getImageUrl().isEmpty()) {
                    File file = new File(medicine.getImageUrl());
                    if (file.exists()) {
                        file.delete();
                    }
                }
                Toast.makeText(MedicineListActivity.this, "Medicine Deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MedicineListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }
}