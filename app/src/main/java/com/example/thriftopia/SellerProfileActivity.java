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
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SellerProfileActivity extends AppCompatActivity {

    TextView btnBack, tvSellerName, tvSellerEmail, tvSellerRating, tvSellerStats;
    TextView btnChatSeller, tvOwnProfileMessage, tvEmptyListings;
    ImageView imgSellerPhoto;
    LinearLayout listingContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String sellerId = "";
    String sellerName = "";
    String sellerEmail = "";
    String sellerPhotoUrl = "";

    String sourceItemId = "";
    String sourceItemTitle = "";

    int activeListingCount = 0;
    int soldListingCount = 0;

    ArrayList<ItemData> sellerItems = new ArrayList<>();

    static class ItemData {
        String itemId;
        String title;
        String category;
        String condition;
        String size;
        String imageUrl;
        String status;
        String saleType;
        double price;

        ItemData(String itemId, String title, String category, String condition,
                 String size, String imageUrl, String status, String saleType, double price) {
            this.itemId = itemId;
            this.title = title;
            this.category = category;
            this.condition = condition;
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

        setContentView(R.layout.activity_seller_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        sellerId = getIntent().getStringExtra("sellerId");
        sourceItemId = getIntent().getStringExtra("sourceItemId");
        sourceItemTitle = getIntent().getStringExtra("sourceItemTitle");

        if (sellerId == null) sellerId = "";
        if (sourceItemId == null) sourceItemId = "";
        if (sourceItemTitle == null) sourceItemTitle = "";

        if (sellerId.trim().isEmpty()) {
            Toast.makeText(this, "Seller not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupClicks();
        setupChatButtonState();

        loadSellerInfo();
        loadSellerListings();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        imgSellerPhoto = findViewById(R.id.imgSellerPhoto);

        tvSellerName = findViewById(R.id.tvSellerName);
        tvSellerEmail = findViewById(R.id.tvSellerEmail);
        tvSellerRating = findViewById(R.id.tvSellerRating);
        tvSellerStats = findViewById(R.id.tvSellerStats);

        btnChatSeller = findViewById(R.id.btnChatSeller);
        tvOwnProfileMessage = findViewById(R.id.tvOwnProfileMessage);
        tvEmptyListings = findViewById(R.id.tvEmptyListings);
        listingContainer = findViewById(R.id.listingContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());
        btnChatSeller.setOnClickListener(v -> openChatWithSeller());
    }

    private void setupChatButtonState() {
        if (firebaseAuth.getCurrentUser() == null) {
            btnChatSeller.setVisibility(View.GONE);
            tvOwnProfileMessage.setVisibility(View.VISIBLE);
            tvOwnProfileMessage.setText("Please login to chat with seller.");
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (currentUserId.equals(sellerId)) {
            btnChatSeller.setVisibility(View.GONE);
            tvOwnProfileMessage.setVisibility(View.VISIBLE);
            tvOwnProfileMessage.setText("This is your seller profile.");
            return;
        }

        if (sourceItemId.trim().isEmpty()) {
            btnChatSeller.setVisibility(View.GONE);
            tvOwnProfileMessage.setVisibility(View.VISIBLE);
            tvOwnProfileMessage.setText("Open one of the seller listings to start chat.");
            return;
        }

        btnChatSeller.setVisibility(View.VISIBLE);
        tvOwnProfileMessage.setVisibility(View.GONE);
    }

    private void loadSellerInfo() {
        firestore.collection("users")
                .document(sellerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        tvSellerName.setText("Thriftopia Seller");
                        tvSellerEmail.setText("-");
                        tvSellerRating.setText("Rating: No ratings yet");
                        return;
                    }

                    sellerName = safe(documentSnapshot.getString("fullName"));
                    sellerEmail = safe(documentSnapshot.getString("email"));

                    if (sellerName.trim().isEmpty()) {
                        sellerName = sellerEmail.trim().isEmpty() ? "Thriftopia Seller" : sellerEmail;
                    }

                    sellerPhotoUrl = safe(documentSnapshot.getString("profileImageUrl"));

                    if (sellerPhotoUrl.trim().isEmpty()) {
                        sellerPhotoUrl = safe(documentSnapshot.getString("profilePhotoUrl"));
                    }

                    if (sellerPhotoUrl.trim().isEmpty()) {
                        sellerPhotoUrl = safe(documentSnapshot.getString("photoUrl"));
                    }

                    if (sellerPhotoUrl.trim().isEmpty()) {
                        sellerPhotoUrl = safe(documentSnapshot.getString("imageUrl"));
                    }

                    tvSellerName.setText(sellerName);
                    tvSellerEmail.setText(!sellerEmail.trim().isEmpty() ? sellerEmail : "-");

                    if (!sellerPhotoUrl.trim().isEmpty()) {
                        Glide.with(this)
                                .load(sellerPhotoUrl)
                                .centerCrop()
                                .into(imgSellerPhoto);
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
                    tvSellerName.setText("Thriftopia Seller");
                    tvSellerEmail.setText("-");
                    tvSellerRating.setText("Rating: No ratings yet");
                });
    }

    private void loadSellerListings() {
        listingContainer.removeAllViews();
        tvEmptyListings.setVisibility(View.GONE);

        TextView loadingText = new TextView(this);
        loadingText.setText("Loading seller listings...");
        loadingText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        loadingText.setTextSize(14);
        listingContainer.addView(loadingText);

        firestore.collection("items")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sellerItems.clear();
                    activeListingCount = 0;
                    soldListingCount = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String itemId = safe(document.getString("itemId"));

                        if (itemId.trim().isEmpty()) {
                            itemId = document.getId();
                        }

                        String title = safe(document.getString("title"));
                        String category = safe(document.getString("category"));
                        String condition = safe(document.getString("condition"));
                        String size = safe(document.getString("size"));
                        String imageUrl = safe(document.getString("imageUrl"));
                        String status = safe(document.getString("status"));
                        String saleType = safe(document.getString("saleType"));

                        if (status.trim().isEmpty()) status = "active";
                        if (saleType.trim().isEmpty()) saleType = "normal";

                        Double priceValue = document.getDouble("price");

                        if (priceValue == null) {
                            priceValue = document.getDouble("priceValue");
                        }

                        double price = priceValue != null ? priceValue : parsePrice(document.getString("priceText"));

                        Boolean isSold = document.getBoolean("isSold");
                        Boolean sold = document.getBoolean("sold");
                        Boolean isHidden = document.getBoolean("isHidden");
                        Boolean hiddenByAdmin = document.getBoolean("hiddenByAdmin");

                        boolean soldItem = status.equalsIgnoreCase("sold")
                                || (isSold != null && isSold)
                                || (sold != null && sold);

                        boolean hiddenItem = status.equalsIgnoreCase("hidden")
                                || status.equalsIgnoreCase("removed")
                                || (isHidden != null && isHidden)
                                || (hiddenByAdmin != null && hiddenByAdmin);

                        if (soldItem) {
                            soldListingCount++;
                            continue;
                        }

                        if (hiddenItem) {
                            continue;
                        }

                        activeListingCount++;

                        sellerItems.add(new ItemData(
                                itemId,
                                title,
                                category,
                                condition,
                                size,
                                imageUrl,
                                status,
                                saleType,
                                price
                        ));
                    }

                    sellerItems.sort((a, b) -> a.title.compareToIgnoreCase(b.title));

                    renderStats();
                    renderListings();
                })
                .addOnFailureListener(e -> {
                    listingContainer.removeAllViews();
                    tvEmptyListings.setVisibility(View.VISIBLE);
                    tvEmptyListings.setText("Failed to load seller listings.");
                    renderStats();
                });
    }

    private void renderStats() {
        tvSellerStats.setText(activeListingCount + " active listing(s) • " + soldListingCount + " sold");
    }

    private void renderListings() {
        listingContainer.removeAllViews();

        if (sellerItems.isEmpty()) {
            tvEmptyListings.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyListings.setVisibility(View.GONE);

        for (ItemData item : sellerItems) {
            addListingCard(item);
        }
    }

    private void addListingCard(ItemData item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        ImageView itemImage = new ImageView(this);
        itemImage.setBackgroundResource(R.drawable.bg_hero);
        itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(104), dp(124));
        itemImage.setLayoutParams(imageParams);

        if (!item.imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(item.imageUrl)
                    .centerCrop()
                    .into(itemImage);
        }

        card.addView(itemImage);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        );
        infoParams.setMargins(dp(14), 0, 0, 0);
        infoLayout.setLayoutParams(infoParams);

        TextView badgeText = new TextView(this);

        if (item.saleType.equalsIgnoreCase("auction")) {
            badgeText.setText("AUCTION");
            badgeText.setTextColor(getResources().getColor(R.color.error));
        } else {
            badgeText.setText("ACTIVE");
            badgeText.setTextColor(getResources().getColor(R.color.accent_lime));
        }

        badgeText.setTextSize(11);
        badgeText.setTypeface(null, Typeface.BOLD);
        infoLayout.addView(badgeText);

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
        priceParams.setMargins(0, dp(4), 0, 0);
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

        TextView viewText = new TextView(this);
        viewText.setText("Tap to view item");
        viewText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        viewText.setTextSize(12);

        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        viewParams.setMargins(0, dp(8), 0, 0);
        viewText.setLayoutParams(viewParams);
        infoLayout.addView(viewText);

        card.addView(infoLayout);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(SellerProfileActivity.this, ItemDetailsActivity.class);
            intent.putExtra("itemId", item.itemId);
            startActivity(intent);
        });

        listingContainer.addView(card);
    }

    private void openChatWithSeller() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (currentUserId.equals(sellerId)) {
            Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sourceItemId.trim().isEmpty()) {
            Toast.makeText(this, "Open a seller listing first to start chat", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatTitle = !sourceItemTitle.trim().isEmpty() ? sourceItemTitle : "Seller Chat";
        String chatId = sourceItemId + "_" + currentUserId + "_" + sellerId;

        Map<String, Object> chat = new HashMap<>();
        chat.put("chatId", chatId);
        chat.put("itemId", sourceItemId);
        chat.put("itemTitle", chatTitle);
        chat.put("buyerId", currentUserId);
        chat.put("sellerId", sellerId);
        chat.put("participants", Arrays.asList(currentUserId, sellerId));
        chat.put("updatedAt", System.currentTimeMillis());

        firestore.collection("chats")
                .document(chatId)
                .set(chat, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(SellerProfileActivity.this, ChatRoomActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("itemId", sourceItemId);
                    intent.putExtra("itemTitle", chatTitle);
                    intent.putExtra("sellerId", sellerId);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to open chat", Toast.LENGTH_SHORT).show();
                });
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