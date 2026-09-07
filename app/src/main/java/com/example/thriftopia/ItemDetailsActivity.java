package com.example.thriftopia;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
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
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ItemDetailsActivity extends AppCompatActivity {

    TextView btnBack, btnWatchlist, btnReportListing;
    TextView tvStatusBadge, tvItemTitle, tvItemPrice, tvItemDetails;
    TextView tvSellerName, tvSellerRating, tvDescription, tvViewSellerProfile;
    TextView btnViewAuction, btnAddToCart, btnBuyNow, btnChatSeller, tvUnavailableMessage;
    ImageView imgItem;
    LinearLayout sellerCard, actionButtonRow;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String itemId = "";
    String auctionId = "";

    String title = "";
    String category = "";
    String condition = "";
    String brand = "";
    String size = "";
    String description = "";
    String imageUrl = "";
    String imagePath = "";
    String sellerId = "";
    String status = "";
    String saleType = "";

    double price = 0;

    boolean isInWatchlist = false;

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

        setContentView(R.layout.activity_item_details);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        itemId = getIntent().getStringExtra("itemId");
        auctionId = getIntent().getStringExtra("auctionId");

        if (itemId == null) itemId = "";
        if (auctionId == null) auctionId = "";

        bindViews();
        setupReportButton();
        setupClicks();

        if (itemId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadItem();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkWatchlistStatus();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnWatchlist = findViewById(R.id.btnWatchlist);
        btnReportListing = findViewById(R.id.btnReportListing);

        imgItem = findViewById(R.id.imgItem);

        actionButtonRow = findViewById(R.id.actionButtonRow);

        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvItemTitle = findViewById(R.id.tvItemTitle);
        tvItemPrice = findViewById(R.id.tvItemPrice);
        tvItemDetails = findViewById(R.id.tvItemDetails);

        sellerCard = findViewById(R.id.sellerCard);
        tvSellerName = findViewById(R.id.tvSellerName);
        tvSellerRating = findViewById(R.id.tvSellerRating);
        tvViewSellerProfile = findViewById(R.id.tvViewSellerProfile);

        tvDescription = findViewById(R.id.tvDescription);

        btnViewAuction = findViewById(R.id.btnViewAuction);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        btnChatSeller = findViewById(R.id.btnChatSeller);
        tvUnavailableMessage = findViewById(R.id.tvUnavailableMessage);
    }

    private void setupReportButton() {
        if (btnReportListing == null) {
            return;
        }

        btnReportListing.setVisibility(View.GONE);

        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        firestore.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String role = userDoc.getString("role");
                    boolean isAdmin = role != null && role.equalsIgnoreCase("admin");

                    if (isAdmin) {
                        btnReportListing.setVisibility(View.GONE);
                        return;
                    }

                    btnReportListing.setVisibility(View.VISIBLE);
                    btnReportListing.setOnClickListener(v -> openReportListing());
                })
                .addOnFailureListener(e -> {
                    btnReportListing.setVisibility(View.VISIBLE);
                    btnReportListing.setOnClickListener(v -> openReportListing());
                });
    }

    private void openReportListing() {
        String reportItemId = itemId;

        if (reportItemId == null || reportItemId.trim().isEmpty()) {
            reportItemId = getIntent().getStringExtra("itemId");
        }

        Intent intent = new Intent(ItemDetailsActivity.this, ReportActivity.class);
        intent.putExtra("targetType", "listing");
        intent.putExtra("itemId", reportItemId);
        startActivity(intent);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        btnWatchlist.setOnClickListener(v -> toggleWatchlist());

        btnAddToCart.setOnClickListener(v -> addToCart());

        btnBuyNow.setOnClickListener(v -> confirmBuyNow());

        btnChatSeller.setOnClickListener(v -> openChat());

        btnViewAuction.setOnClickListener(v -> openAuction());

        sellerCard.setOnClickListener(v -> openSellerProfile());

        tvSellerName.setOnClickListener(v -> openSellerProfile());
        tvSellerRating.setOnClickListener(v -> openSellerProfile());
        tvViewSellerProfile.setOnClickListener(v -> openSellerProfile());
    }

    private void loadItem() {
        firestore.collection("items")
                .document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        readItemData(documentSnapshot.getId(), documentSnapshot.getData());
                    } else {
                        findItemByItemIdField();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void findItemByItemIdField() {
        firestore.collection("items")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    QueryDocumentSnapshot document = queryDocumentSnapshots.iterator().next();
                    readItemData(document.getId(), document.getData());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to find item", Toast.LENGTH_SHORT).show();
                });
    }

    private void readItemData(String documentId, Map<String, Object> data) {
        itemId = documentId;

        title = getStringValue(data, "title");
        category = getStringValue(data, "category");
        condition = getStringValue(data, "condition");
        brand = getStringValue(data, "brand");
        size = getStringValue(data, "size");
        description = getStringValue(data, "description");
        imageUrl = getStringValue(data, "imageUrl");
        imagePath = getStringValue(data, "imagePath");
        sellerId = getStringValue(data, "sellerId");
        status = getStringValue(data, "status");
        saleType = getStringValue(data, "saleType");

        Object priceObject = data.get("price");

        if (priceObject instanceof Number) {
            price = ((Number) priceObject).doubleValue();
        } else {
            Object priceValueObject = data.get("priceValue");

            if (priceValueObject instanceof Number) {
                price = ((Number) priceValueObject).doubleValue();
            } else {
                price = parsePrice(getStringValue(data, "priceText"));
            }
        }

        if (status.trim().isEmpty()) {
            status = "active";
        }

        if (saleType.trim().isEmpty()) {
            saleType = "normal";
        }

        renderItem();
        loadSellerInfo();
        checkWatchlistStatus();
        setupButtons();
    }

    private void renderItem() {
        tvItemTitle.setText(!title.trim().isEmpty() ? title : "Untitled Item");
        tvItemPrice.setText("RM " + String.format(Locale.getDefault(), "%.2f", price));

        String details = valueOrDash(category) + " • " + valueOrDash(condition) + " • " + valueOrDash(size);

        if (!brand.trim().isEmpty()) {
            details = details + "\nBrand: " + brand;
        }

        tvItemDetails.setText(details);

        if (description.trim().isEmpty()) {
            tvDescription.setText("No description provided.");
        } else {
            tvDescription.setText(description);
        }

        if (!imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(imgItem);
        }

        if (status.equalsIgnoreCase("sold")) {
            tvStatusBadge.setText("SOLD");
            tvStatusBadge.setTextColor(getResources().getColor(R.color.error));
        } else if (saleType.equalsIgnoreCase("auction")) {
            tvStatusBadge.setText("AUCTION");
            tvStatusBadge.setTextColor(getResources().getColor(R.color.error));
        } else {
            tvStatusBadge.setText("ACTIVE");
            tvStatusBadge.setTextColor(getResources().getColor(R.color.accent_lime));
        }
    }

    private void loadSellerInfo() {
        if (sellerId.trim().isEmpty()) {
            tvSellerName.setText("Unknown Seller");
            tvSellerRating.setText("Rating: No ratings yet");
            return;
        }

        firestore.collection("users")
                .document(sellerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        tvSellerName.setText("Unknown Seller");
                        tvSellerRating.setText("Rating: No ratings yet");
                        return;
                    }

                    String fullName = documentSnapshot.getString("fullName");
                    String email = documentSnapshot.getString("email");

                    if (fullName != null && !fullName.trim().isEmpty()) {
                        tvSellerName.setText(fullName);
                    } else if (email != null && !email.trim().isEmpty()) {
                        tvSellerName.setText(email);
                    } else {
                        tvSellerName.setText("Thriftopia Seller");
                    }

                    Double average = documentSnapshot.getDouble("ratingAverage");
                    Long count = documentSnapshot.getLong("ratingCount");

                    if (average != null && count != null && count > 0) {
                        tvSellerRating.setText("Rating: " + String.format(Locale.getDefault(), "%.1f", average) + "/5.0 (" + count + ")");
                    } else {
                        tvSellerRating.setText("Rating: No ratings yet");
                    }
                })
                .addOnFailureListener(e -> {
                    tvSellerName.setText("Unknown Seller");
                    tvSellerRating.setText("Rating: No ratings yet");
                });
    }

    private void setupButtons() {
        btnViewAuction.setVisibility(View.GONE);
        actionButtonRow.setVisibility(View.VISIBLE);

        btnAddToCart.setVisibility(View.VISIBLE);
        btnBuyNow.setVisibility(View.VISIBLE);
        btnChatSeller.setVisibility(View.VISIBLE);
        btnWatchlist.setVisibility(View.VISIBLE);

        tvUnavailableMessage.setVisibility(View.GONE);

        if (firebaseAuth.getCurrentUser() == null) {
            actionButtonRow.setVisibility(View.GONE);
            btnViewAuction.setVisibility(View.GONE);
            tvUnavailableMessage.setVisibility(View.VISIBLE);
            tvUnavailableMessage.setText("Please login first.");
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (currentUserId.equals(sellerId)) {
            actionButtonRow.setVisibility(View.GONE);
            btnViewAuction.setVisibility(View.GONE);
            tvUnavailableMessage.setVisibility(View.VISIBLE);
            tvUnavailableMessage.setText("This is your own listing.");
            return;
        }

        if (status.equalsIgnoreCase("sold")) {
            actionButtonRow.setVisibility(View.GONE);
            btnViewAuction.setVisibility(View.GONE);
            tvUnavailableMessage.setVisibility(View.VISIBLE);
            tvUnavailableMessage.setText("This item has been sold.");
            return;
        }

        if (saleType.equalsIgnoreCase("auction")) {
            btnAddToCart.setVisibility(View.GONE);
            btnBuyNow.setVisibility(View.GONE);
            btnViewAuction.setVisibility(View.VISIBLE);

            btnChatSeller.setVisibility(View.VISIBLE);
            btnWatchlist.setVisibility(View.VISIBLE);
        }
    }

    private void openSellerProfile() {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            Toast.makeText(this, "Seller not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(ItemDetailsActivity.this, SellerProfileActivity.class);
        intent.putExtra("sellerId", sellerId);
        intent.putExtra("sourceItemId", itemId);
        intent.putExtra("sourceItemTitle", title);
        startActivity(intent);
    }

    private void checkWatchlistStatus() {
        if (firebaseAuth.getCurrentUser() == null || itemId.trim().isEmpty()) {
            setWatchlistIcon(false);
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String watchlistId = currentUserId + "_" + itemId;

        firestore.collection("watchlist")
                .document(watchlistId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isInWatchlist = documentSnapshot.exists();
                    setWatchlistIcon(isInWatchlist);
                })
                .addOnFailureListener(e -> {
                    isInWatchlist = false;
                    setWatchlistIcon(false);
                });
    }

    private void setWatchlistIcon(boolean active) {
        int color = getResources().getColor(active ? R.color.accent_lime : R.color.text_secondary_light);

        btnWatchlist.setText("");
        btnWatchlist.setCompoundDrawableTintList(ColorStateList.valueOf(color));
        btnWatchlist.setTextColor(color);
    }

    private void toggleWatchlist() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (status.equalsIgnoreCase("sold")) {
            Toast.makeText(this, "Sold item cannot be added to watchlist", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (currentUserId.equals(sellerId)) {
            Toast.makeText(this, "You cannot watchlist your own item", Toast.LENGTH_SHORT).show();
            return;
        }

        String watchlistId = currentUserId + "_" + itemId;

        if (isInWatchlist) {
            firestore.collection("watchlist")
                    .document(watchlistId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        isInWatchlist = false;
                        setWatchlistIcon(false);
                        Toast.makeText(this, "Removed from watchlist", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to remove watchlist", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Map<String, Object> watchlist = new HashMap<>();
            watchlist.put("watchlistId", watchlistId);
            watchlist.put("userId", currentUserId);
            watchlist.put("itemId", itemId);
            watchlist.put("sellerId", sellerId);
            watchlist.put("title", title);
            watchlist.put("category", category);
            watchlist.put("price", price);
            watchlist.put("imageUrl", imageUrl);
            watchlist.put("saleType", saleType);
            watchlist.put("status", status);
            watchlist.put("createdAt", System.currentTimeMillis());

            firestore.collection("watchlist")
                    .document(watchlistId)
                    .set(watchlist)
                    .addOnSuccessListener(unused -> {
                        isInWatchlist = true;
                        setWatchlistIcon(true);
                        Toast.makeText(this, "Added to watchlist", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add watchlist", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void addToCart() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (currentUserId.equals(sellerId)) {
            Toast.makeText(this, "You cannot add your own item to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        if (status.equalsIgnoreCase("sold")) {
            Toast.makeText(this, "This item is already sold", Toast.LENGTH_SHORT).show();
            return;
        }

        if (saleType.equalsIgnoreCase("auction")) {
            Toast.makeText(this, "Auction item cannot be added to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        String cartId = currentUserId + "_" + itemId;

        firestore.collection("cart")
                .document(cartId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Toast.makeText(this, "Item already in cart", Toast.LENGTH_SHORT).show();
                    } else {
                        saveCartItem(cartId, currentUserId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check cart", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveCartItem(String cartId, String currentUserId) {
        Map<String, Object> cart = new HashMap<>();
        cart.put("cartId", cartId);
        cart.put("userId", currentUserId);
        cart.put("itemId", itemId);
        cart.put("sellerId", sellerId);
        cart.put("title", title);
        cart.put("price", price);
        cart.put("imageUrl", imageUrl);
        cart.put("status", "active");
        cart.put("itemStatus", status);
        cart.put("createdAt", System.currentTimeMillis());

        firestore.collection("cart")
                .document(cartId)
                .set(cart)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add to cart: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void confirmBuyNow() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (currentUserId.equals(sellerId)) {
            Toast.makeText(this, "You cannot buy your own item", Toast.LENGTH_SHORT).show();
            return;
        }

        if (status.equalsIgnoreCase("sold")) {
            Toast.makeText(this, "This item is already sold", Toast.LENGTH_SHORT).show();
            return;
        }

        if (saleType.equalsIgnoreCase("auction")) {
            Toast.makeText(this, "Auction item must be purchased through auction", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Buy Now");
        builder.setMessage("Confirm purchase for " + title + "?\n\nPrice: RM " + String.format(Locale.getDefault(), "%.2f", price));

        builder.setPositiveButton("Continue", (dialog, which) -> openCheckoutPayment());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openCheckoutPayment() {
        Intent intent = new Intent(ItemDetailsActivity.this, CheckoutPaymentActivity.class);

        intent.putExtra("itemId", itemId);
        intent.putExtra("sellerId", sellerId);
        intent.putExtra("title", title);
        intent.putExtra("category", category);
        intent.putExtra("condition", condition);
        intent.putExtra("brand", brand);
        intent.putExtra("size", size);
        intent.putExtra("imageUrl", imageUrl);
        intent.putExtra("priceValue", price);

        startActivity(intent);
    }

    private void buyNow() {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String buyerId = firebaseAuth.getCurrentUser().getUid();
        String orderId = "order_" + System.currentTimeMillis() + "_" + buyerId;

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
        order.put("priceText", "RM " + String.format(Locale.getDefault(), "%.2f", price));
        order.put("priceValue", price);
        order.put("imageUrl", imageUrl);
        order.put("status", "purchased");
        order.put("purchaseType", "buy_now");
        order.put("createdAt", System.currentTimeMillis());

        firestore.collection("orders")
                .document(orderId)
                .set(order)
                .addOnSuccessListener(unused -> updateItemAsSold(orderId, buyerId))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateItemAsSold(String orderId, String buyerId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "sold");
        updates.put("buyerId", buyerId);
        updates.put("soldAt", System.currentTimeMillis());
        updates.put("soldBy", "buy_now");
        updates.put("orderId", orderId);

        firestore.collection("items")
                .document(itemId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    deleteWatchlistForSoldItem();
                    removeBuyerCartItem(buyerId);
                    notifySellerItemSold(orderId, buyerId);
                    notifyCartUsersItemSold(orderId, buyerId);

                    Toast.makeText(this, "Purchase successful", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ItemDetailsActivity.this, OrderDetailsActivity.class);
                    intent.putExtra("orderId", orderId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
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

        firestore.collection("cart")
                .document(cartId)
                .delete();
    }

    private void notifySellerItemSold(String orderId, String buyerId) {
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

    private void openChat() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (currentUserId.equals(sellerId)) {
            Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatId = itemId + "_" + currentUserId + "_" + sellerId;

        Map<String, Object> chat = new HashMap<>();
        chat.put("chatId", chatId);
        chat.put("itemId", itemId);
        chat.put("itemTitle", title);
        chat.put("buyerId", currentUserId);
        chat.put("sellerId", sellerId);
        chat.put("participants", Arrays.asList(currentUserId, sellerId));
        chat.put("updatedAt", System.currentTimeMillis());

        firestore.collection("chats")
                .document(chatId)
                .set(chat, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(ItemDetailsActivity.this, ChatRoomActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("itemId", itemId);
                    intent.putExtra("itemTitle", title);
                    intent.putExtra("sellerId", sellerId);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to open chat", Toast.LENGTH_SHORT).show();
                });
    }

    private void openAuction() {
        if (!auctionId.trim().isEmpty()) {
            Intent intent = new Intent(ItemDetailsActivity.this, AuctionDetailActivity.class);
            intent.putExtra("auctionId", auctionId);
            intent.putExtra("itemId", itemId);
            startActivity(intent);
            return;
        }

        firestore.collection("auctions")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Auction not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    QueryDocumentSnapshot document = queryDocumentSnapshots.iterator().next();
                    String foundAuctionId = document.getString("auctionId");

                    if (foundAuctionId == null || foundAuctionId.trim().isEmpty()) {
                        foundAuctionId = document.getId();
                    }

                    Intent intent = new Intent(ItemDetailsActivity.this, AuctionDetailActivity.class);
                    intent.putExtra("auctionId", foundAuctionId);
                    intent.putExtra("itemId", itemId);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to open auction", Toast.LENGTH_SHORT).show();
                });
    }

    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);

        if (value == null) {
            return "";
        }

        return String.valueOf(value);
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
}