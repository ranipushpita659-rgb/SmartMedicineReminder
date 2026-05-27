package com.example.smartmedicinereminder.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.example.smartmedicinereminder.R;
import com.example.smartmedicinereminder.utils.AlarmHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private MaterialSwitch switchDarkMode, switchNotifications;
    private RadioGroup rgRole;
    private RadioButton rbUser, rbCaregiver;
    private TextInputEditText etOldPassword, etNewPassword, etConfirmNewPassword;
    private TextInputLayout tilOldPassword, tilNewPassword, tilConfirmNewPassword;
    private MaterialButton btnUpdatePassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private SharedPreferences themePrefs, appPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        themePrefs = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        appPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        initViews();
        setupToolbar();
        setupThemePreference();
        setupNotificationPreference();
        setupRolePreference();
        setupListeners();
    }

    private void initViews() {
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        rgRole = findViewById(R.id.rgRole);
        rbUser = findViewById(R.id.rbUser);
        rbCaregiver = findViewById(R.id.rbCaregiver);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        tilOldPassword = findViewById(R.id.tilOldPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmNewPassword = findViewById(R.id.tilConfirmNewPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupThemePreference() {
        boolean isDarkMode = themePrefs.getBoolean("DarkMode", false);
        switchDarkMode.setChecked(isDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = themePrefs.edit();
            editor.putBoolean("DarkMode", isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    private void setupNotificationPreference() {
        boolean notificationsEnabled = appPrefs.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(notificationsEnabled);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = appPrefs.edit();
            editor.putBoolean("notifications_enabled", isChecked);
            editor.apply();
            
            if (isChecked) {
                AlarmHelper.rescheduleAllAlarms(this);
                Toast.makeText(this, "Reminders enabled", Toast.LENGTH_SHORT).show();
            } else {
                AlarmHelper.cancelAllAlarms(this);
                Toast.makeText(this, "Reminders disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRolePreference() {
        String role = appPrefs.getString("user_role", "user");
        if (role.equals("caregiver")) {
            rbCaregiver.setChecked(true);
        } else {
            rbUser.setChecked(true);
        }

        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = appPrefs.edit();
            if (checkedId == R.id.rbCaregiver) {
                editor.putString("user_role", "caregiver");
            } else {
                editor.putString("user_role", "user");
            }
            editor.apply();
            Toast.makeText(this, "Role updated. Please return to dashboard.", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners() {
        btnUpdatePassword.setOnClickListener(v -> attemptPasswordUpdate());
    }

    private void attemptPasswordUpdate() {
        if (etOldPassword.getText() == null || etNewPassword.getText() == null || etConfirmNewPassword.getText() == null) {
            return;
        }

        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmNewPassword.getText().toString().trim();

        boolean isValid = true;
        tilOldPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmNewPassword.setError(null);

        if (TextUtils.isEmpty(oldPass)) {
            tilOldPassword.setError(getString(R.string.error_field_required));
            isValid = false;
        }
        if (TextUtils.isEmpty(newPass)) {
            tilNewPassword.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (newPass.length() < 6) {
            tilNewPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }
        if (!newPass.equals(confirmPass)) {
            tilConfirmNewPassword.setError(getString(R.string.error_password_mismatch));
            isValid = false;
        }

        if (isValid) {
            updateFirebasePassword(oldPass, newPass);
        }
    }

    private void updateFirebasePassword(String oldPass, String newPass) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            setLoading(true);
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        setLoading(false);
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            clearFields();
                        } else {
                            String error = updateTask.getException() != null ? updateTask.getException().getMessage() : "Update failed";
                            Toast.makeText(SettingsActivity.this, "Update Failed: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    setLoading(false);
                    tilOldPassword.setError("Incorrect old password");
                    Toast.makeText(SettingsActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnUpdatePassword.setEnabled(!isLoading);
        etOldPassword.setEnabled(!isLoading);
        etNewPassword.setEnabled(!isLoading);
        etConfirmNewPassword.setEnabled(!isLoading);
    }

    private void clearFields() {
        etOldPassword.setText("");
        etNewPassword.setText("");
        etConfirmNewPassword.setText("");
    }
}
