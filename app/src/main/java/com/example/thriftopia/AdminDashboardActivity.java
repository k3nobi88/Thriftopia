package com.example.thriftopia;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    TextView tvAdminEmail;

    TextView tvTotalUsers, tvTotalListings, tvActiveListings, tvSoldItems;
    TextView tvLiveAuctions, tvTotalOrders, tvTotalRevenue;

    TextView btnRefresh, menuManageUsersListings, menuAnalytics, btnLogout;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    int totalUsers = 0;
    int totalListings = 0;
    int activeListings = 0;
    int soldItems = 0;
    int liveAuctions = 0;
    int totalOrders = 0;
    double totalRevenue = 0;

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

        setContentView(R.layout.activity_admin_dashboard);

        TextView btnAdminReports = findViewById(R.id.btnAdminReports);

        if (btnAdminReports != null) {
            btnAdminReports.setOnClickListener(v ->
                    startActivity(new Intent(AdminDashboardActivity.this, AdminReportsActivity.class))
            );
        }

        TextView btnAuditLogs = findViewById(R.id.btnAuditLogs);

        if (btnAuditLogs != null) {
            btnAuditLogs.setOnClickListener(v ->
                    startActivity(new Intent(AdminDashboardActivity.this, AdminAuditLogActivity.class))
            );
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();
        verifyAdminAccess();
        loadDashboardStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAdminInfo();
        loadDashboardStats();
    }

    private void bindViews() {
        tvAdminEmail = findViewById(R.id.tvAdminEmail);

        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalListings = findViewById(R.id.tvTotalListings);
        tvActiveListings = findViewById(R.id.tvActiveListings);
        tvSoldItems = findViewById(R.id.tvSoldItems);
        tvLiveAuctions = findViewById(R.id.tvLiveAuctions);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);

        btnRefresh = findViewById(R.id.btnRefresh);
        menuManageUsersListings = findViewById(R.id.menuManageUsersListings);
        menuAnalytics = findViewById(R.id.menuAnalytics);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupClicks() {
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing dashboard...", Toast.LENGTH_SHORT).show();
            loadDashboardStats();
        });

        menuManageUsersListings.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageUsersListingsActivity.class));
        });

        menuAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AnalyticsDashboardActivity.class);
            intent.putExtra("analyticsMode", "admin");
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void verifyAdminAccess() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login as admin first", Toast.LENGTH_LONG).show();
            goToLogin();
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        firebaseAuth.signOut();
                        Toast.makeText(this, "Admin profile not found", Toast.LENGTH_LONG).show();
                        goToLogin();
                        return;
                    }

                    String role = documentSnapshot.getString("role");

                    if (role == null || !role.equalsIgnoreCase("admin")) {
                        firebaseAuth.signOut();
                        Toast.makeText(this, "Access denied. Admin only.", Toast.LENGTH_LONG).show();
                        goToLogin();
                        return;
                    }

                    loadAdminInfo();
                })
                .addOnFailureListener(e -> {
                    firebaseAuth.signOut();
                    Toast.makeText(this, "Failed to verify admin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    goToLogin();
                });
    }

    private void loadAdminInfo() {
        String emailFromIntent = getIntent().getStringExtra("adminEmail");

        if (emailFromIntent != null && !emailFromIntent.trim().isEmpty()) {
            tvAdminEmail.setText("Admin: " + emailFromIntent);
            return;
        }

        if (firebaseAuth.getCurrentUser() != null) {
            String email = firebaseAuth.getCurrentUser().getEmail();

            if (email != null && !email.trim().isEmpty()) {
                tvAdminEmail.setText("Admin: " + email);
                return;
            }
        }

        tvAdminEmail.setText("Admin Mode");
    }

    private void resetStats() {
        totalUsers = 0;
        totalListings = 0;
        activeListings = 0;
        soldItems = 0;
        liveAuctions = 0;
        totalOrders = 0;
        totalRevenue = 0;

        renderStats();
    }

    private void loadDashboardStats() {
        resetStats();

        loadUserCount();
        loadListingStats();
        loadAuctionStats();
        loadOrderStats();
    }

    private void loadUserCount() {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalUsers = queryDocumentSnapshots.size();
                    renderStats();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load users count", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadListingStats() {
        firestore.collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalListings = queryDocumentSnapshots.size();
                    activeListings = 0;
                    soldItems = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");

                        if (status != null && status.equalsIgnoreCase("sold")) {
                            soldItems++;
                        } else {
                            activeListings++;
                        }
                    }

                    renderStats();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load listings count", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAuctionStats() {
        firestore.collection("auctions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    liveAuctions = 0;
                    long now = System.currentTimeMillis();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");

                        Long endAtValue = document.getLong("endAt");
                        if (endAtValue == null) {
                            endAtValue = document.getLong("endTime");
                        }

                        long endAt = endAtValue != null ? endAtValue : 0;

                        if (status != null && status.equalsIgnoreCase("live") && endAt > now) {
                            liveAuctions++;
                        }
                    }

                    renderStats();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load auctions count", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadOrderStats() {
        firestore.collection("orders")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalOrders = queryDocumentSnapshots.size();
                    totalRevenue = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double priceValue = document.getDouble("priceValue");

                        if (priceValue == null) {
                            priceValue = document.getDouble("price");
                        }

                        if (priceValue != null) {
                            totalRevenue += priceValue;
                        }
                    }

                    renderStats();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load orders count", Toast.LENGTH_SHORT).show();
                });
    }

    private void renderStats() {
        tvTotalUsers.setText(String.valueOf(totalUsers));
        tvTotalListings.setText(String.valueOf(totalListings));
        tvActiveListings.setText(String.valueOf(activeListings));
        tvSoldItems.setText(String.valueOf(soldItems));
        tvLiveAuctions.setText(String.valueOf(liveAuctions));
        tvTotalOrders.setText(String.valueOf(totalOrders));
        tvTotalRevenue.setText("RM " + String.format(Locale.getDefault(), "%.2f", totalRevenue));
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout Admin");
        builder.setMessage("Are you sure you want to logout admin mode?");

        builder.setPositiveButton("Logout", (dialog, which) -> {
            firebaseAuth.signOut();
            goToLogin();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void goToLogin() {
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}