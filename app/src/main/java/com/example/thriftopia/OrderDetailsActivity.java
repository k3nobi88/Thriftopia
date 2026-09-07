package com.example.thriftopia;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OrderDetailsActivity extends AppCompatActivity {

    TextView btnBack, btnRateSeller, btnViewItem;
    TextView tvOrderTitle, tvOrderPrice, tvOrderId, tvPurchaseType, tvPurchaseDate, tvOrderStatus;
    TextView tvPaymentStatus, tvPaymentMethod, tvTransactionId, tvPaidAt;
    TextView tvSubtotal, tvServiceFee, tvPaymentFee, tvTotalPayment;
    TextView tvOrderCategory, tvOrderCondition, tvOrderBrand, tvOrderSize;
    TextView tvBuyerName, tvSellerName, tvRatingStatus;
    ImageView imgOrderItem;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String orderId = "";
    String itemId = "";
    String buyerId = "";
    String sellerId = "";
    String currentUserId = "";

    int selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupSystemBars();

        setContentView(R.layout.activity_order_details);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        orderId = getIntent().getStringExtra("orderId");

        if (orderId == null || orderId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(OrderDetailsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        currentUserId = firebaseAuth.getCurrentUser().getUid();

        bindViews();
        setupClicks();
        loadOrderDetails();
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

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnRateSeller = findViewById(R.id.btnRateSeller);
        btnViewItem = findViewById(R.id.btnViewItem);

        imgOrderItem = findViewById(R.id.imgOrderItem);

        tvOrderTitle = findViewById(R.id.tvOrderTitle);
        tvOrderPrice = findViewById(R.id.tvOrderPrice);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvPurchaseType = findViewById(R.id.tvPurchaseType);
        tvPurchaseDate = findViewById(R.id.tvPurchaseDate);
        tvOrderStatus = findViewById(R.id.tvOrderStatus);

        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvTransactionId = findViewById(R.id.tvTransactionId);
        tvPaidAt = findViewById(R.id.tvPaidAt);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvServiceFee = findViewById(R.id.tvServiceFee);
        tvPaymentFee = findViewById(R.id.tvPaymentFee);
        tvTotalPayment = findViewById(R.id.tvTotalPayment);

        tvOrderCategory = findViewById(R.id.tvOrderCategory);
        tvOrderCondition = findViewById(R.id.tvOrderCondition);
        tvOrderBrand = findViewById(R.id.tvOrderBrand);
        tvOrderSize = findViewById(R.id.tvOrderSize);

        tvBuyerName = findViewById(R.id.tvBuyerName);
        tvSellerName = findViewById(R.id.tvSellerName);
        tvRatingStatus = findViewById(R.id.tvRatingStatus);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        btnRateSeller.setOnClickListener(v -> showRatingDialog());

        btnViewItem.setOnClickListener(v -> {
            if (itemId == null || itemId.trim().isEmpty()) {
                Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(OrderDetailsActivity.this, ItemDetailsActivity.class);
            intent.putExtra("itemId", itemId);
            startActivity(intent);
        });
    }

    private void loadOrderDetails() {
        firestore.collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    itemId = documentSnapshot.getString("itemId");
                    buyerId = documentSnapshot.getString("buyerId");
                    sellerId = documentSnapshot.getString("sellerId");

                    String title = documentSnapshot.getString("title");
                    String category = documentSnapshot.getString("category");
                    String condition = documentSnapshot.getString("condition");
                    String brand = documentSnapshot.getString("brand");
                    String size = documentSnapshot.getString("size");
                    String imageUrl = documentSnapshot.getString("imageUrl");
                    String status = documentSnapshot.getString("status");
                    String purchaseType = documentSnapshot.getString("purchaseType");

                    String paymentStatus = documentSnapshot.getString("paymentStatus");
                    String paymentMethod = documentSnapshot.getString("paymentMethod");
                    String transactionId = documentSnapshot.getString("transactionId");
                    Long paidAt = documentSnapshot.getLong("paidAt");
                    Double subtotal = documentSnapshot.getDouble("subtotal");
                    Double serviceFee = documentSnapshot.getDouble("serviceFee");
                    Double paymentFee = documentSnapshot.getDouble("paymentFee");
                    Double totalPayment = documentSnapshot.getDouble("totalPayment");

                    Double priceValue = documentSnapshot.getDouble("priceValue");
                    String priceText = documentSnapshot.getString("priceText");

                    Long createdAt = documentSnapshot.getLong("createdAt");

                    tvOrderTitle.setText(title != null && !title.trim().isEmpty() ? title : "Purchased Item");

                    if (priceValue != null) {
                        tvOrderPrice.setText("RM " + String.format(Locale.getDefault(), "%.2f", priceValue));
                    } else if (priceText != null && !priceText.trim().isEmpty()) {
                        tvOrderPrice.setText(priceText);
                    } else {
                        tvOrderPrice.setText("RM 0.00");
                    }

                    tvOrderId.setText("Order ID: " + orderId);

                    if (purchaseType == null || purchaseType.trim().isEmpty()) {
                        String auctionId = documentSnapshot.getString("auctionId");
                        purchaseType = auctionId != null && !auctionId.trim().isEmpty() ? "auction" : "buy_now";
                    }

                    tvPurchaseType.setText("Purchase Type: " + formatPurchaseType(purchaseType));
                    tvPurchaseDate.setText("Purchase Date: " + formatDate(createdAt));
                    tvOrderStatus.setText("Status: " + formatStatus(status));

                    tvPaymentStatus.setText("Payment Status: " + formatPaymentStatus(paymentStatus));
                    tvPaymentMethod.setText("Payment Method: " + safeText(paymentMethod));
                    tvTransactionId.setText("Transaction ID: " + safeText(transactionId));
                    tvPaidAt.setText("Paid At: " + formatDate(paidAt));
                    setPaymentCalculationTexts(subtotal, serviceFee, paymentFee, totalPayment, priceValue);

                    tvOrderCategory.setText("Category: " + safeText(category));
                    tvOrderCondition.setText("Condition: " + safeText(condition));
                    tvOrderBrand.setText("Brand: " + safeText(brand));
                    tvOrderSize.setText("Size: " + safeText(size));

                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        Glide.with(this)
                                .load(imageUrl)
                                .centerCrop()
                                .into(imgOrderItem);
                    }

                    loadUserName(buyerId, tvBuyerName, "Buyer");
                    loadUserName(sellerId, tvSellerName, "Seller");

                    setupRatingButtonVisibility();
                    checkExistingRating();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void setupRatingButtonVisibility() {
        if (buyerId == null || sellerId == null) {
            btnRateSeller.setVisibility(View.GONE);
            return;
        }

        if (!currentUserId.equals(buyerId)) {
            btnRateSeller.setVisibility(View.GONE);
            tvRatingStatus.setText("Rating: Only buyer can rate seller");
            return;
        }

        if (currentUserId.equals(sellerId)) {
            btnRateSeller.setVisibility(View.GONE);
            tvRatingStatus.setText("Rating: Seller cannot rate own sale");
            return;
        }

        btnRateSeller.setVisibility(View.VISIBLE);
    }

    private void checkExistingRating() {
        firestore.collection("ratings")
                .document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long ratingValue = documentSnapshot.getLong("rating");
                        String reviewText = documentSnapshot.getString("reviewText");

                        String ratingLine = "Rating: " + buildStars(ratingValue != null ? ratingValue.intValue() : 0);

                        if (reviewText != null && !reviewText.trim().isEmpty()) {
                            ratingLine = ratingLine + " - " + reviewText;
                        }

                        tvRatingStatus.setText(ratingLine);
                        btnRateSeller.setVisibility(View.GONE);
                    } else {
                        if (btnRateSeller.getVisibility() == View.VISIBLE) {
                            tvRatingStatus.setText("Rating: Not rated yet");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvRatingStatus.setText("Rating: Unable to check rating");
                });
    }

    private void showRatingDialog() {
        selectedRating = 0;

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(18), dp(10), dp(18), 0);

        TextView titleText = new TextView(this);
        titleText.setText("How was the seller?");
        titleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        titleText.setTextSize(18);
        titleText.setTypeface(null, Typeface.BOLD);
        dialogLayout.addView(titleText);

        TextView hintText = new TextView(this);
        hintText.setText("Tap a star to rate this seller.");
        hintText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        hintText.setTextSize(13);

        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.setMargins(0, dp(8), 0, 0);
        hintText.setLayoutParams(hintParams);
        dialogLayout.addView(hintText);

        LinearLayout starsLayout = new LinearLayout(this);
        starsLayout.setOrientation(LinearLayout.HORIZONTAL);
        starsLayout.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams starsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        starsParams.setMargins(0, dp(16), 0, dp(10));
        starsLayout.setLayoutParams(starsParams);

        TextView[] starViews = new TextView[5];

        for (int i = 0; i < 5; i++) {
            final int rating = i + 1;

            TextView star = new TextView(this);
            star.setText("☆");
            star.setTextSize(38);
            star.setGravity(Gravity.CENTER);
            star.setTextColor(getResources().getColor(R.color.text_secondary_light));
            star.setPadding(dp(4), 0, dp(4), 0);

            LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );
            star.setLayoutParams(starParams);

            star.setOnClickListener(v -> {
                selectedRating = rating;
                updateStarViews(starViews, selectedRating);
            });

            starViews[i] = star;
            starsLayout.addView(star);
        }

        dialogLayout.addView(starsLayout);

        EditText reviewInput = new EditText(this);
        reviewInput.setBackgroundResource(R.drawable.bg_input);
        reviewInput.setHint("Optional review");
        reviewInput.setMinLines(3);
        reviewInput.setGravity(Gravity.TOP);
        reviewInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        reviewInput.setTextColor(getResources().getColor(R.color.text_primary_light));
        reviewInput.setHintTextColor(getResources().getColor(R.color.text_secondary_light));

        LinearLayout.LayoutParams reviewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(110)
        );
        reviewParams.setMargins(0, dp(10), 0, 0);
        reviewInput.setLayoutParams(reviewParams);
        dialogLayout.addView(reviewInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogLayout)
                .setPositiveButton("Submit", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String reviewText = reviewInput.getText().toString().trim();

                if (selectedRating <= 0) {
                    Toast.makeText(this, "Please select rating", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                submitRating(selectedRating, reviewText, dialog);
            });
        });

        dialog.show();
    }

    private void updateStarViews(TextView[] starViews, int rating) {
        for (int i = 0; i < starViews.length; i++) {
            if (i < rating) {
                starViews[i].setText("★");
                starViews[i].setTextColor(getResources().getColor(R.color.accent_lime));
            } else {
                starViews[i].setText("☆");
                starViews[i].setTextColor(getResources().getColor(R.color.text_secondary_light));
            }
        }
    }

    private void submitRating(int rating, String reviewText, AlertDialog dialog) {
        firestore.collection("ratings")
                .document(orderId)
                .get()
                .addOnSuccessListener(existingRating -> {
                    if (existingRating.exists()) {
                        Toast.makeText(this, "You already rated this seller", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        checkExistingRating();
                        return;
                    }

                    Map<String, Object> ratingData = new HashMap<>();
                    ratingData.put("orderId", orderId);
                    ratingData.put("itemId", itemId);
                    ratingData.put("buyerId", buyerId);
                    ratingData.put("sellerId", sellerId);
                    ratingData.put("rating", rating);
                    ratingData.put("reviewText", reviewText);
                    ratingData.put("createdAt", System.currentTimeMillis());

                    firestore.collection("ratings")
                            .document(orderId)
                            .set(ratingData)
                            .addOnSuccessListener(unused -> {
                                updateSellerRatingAggregate(rating);
                                dialog.dismiss();

                                Toast.makeText(this, "Seller rated successfully", Toast.LENGTH_SHORT).show();

                                tvRatingStatus.setText(
                                        reviewText.isEmpty()
                                                ? "Rating: " + buildStars(rating)
                                                : "Rating: " + buildStars(rating) + " - " + reviewText
                                );

                                btnRateSeller.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to save rating: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check rating: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateSellerRatingAggregate(int newRating) {
        firestore.collection("users")
                .document(sellerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    double oldAverage = 0;
                    long oldCount = 0;

                    Double ratingAverageValue = documentSnapshot.getDouble("ratingAverage");
                    Long ratingCountValue = documentSnapshot.getLong("ratingCount");

                    if (ratingAverageValue != null) {
                        oldAverage = ratingAverageValue;
                    }

                    if (ratingCountValue != null) {
                        oldCount = ratingCountValue;
                    }

                    long newCount = oldCount + 1;
                    double newAverage = ((oldAverage * oldCount) + newRating) / newCount;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("ratingAverage", newAverage);
                    updates.put("ratingCount", newCount);
                    updates.put("updatedAt", System.currentTimeMillis());

                    firestore.collection("users")
                            .document(sellerId)
                            .set(updates, SetOptions.merge());
                });
    }

    private void loadUserName(String userId, TextView targetTextView, String label) {
        if (userId == null || userId.trim().isEmpty()) {
            targetTextView.setText(label + ": Unknown");
            return;
        }

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String email = documentSnapshot.getString("email");

                        if (fullName != null && !fullName.trim().isEmpty()) {
                            targetTextView.setText(label + ": " + fullName);
                        } else if (email != null && !email.trim().isEmpty()) {
                            targetTextView.setText(label + ": " + email);
                        } else {
                            targetTextView.setText(label + ": Thriftopia User");
                        }
                    } else {
                        targetTextView.setText(label + ": Thriftopia User");
                    }
                })
                .addOnFailureListener(e -> {
                    targetTextView.setText(label + ": Thriftopia User");
                });
    }

    private String formatPurchaseType(String purchaseType) {
        if (purchaseType == null) {
            return "-";
        }

        if (purchaseType.equalsIgnoreCase("auction")) {
            return "Auction";
        }

        if (purchaseType.equalsIgnoreCase("buy_now")) {
            return "Buy Now";
        }

        if (purchaseType.equalsIgnoreCase("normal")) {
            return "Buy Now";
        }

        return purchaseType;
    }

    private String formatStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "Purchased";
        }

        if (status.equalsIgnoreCase("purchased")) {
            return "Purchased";
        }

        if (status.equalsIgnoreCase("completed")) {
            return "Completed";
        }

        if (status.equalsIgnoreCase("cancelled")) {
            return "Cancelled";
        }

        return status;
    }

    private String formatPaymentStatus(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.trim().isEmpty()) {
            return "-";
        }

        if (paymentStatus.equalsIgnoreCase("paid")) {
            return "Paid";
        }

        if (paymentStatus.equalsIgnoreCase("pending")) {
            return "Pending";
        }

        if (paymentStatus.equalsIgnoreCase("failed")) {
            return "Failed";
        }

        return paymentStatus;
    }

    private String formatDate(Long time) {
        if (time == null || time <= 0) {
            return "-";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private String safeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }

    private String buildStars(int rating) {
        StringBuilder stars = new StringBuilder();

        for (int i = 1; i <= 5; i++) {
            if (i <= rating) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }

        return stars.toString();
    }

    private void setPaymentCalculationTexts(Double subtotalValue, Double serviceFeeValue, Double paymentFeeValue, Double totalPaymentValue, Double priceValue) {
        double safeSubtotal = subtotalValue != null ? subtotalValue : (priceValue != null ? priceValue : 0);
        double safeServiceFee = serviceFeeValue != null ? serviceFeeValue : 0;
        double safePaymentFee = paymentFeeValue != null ? paymentFeeValue : 0;
        double safeTotalPayment = totalPaymentValue != null ? totalPaymentValue : safeSubtotal + safeServiceFee + safePaymentFee;

        tvSubtotal.setText("Subtotal: RM " + formatMoney(safeSubtotal));
        tvServiceFee.setText("Service Fee: RM " + formatMoney(safeServiceFee));
        tvPaymentFee.setText("Payment Fee: RM " + formatMoney(safePaymentFee));
        tvTotalPayment.setText("Total Payment: RM " + formatMoney(safeTotalPayment));
    }

    private String formatMoney(double amount) {
        return String.format(Locale.getDefault(), "%.2f", amount);
    }
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}