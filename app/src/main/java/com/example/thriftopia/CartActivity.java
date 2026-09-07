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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    TextView btnBack, tvCartSubtitle;
    LinearLayout cartContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    int currentLoadToken = 0;
    int loadedItemCount = 0;
    int renderedItemCount = 0;

    static class CartData {
        String cartId;
        String itemId;
        String userId;
        String sellerId;
        String title;
        String imageUrl;
        String status;
        double price;

        CartData(String cartId, String itemId, String userId, String sellerId,
                 String title, String imageUrl, String status, double price) {
            this.cartId = cartId;
            this.itemId = itemId;
            this.userId = userId;
            this.sellerId = sellerId;
            this.title = title;
            this.imageUrl = imageUrl;
            this.status = status;
            this.price = price;
        }
    }

    static class ItemData {
        String itemId;
        String sellerId;
        String title;
        String category;
        String condition;
        String brand;
        String size;
        String imageUrl;
        String status;
        String saleType;
        double price;

        ItemData(String itemId, String sellerId, String title, String category,
                 String condition, String brand, String size, String imageUrl,
                 String status, String saleType, double price) {
            this.itemId = itemId;
            this.sellerId = sellerId;
            this.title = title;
            this.category = category;
            this.condition = condition;
            this.brand = brand;
            this.size = size;
            this.imageUrl = imageUrl;
            this.status = status;
            this.saleType = saleType;
            this.price = price;
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

        setContentView(R.layout.activity_cart);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartItems();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvCartSubtitle = findViewById(R.id.tvCartSubtitle);
        cartContainer = findViewById(R.id.cartContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCartItems() {
        int loadToken = ++currentLoadToken;

        cartContainer.removeAllViews();
        showLoadingState();

        if (firebaseAuth.getCurrentUser() == null) {
            cartContainer.removeAllViews();
            showEmptyState("Please login first", "Login to view your saved cart items.");
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("cart")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    cartContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvCartSubtitle.setText("0 item(s)");
                        showEmptyState("Your cart is empty", "Add thrift items to cart and buy them later.");
                        return;
                    }

                    HashSet<String> seenItemIds = new HashSet<>();

                    loadedItemCount = 0;
                    renderedItemCount = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String cartId = document.getString("cartId");

                        if (cartId == null || cartId.trim().isEmpty()) {
                            cartId = document.getId();
                        }

                        String itemId = safe(document.getString("itemId"));

                        if (itemId.trim().isEmpty()) {
                            firestore.collection("cart").document(document.getId()).delete();
                            continue;
                        }

                        if (seenItemIds.contains(itemId)) {
                            firestore.collection("cart").document(document.getId()).delete();
                            continue;
                        }

                        seenItemIds.add(itemId);
                        loadedItemCount++;

                        String userId = safe(document.getString("userId"));
                        String sellerId = safe(document.getString("sellerId"));
                        String title = safe(document.getString("title"));
                        String imageUrl = safe(document.getString("imageUrl"));
                        String status = safe(document.getString("status"));

                        Double priceValue = document.getDouble("price");
                        double price = priceValue != null ? priceValue : 0;

                        CartData cart = new CartData(
                                cartId,
                                itemId,
                                userId,
                                sellerId,
                                title,
                                imageUrl,
                                status,
                                price
                        );

                        loadItemForCart(loadToken, cart);
                    }

                    if (loadedItemCount == 0) {
                        tvCartSubtitle.setText("0 item(s)");
                        showEmptyState("Your cart is empty", "Add thrift items to cart and buy them later.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    cartContainer.removeAllViews();
                    showEmptyState("Failed to load cart", e.getMessage());
                });
    }

    private void loadItemForCart(int loadToken, CartData cart) {
        firestore.collection("items")
                .document(cart.itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    if (!documentSnapshot.exists()) {
                        firestore.collection("cart").document(cart.cartId).delete();
                        loadedItemCount--;
                        checkRenderDone();
                        return;
                    }

                    String sellerId = safe(documentSnapshot.getString("sellerId"));
                    String title = safe(documentSnapshot.getString("title"));
                    String category = safe(documentSnapshot.getString("category"));
                    String condition = safe(documentSnapshot.getString("condition"));
                    String brand = safe(documentSnapshot.getString("brand"));
                    String size = safe(documentSnapshot.getString("size"));
                    String imageUrl = safe(documentSnapshot.getString("imageUrl"));
                    String status = safe(documentSnapshot.getString("status"));
                    String saleType = safe(documentSnapshot.getString("saleType"));

                    Double priceValue = documentSnapshot.getDouble("price");

                    if (priceValue == null) {
                        priceValue = documentSnapshot.getDouble("priceValue");
                    }

                    double price = priceValue != null ? priceValue : cart.price;

                    if (status.trim().isEmpty()) {
                        status = "active";
                    }

                    if (saleType.trim().isEmpty()) {
                        saleType = "normal";
                    }

                    ItemData item = new ItemData(
                            cart.itemId,
                            sellerId,
                            title,
                            category,
                            condition,
                            brand,
                            size,
                            imageUrl,
                            status,
                            saleType,
                            price
                    );

                    addCartCard(cart, item);
                    renderedItemCount++;
                    checkRenderDone();
                })
                .addOnFailureListener(e -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    loadedItemCount--;
                    checkRenderDone();
                });
    }

    private void checkRenderDone() {
        tvCartSubtitle.setText(renderedItemCount + " item(s)");

        if (loadedItemCount <= 0 && renderedItemCount == 0) {
            cartContainer.removeAllViews();
            showEmptyState("Your cart is empty", "Add thrift items to cart and buy them later.");
        }
    }

    private void showLoadingState() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(28), dp(18), dp(28));

        TextView title = new TextView(this);
        title.setText("Loading cart...");
        title.setTextColor(getResources().getColor(R.color.text_primary_light));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Please wait while we fetch your cart items.");
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
        cartContainer.addView(card);
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
            Intent intent = new Intent(CartActivity.this, ExploreActivity.class);
            startActivity(intent);
            finish();
        });

        card.addView(btnExplore);

        cartContainer.addView(card);
    }

    private void addCartCard(CartData cart, ItemData item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

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

        if (!item.imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(item.imageUrl)
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

        if (item.status.equalsIgnoreCase("sold")) {
            statusText.setText("SOLD");
            statusText.setTextColor(getResources().getColor(R.color.error));
        } else {
            statusText.setText("IN CART");
            statusText.setTextColor(getResources().getColor(R.color.accent_lime));
        }

        statusText.setTextSize(11);
        statusText.setTypeface(null, Typeface.BOLD);
        infoLayout.addView(statusText);

        TextView titleText = new TextView(this);
        titleText.setText(!item.title.trim().isEmpty() ? item.title : "Untitled Item");
        titleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setMaxLines(2);
        infoLayout.addView(titleText);

        TextView priceText = new TextView(this);
        priceText.setText("RM " + String.format(Locale.getDefault(), "%.2f", item.price));
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
        detailText.setText(valueOrDash(item.category) + " • " + valueOrDash(item.condition) + " • " + valueOrDash(item.size));
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

        topRow.addView(infoLayout);
        card.addView(topRow);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams buttonRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonRowParams.setMargins(0, dp(14), 0, 0);
        buttonRow.setLayoutParams(buttonRowParams);

        TextView btnView = createActionButton("VIEW", true);
        TextView btnBuy = createActionButton(item.status.equalsIgnoreCase("sold") ? "SOLD" : "BUY NOW", true);
        TextView btnRemove = createActionButton("REMOVE", false);

        LinearLayout.LayoutParams btn1Params = new LinearLayout.LayoutParams(0, dp(48), 1);
        btn1Params.setMargins(0, 0, dp(6), 0);
        btnView.setLayoutParams(btn1Params);

        LinearLayout.LayoutParams btn2Params = new LinearLayout.LayoutParams(0, dp(48), 1);
        btn2Params.setMargins(dp(6), 0, dp(6), 0);
        btnBuy.setLayoutParams(btn2Params);

        LinearLayout.LayoutParams btn3Params = new LinearLayout.LayoutParams(0, dp(48), 1);
        btn3Params.setMargins(dp(6), 0, 0, 0);
        btnRemove.setLayoutParams(btn3Params);

        btnView.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, ItemDetailsActivity.class);
            intent.putExtra("itemId", item.itemId);
            startActivity(intent);
        });

        btnBuy.setOnClickListener(v -> {
            if (item.status.equalsIgnoreCase("sold")) {
                Toast.makeText(this, "This item has been sold", Toast.LENGTH_SHORT).show();
            } else {
                confirmBuyNow(cart, item);
            }
        });

        btnRemove.setOnClickListener(v -> confirmRemoveCart(cart));

        buttonRow.addView(btnView);
        buttonRow.addView(btnBuy);
        buttonRow.addView(btnRemove);

        card.addView(buttonRow);
        cartContainer.addView(card);
    }

    private TextView createActionButton(String text, boolean primary) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(11);
        button.setTypeface(null, Typeface.BOLD);
        button.setClickable(true);
        button.setFocusable(true);
        button.setTextColor(getResources().getColor(R.color.text_primary_light));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_outline_button);
        return button;
    }

    private void confirmRemoveCart(CartData cart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Item");
        builder.setMessage("Remove this item from your cart?");

        builder.setPositiveButton("Remove", (dialog, which) -> removeCartItem(cart.cartId));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void removeCartItem(String cartId) {
        firestore.collection("cart")
                .document(cartId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Removed from cart", Toast.LENGTH_SHORT).show();
                    loadCartItems();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove item", Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmBuyNow(CartData cart, ItemData item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Buy Now");
        builder.setMessage("Confirm purchase for " + item.title + "?\n\nPrice: RM " + String.format(Locale.getDefault(), "%.2f", item.price));

        builder.setPositiveButton("Buy", (dialog, which) -> buyNow(cart, item));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void buyNow(CartData cart, ItemData item) {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String buyerId = firebaseAuth.getCurrentUser().getUid();

        String orderId = "order_" + System.currentTimeMillis() + "_" + buyerId;

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("itemId", item.itemId);
        order.put("buyerId", buyerId);
        order.put("sellerId", item.sellerId);
        order.put("title", item.title);
        order.put("category", item.category);
        order.put("condition", item.condition);
        order.put("brand", item.brand);
        order.put("size", item.size);
        order.put("priceText", "RM " + String.format(Locale.getDefault(), "%.2f", item.price));
        order.put("priceValue", item.price);
        order.put("imageUrl", item.imageUrl);
        order.put("status", "purchased");
        order.put("purchaseType", "buy_now");
        order.put("createdAt", System.currentTimeMillis());

        firestore.collection("orders")
                .document(orderId)
                .set(order)
                .addOnSuccessListener(unused -> updateItemAsSold(cart, item, orderId, buyerId))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateItemAsSold(CartData cart, ItemData item, String orderId, String buyerId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "sold");
        updates.put("buyerId", buyerId);
        updates.put("soldAt", System.currentTimeMillis());
        updates.put("soldBy", "buy_now");
        updates.put("orderId", orderId);

        firestore.collection("items")
                .document(item.itemId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    deleteWatchlistForSoldItem(item.itemId);
                    removeCartItemSilent(cart.cartId);
                    notifySellerItemSold(item, orderId);
                    notifyCartUsersItemSold(item, orderId, buyerId);

                    Toast.makeText(this, "Purchase successful", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(CartActivity.this, OrderDetailsActivity.class);
                    intent.putExtra("orderId", orderId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteWatchlistForSoldItem(String itemId) {
        firestore.collection("watchlist")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        firestore.collection("watchlist").document(document.getId()).delete();
                    }
                });
    }

    private void removeCartItemSilent(String cartId) {
        firestore.collection("cart")
                .document(cartId)
                .delete();
    }

    private void notifySellerItemSold(ItemData item, String orderId) {
        createNotification(
                item.sellerId,
                "Item Sold",
                item.title + " has been purchased.",
                "item_sold",
                item.itemId,
                orderId
        );
    }

    private void notifyCartUsersItemSold(ItemData item, String orderId, String buyerId) {
        firestore.collection("cart")
                .whereEqualTo("itemId", item.itemId)
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
                                item.title + " in your cart has been sold to another buyer.",
                                "cart_item_sold",
                                item.itemId,
                                orderId
                        );
                    }
                });
    }

    private void createNotification(String userId, String notificationTitle, String message,
                                    String type, String itemId, String orderId) {
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

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}