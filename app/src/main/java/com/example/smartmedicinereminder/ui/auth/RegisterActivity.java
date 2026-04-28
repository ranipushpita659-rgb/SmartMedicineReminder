package com.example.smartmedicinereminder.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartmedicinereminder.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * RegisterActivity handles new user registration using Firebase Authentication.
 * It also stores user profile information in Firebase Realtime Database.
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword, etConfirmPassword;
    private TextInputLayout tilUsername, tilPassword, tilConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // We should add a ProgressBar to activity_register.xml for better UX
        // For now, I'll just use a Toast to show progress or assume it exists.
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegistration());

        tvLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void attemptRegistration() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean isValid = true;

        // Reset errors
        tilUsername.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        // Validate username (Firebase Auth requires an email, so we treat username as email)
        if (TextUtils.isEmpty(username)) {
            tilUsername.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            tilUsername.setError("Please enter a valid email address for Firebase registration");
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            isValid = false;
        }

        if (isValid) {
            performFirebaseRegistration(username, password);
        }
    }

    private void performFirebaseRegistration(String email, String password) {
        // Show progress
        btnRegister.setEnabled(false);
        Toast.makeText(this, "Creating Account...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration success
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToDatabase(userId, email);
                    } else {
                        // Registration failed
                        btnRegister.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration Failed";
                        Toast.makeText(RegisterActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("role", "primary_user"); // Default role

        mDatabase.child("users").child(userId).setValue(userMap)
                .addOnCompleteListener(task -> {
                    btnRegister.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}