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
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.Locale;

public class MyListingsActivity extends AppCompatActivity {

    TextView btnBack, tvListingSubtitle;
    LinearLayout listingContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    FirebaseStorage storage;

    int currentLoadToken = 0;

    static class ListingData {
        String documentId;
        String itemId;
        String title;
        String category;
        String condition;
        String brand;
        String size;
        String description;
        String imageUrl;
        String imagePath;
        String sellerId;
        String status;
        String saleType;
        double price;
        long createdAt;

        ListingData(String documentId, String itemId, String title, String category,
                    String condition, String brand, String size, String description,
                    String imageUrl, String imagePath, String sellerId, String status,
                    String saleType, double price, long createdAt) {
            this.documentId = documentId;
            this.itemId = itemId;
            this.title = title;
            this.category = category;
            this.condition = condition;
            this.brand = brand;
            this.size = size;
            this.description = description;
            this.imageUrl = imageUrl;
            this.imagePath = imagePath;
            this.sellerId = sellerId;
            this.status = status;
            this.saleType = saleType;
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

        setContentView(R.layout.activity_my_listings);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        bindViews();
        setupClicks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyListings();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvListingSubtitle = findViewById(R.id.tvListingSubtitle);
        listingContainer = findViewById(R.id.listingContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadMyListings() {
        int loadToken = ++currentLoadToken;

        listingContainer.removeAllViews();
        showLoadingState();

        if (firebaseAuth.getCurrentUser() == null) {
            listingContainer.removeAllViews();
            tvListingSubtitle.setText("0 listing(s)");
            showEmptyState("Please login first", "Login to manage your listings.");
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("items")
                .whereEqualTo("sellerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    listingContainer.removeAllViews();

                    ArrayList<ListingData> listingList = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String documentId = document.getId();

                        String itemId = document.getString("itemId");
                        if (itemId == null || itemId.trim().isEmpty()) {
                            itemId = documentId;
                        }

                        String title = safe(document.getString("title"));
                        String category = safe(document.getString("category"));
                        String condition = safe(document.getString("condition"));
                        String brand = safe(document.getString("brand"));
                        String size = safe(document.getString("size"));
                        String description = safe(document.getString("description"));
                        String imageUrl = safe(document.getString("imageUrl"));
                        String imagePath = safe(document.getString("imagePath"));
                        String sellerId = safe(document.getString("sellerId"));
                        String status = safe(document.getString("status"));
                        String saleType = safe(document.getString("saleType"));

                        Double priceValue = document.getDouble("price");

                        if (priceValue == null) {
                            priceValue = document.getDouble("priceValue");
                        }

                        double price = priceValue != null ? priceValue : parsePrice(document.getString("priceText"));

                        Long createdAtValue = document.getLong("createdAt");
                        long createdAt = createdAtValue != null ? createdAtValue : 0;

                        if (status.trim().isEmpty()) {
                            status = "active";
                        }

                        if (saleType.trim().isEmpty()) {
                            saleType = "normal";
                        }

                        listingList.add(new ListingData(
                                documentId,
                                itemId,
                                title,
                                category,
                                condition,
                                brand,
                                size,
                                description,
                                imageUrl,
                                imagePath,
                                sellerId,
                                status,
                                saleType,
                                price,
                                createdAt
                        ));
                    }

                    listingList.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

                    if (listingList.isEmpty()) {
                        tvListingSubtitle.setText("0 listing(s)");
                        showEmptyState("No listings yet", "Items you sell will appear here.");
                        return;
                    }

                    tvListingSubtitle.setText(listingList.size() + " listing(s)");

                    for (ListingData listing : listingList) {
                        addListingCard(listing);
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    listingContainer.removeAllViews();
                    tvListingSubtitle.setText("0 listing(s)");
                    showEmptyState("Failed to load listings", e.getMessage());
                });
    }

    private void showLoadingState() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(28), dp(18), dp(28));

        TextView title = new TextView(this);
        title.setText("Loading listings...");
        title.setTextColor(getResources().getColor(R.color.text_primary_light));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Please wait while we fetch your selling items.");
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
        listingContainer.addView(card);
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
        icon.setImageResource(R.drawable.ic_add_circle);
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

        TextView btnSell = new TextView(this);
        btnSell.setText("SELL ITEM");
        btnSell.setTextColor(getResources().getColor(R.color.text_primary_light));
        btnSell.setTextSize(13);
        btnSell.setTypeface(null, Typeface.BOLD);
        btnSell.setGravity(Gravity.CENTER);
        btnSell.setBackgroundResource(R.drawable.bg_primary_button);
        btnSell.setClickable(true);
        btnSell.setFocusable(true);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        btnParams.setMargins(0, dp(22), 0, 0);
        btnSell.setLayoutParams(btnParams);

        btnSell.setOnClickListener(v -> {
            Intent intent = new Intent(MyListingsActivity.this, SellItemActivity.class);
            intent.putExtra("sellMode", "normal");
            startActivity(intent);
        });

        card.addView(btnSell);
        listingContainer.addView(card);
    }

    private void addListingCard(ListingData listing) {
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

        if (!listing.imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(listing.imageUrl)
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
        statusText.setText(getStatusLabel(listing));
        statusText.setTextColor(getResources().getColor(getStatusColor(listing)));
        statusText.setTextSize(11);
        statusText.setTypeface(null, Typeface.BOLD);
        infoLayout.addView(statusText);

        TextView titleText = new TextView(this);
        titleText.setText(!listing.title.trim().isEmpty() ? listing.title : "Untitled Item");
        titleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setMaxLines(2);
        infoLayout.addView(titleText);

        TextView priceText = new TextView(this);
        priceText.setText("RM " + String.format(Locale.getDefault(), "%.2f", listing.price));
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
        detailText.setText(valueOrDash(listing.category) + " • " + valueOrDash(listing.condition) + " • " + valueOrDash(listing.size));
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
        TextView btnEdit = createActionButton("EDIT", true);
        TextView btnDelete = createActionButton("DELETE", false);

        LinearLayout.LayoutParams btn1Params = new LinearLayout.LayoutParams(0, dp(48), 1);
        btn1Params.setMargins(0, 0, dp(6), 0);
        btnView.setLayoutParams(btn1Params);

        LinearLayout.LayoutParams btn2Params = new LinearLayout.LayoutParams(0, dp(48), 1);
        btn2Params.setMargins(dp(6), 0, dp(6), 0);
        btnEdit.setLayoutParams(btn2Params);

        LinearLayout.LayoutParams btn3Params = new LinearLayout.LayoutParams(0, dp(48), 1);
        btn3Params.setMargins(dp(6), 0, 0, 0);
        btnDelete.setLayoutParams(btn3Params);

        btnView.setOnClickListener(v -> openItemDetails(listing));

        btnEdit.setOnClickListener(v -> {
            if (listing.status.equalsIgnoreCase("sold")) {
                Toast.makeText(this, "Sold listing cannot be edited", Toast.LENGTH_SHORT).show();
            } else {
                openEditListing(listing);
            }
        });

        btnDelete.setOnClickListener(v -> confirmDeleteListing(listing));

        card.setOnClickListener(v -> openItemDetails(listing));

        buttonRow.addView(btnView);
        buttonRow.addView(btnEdit);
        buttonRow.addView(btnDelete);

        card.addView(buttonRow);
        listingContainer.addView(card);
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

    private void openItemDetails(ListingData listing) {
        Intent intent = new Intent(MyListingsActivity.this, ItemDetailsActivity.class);
        intent.putExtra("itemId", listing.documentId);
        startActivity(intent);
    }

    private void openEditListing(ListingData listing) {
        Intent intent = new Intent(MyListingsActivity.this, EditListingActivity.class);
        intent.putExtra("itemId", listing.documentId);
        startActivity(intent);
    }

    private void confirmDeleteListing(ListingData listing) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Listing");
        builder.setMessage("Are you sure you want to delete this listing?\n\n" + listing.title);

        builder.setPositiveButton("Delete", (dialog, which) -> deleteListing(listing));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteListing(ListingData listing) {
        deleteRelatedData(listing);

        firestore.collection("items")
                .document(listing.documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    deleteListingImage(listing);
                    Toast.makeText(this, "Listing deleted", Toast.LENGTH_SHORT).show();
                    loadMyListings();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete listing: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteRelatedData(ListingData listing) {
        firestore.collection("watchlist")
                .whereEqualTo("itemId", listing.documentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        firestore.collection("watchlist").document(document.getId()).delete();
                    }
                });

        firestore.collection("cart")
                .whereEqualTo("itemId", listing.documentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        firestore.collection("cart").document(document.getId()).delete();
                    }
                });

        firestore.collection("auctions")
                .whereEqualTo("itemId", listing.documentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        firestore.collection("auctions").document(document.getId()).delete();
                    }
                });
    }

    private void deleteListingImage(ListingData listing) {
        if (listing.imagePath != null && !listing.imagePath.trim().isEmpty()) {
            storage.getReference()
                    .child(listing.imagePath)
                    .delete();
        }
    }

    private String getStatusLabel(ListingData listing) {
        if (listing.status.equalsIgnoreCase("sold")) {
            return "SOLD";
        }

        if (listing.saleType.equalsIgnoreCase("auction")) {
            return "AUCTION";
        }

        return "ACTIVE";
    }

    private int getStatusColor(ListingData listing) {
        if (listing.status.equalsIgnoreCase("sold")) {
            return R.color.error;
        }

        if (listing.saleType.equalsIgnoreCase("auction")) {
            return R.color.error;
        }

        return R.color.accent_lime;
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