package com.example.thriftopia;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText etFullName, etRegisterEmail, etRegisterPassword, etConfirmPassword;
    TextView btnRegister, tvGoLogin;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupSystemBars();

        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etFullName = findViewById(R.id.etFullName);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        btnRegister.setOnClickListener(v -> registerUser());

        tvGoLogin.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupSystemBars();
    }

    private void setupSystemBars() {
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_light));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.bg_light));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean isDarkMode = (getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

            if (isDarkMode) {
                getWindow().getDecorView().setSystemUiVisibility(0);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etRegisterEmail.setError("Email is required");
            etRegisterEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etRegisterPassword.setError("Password is required");
            etRegisterPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etRegisterPassword.setError("Password must be at least 6 characters");
            etRegisterPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Password does not match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("REGISTERING...");

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        if (firebaseAuth.getCurrentUser() == null) {
                            btnRegister.setEnabled(true);
                            btnRegister.setText("REGISTER");
                            Toast.makeText(RegisterActivity.this, "User session not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String userId = firebaseAuth.getCurrentUser().getUid();

                        Map<String, Object> user = new HashMap<>();
                        user.put("userId", userId);
                        user.put("fullName", fullName);
                        user.put("email", email);
                        user.put("role", "user");
                        user.put("phone", "");
                        user.put("profileImage", "");
                        user.put("profileImageUrl", "");
                        user.put("isSuspended", false);
                        user.put("status", "active");
                        user.put("ratingAverage", 0.0);
                        user.put("ratingCount", 0);
                        user.put("createdAt", System.currentTimeMillis());

                        firestore.collection("users")
                                .document(userId)
                                .set(user)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(RegisterActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    btnRegister.setEnabled(true);
                                    btnRegister.setText("REGISTER");
                                    Toast.makeText(RegisterActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("REGISTER");

                        String message = "Register failed";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            message = "Register failed: " + task.getException().getMessage();
                        }

                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }
}