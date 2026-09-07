package com.example.thriftopia;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AnalyticsDashboardActivity extends AppCompatActivity {

    TextView btnBack, btnRefreshAnalytics;
    TextView tvAnalyticsTitle, tvAnalyticsSubtitle;

    TextView tvTotalListings, tvActiveListings, tvTotalValue, tvSavedItems;
    TextView tvTotalListingsLabel, tvActiveListingsLabel, tvTotalValueLabel, tvSavedItemsLabel;

    TextView tvChartTitle, tvSummaryTitle, tvPerformanceSummary;
    LinearLayout categoryChartContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String analyticsMode = "seller";
    String currentUserId = "";

    int totalListings = 0;
    int activeListings = 0;
    int soldListings = 0;
    int savedItems = 0;
    int totalUsers = 0;
    int totalOrders = 0;
    int liveAuctions = 0;

    double totalValue = 0;
    double totalRevenue = 0;

    Map<String, Integer> categoryCounts = new HashMap<>();

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

        setContentView(R.layout.activity_analytics_dashboard);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        String modeFromIntent = getIntent().getStringExtra("analyticsMode");

        if (modeFromIntent != null && modeFromIntent.equalsIgnoreCase("admin")) {
            analyticsMode = "admin";
        } else {
            analyticsMode = "seller";
        }

        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }

        bindViews();
        setupClicks();
        setupModeUi();
        loadAnalytics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAnalytics();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnRefreshAnalytics = findViewById(R.id.btnRefreshAnalytics);

        tvAnalyticsTitle = findViewById(R.id.tvAnalyticsTitle);
        tvAnalyticsSubtitle = findViewById(R.id.tvAnalyticsSubtitle);

        tvTotalListings = findViewById(R.id.tvTotalListings);
        tvActiveListings = findViewById(R.id.tvActiveListings);
        tvTotalValue = findViewById(R.id.tvTotalValue);
        tvSavedItems = findViewById(R.id.tvSavedItems);

        tvTotalListingsLabel = findViewById(R.id.tvTotalListingsLabel);
        tvActiveListingsLabel = findViewById(R.id.tvActiveListingsLabel);
        tvTotalValueLabel = findViewById(R.id.tvTotalValueLabel);
        tvSavedItemsLabel = findViewById(R.id.tvSavedItemsLabel);

        tvChartTitle = findViewById(R.id.tvChartTitle);
        tvSummaryTitle = findViewById(R.id.tvSummaryTitle);
        tvPerformanceSummary = findViewById(R.id.tvPerformanceSummary);

        categoryChartContainer = findViewById(R.id.categoryChartContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        btnRefreshAnalytics.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing analytics...", Toast.LENGTH_SHORT).show();
            loadAnalytics();
        });
    }

    private void setupModeUi() {
        if (analyticsMode.equals("admin")) {
            tvAnalyticsTitle.setText("Admin Analytics");
            tvAnalyticsSubtitle.setText("Overall platform analytics based on Firestore data.");

            tvTotalListingsLabel.setText("Total Listings");
            tvActiveListingsLabel.setText("Active Listings");
            tvTotalValueLabel.setText("Total Revenue");
            tvSavedItemsLabel.setText("Total Users");

            tvChartTitle.setText("Platform Listings by Category");
            tvSummaryTitle.setText("Platform Summary");
        } else {
            tvAnalyticsTitle.setText("Seller Analytics");
            tvAnalyticsSubtitle.setText("Your seller statistics based on Firestore data.");

            tvTotalListingsLabel.setText("Total Listings");
            tvActiveListingsLabel.setText("Active Listings");
            tvTotalValueLabel.setText("Total Value");
            tvSavedItemsLabel.setText("Saved Items");

            tvChartTitle.setText("Your Listings by Category");
            tvSummaryTitle.setText("Performance Summary");
        }
    }

    private void resetValues() {
        totalListings = 0;
        activeListings = 0;
        soldListings = 0;
        savedItems = 0;
        totalUsers = 0;
        totalOrders = 0;
        liveAuctions = 0;

        totalValue = 0;
        totalRevenue = 0;

        categoryCounts.clear();

        renderLoading();
    }

    private void renderLoading() {
        tvTotalListings.setText("0");
        tvActiveListings.setText("0");
        tvTotalValue.setText("RM0.00");
        tvSavedItems.setText("0");

        categoryChartContainer.removeAllViews();

        TextView loadingText = new TextView(this);
        loadingText.setText("Loading chart...");
        loadingText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        loadingText.setTextSize(14);
        categoryChartContainer.addView(loadingText);

        tvPerformanceSummary.setText("Loading analytics summary...");
    }

    private void loadAnalytics() {
        resetValues();

        if (analyticsMode.equals("admin")) {
            loadAdminItems();
            loadAdminUsers();
            loadAdminOrders();
            loadAdminAuctions();
        } else {
            if (currentUserId == null || currentUserId.trim().isEmpty()) {
                tvPerformanceSummary.setText("Please login first to view seller analytics.");
                return;
            }

            loadSellerItems();
            loadSellerWatchlist();
        }
    }

    private void loadAdminItems() {
        firestore.collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalListings = queryDocumentSnapshots.size();
                    activeListings = 0;
                    soldListings = 0;
                    totalValue = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        String category = document.getString("category");

                        Double price = document.getDouble("price");

                        if (price == null) {
                            price = document.getDouble("priceValue");
                        }

                        if (status != null && status.equalsIgnoreCase("sold")) {
                            soldListings++;
                        } else {
                            activeListings++;
                        }

                        if (price != null) {
                            totalValue += price;
                        }

                        addCategory(category);
                    }

                    renderAnalytics();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load items", Toast.LENGTH_SHORT).show();
                    renderAnalytics();
                });
    }

    private void loadAdminUsers() {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalUsers = queryDocumentSnapshots.size();
                    renderAnalytics();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    renderAnalytics();
                });
    }

    private void loadAdminOrders() {
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

                    renderAnalytics();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                    renderAnalytics();
                });
    }

    private void loadAdminAuctions() {
        firestore.collection("auctions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    liveAuctions = 0;
                    long now = System.currentTimeMillis();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");

                        Long endAt = document.getLong("endAt");

                        if (endAt == null) {
                            endAt = document.getLong("endTime");
                        }

                        long endTime = endAt != null ? endAt : 0;

                        if (status != null && status.equalsIgnoreCase("live") && endTime > now) {
                            liveAuctions++;
                        }
                    }

                    renderAnalytics();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load auctions", Toast.LENGTH_SHORT).show();
                    renderAnalytics();
                });
    }

    private void loadSellerItems() {
        firestore.collection("items")
                .whereEqualTo("sellerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalListings = queryDocumentSnapshots.size();
                    activeListings = 0;
                    soldListings = 0;
                    totalValue = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        String category = document.getString("category");

                        Double price = document.getDouble("price");

                        if (price == null) {
                            price = document.getDouble("priceValue");
                        }

                        if (status != null && status.equalsIgnoreCase("sold")) {
                            soldListings++;
                        } else {
                            activeListings++;
                        }

                        if (price != null) {
                            totalValue += price;
                        }

                        addCategory(category);
                    }

                    renderAnalytics();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load seller items", Toast.LENGTH_SHORT).show();
                    renderAnalytics();
                });
    }

    private void loadSellerWatchlist() {
        firestore.collection("watchlist")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savedItems = queryDocumentSnapshots.size();
                    renderAnalytics();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load saved items", Toast.LENGTH_SHORT).show();
                    renderAnalytics();
                });
    }

    private void addCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            category = "Others";
        }

        Integer current = categoryCounts.get(category);

        if (current == null) {
            categoryCounts.put(category, 1);
        } else {
            categoryCounts.put(category, current + 1);
        }
    }

    private void renderAnalytics() {
        if (analyticsMode.equals("admin")) {
            tvTotalListings.setText(String.valueOf(totalListings));
            tvActiveListings.setText(String.valueOf(activeListings));
            tvTotalValue.setText("RM" + String.format(Locale.getDefault(), "%.2f", totalRevenue));
            tvSavedItems.setText(String.valueOf(totalUsers));

            renderCategoryChart();
            renderAdminSummary();
        } else {
            tvTotalListings.setText(String.valueOf(totalListings));
            tvActiveListings.setText(String.valueOf(activeListings));
            tvTotalValue.setText("RM" + String.format(Locale.getDefault(), "%.2f", totalValue));
            tvSavedItems.setText(String.valueOf(savedItems));

            renderCategoryChart();
            renderSellerSummary();
        }
    }

    private void renderCategoryChart() {
        categoryChartContainer.removeAllViews();

        if (categoryCounts.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No category data yet.");
            emptyText.setTextColor(getResources().getColor(R.color.text_secondary_light));
            emptyText.setTextSize(14);
            categoryChartContainer.addView(emptyText);
            return;
        }

        int max = 1;

        for (Integer count : categoryCounts.values()) {
            if (count != null && count > max) {
                max = count;
            }
        }

        for (String category : categoryCounts.keySet()) {
            int count = categoryCounts.get(category) != null ? categoryCounts.get(category) : 0;
            addCategoryBar(category, count, max);
        }
    }

    private void addCategoryBar(String category, int count, int max) {
        TextView labelText = new TextView(this);
        labelText.setText(category + " (" + count + ")");
        labelText.setTextColor(getResources().getColor(R.color.text_primary_light));
        labelText.setTextSize(14);
        labelText.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(0, 0, 0, dp(6));
        labelText.setLayoutParams(labelParams);

        categoryChartContainer.addView(labelText);

        LinearLayout barBackground = new LinearLayout(this);
        barBackground.setOrientation(LinearLayout.HORIZONTAL);
        barBackground.setBackgroundResource(R.drawable.bg_input);

        LinearLayout.LayoutParams bgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(22)
        );
        bgParams.setMargins(0, 0, 0, dp(14));
        barBackground.setLayoutParams(bgParams);

        TextView barFill = new TextView(this);
        barFill.setText("");
        barFill.setBackgroundResource(R.drawable.bg_primary_button);

        LinearLayout.LayoutParams fillParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                count
        );
        barFill.setLayoutParams(fillParams);

        TextView emptyFill = new TextView(this);

        LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(max - count, 0)
        );
        emptyFill.setLayoutParams(emptyParams);

        barBackground.addView(barFill);
        barBackground.addView(emptyFill);

        categoryChartContainer.addView(barBackground);
    }

    private void renderAdminSummary() {
        String summary =
                "Platform currently has " + totalUsers + " registered user(s), "
                        + totalListings + " listing(s), "
                        + activeListings + " active item(s), "
                        + soldListings + " sold item(s), "
                        + liveAuctions + " live auction(s), "
                        + totalOrders + " order(s), and RM"
                        + String.format(Locale.getDefault(), "%.2f", totalRevenue)
                        + " total sales value.";

        tvPerformanceSummary.setText(summary);
    }

    private void renderSellerSummary() {
        String summary =
                "You currently have " + totalListings + " listing(s), "
                        + activeListings + " active item(s), "
                        + soldListings + " sold item(s), and "
                        + savedItems + " saved item(s). Total listed value is RM"
                        + String.format(Locale.getDefault(), "%.2f", totalValue)
                        + ".";

        tvPerformanceSummary.setText(summary);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}