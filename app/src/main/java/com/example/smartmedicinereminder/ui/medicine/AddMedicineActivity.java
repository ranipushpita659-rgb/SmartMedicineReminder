package com.example.smartmedicinereminder.ui.medicine;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smartmedicinereminder.R;
import com.example.smartmedicinereminder.models.Medicine;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddMedicineActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView ivMedicinePhoto;
    private Button btnSelectPhoto, btnSaveMedicine;
    private TextInputEditText etMedName, etQuantity;
    private CheckBox cbMorning, cbNoon, cbNight;
    private Uri imageUri;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("medicines");

        initViews();
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        ivMedicinePhoto = findViewById(R.id.ivMedicinePhoto);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnSaveMedicine = findViewById(R.id.btnSaveMedicine);
        etMedName = findViewById(R.id.etMedName);
        etQuantity = findViewById(R.id.etQuantity);
        cbMorning = findViewById(R.id.cbMorning);
        cbNoon = findViewById(R.id.cbNoon);
        cbNight = findViewById(R.id.cbNight);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnSelectPhoto.setOnClickListener(v -> openFileChooser());
        btnSaveMedicine.setOnClickListener(v -> saveMedicine());
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivMedicinePhoto.setImageURI(imageUri);
        }
    }

    private void saveMedicine() {
        String name = etMedName.getText().toString().trim();
        String quantity = etQuantity.getText().toString().trim();

        List<String> selectedTimeSlots = new ArrayList<>();
        List<String> selectedTimeRanges = new ArrayList<>();

        if (cbMorning.isChecked()) {
            selectedTimeSlots.add("Morning");
            selectedTimeRanges.add("6:00 AM - 10:30 AM");
        }
        if (cbNoon.isChecked()) {
            selectedTimeSlots.add("Noon");
            selectedTimeRanges.add("12:00 PM - 3:00 PM");
        }
        if (cbNight.isChecked()) {
            selectedTimeSlots.add("Night");
            selectedTimeRanges.add("8:00 PM - 11:30 PM");
        }

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(quantity) || selectedTimeSlots.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and select at least one time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeSlotString = TextUtils.join(", ", selectedTimeSlots);
        String timeRangeString = TextUtils.join(", ", selectedTimeRanges);

        saveDataLocallyAndSync(name, quantity, timeSlotString, timeRangeString);
    }

    private void saveDataLocallyAndSync(String name, String quantity, String timeSlot, String timeRange) {
        btnSaveMedicine.setEnabled(false);
        String medicineId = mDatabase.push().getKey();
        String userId = mAuth.getCurrentUser().getUid();

        String localImagePath = "";
        if (imageUri != null) {
            localImagePath = saveImageToInternalStorage(imageUri, medicineId);
        }

        saveToDatabase(medicineId, name, quantity, timeSlot, timeRange, localImagePath, userId);
    }

    private String saveImageToInternalStorage(Uri uri, String fileName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            File directory = getDir("medicine_images", Context.MODE_PRIVATE);
            File myPath = new File(directory, fileName + ".jpg");

            FileOutputStream fos = new FileOutputStream(myPath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            return myPath.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void saveToDatabase(String id, String name, String quantity, String timeSlot, String timeRange, String imageUrl, String userId) {
        Medicine medicine = new Medicine(id, name, quantity, timeSlot, timeRange, imageUrl, userId);
        mDatabase.child(id).setValue(medicine).addOnCompleteListener(task -> {
            btnSaveMedicine.setEnabled(true);
            if (task.isSuccessful()) {
                Toast.makeText(AddMedicineActivity.this, "Medicine Added Successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(AddMedicineActivity.this, "Failed to save medicine to cloud", Toast.LENGTH_SHORT).show();
            }
        });
    }
}