package com.example.smartmedicinereminder.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartmedicinereminder.MainActivity;
import com.example.smartmedicinereminder.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

/**
 * LoginActivity handles user authentication using Firebase Authentication.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private TextInputLayout tilUsername, tilPassword;
    private Button btnLogin;
    private TextView tvSignUp;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean isValid = true;

        tilUsername.setError(null);
        tilPassword.setError(null);

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password cannot be empty");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            tilUsername.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilUsername.setError("Please enter a valid email address");
            isValid = false;
        }

        if (isValid) {
            performFirebaseLogin(email, password);
        }
    }

    private void performFirebaseLogin(String email, String password) {
        btnLogin.setEnabled(false);
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Authentication Failed";
                        Toast.makeText(LoginActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}