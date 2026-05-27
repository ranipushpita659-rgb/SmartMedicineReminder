package com.example.smartmedicinereminder.ui.medicine;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smartmedicinereminder.R;
import com.example.smartmedicinereminder.models.Medicine;
import com.example.smartmedicinereminder.utils.AlarmHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddMedicineActivity extends AppCompatActivity {

    private TextInputEditText etMedName, etDosage, etMedicineTime, etStock;
    private CheckBox cbMorning, cbAfternoon, cbNight;
    private Button btnSaveMedicine;
    private ProgressBar progressBar;

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
        etMedName = findViewById(R.id.etMedName);
        etDosage = findViewById(R.id.etDosage);
        etMedicineTime = findViewById(R.id.etMedicineTime);
        etStock = findViewById(R.id.etStock);
        cbMorning = findViewById(R.id.cbMorning);
        cbAfternoon = findViewById(R.id.cbAfternoon);
        cbNight = findViewById(R.id.cbNight);
        btnSaveMedicine = findViewById(R.id.btnSaveMedicine);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add New Medicine");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        etMedicineTime.setOnClickListener(v -> showTimePicker());
        btnSaveMedicine.setOnClickListener(v -> saveMedicine());
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
            etMedicineTime.setText(time);
        }, hour, minute, false);
        timePickerDialog.show();
    }

    private void saveMedicine() {
        if (etMedName.getText() == null || etDosage.getText() == null || etMedicineTime.getText() == null || etStock.getText() == null) return;

        String name = etMedName.getText().toString().trim();
        String dosage = etDosage.getText().toString().trim();
        String medTime = etMedicineTime.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        List<String> selectedPeriods = new ArrayList<>();
        if (cbMorning.isChecked()) selectedPeriods.add("Morning");
        if (cbAfternoon.isChecked()) selectedPeriods.add("Afternoon");
        if (cbNight.isChecked()) selectedPeriods.add("Night");

        if (selectedPeriods.isEmpty()) {
            Toast.makeText(this, "Please select at least one time period", Toast.LENGTH_SHORT).show();
            return;
        }

        String period = TextUtils.join(", ", selectedPeriods);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(dosage) || TextUtils.isEmpty(medTime) || TextUtils.isEmpty(stockStr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int stock;
        try {
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid stock value", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        String medicineId = mDatabase.push().getKey();
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        if (userId.isEmpty() || medicineId == null) {
            Toast.makeText(this, "Error: Authentication or Database error", Toast.LENGTH_SHORT).show();
            setLoading(false);
            return;
        }

        Medicine medicine = new Medicine(medicineId, name, dosage, period, medTime, "", userId);
        medicine.setStockQuantity(stock);

        mDatabase.child(medicineId).setValue(medicine).addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Medicine Added", Toast.LENGTH_SHORT).show();
                AlarmHelper.scheduleMedicineAlarms(this, medicine);
                finish();
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "Error saving: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveMedicine.setEnabled(!isLoading);
    }
}
