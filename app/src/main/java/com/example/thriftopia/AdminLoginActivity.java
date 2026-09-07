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

public class AdminLoginActivity extends AppCompatActivity {

    TextView btnBack, btnAdminLogin, tvAdminHint;
    EditText etAdminEmail, etAdminPassword;

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

        setContentView(R.layout.activity_admin_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnAdminLogin = findViewById(R.id.btnAdminLogin);
        tvAdminHint = findViewById(R.id.tvAdminHint);

        etAdminEmail = findViewById(R.id.etAdminEmail);
        etAdminPassword = findViewById(R.id.etAdminPassword);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        btnAdminLogin.setOnClickListener(v -> loginAdmin());
    }

    private void loginAdmin() {
        String email = etAdminEmail.getText().toString().trim();
        String password = etAdminPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etAdminEmail.setError("Admin email is required");
            etAdminEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etAdminPassword.setError("Password is required");
            etAdminPassword.requestFocus();
            return;
        }

        btnAdminLogin.setEnabled(false);
        btnAdminLogin.setText("CHECKING...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (firebaseAuth.getCurrentUser() == null) {
                        resetButton();
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = firebaseAuth.getCurrentUser().getUid();
                    checkAdminRole(uid, email);
                })
                .addOnFailureListener(e -> {
                    resetButton();
                    Toast.makeText(this, "Admin login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkAdminRole(String uid, String email) {
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        firebaseAuth.signOut();
                        resetButton();
                        Toast.makeText(this, "Admin profile not found in Firestore", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String role = documentSnapshot.getString("role");

                    if (role != null && role.equalsIgnoreCase("admin")) {
                        Toast.makeText(this, "Admin login successful", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                        intent.putExtra("adminEmail", email);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        firebaseAuth.signOut();
                        resetButton();
                        Toast.makeText(this, "Access denied. This account is not admin.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    firebaseAuth.signOut();
                    resetButton();
                    Toast.makeText(this, "Failed to check admin role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetButton() {
        btnAdminLogin.setEnabled(true);
        btnAdminLogin.setText("LOGIN AS ADMIN");
    }
}