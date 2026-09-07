package com.example.thriftopia;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExploreActivity extends AppCompatActivity {

    EditText etSearch;
    TextView tvExploreSubtitle, tvResultCount, btnSortItems;
    TextView btnSaveSearch, btnSavedSearches;

    TextView chipAll, chipTops, chipBottoms, chipOuterwear, chipShoes, chipAccessories;
    LinearLayout exploreContainer;

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

        setContentView(R.layout.activity_explore);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();
        handleIntentFilters();

        BottomNavHelper.setup(this, BottomNavHelper.PAGE_EXPLORE);

        loadItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    private void bindViews() {
        etSearch = findViewById(R.id.etSearch);
        tvExploreSubtitle = findViewById(R.id.tvExploreSubtitle);
        tvResultCount = findViewById(R.id.tvResultCount);
        btnSortItems = findViewById(R.id.btnSortItems);

        btnSaveSearch = findViewById(R.id.btnSaveSearch);
        btnSavedSearches = findViewById(R.id.btnSavedSearches);

        chipAll = findViewById(R.id.chipAll);
        chipTops = findViewById(R.id.chipTops);
        chipBottoms = findViewById(R.id.chipBottoms);
        chipOuterwear = findViewById(R.id.chipOuterwear);
        chipShoes = findViewById(R.id.chipShoes);
        chipAccessories = findViewById(R.id.chipAccessories);

        exploreContainer = findViewById(R.id.exploreContainer);
    }

    private void setupClicks() {
        chipAll.setOnClickListener(v -> selectCategory("All"));
        chipTops.setOnClickListener(v -> selectCategory("Tops"));
        chipBottoms.setOnClickListener(v -> selectCategory("Bottoms"));
        chipOuterwear.setOnClickListener(v -> selectCategory("Outerwear"));
        chipShoes.setOnClickListener(v -> selectCategory("Shoes"));
        chipAccessories.setOnClickListener(v -> selectCategory("Accessories"));

        btnSortItems.setOnClickListener(v -> showSortDialog());
        updateSortButtonText();

        btnSaveSearch.setOnClickListener(v -> saveCurrentSearch());

        btnSavedSearches.setOnClickListener(v ->
                startActivity(new Intent(ExploreActivity.this, SavedSearchesActivity.class))
        );

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderItems();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void handleIntentFilters() {
        String categoryExtra = getIntent().getStringExtra("category");

        if (categoryExtra == null || categoryExtra.trim().isEmpty()) {
            categoryExtra = getIntent().getStringExtra("selectedCategory");
        }

        if (categoryExtra == null || categoryExtra.trim().isEmpty()) {
            categoryExtra = getIntent().getStringExtra("savedCategory");
        }

        if (categoryExtra != null && !categoryExtra.trim().isEmpty()) {
            selectedCategory = categoryExtra;
        }

        String searchExtra = getIntent().getStringExtra("searchQuery");

        if (searchExtra == null || searchExtra.trim().isEmpty()) {
            searchExtra = getIntent().getStringExtra("search");
        }

        if (searchExtra == null || searchExtra.trim().isEmpty()) {
            searchExtra = getIntent().getStringExtra("keyword");
        }

        if (searchExtra != null && !searchExtra.trim().isEmpty()) {
            etSearch.setText(searchExtra);
            etSearch.setSelection(etSearch.getText().length());
        }

        updateChipStyles();
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        updateChipStyles();
        renderItems();
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

    private void saveCurrentSearch() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String keyword = etSearch.getText().toString().trim();
        String category = selectedCategory;

        if (keyword.isEmpty() && category.equalsIgnoreCase("All")) {
            Toast.makeText(this, "Enter keyword or choose category first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String searchId = currentUserId + "_" + cleanForId(keyword) + "_" + cleanForId(category);

        Map<String, Object> savedSearch = new HashMap<>();
        savedSearch.put("searchId", searchId);
        savedSearch.put("userId", currentUserId);
        savedSearch.put("keyword", keyword);
        savedSearch.put("category", category);
        savedSearch.put("createdAt", System.currentTimeMillis());
        savedSearch.put("updatedAt", System.currentTimeMillis());

        firestore.collection("savedSearches")
                .document(searchId)
                .set(savedSearch)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Search saved", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save search: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private String cleanForId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "all";
        }

        return value.toLowerCase(Locale.getDefault())
                .replace(" ", "_")
                .replace("/", "_")
                .replace("\\", "_")
                .replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_");
    }

    private void loadItems() {
        exploreContainer.removeAllViews();
        tvResultCount.setText("Loading items...");
        showLoadingState();

        firestore.collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allItems.clear();
                    exploreContainer.removeAllViews();

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

                    renderItems();
                })
                .addOnFailureListener(e -> {
                    exploreContainer.removeAllViews();
                    tvResultCount.setText("0 item(s)");
                    showEmptyState("Failed to load items", e.getMessage(), false);
                });
    }

    private void renderItems() {
        if (exploreContainer == null) {
            return;
        }

        exploreContainer.removeAllViews();

        String search = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());

        ArrayList<ItemData> visibleItems = new ArrayList<>();

        for (ItemData item : allItems) {
            if (item.sold || item.hidden) {
                continue;
            }

            if (!matchesCategory(item)) {
                continue;
            }

            if (!matchesSearch(item, search)) {
                continue;
            }

            visibleItems.add(item);
        }

        sortItems(visibleItems);

        for (ItemData item : visibleItems) {
            addItemCard(item);
        }

        updateResultText(visibleItems.size());

        if (visibleItems.isEmpty()) {
            if (allItems.isEmpty()) {
                showEmptyState("No items available yet", "New thrift listings will appear here.", false);
            } else {
                showEmptyState("No items found", "Try another keyword or category.", true);
            }
        }
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
                    renderItems();
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

    private boolean matchesCategory(ItemData item) {
        if (selectedCategory.equalsIgnoreCase("All")) {
            return true;
        }

        return item.category.equalsIgnoreCase(selectedCategory);
    }

    private boolean matchesSearch(ItemData item, String search) {
        if (search.isEmpty()) {
            return true;
        }

        return item.title.toLowerCase(Locale.getDefault()).contains(search)
                || item.category.toLowerCase(Locale.getDefault()).contains(search)
                || item.condition.toLowerCase(Locale.getDefault()).contains(search)
                || item.brand.toLowerCase(Locale.getDefault()).contains(search)
                || item.size.toLowerCase(Locale.getDefault()).contains(search)
                || item.saleType.toLowerCase(Locale.getDefault()).contains(search);
    }

    private void updateResultText(int count) {
        String search = etSearch.getText().toString().trim();

        if (selectedCategory.equalsIgnoreCase("All") && search.isEmpty()) {
            tvResultCount.setText(count + " item(s) available");
        } else if (!selectedCategory.equalsIgnoreCase("All") && search.isEmpty()) {
            tvResultCount.setText(count + " item(s) in " + selectedCategory);
        } else if (selectedCategory.equalsIgnoreCase("All")) {
            tvResultCount.setText(count + " result(s) for \"" + search + "\"");
        } else {
            tvResultCount.setText(count + " result(s) for \"" + search + "\" in " + selectedCategory);
        }

        tvExploreSubtitle.setText("Browse active thrift listings");
    }

    private void showLoadingState() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(28), dp(18), dp(28));

        TextView title = new TextView(this);
        title.setText("Loading items...");
        title.setTextColor(getResources().getColor(R.color.text_primary_light));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Please wait while we fetch thrift listings.");
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
        exploreContainer.addView(card);
    }

    private void showEmptyState(String titleText, String subtitleText, boolean showClearButton) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(22), dp(34), dp(22), dp(34));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(6), 0, 0);
        card.setLayoutParams(cardParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_search);
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

        TextView button = new TextView(this);
        button.setText(showClearButton ? "CLEAR SEARCH" : "REFRESH");
        button.setTextColor(getResources().getColor(R.color.black));
        button.setTextSize(13);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_primary_button);
        button.setClickable(true);
        button.setFocusable(true);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        btnParams.setMargins(0, dp(22), 0, 0);
        button.setLayoutParams(btnParams);

        button.setOnClickListener(v -> {
            if (showClearButton) {
                etSearch.setText("");
                selectedCategory = "All";
                updateChipStyles();
                renderItems();
            } else {
                loadItems();
            }
        });

        card.addView(button);
        exploreContainer.addView(card);
    }

    private void addItemCard(ItemData item) {
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

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(106), dp(126));
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
        viewText.setText("Tap to view");
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
            Intent intent = new Intent(ExploreActivity.this, ItemDetailsActivity.class);
            intent.putExtra("itemId", item.documentId);
            startActivity(intent);
        });

        exploreContainer.addView(card);
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