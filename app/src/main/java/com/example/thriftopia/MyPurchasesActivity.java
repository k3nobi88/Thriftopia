package com.example.thriftopia;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MyPurchasesActivity extends AppCompatActivity {

    TextView btnBack, tvPurchaseSubtitle;
    LinearLayout purchasesContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    int currentLoadToken = 0;

    static class PurchaseData {
        String orderId;
        String itemId;
        String sellerId;
        String title;
        String category;
        String condition;
        String brand;
        String size;
        String imageUrl;
        String status;
        String purchaseType;
        double price;
        long createdAt;

        PurchaseData(String orderId, String itemId, String sellerId, String title,
                     String category, String condition, String brand, String size,
                     String imageUrl, String status, String purchaseType,
                     double price, long createdAt) {
            this.orderId = orderId;
            this.itemId = itemId;
            this.sellerId = sellerId;
            this.title = title;
            this.category = category;
            this.condition = condition;
            this.brand = brand;
            this.size = size;
            this.imageUrl = imageUrl;
            this.status = status;
            this.purchaseType = purchaseType;
            this.price = price;
            this.createdAt = createdAt;
        }
    }

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

        setContentView(R.layout.activity_my_purchases);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPurchases();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvPurchaseSubtitle = findViewById(R.id.tvPurchaseSubtitle);
        purchasesContainer = findViewById(R.id.purchasesContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadPurchases() {
        int loadToken = ++currentLoadToken;

        purchasesContainer.removeAllViews();
        showLoadingState();

        if (firebaseAuth.getCurrentUser() == null) {
            purchasesContainer.removeAllViews();
            tvPurchaseSubtitle.setText("0 purchase(s)");
            showEmptyState("Please login first", "Login to view your purchases.");
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("orders")
                .whereEqualTo("buyerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    purchasesContainer.removeAllViews();

                    ArrayList<PurchaseData> purchaseList = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String orderId = document.getString("orderId");

                        if (orderId == null || orderId.trim().isEmpty()) {
                            orderId = document.getId();
                        }

                        String itemId = safe(document.getString("itemId"));
                        String sellerId = safe(document.getString("sellerId"));
                        String title = safe(document.getString("title"));
                        String category = safe(document.getString("category"));
                        String condition = safe(document.getString("condition"));
                        String brand = safe(document.getString("brand"));
                        String size = safe(document.getString("size"));
                        String imageUrl = safe(document.getString("imageUrl"));
                        String status = safe(document.getString("status"));
                        String purchaseType = safe(document.getString("purchaseType"));

                        Double priceValue = document.getDouble("priceValue");

                        if (priceValue == null) {
                            priceValue = document.getDouble("price");
                        }

                        if (priceValue == null) {
                            priceValue = document.getDouble("winningBid");
                        }

                        if (priceValue == null) {
                            priceValue = document.getDouble("finalPrice");
                        }

                        double price = priceValue != null ? priceValue : parsePrice(document.getString("priceText"));

                        Long createdAtValue = document.getLong("createdAt");
                        long createdAt = createdAtValue != null ? createdAtValue : 0;

                        if (status.trim().isEmpty()) {
                            status = "purchased";
                        }

                        if (purchaseType.trim().isEmpty()) {
                            purchaseType = "buy_now";
                        }

                        purchaseList.add(new PurchaseData(
                                orderId,
                                itemId,
                                sellerId,
                                title,
                                category,
                                condition,
                                brand,
                                size,
                                imageUrl,
                                status,
                                purchaseType,
                                price,
                                createdAt
                        ));
                    }

                    purchaseList.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

                    if (purchaseList.isEmpty()) {
                        tvPurchaseSubtitle.setText("0 purchase(s)");
                        showEmptyState("No purchases yet", "Items you buy will appear here.");
                        return;
                    }

                    tvPurchaseSubtitle.setText(purchaseList.size() + " purchase(s)");

                    for (PurchaseData purchase : purchaseList) {
                        addPurchaseCard(purchase);
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    purchasesContainer.removeAllViews();
                    tvPurchaseSubtitle.setText("0 purchase(s)");
                    showEmptyState("Failed to load purchases", e.getMessage());
                });
    }

    private void showLoadingState() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(28), dp(18), dp(28));

        TextView title = new TextView(this);
        title.setText("Loading purchases...");
        title.setTextColor(getResources().getColor(R.color.text_primary_light));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Please wait while we fetch your orders.");
        subtitle.setTextColor(getResources().getColor(R.color.text_secondary_light));
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.setMargins(0, dp(6), 0, 0);
        subtitle.setLayoutParams(subParams);

        card.addView(subtitle);
        purchasesContainer.addView(card);
    }

    private void showEmptyState(String titleText, String subtitleText) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(22), dp(34), dp(22), dp(34));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(20), 0, 0);
        card.setLayoutParams(cardParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_cart);
        icon.setColorFilter(getResources().getColor(R.color.text_secondary_light));

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(58), dp(58));
        icon.setLayoutParams(iconParams);
        card.addView(icon);

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(getResources().getColor(R.color.text_primary_light));
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(16), 0, 0);
        title.setLayoutParams(titleParams);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(subtitleText != null ? subtitleText : "");
        subtitle.setTextColor(getResources().getColor(R.color.text_secondary_light));
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.setMargins(0, dp(8), 0, 0);
        subtitle.setLayoutParams(subParams);
        card.addView(subtitle);

        TextView btnExplore = new TextView(this);
        btnExplore.setText("EXPLORE ITEMS");
        btnExplore.setTextColor(getResources().getColor(R.color.text_primary_light));
        btnExplore.setTextSize(13);
        btnExplore.setTypeface(null, Typeface.BOLD);
        btnExplore.setGravity(Gravity.CENTER);
        btnExplore.setBackgroundResource(R.drawable.bg_primary_button);
        btnExplore.setClickable(true);
        btnExplore.setFocusable(true);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        btnParams.setMargins(0, dp(22), 0, 0);
        btnExplore.setLayoutParams(btnParams);

        btnExplore.setOnClickListener(v -> {
            Intent intent = new Intent(MyPurchasesActivity.this, ExploreActivity.class);
            startActivity(intent);
            finish();
        });

        card.addView(btnExplore);
        purchasesContainer.addView(card);
    }

    private void addPurchaseCard(PurchaseData purchase) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView itemImage = new ImageView(this);
        itemImage.setBackgroundResource(R.drawable.bg_hero);
        itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(96), dp(112));
        itemImage.setLayoutParams(imageParams);

        if (!purchase.imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(purchase.imageUrl)
                    .centerCrop()
                    .into(itemImage);
        }

        topRow.addView(itemImage);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        infoParams.setMargins(dp(14), 0, 0, 0);
        infoLayout.setLayoutParams(infoParams);

        TextView statusText = new TextView(this);
        statusText.setText(getStatusLabel(purchase));
        statusText.setTextColor(getResources().getColor(R.color.accent_lime));
        statusText.setTextSize(11);
        statusText.setTypeface(null, Typeface.BOLD);
        infoLayout.addView(statusText);

        TextView titleText = new TextView(this);
        titleText.setText(!purchase.title.trim().isEmpty() ? purchase.title : "Purchased Item");
        titleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setMaxLines(2);
        infoLayout.addView(titleText);

        TextView priceText = new TextView(this);
        priceText.setText("RM " + String.format(Locale.getDefault(), "%.2f", purchase.price));
        priceText.setTextColor(getResources().getColor(R.color.text_primary_light));
        priceText.setTextSize(15);
        priceText.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        priceParams.setMargins(0, dp(5), 0, 0);
        priceText.setLayoutParams(priceParams);
        infoLayout.addView(priceText);

        TextView detailText = new TextView(this);
        detailText.setText(valueOrDash(purchase.category) + " • " + valueOrDash(purchase.condition) + " • " + valueOrDash(purchase.size));
        detailText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        detailText.setTextSize(12);
        detailText.setMaxLines(2);

        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.setMargins(0, dp(6), 0, 0);
        detailText.setLayoutParams(detailParams);
        infoLayout.addView(detailText);

        TextView dateText = new TextView(this);
        dateText.setText("Bought on " + formatDate(purchase.createdAt));
        dateText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        dateText.setTextSize(12);

        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateParams.setMargins(0, dp(6), 0, 0);
        dateText.setLayoutParams(dateParams);
        infoLayout.addView(dateText);

        topRow.addView(infoLayout);
        card.addView(topRow);

        TextView btnViewReceipt = new TextView(this);
        btnViewReceipt.setText("VIEW RECEIPT");
        btnViewReceipt.setTextColor(getResources().getColor(R.color.text_primary_light));
        btnViewReceipt.setTextSize(13);
        btnViewReceipt.setTypeface(null, Typeface.BOLD);
        btnViewReceipt.setGravity(Gravity.CENTER);
        btnViewReceipt.setBackgroundResource(R.drawable.bg_primary_button);
        btnViewReceipt.setClickable(true);
        btnViewReceipt.setFocusable(true);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        buttonParams.setMargins(0, dp(14), 0, 0);
        btnViewReceipt.setLayoutParams(buttonParams);

        btnViewReceipt.setOnClickListener(v -> openOrderDetails(purchase.orderId));
        card.setOnClickListener(v -> openOrderDetails(purchase.orderId));

        card.addView(btnViewReceipt);
        purchasesContainer.addView(card);
    }

    private String getStatusLabel(PurchaseData purchase) {
        if (purchase.purchaseType.equalsIgnoreCase("auction")) {
            return "AUCTION WON";
        }

        if (purchase.purchaseType.equalsIgnoreCase("auction_win")) {
            return "AUCTION WON";
        }

        return "PURCHASED";
    }

    private void openOrderDetails(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MyPurchasesActivity.this, OrderDetailsActivity.class);
        intent.putExtra("orderId", orderId);
        startActivity(intent);
    }

    private String formatDate(long time) {
        if (time <= 0) {
            return "Unknown date";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }

    private double parsePrice(String priceText) {
        if (priceText == null || priceText.trim().isEmpty()) {
            return 0;
        }

        try {
            String cleaned = priceText
                    .replace("RM", "")
                    .replace("rm", "")
                    .replace(",", "")
                    .trim();

            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}