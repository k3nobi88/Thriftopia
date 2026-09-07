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

import java.util.HashSet;
import java.util.Locale;

public class WatchlistActivity extends AppCompatActivity {

    TextView btnBack, tvWatchlistSubtitle;
    LinearLayout watchlistContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    int currentLoadToken = 0;
    int pendingLoads = 0;
    int renderedCount = 0;

    static class WatchlistData {
        String watchlistId;
        String itemId;
        String userId;
        String sellerId;

        WatchlistData(String watchlistId, String itemId, String userId, String sellerId) {
            this.watchlistId = watchlistId;
            this.itemId = itemId;
            this.userId = userId;
            this.sellerId = sellerId;
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

        setContentView(R.layout.activity_watchlist);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWatchlist();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvWatchlistSubtitle = findViewById(R.id.tvWatchlistSubtitle);
        watchlistContainer = findViewById(R.id.watchlistContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadWatchlist() {
        int loadToken = ++currentLoadToken;

        watchlistContainer.removeAllViews();
        showLoadingState();

        if (firebaseAuth.getCurrentUser() == null) {
            watchlistContainer.removeAllViews();
            tvWatchlistSubtitle.setText("0 saved item(s)");
            showEmptyState("Please login first", "Login to view your saved thrift items.");
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("watchlist")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    watchlistContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvWatchlistSubtitle.setText("0 saved item(s)");
                        showEmptyState("No saved items yet", "Tap the heart icon on any item to save it here.");
                        return;
                    }

                    HashSet<String> seenItemIds = new HashSet<>();

                    pendingLoads = 0;
                    renderedCount = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String watchlistId = document.getString("watchlistId");

                        if (watchlistId == null || watchlistId.trim().isEmpty()) {
                            watchlistId = document.getId();
                        }

                        String itemId = safe(document.getString("itemId"));

                        if (itemId.trim().isEmpty()) {
                            firestore.collection("watchlist").document(document.getId()).delete();
                            continue;
                        }

                        if (seenItemIds.contains(itemId)) {
                            firestore.collection("watchlist").document(document.getId()).delete();
                            continue;
                        }

                        seenItemIds.add(itemId);

                        String userId = safe(document.getString("userId"));
                        String sellerId = safe(document.getString("sellerId"));

                        WatchlistData watchlist = new WatchlistData(
                                watchlistId,
                                itemId,
                                userId,
                                sellerId
                        );

                        pendingLoads++;
                        loadItemForWatchlist(loadToken, watchlist);
                    }

                    if (pendingLoads == 0) {
                        tvWatchlistSubtitle.setText("0 saved item(s)");
                        showEmptyState("No saved items yet", "Tap the heart icon on any item to save it here.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    watchlistContainer.removeAllViews();
                    tvWatchlistSubtitle.setText("0 saved item(s)");
                    showEmptyState("Failed to load watchlist", e.getMessage());
                });
    }

    private void loadItemForWatchlist(int loadToken, WatchlistData watchlist) {
        firestore.collection("items")
                .document(watchlist.itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    if (!documentSnapshot.exists()) {
                        firestore.collection("watchlist").document(watchlist.watchlistId).delete();
                        pendingLoads--;
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

                    double price = priceValue != null ? priceValue : parsePrice(documentSnapshot.getString("priceText"));

                    if (status.trim().isEmpty()) {
                        status = "active";
                    }

                    if (saleType.trim().isEmpty()) {
                        saleType = "normal";
                    }

                    ItemData item = new ItemData(
                            watchlist.itemId,
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

                    addWatchlistCard(watchlist, item);
                    renderedCount++;
                    pendingLoads--;
                    checkRenderDone();
                })
                .addOnFailureListener(e -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    pendingLoads--;
                    checkRenderDone();
                });
    }

    private void checkRenderDone() {
        tvWatchlistSubtitle.setText(renderedCount + " saved item(s)");

        if (pendingLoads <= 0 && renderedCount == 0) {
            watchlistContainer.removeAllViews();
            showEmptyState("No saved items yet", "Tap the heart icon on any item to save it here.");
        }
    }

    private void showLoadingState() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(28), dp(18), dp(28));

        TextView title = new TextView(this);
        title.setText("Loading watchlist...");
        title.setTextColor(getResources().getColor(R.color.text_primary_light));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Please wait while we fetch your saved items.");
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
        watchlistContainer.addView(card);
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
        icon.setImageResource(R.drawable.ic_heart);
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
            Intent intent = new Intent(WatchlistActivity.this, ExploreActivity.class);
            startActivity(intent);
            finish();
        });

        card.addView(btnExplore);
        watchlistContainer.addView(card);
    }

    private void addWatchlistCard(WatchlistData watchlist, ItemData item) {
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
        } else if (item.saleType.equalsIgnoreCase("auction")) {
            statusText.setText("AUCTION");
            statusText.setTextColor(getResources().getColor(R.color.error));
        } else {
            statusText.setText("SAVED");
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

        TextView btnView = createActionButton("VIEW ITEM", true);
        TextView btnRemove = createActionButton("REMOVE", false);

        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        viewParams.setMargins(0, 0, dp(7), 0);
        btnView.setLayoutParams(viewParams);

        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        removeParams.setMargins(dp(7), 0, 0, 0);
        btnRemove.setLayoutParams(removeParams);

        btnView.setOnClickListener(v -> {
            Intent intent = new Intent(WatchlistActivity.this, ItemDetailsActivity.class);
            intent.putExtra("itemId", item.itemId);
            startActivity(intent);
        });

        btnRemove.setOnClickListener(v -> confirmRemoveWatchlist(watchlist));

        buttonRow.addView(btnView);
        buttonRow.addView(btnRemove);

        card.addView(buttonRow);
        watchlistContainer.addView(card);
    }

    private TextView createActionButton(String text, boolean primary) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(12);
        button.setTypeface(null, Typeface.BOLD);
        button.setClickable(true);
        button.setFocusable(true);
        button.setTextColor(getResources().getColor(R.color.text_primary_light));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_outline_button);
        return button;
    }

    private void confirmRemoveWatchlist(WatchlistData watchlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Saved Item");
        builder.setMessage("Remove this item from your watchlist?");

        builder.setPositiveButton("Remove", (dialog, which) -> removeWatchlist(watchlist.watchlistId));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void removeWatchlist(String watchlistId) {
        firestore.collection("watchlist")
                .document(watchlistId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Removed from watchlist", Toast.LENGTH_SHORT).show();
                    loadWatchlist();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove item", Toast.LENGTH_SHORT).show();
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