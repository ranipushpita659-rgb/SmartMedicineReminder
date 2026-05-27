package com.example.smartmedicinereminder.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smartmedicinereminder.R;
import com.example.smartmedicinereminder.models.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etAge, etGender, etBloodGroup, etDiseases, etAllergies, etDoctorName;
    private TextInputEditText etFamilyName, etFamilyPhone, etRelationship;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        mDatabase = FirebaseDatabase.getInstance().getReference("users");
        userId = FirebaseAuth.getInstance().getUid();

        initViews();
        loadProfile();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etAge = findViewById(R.id.etAge);
        etGender = findViewById(R.id.etGender);
        etBloodGroup = findViewById(R.id.etBloodGroup);
        etDiseases = findViewById(R.id.etDiseases);
        etAllergies = findViewById(R.id.etAllergies);
        etDoctorName = findViewById(R.id.etDoctorName);
        etFamilyName = findViewById(R.id.etFamilyName);
        etFamilyPhone = findViewById(R.id.etFamilyPhone);
        etRelationship = findViewById(R.id.etRelationship);
        btnSave = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadProfile() {
        if (userId == null) return;

        showLoading(true);
        mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);
                UserProfile profile = snapshot.getValue(UserProfile.class);
                if (profile != null) {
                    etFullName.setText(profile.getFullName());
                    etAge.setText(profile.getAge());
                    etGender.setText(profile.getGender());
                    etBloodGroup.setText(profile.getBloodGroup());
                    etDiseases.setText(profile.getDiseases());
                    etAllergies.setText(profile.getAllergies());
                    etDoctorName.setText(profile.getRegularDoctorName());
                    etFamilyName.setText(profile.getEmergencyContactName());
                    etFamilyPhone.setText(profile.getEmergencyContactPhone());
                    etRelationship.setText(profile.getEmergencyContactRelationship());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String gender = etGender.getText().toString().trim();
        String bloodGroup = etBloodGroup.getText().toString().trim();
        String diseases = etDiseases.getText().toString().trim();
        String allergies = etAllergies.getText().toString().trim();
        String doctorName = etDoctorName.getText().toString().trim();
        String familyName = etFamilyName.getText().toString().trim();
        String familyPhone = etFamilyPhone.getText().toString().trim();
        String relationship = etRelationship.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Required");
            return;
        }

        UserProfile profile = new UserProfile(fullName, age, gender, bloodGroup, diseases, allergies, doctorName, familyName, familyPhone, relationship);

        showLoading(true);
        mDatabase.child(userId).setValue(profile).addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(ProfileActivity.this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
    }
}
