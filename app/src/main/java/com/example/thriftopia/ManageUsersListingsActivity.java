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
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageUsersListingsActivity extends AppCompatActivity {

    TextView btnBack;
    EditText etAdminSearch;

    TextView chipAll, chipUsers, chipActive, chipSold, chipAuction;
    TextView tvResultCount;

    LinearLayout adminContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    FirebaseStorage firebaseStorage;

    String selectedFilter = "all";

    List<UserData> userList = new ArrayList<>();
    List<ListingData> listingList = new ArrayList<>();

    static class UserData {
        String uid;
        String fullName;
        String email;
        String role;
        String profileImageUrl;
        long createdAt;

        UserData(String uid, String fullName, String email, String role, String profileImageUrl, long createdAt) {
            this.uid = uid;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.profileImageUrl = profileImageUrl;
            this.createdAt = createdAt;
        }
    }

    static class ListingData {
        String itemId;
        String sellerId;
        String title;
        String category;
        String condition;
        String brand;
        String size;
        String imageUrl;
        String imagePath;
        String status;
        String saleType;
        double price;

        ListingData(String itemId, String sellerId, String title, String category,
                    String condition, String brand, String size, String imageUrl,
                    String imagePath, String status, String saleType, double price) {
            this.itemId = itemId;
            this.sellerId = sellerId;
            this.title = title;
            this.category = category;
            this.condition = condition;
            this.brand = brand;
            this.size = size;
            this.imageUrl = imageUrl;
            this.imagePath = imagePath;
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

        setContentView(R.layout.activity_manage_users_listings);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        bindViews();
        setupClicks();
        loadAllData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllData();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        etAdminSearch = findViewById(R.id.etAdminSearch);

        chipAll = findViewById(R.id.chipAll);
        chipUsers = findViewById(R.id.chipUsers);
        chipActive = findViewById(R.id.chipActive);
        chipSold = findViewById(R.id.chipSold);
        chipAuction = findViewById(R.id.chipAuction);

        tvResultCount = findViewById(R.id.tvResultCount);
        adminContainer = findViewById(R.id.adminContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        chipAll.setOnClickListener(v -> selectFilter("all"));
        chipUsers.setOnClickListener(v -> selectFilter("users"));
        chipActive.setOnClickListener(v -> selectFilter("active"));
        chipSold.setOnClickListener(v -> selectFilter("sold"));
        chipAuction.setOnClickListener(v -> selectFilter("auction"));

        etAdminSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderAdminList();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void selectFilter(String filter) {
        selectedFilter = filter;
        updateChipStyles();
        renderAdminList();
    }

    private void updateChipStyles() {
        setChip(chipAll, selectedFilter.equals("all"));
        setChip(chipUsers, selectedFilter.equals("users"));
        setChip(chipActive, selectedFilter.equals("active"));
        setChip(chipSold, selectedFilter.equals("sold"));
        setChip(chipAuction, selectedFilter.equals("auction"));
    }

    private void setChip(TextView chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_card);
        chip.setTextColor(getResources().getColor(active ? R.color.text_primary_light : R.color.text_secondary_light));
    }

    private void loadAllData() {
        adminContainer.removeAllViews();

        TextView loadingText = new TextView(this);
        loadingText.setText("Loading admin data...");
        loadingText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        loadingText.setTextSize(14);
        adminContainer.addView(loadingText);

        userList.clear();
        listingList.clear();

        loadUsers();
        loadListings();
    }

    private void loadUsers() {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String uid = document.getString("uid");

                        if (uid == null || uid.trim().isEmpty()) {
                            uid = document.getId();
                        }

                        String fullName = document.getString("fullName");
                        String email = document.getString("email");
                        String role = document.getString("role");
                        String profileImageUrl = document.getString("profileImageUrl");
                        Long createdAtValue = document.getLong("createdAt");

                        userList.add(new UserData(
                                uid,
                                safe(fullName),
                                safe(email),
                                safe(role),
                                safe(profileImageUrl),
                                createdAtValue != null ? createdAtValue : 0
                        ));
                    }

                    renderAdminList();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    renderAdminList();
                });
    }

    private void loadListings() {
        firestore.collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listingList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String itemId = document.getString("itemId");

                        if (itemId == null || itemId.trim().isEmpty()) {
                            itemId = document.getId();
                        }

                        String sellerId = document.getString("sellerId");
                        String title = document.getString("title");
                        String category = document.getString("category");
                        String condition = document.getString("condition");
                        String brand = document.getString("brand");
                        String size = document.getString("size");
                        String imageUrl = document.getString("imageUrl");
                        String imagePath = document.getString("imagePath");
                        String status = document.getString("status");
                        String saleType = document.getString("saleType");

                        Double priceValue = document.getDouble("price");

                        listingList.add(new ListingData(
                                itemId,
                                safe(sellerId),
                                safe(title),
                                safe(category),
                                safe(condition),
                                safe(brand),
                                safe(size),
                                safe(imageUrl),
                                safe(imagePath),
                                safe(status),
                                safe(saleType),
                                priceValue != null ? priceValue : 0
                        ));
                    }

                    renderAdminList();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load listings", Toast.LENGTH_SHORT).show();
                    renderAdminList();
                });
    }

    private void renderAdminList() {
        if (adminContainer == null) {
            return;
        }

        adminContainer.removeAllViews();

        String search = etAdminSearch.getText().toString().trim().toLowerCase(Locale.getDefault());

        int resultCount = 0;

        if (selectedFilter.equals("all") || selectedFilter.equals("users")) {
            TextView sectionUsers = createSectionTitle("Users");
            adminContainer.addView(sectionUsers);

            int userCount = 0;

            for (UserData user : userList) {
                if (matchesUserSearch(user, search)) {
                    addUserCard(user);
                    userCount++;
                    resultCount++;
                }
            }

            if (userCount == 0) {
                addSmallEmpty("No users found.");
            }
        }

        if (!selectedFilter.equals("users")) {
            TextView sectionListings = createSectionTitle("Listings");
            adminContainer.addView(sectionListings);

            int listingCount = 0;

            for (ListingData listing : listingList) {
                if (matchesListingFilter(listing) && matchesListingSearch(listing, search)) {
                    addListingCard(listing);
                    listingCount++;
                    resultCount++;
                }
            }

            if (listingCount == 0) {
                addSmallEmpty("No listings found.");
            }
        }

        tvResultCount.setText(resultCount + " result(s)");

        if (resultCount == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No data found.");
            emptyText.setTextColor(getResources().getColor(R.color.text_secondary_light));
            emptyText.setTextSize(14);
            emptyText.setPadding(0, dp(20), 0, 0);
            adminContainer.addView(emptyText);
        }
    }

    private boolean matchesUserSearch(UserData user, String search) {
        if (search.isEmpty()) {
            return true;
        }

        return user.fullName.toLowerCase(Locale.getDefault()).contains(search)
                || user.email.toLowerCase(Locale.getDefault()).contains(search)
                || user.role.toLowerCase(Locale.getDefault()).contains(search)
                || user.uid.toLowerCase(Locale.getDefault()).contains(search);
    }

    private boolean matchesListingSearch(ListingData listing, String search) {
        if (search.isEmpty()) {
            return true;
        }

        return listing.title.toLowerCase(Locale.getDefault()).contains(search)
                || listing.category.toLowerCase(Locale.getDefault()).contains(search)
                || listing.condition.toLowerCase(Locale.getDefault()).contains(search)
                || listing.brand.toLowerCase(Locale.getDefault()).contains(search)
                || listing.size.toLowerCase(Locale.getDefault()).contains(search)
                || listing.status.toLowerCase(Locale.getDefault()).contains(search)
                || listing.saleType.toLowerCase(Locale.getDefault()).contains(search);
    }

    private boolean matchesListingFilter(ListingData listing) {
        if (selectedFilter.equals("all")) {
            return true;
        }

        if (selectedFilter.equals("active")) {
            return listing.status.equalsIgnoreCase("active") || listing.status.trim().isEmpty();
        }

        if (selectedFilter.equals("sold")) {
            return listing.status.equalsIgnoreCase("sold");
        }

        if (selectedFilter.equals("auction")) {
            return listing.saleType.equalsIgnoreCase("auction");
        }

        return true;
    }

    private TextView createSectionTitle(String title) {
        TextView textView = new TextView(this);
        textView.setText(title);
        textView.setTextColor(getResources().getColor(R.color.text_primary_light));
        textView.setTextSize(20);
        textView.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(8), 0, dp(12));
        textView.setLayoutParams(params);

        return textView;
    }

    private void addUserCard(UserData user) {
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

        ImageView profileImage = new ImageView(this);
        profileImage.setBackgroundResource(R.drawable.bg_hero);
        profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(60), dp(60));
        profileImage.setLayoutParams(imageParams);

        if (!user.profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(user.profileImageUrl)
                    .centerCrop()
                    .into(profileImage);
        }

        topRow.addView(profileImage);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        infoParams.setMargins(dp(14), 0, 0, 0);
        infoLayout.setLayoutParams(infoParams);

        TextView nameText = new TextView(this);
        nameText.setText(!user.fullName.isEmpty() ? user.fullName : "Thriftopia User");
        nameText.setTextColor(getResources().getColor(R.color.text_primary_light));
        nameText.setTextSize(16);
        nameText.setTypeface(null, Typeface.BOLD);
        infoLayout.addView(nameText);

        TextView emailText = new TextView(this);
        emailText.setText(!user.email.isEmpty() ? user.email : "No email");
        emailText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        emailText.setTextSize(13);
        infoLayout.addView(emailText);

        TextView roleText = new TextView(this);
        roleText.setText("Role: " + (!user.role.isEmpty() ? user.role : "user"));
        roleText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        roleText.setTextSize(12);
        infoLayout.addView(roleText);

        topRow.addView(infoLayout);
        card.addView(topRow);

        TextView btnDelete = createActionButton("DELETE USER", false);

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        deleteParams.setMargins(0, dp(14), 0, 0);
        btnDelete.setLayoutParams(deleteParams);

        btnDelete.setOnClickListener(v -> confirmDeleteUser(user));

        card.addView(btnDelete);
        adminContainer.addView(card);
    }

    private void addListingCard(ListingData listing) {
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

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(84), dp(84));
        itemImage.setLayoutParams(imageParams);

        if (!listing.imageUrl.isEmpty()) {
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

        TextView titleText = new TextView(this);
        titleText.setText(!listing.title.isEmpty() ? listing.title : "Untitled Item");
        titleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);
        infoLayout.addView(titleText);

        TextView priceText = new TextView(this);
        priceText.setText("RM " + String.format(Locale.getDefault(), "%.2f", listing.price));
        priceText.setTextColor(getResources().getColor(R.color.accent_lime));
        priceText.setTextSize(14);
        priceText.setTypeface(null, Typeface.BOLD);
        infoLayout.addView(priceText);

        TextView detailText = new TextView(this);
        detailText.setText(valueOrDash(listing.category) + " • " + valueOrDash(listing.condition) + " • " + valueOrDash(listing.size));
        detailText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        detailText.setTextSize(12);
        infoLayout.addView(detailText);

        TextView statusText = new TextView(this);
        statusText.setText("Status: " + valueOrDash(listing.status) + " | Type: " + valueOrDash(listing.saleType));
        statusText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        statusText.setTextSize(12);
        infoLayout.addView(statusText);

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
        TextView btnDelete = createActionButton("DELETE", false);

        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        viewParams.setMargins(0, 0, dp(7), 0);
        btnView.setLayoutParams(viewParams);

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        deleteParams.setMargins(dp(7), 0, 0, 0);
        btnDelete.setLayoutParams(deleteParams);

        btnView.setOnClickListener(v -> {
            Intent intent = new Intent(ManageUsersListingsActivity.this, ItemDetailsActivity.class);
            intent.putExtra("itemId", listing.itemId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> confirmDeleteListing(listing));

        buttonRow.addView(btnView);
        buttonRow.addView(btnDelete);
        card.addView(buttonRow);

        adminContainer.addView(card);
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

        if (primary) {
            button.setBackgroundResource(R.drawable.bg_primary_button);
        } else {
            button.setBackgroundResource(R.drawable.bg_outline_button);
        }

        return button;
    }

    private void confirmDeleteUser(UserData user) {
        if (firebaseAuth.getCurrentUser() != null && user.uid.equals(firebaseAuth.getCurrentUser().getUid())) {
            Toast.makeText(this, "Admin cannot delete own account here", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete User");
        builder.setMessage("Delete this user profile?\n\n" + valueOrDash(user.fullName) + "\n" + valueOrDash(user.email));

        builder.setPositiveButton("Delete", (dialog, which) -> deleteUser(user));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteUser(UserData user) {
        firestore.collection("users")
                .document(user.uid)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "User profile deleted", Toast.LENGTH_SHORT).show();
                    loadAllData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void confirmDeleteListing(ListingData listing) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Listing");
        builder.setMessage("Delete this listing permanently?\n\n" + valueOrDash(listing.title));

        builder.setPositiveButton("Delete", (dialog, which) -> deleteListing(listing));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteListing(ListingData listing) {
        firestore.collection("items")
                .document(listing.itemId)
                .delete()
                .addOnSuccessListener(unused -> {
                    deleteRelatedData(listing.itemId);
                    deleteListingImage(listing);
                    Toast.makeText(this, "Listing deleted", Toast.LENGTH_SHORT).show();
                    loadAllData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete listing: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteRelatedData(String itemId) {
        firestore.collection("watchlist")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        firestore.collection("watchlist").document(document.getId()).delete();
                    }
                });

        firestore.collection("cart")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        firestore.collection("cart").document(document.getId()).delete();
                    }
                });

        firestore.collection("auctions")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        firestore.collection("auctions").document(document.getId()).delete();
                    }
                });
    }

    private void deleteListingImage(ListingData listing) {
        if (listing.imagePath == null || listing.imagePath.trim().isEmpty()) {
            return;
        }

        firebaseStorage.getReference()
                .child(listing.imagePath)
                .delete();
    }

    private void addSmallEmpty(String message) {
        TextView emptyText = new TextView(this);
        emptyText.setText(message);
        emptyText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        emptyText.setTextSize(13);
        emptyText.setPadding(0, 0, 0, dp(12));
        adminContainer.addView(emptyText);
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