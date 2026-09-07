package com.example.thriftopia;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    TextView btnLogin, tvRegister, tvAdminLogin;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_light));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.bg_light));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();
    }

    private void bindViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvAdminLogin = findViewById(R.id.tvAdminLogin);
    }

    private void setupClicks() {
        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        tvAdminLogin.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, AdminLoginActivity.class))
        );
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("CHECKING...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (firebaseAuth.getCurrentUser() == null) {
                        resetButton();
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = firebaseAuth.getCurrentUser().getUid();
                    checkRoleAfterLogin(uid, email);
                })
                .addOnFailureListener(e -> {
                    resetButton();
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkRoleAfterLogin(String uid, String email) {
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    resetButton();

                    if (!documentSnapshot.exists()) {
                        firebaseAuth.signOut();
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Boolean isSuspendedValue = documentSnapshot.getBoolean("isSuspended");
                    String status = documentSnapshot.getString("status");

                    boolean isSuspended = (isSuspendedValue != null && isSuspendedValue)
                            || (status != null && status.equalsIgnoreCase("suspended"));

                    if (isSuspended) {
                        firebaseAuth.signOut();
                        Toast.makeText(this, "Your account has been suspended by admin.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String role = documentSnapshot.getString("role");

                    if (role != null && role.equalsIgnoreCase("admin")) {
                        Toast.makeText(this, "Admin account detected. Redirecting to Admin Dashboard.", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        intent.putExtra("adminEmail", email);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    firebaseAuth.signOut();
                    resetButton();
                    Toast.makeText(this, "Failed to check user role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("LOGIN");
    }
}