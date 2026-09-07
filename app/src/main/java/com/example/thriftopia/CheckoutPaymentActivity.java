package com.example.thriftopia;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CheckoutPaymentActivity extends AppCompatActivity {

    TextView btnBack, tvItemTitle, tvItemMeta, tvItemPrice, tvPaymentNote;
    TextView tvSubtotal, tvServiceFee, tvPaymentFee, tvTotalPayment;
    TextView methodEWallet, methodOnlineTransfer, methodCashOnDelivery;
    TextView btnConfirmPayment, btnCancelPayment;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String itemId = "";
    String sellerId = "";
    String title = "";
    String category = "";
    String condition = "";
    String brand = "";
    String size = "";
    String imageUrl = "";

    double priceValue = 0;
    double subtotal = 0;
    double serviceFee = 0;
    double paymentFee = 0;
    double totalPayment = 0;

    String selectedPaymentMethod = "E-Wallet";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupSystemBars();

        setContentView(R.layout.activity_checkout_payment);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        getIntentData();
        renderData();
        setupClicks();
        updatePaymentMethodUi();
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
        tvItemTitle = findViewById(R.id.tvItemTitle);
        tvItemMeta = findViewById(R.id.tvItemMeta);
        tvItemPrice = findViewById(R.id.tvItemPrice);
        tvPaymentNote = findViewById(R.id.tvPaymentNote);

        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvServiceFee = findViewById(R.id.tvServiceFee);
        tvPaymentFee = findViewById(R.id.tvPaymentFee);
        tvTotalPayment = findViewById(R.id.tvTotalPayment);

        methodEWallet = findViewById(R.id.methodEWallet);
        methodOnlineTransfer = findViewById(R.id.methodOnlineTransfer);
        methodCashOnDelivery = findViewById(R.id.methodCashOnDelivery);

        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        btnCancelPayment = findViewById(R.id.btnCancelPayment);
    }

    private void getIntentData() {
        itemId = getIntent().getStringExtra("itemId");
        sellerId = getIntent().getStringExtra("sellerId");
        title = getIntent().getStringExtra("title");
        category = getIntent().getStringExtra("category");
        condition = getIntent().getStringExtra("condition");
        brand = getIntent().getStringExtra("brand");
        size = getIntent().getStringExtra("size");
        imageUrl = getIntent().getStringExtra("imageUrl");
        priceValue = getIntent().getDoubleExtra("priceValue", 0);

        if (itemId == null) itemId = "";
        if (sellerId == null) sellerId = "";
        if (title == null) title = "";
        if (category == null) category = "";
        if (condition == null) condition = "";
        if (brand == null) brand = "";
        if (size == null) size = "";
        if (imageUrl == null) imageUrl = "";
    }

    private void renderData() {
        tvItemTitle.setText(title.trim().isEmpty() ? "Thriftopia Item" : title);

        String meta = valueOrDash(category) + " • " + valueOrDash(condition) + " • " + valueOrDash(size);

        if (!brand.trim().isEmpty()) {
            meta = meta + "\nBrand: " + brand;
        }

        tvItemMeta.setText(meta);
        tvItemPrice.setText("Item Price: RM " + formatMoney(priceValue));
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        btnCancelPayment.setOnClickListener(v -> finish());

        methodEWallet.setOnClickListener(v -> {
            selectedPaymentMethod = "E-Wallet";
            updatePaymentMethodUi();
        });

        methodOnlineTransfer.setOnClickListener(v -> {
            selectedPaymentMethod = "Online Transfer";
            updatePaymentMethodUi();
        });

        methodCashOnDelivery.setOnClickListener(v -> {
            selectedPaymentMethod = "Cash on Delivery";
            updatePaymentMethodUi();
        });

        btnConfirmPayment.setOnClickListener(v -> confirmPayment());
    }

    private void updatePaymentMethodUi() {
        methodEWallet.setBackgroundResource(R.drawable.bg_payment_method_normal);
        methodOnlineTransfer.setBackgroundResource(R.drawable.bg_payment_method_normal);
        methodCashOnDelivery.setBackgroundResource(R.drawable.bg_payment_method_normal);

        if (selectedPaymentMethod.equals("E-Wallet")) {
            methodEWallet.setBackgroundResource(R.drawable.bg_payment_method_selected);
            paymentFee = 1.00;
            tvPaymentNote.setText("Demo E-Wallet payment will be marked as paid after confirmation.");
        } else if (selectedPaymentMethod.equals("Online Transfer")) {
            methodOnlineTransfer.setBackgroundResource(R.drawable.bg_payment_method_selected);
            paymentFee = 0.50;
            tvPaymentNote.setText("Demo online transfer will be marked as paid after confirmation.");
        } else {
            methodCashOnDelivery.setBackgroundResource(R.drawable.bg_payment_method_selected);
            paymentFee = 0.00;
            tvPaymentNote.setText("Cash on Delivery will be recorded as pending payment until the buyer receives the item.");
        }

        calculateTotalPayment();
        renderCalculation();
    }

    private void calculateTotalPayment() {
        subtotal = priceValue;
        serviceFee = subtotal * 0.05;
        totalPayment = subtotal + serviceFee + paymentFee;
    }

    private void renderCalculation() {
        tvSubtotal.setText("Subtotal: RM " + formatMoney(subtotal));
        tvServiceFee.setText("Service Fee (5%): RM " + formatMoney(serviceFee));
        tvPaymentFee.setText("Payment Fee: RM " + formatMoney(paymentFee));
        tvTotalPayment.setText("Total Payment: RM " + formatMoney(totalPayment));
    }

    private void confirmPayment() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (itemId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            return;
        }

        calculateTotalPayment();

        btnConfirmPayment.setEnabled(false);
        btnConfirmPayment.setText("PROCESSING...");

        String buyerId = firebaseAuth.getCurrentUser().getUid();
        String orderId = "order_" + System.currentTimeMillis() + "_" + buyerId;
        String transactionId = "DEMO-" + System.currentTimeMillis();
        long now = System.currentTimeMillis();

        String paymentStatus = selectedPaymentMethod.equals("Cash on Delivery") ? "pending" : "paid";

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("itemId", itemId);
        order.put("buyerId", buyerId);
        order.put("sellerId", sellerId);
        order.put("title", title);
        order.put("category", category);
        order.put("condition", condition);
        order.put("brand", brand);
        order.put("size", size);
        order.put("priceText", "RM " + formatMoney(priceValue));
        order.put("priceValue", priceValue);
        order.put("imageUrl", imageUrl);
        order.put("status", "purchased");
        order.put("purchaseType", "buy_now");

        order.put("subtotal", subtotal);
        order.put("serviceFeeRate", 0.05);
        order.put("serviceFee", serviceFee);
        order.put("paymentFee", paymentFee);
        order.put("totalPayment", totalPayment);
        order.put("totalPaymentText", "RM " + formatMoney(totalPayment));

        order.put("paymentStatus", paymentStatus);
        order.put("paymentMethod", selectedPaymentMethod);
        order.put("transactionId", transactionId);
        order.put("paidAt", selectedPaymentMethod.equals("Cash on Delivery") ? 0 : now);
        order.put("createdAt", now);

        firestore.collection("orders")
                .document(orderId)
                .set(order)
                .addOnSuccessListener(unused -> updateItemAsSold(orderId, buyerId, transactionId, paymentStatus))
                .addOnFailureListener(e -> {
                    resetButton();
                    Toast.makeText(this, "Failed to create order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateItemAsSold(String orderId, String buyerId, String transactionId, String paymentStatus) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "sold");
        updates.put("buyerId", buyerId);
        updates.put("soldAt", System.currentTimeMillis());
        updates.put("soldBy", "buy_now");
        updates.put("orderId", orderId);
        updates.put("paymentStatus", paymentStatus);
        updates.put("paymentMethod", selectedPaymentMethod);
        updates.put("transactionId", transactionId);
        updates.put("subtotal", subtotal);
        updates.put("serviceFee", serviceFee);
        updates.put("paymentFee", paymentFee);
        updates.put("totalPayment", totalPayment);

        firestore.collection("items")
                .document(itemId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    deleteWatchlistForSoldItem();
                    removeBuyerCartItem(buyerId);
                    notifySellerItemSold(orderId);
                    notifyCartUsersItemSold(orderId, buyerId);

                    Toast.makeText(this, "Payment confirmed", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(CheckoutPaymentActivity.this, OrderDetailsActivity.class);
                    intent.putExtra("orderId", orderId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetButton();
                    Toast.makeText(this, "Failed to update item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteWatchlistForSoldItem() {
        firestore.collection("watchlist")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        firestore.collection("watchlist").document(document.getId()).delete();
                    }
                });
    }

    private void removeBuyerCartItem(String buyerId) {
        String cartId = buyerId + "_" + itemId;
        firestore.collection("cart").document(cartId).delete();
    }

    private void notifySellerItemSold(String orderId) {
        createNotification(
                sellerId,
                "Item Sold",
                title + " has been purchased.",
                "item_sold",
                orderId
        );
    }

    private void notifyCartUsersItemSold(String orderId, String buyerId) {
        firestore.collection("cart")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String cartUserId = document.getString("userId");

                        if (cartUserId == null || cartUserId.trim().isEmpty()) {
                            continue;
                        }

                        if (cartUserId.equals(buyerId)) {
                            continue;
                        }

                        firestore.collection("cart")
                                .document(document.getId())
                                .update("status", "sold", "itemStatus", "sold");

                        createNotification(
                                cartUserId,
                                "Cart Item Sold",
                                title + " in your cart has been sold to another buyer.",
                                "cart_item_sold",
                                orderId
                        );
                    }
                });
    }

    private void createNotification(String userId, String notificationTitle, String message, String type, String orderId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        String notificationId = firestore.collection("notifications").document().getId();

        Map<String, Object> notification = new HashMap<>();
        notification.put("notificationId", notificationId);
        notification.put("userId", userId);
        notification.put("title", notificationTitle);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("itemId", itemId);
        notification.put("orderId", orderId);
        notification.put("isRead", false);
        notification.put("createdAt", System.currentTimeMillis());

        firestore.collection("notifications")
                .document(notificationId)
                .set(notification);
    }

    private void resetButton() {
        btnConfirmPayment.setEnabled(true);
        btnConfirmPayment.setText("CONFIRM PAYMENT");
    }

    private String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }

    private String formatMoney(double amount) {
        return String.format(Locale.getDefault(), "%.2f", amount);
    }
}