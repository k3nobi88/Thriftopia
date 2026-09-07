package com.example.thriftopia;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    ImageView btnCart, btnNotifications;
    TextView tvCartBadge, tvNotificationBadge;
    TextView btnSortItems;

    EditText etSearch;

    TextView chipAll, chipTops, chipBottoms, chipOuterwear, chipShoes, chipAccessories;

    LinearLayout recommendedContainer, latestContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String selectedCategory = "All";
    String selectedSort = "latest";

    List<ItemData> allItems = new ArrayList<>();

    static class ItemData {
        String documentId;
        String itemId;
        String title;
        String category;
        String condition;
        String brand;
        String size;
        String imageUrl;
        String status;
        String saleType;
        String sellerId;
        double price;
        long createdAt;
        boolean sold;
        boolean hidden;

        ItemData(String documentId, String itemId, String title, String category,
                 String condition, String brand, String size, String imageUrl,
                 String status, String saleType, String sellerId, double price,
                 long createdAt, boolean sold, boolean hidden) {
            this.documentId = documentId;
            this.itemId = itemId;
            this.title = title;
            this.category = category;
            this.condition = condition;
            this.brand = brand;
            this.size = size;
            this.imageUrl = imageUrl;
            this.status = status;
            this.saleType = saleType;
            this.sellerId = sellerId;
            this.price = price;
            this.createdAt = createdAt;
            this.sold = sold;
            this.hidden = hidden;
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

        setContentView(R.layout.activity_home);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();

        BottomNavHelper.setup(this, BottomNavHelper.PAGE_HOME);

        loadHeaderBadges();
        loadItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHeaderBadges();
        loadItems();
    }

    private void bindViews() {
        btnCart = findViewById(R.id.btnCart);
        btnNotifications = findViewById(R.id.btnNotifications);

        tvCartBadge = findViewById(R.id.tvCartBadge);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        btnSortItems = findViewById(R.id.btnSortItems);
        etSearch = findViewById(R.id.etSearch);

        chipAll = findViewById(R.id.chipAll);
        chipTops = findViewById(R.id.chipTops);
        chipBottoms = findViewById(R.id.chipBottoms);
        chipOuterwear = findViewById(R.id.chipOuterwear);
        chipShoes = findViewById(R.id.chipShoes);
        chipAccessories = findViewById(R.id.chipAccessories);

        recommendedContainer = findViewById(R.id.recommendedContainer);
        latestContainer = findViewById(R.id.latestContainer);
    }

    private void setupClicks() {
        btnCart.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, CartActivity.class)));

        btnNotifications.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, NotificationsActivity.class)));

        btnSortItems.setOnClickListener(v -> showSortDialog());
        updateSortButtonText();

        chipAll.setOnClickListener(v -> selectCategory("All"));
        chipTops.setOnClickListener(v -> selectCategory("Tops"));
        chipBottoms.setOnClickListener(v -> selectCategory("Bottoms"));
        chipOuterwear.setOnClickListener(v -> selectCategory("Outerwear"));
        chipShoes.setOnClickListener(v -> selectCategory("Shoes"));
        chipAccessories.setOnClickListener(v -> selectCategory("Accessories"));

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                openExploreWithSearch();
                return true;
            }

            return false;
        });

        etSearch.setOnClickListener(v -> {
            String search = etSearch.getText().toString().trim();

            if (!search.isEmpty()) {
                openExploreWithSearch();
            }
        });
    }

    private void openExploreWithSearch() {
        String search = etSearch.getText().toString().trim();

        Intent intent = new Intent(HomeActivity.this, ExploreActivity.class);
        intent.putExtra("searchQuery", search);
        intent.putExtra("category", selectedCategory);
        startActivity(intent);
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        updateChipStyles();
        renderLatestItems();
    }

    private void updateChipStyles() {
        setChipStyle(chipAll, selectedCategory.equalsIgnoreCase("All"));
        setChipStyle(chipTops, selectedCategory.equalsIgnoreCase("Tops"));
        setChipStyle(chipBottoms, selectedCategory.equalsIgnoreCase("Bottoms"));
        setChipStyle(chipOuterwear, selectedCategory.equalsIgnoreCase("Outerwear"));
        setChipStyle(chipShoes, selectedCategory.equalsIgnoreCase("Shoes"));
        setChipStyle(chipAccessories, selectedCategory.equalsIgnoreCase("Accessories"));
    }

    private void setChipStyle(TextView chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_card);
        chip.setTextColor(getResources().getColor(active ? R.color.text_primary_light : R.color.text_secondary_light));
    }

    private void showSortDialog() {
        String[] options = {
                "Latest",
                "Oldest",
                "Price: Low to High",
                "Price: High to Low"
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Sort Items")
                .setSingleChoiceItems(options, getSortCheckedIndex(), (dialogInterface, which) -> {
                    if (which == 0) {
                        selectedSort = "latest";
                    } else if (which == 1) {
                        selectedSort = "oldest";
                    } else if (which == 2) {
                        selectedSort = "price_low";
                    } else {
                        selectedSort = "price_high";
                    }

                    updateSortButtonText();
                    renderLatestItems();
                    dialogInterface.dismiss();
                })
                .create();

        dialog.show();
    }

    private int getSortCheckedIndex() {
        if (selectedSort.equals("oldest")) {
            return 1;
        }

        if (selectedSort.equals("price_low")) {
            return 2;
        }

        if (selectedSort.equals("price_high")) {
            return 3;
        }

        return 0;
    }

    private void updateSortButtonText() {
        if (btnSortItems == null) {
            return;
        }

        if (selectedSort.equals("oldest")) {
            btnSortItems.setText("Sort: Oldest ▾");
        } else if (selectedSort.equals("price_low")) {
            btnSortItems.setText("Sort: Low RM ▾");
        } else if (selectedSort.equals("price_high")) {
            btnSortItems.setText("Sort: High RM ▾");
        } else {
            btnSortItems.setText("Sort: Latest ▾");
        }
    }

    private void loadHeaderBadges() {
        if (firebaseAuth.getCurrentUser() == null) {
            setBadge(tvCartBadge, 0);
            setBadge(tvNotificationBadge, 0);
            return;
        }

        loadCartBadge();
        loadNotificationBadge();
    }

    private void loadCartBadge() {
        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("cart")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        String itemStatus = document.getString("itemStatus");

                        boolean activeCart = status == null || status.trim().isEmpty() || status.equalsIgnoreCase("active");
                        boolean itemNotSold = itemStatus == null || !itemStatus.equalsIgnoreCase("sold");

                        if (activeCart && itemNotSold) {
                            count++;
                        }
                    }

                    setBadge(tvCartBadge, count);
                })
                .addOnFailureListener(e -> setBadge(tvCartBadge, 0));
    }

    private void loadNotificationBadge() {
        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Boolean isRead = document.getBoolean("isRead");

                        if (isRead == null || !isRead) {
                            count++;
                        }
                    }

                    setBadge(tvNotificationBadge, count);
                })
                .addOnFailureListener(e -> setBadge(tvNotificationBadge, 0));
    }

    private void setBadge(TextView badgeView, int count) {
        if (badgeView == null) {
            return;
        }

        if (count <= 0) {
            badgeView.setVisibility(View.GONE);
            return;
        }

        badgeView.setVisibility(View.VISIBLE);

        if (count > 99) {
            badgeView.setText("99+");
            badgeView.setTextSize(9);
        } else {
            badgeView.setText(String.valueOf(count));
            badgeView.setTextSize(11);
        }
    }

    private void loadItems() {
        recommendedContainer.removeAllViews();
        latestContainer.removeAllViews();

        showLoadingCard(recommendedContainer, "Loading recommendations...");
        showLoadingCard(latestContainer, "Loading latest items...");

        firestore.collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allItems.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String documentId = document.getId();

                        String itemId = safe(document.getString("itemId"));
                        if (itemId.trim().isEmpty()) {
                            itemId = documentId;
                        }

                        String title = safe(document.getString("title"));
                        String category = safe(document.getString("category"));
                        String condition = safe(document.getString("condition"));
                        String brand = safe(document.getString("brand"));
                        String size = safe(document.getString("size"));
                        String imageUrl = safe(document.getString("imageUrl"));
                        String status = safe(document.getString("status"));
                        String saleType = safe(document.getString("saleType"));
                        String sellerId = safe(document.getString("sellerId"));

                        Double priceValue = document.getDouble("price");

                        if (priceValue == null) {
                            priceValue = document.getDouble("priceValue");
                        }

                        double price = priceValue != null ? priceValue : parsePrice(document.getString("priceText"));

                        Long createdAtValue = document.getLong("createdAt");

                        if (createdAtValue == null) {
                            createdAtValue = document.getLong("updatedAt");
                        }

                        long createdAt = createdAtValue != null ? createdAtValue : 0;

                        if (status.trim().isEmpty()) {
                            status = "active";
                        }

                        if (saleType.trim().isEmpty()) {
                            saleType = "normal";
                        }

                        Boolean isSold = document.getBoolean("isSold");
                        Boolean soldValue = document.getBoolean("sold");
                        Boolean isHidden = document.getBoolean("isHidden");
                        Boolean hiddenByAdmin = document.getBoolean("hiddenByAdmin");

                        boolean sold = status.equalsIgnoreCase("sold")
                                || (isSold != null && isSold)
                                || (soldValue != null && soldValue);

                        boolean hidden = status.equalsIgnoreCase("hidden")
                                || status.equalsIgnoreCase("removed")
                                || (isHidden != null && isHidden)
                                || (hiddenByAdmin != null && hiddenByAdmin);

                        allItems.add(new ItemData(
                                documentId,
                                itemId,
                                title,
                                category,
                                condition,
                                brand,
                                size,
                                imageUrl,
                                status,
                                saleType,
                                sellerId,
                                price,
                                createdAt,
                                sold,
                                hidden
                        ));
                    }

                    renderRecommendedItems();
                    renderLatestItems();
                })
                .addOnFailureListener(e -> {
                    recommendedContainer.removeAllViews();
                    latestContainer.removeAllViews();

                    showEmptyCard(recommendedContainer, "Failed to load recommendations");
                    showEmptyCard(latestContainer, "Failed to load items");

                    Toast.makeText(this, "Failed to load items: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void renderRecommendedItems() {
        recommendedContainer.removeAllViews();

        ArrayList<ItemData> visibleItems = getVisibleActiveItems();

        visibleItems.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

        if (visibleItems.isEmpty()) {
            showEmptyCard(recommendedContainer, "No recommendations yet");
            return;
        }

        int max = Math.min(2, visibleItems.size());

        for (int i = 0; i < max; i++) {
            addItemCard(recommendedContainer, visibleItems.get(i), true);
        }
    }

    private void renderLatestItems() {
        latestContainer.removeAllViews();

        ArrayList<ItemData> visibleItems = getVisibleActiveItems();

        ArrayList<ItemData> filteredItems = new ArrayList<>();

        for (ItemData item : visibleItems) {
            if (selectedCategory.equalsIgnoreCase("All")) {
                filteredItems.add(item);
            } else if (item.category.equalsIgnoreCase(selectedCategory)) {
                filteredItems.add(item);
            }
        }

        sortItems(filteredItems);

        if (filteredItems.isEmpty()) {
            showEmptyCard(latestContainer, "No items available");
            return;
        }

        for (ItemData item : filteredItems) {
            addItemCard(latestContainer, item, false);
        }
    }

    private ArrayList<ItemData> getVisibleActiveItems() {
        ArrayList<ItemData> visibleItems = new ArrayList<>();

        for (ItemData item : allItems) {
            if (item.sold || item.hidden) {
                continue;
            }

            visibleItems.add(item);
        }

        return visibleItems;
    }

    private void sortItems(ArrayList<ItemData> items) {
        if (selectedSort.equals("oldest")) {
            items.sort((a, b) -> Long.compare(a.createdAt, b.createdAt));
        } else if (selectedSort.equals("price_low")) {
            items.sort((a, b) -> Double.compare(a.price, b.price));
        } else if (selectedSort.equals("price_high")) {
            items.sort((a, b) -> Double.compare(b.price, a.price));
        } else {
            items.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
        }
    }

    private void addItemCard(LinearLayout parent, ItemData item, boolean recommended) {
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

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(112), dp(126));
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

        if (recommended) {
            badgeText.setText("FOR YOU");
            badgeText.setTextColor(getResources().getColor(R.color.accent_lime));
        } else if (item.saleType.equalsIgnoreCase("auction")) {
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

        TextView subText = new TextView(this);

        if (recommended) {
            subText.setText("Based on your activity");
        } else {
            subText.setText("Tap to view");
        }

        subText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        subText.setTextSize(12);

        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.setMargins(0, dp(8), 0, 0);
        subText.setLayoutParams(subParams);
        infoLayout.addView(subText);

        card.addView(infoLayout);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ItemDetailsActivity.class);
            intent.putExtra("itemId", item.documentId);
            startActivity(intent);
        });

        parent.addView(card);
    }

    private void showLoadingCard(LinearLayout parent, String text) {
        parent.removeAllViews();

        TextView loading = new TextView(this);
        loading.setText(text);
        loading.setTextColor(getResources().getColor(R.color.text_secondary_light));
        loading.setTextSize(14);
        loading.setGravity(Gravity.CENTER);
        loading.setBackgroundResource(R.drawable.bg_card);
        loading.setPadding(dp(18), dp(22), dp(18), dp(22));

        parent.addView(loading);
    }

    private void showEmptyCard(LinearLayout parent, String text) {
        TextView empty = new TextView(this);
        empty.setText(text);
        empty.setTextColor(getResources().getColor(R.color.text_secondary_light));
        empty.setTextSize(14);
        empty.setGravity(Gravity.CENTER);
        empty.setBackgroundResource(R.drawable.bg_card);
        empty.setPadding(dp(18), dp(24), dp(18), dp(24));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));
        empty.setLayoutParams(params);

        parent.addView(empty);
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