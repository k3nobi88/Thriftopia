package com.example.thriftopia;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SavedSearchesActivity extends AppCompatActivity {

    TextView btnBack, tvSearchCount;
    LinearLayout savedSearchContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    ArrayList<SavedSearchData> savedSearchList = new ArrayList<>();

    static class SavedSearchData {
        String searchId;
        String keyword;
        String category;
        long createdAt;

        SavedSearchData(String searchId, String keyword, String category, long createdAt) {
            this.searchId = searchId;
            this.keyword = keyword;
            this.category = category;
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

        setContentView(R.layout.activity_saved_searches);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadSavedSearches();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedSearches();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvSearchCount = findViewById(R.id.tvSearchCount);
        savedSearchContainer = findViewById(R.id.savedSearchContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadSavedSearches() {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        savedSearchContainer.removeAllViews();
        tvSearchCount.setText("Loading...");
        showSimpleState("Loading saved searches...", "Please wait.");

        firestore.collection("savedSearches")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savedSearchList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String searchId = safe(document.getString("searchId"));

                        if (searchId.trim().isEmpty()) {
                            searchId = document.getId();
                        }

                        String keyword = safe(document.getString("keyword"));
                        String category = safe(document.getString("category"));

                        if (category.trim().isEmpty()) {
                            category = "All";
                        }

                        Long createdAtValue = document.getLong("createdAt");
                        long createdAt = createdAtValue != null ? createdAtValue : 0;

                        savedSearchList.add(new SavedSearchData(
                                searchId,
                                keyword,
                                category,
                                createdAt
                        ));
                    }

                    savedSearchList.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

                    renderSavedSearches();
                })
                .addOnFailureListener(e -> {
                    tvSearchCount.setText("0 saved search(es)");
                    showSimpleState("Failed to load saved searches", e.getMessage());
                });
    }

    private void renderSavedSearches() {
        savedSearchContainer.removeAllViews();
        tvSearchCount.setText(savedSearchList.size() + " saved search(es)");

        if (savedSearchList.isEmpty()) {
            showSimpleState("No saved searches yet", "Save a keyword or category from Explore.");
            return;
        }

        for (SavedSearchData search : savedSearchList) {
            addSavedSearchCard(search);
        }
    }

    private void addSavedSearchCard(SavedSearchData search) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        TextView titleText = new TextView(this);
        titleText.setText(getSavedSearchTitle(search));
        titleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        titleText.setTextSize(18);
        titleText.setTypeface(null, Typeface.BOLD);
        card.addView(titleText);

        TextView metaText = new TextView(this);
        metaText.setText(getSavedSearchMeta(search));
        metaText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        metaText.setTextSize(13);

        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metaParams.setMargins(0, dp(8), 0, 0);
        metaText.setLayoutParams(metaParams);
        card.addView(metaText);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, dp(16), 0, 0);
        actionRow.setLayoutParams(rowParams);

        TextView btnOpen = new TextView(this);
        btnOpen.setText("OPEN");
        btnOpen.setTextColor(getResources().getColor(R.color.text_primary_light));
        btnOpen.setTextSize(13);
        btnOpen.setTypeface(null, Typeface.BOLD);
        btnOpen.setGravity(Gravity.CENTER);
        btnOpen.setBackgroundResource(R.drawable.bg_primary_button);
        btnOpen.setClickable(true);
        btnOpen.setFocusable(true);

        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(
                0,
                dp(48),
                1
        );
        btnOpen.setLayoutParams(openParams);

        TextView btnDelete = new TextView(this);
        btnDelete.setText("DELETE");
        btnDelete.setTextColor(getResources().getColor(R.color.text_primary_light));
        btnDelete.setTextSize(13);
        btnDelete.setTypeface(null, Typeface.BOLD);
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setBackgroundResource(R.drawable.bg_outline_button);
        btnDelete.setClickable(true);
        btnDelete.setFocusable(true);

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                0,
                dp(48),
                1
        );
        deleteParams.setMargins(dp(12), 0, 0, 0);
        btnDelete.setLayoutParams(deleteParams);

        actionRow.addView(btnOpen);
        actionRow.addView(btnDelete);

        card.addView(actionRow);

        View.OnClickListener openListener = v -> openSavedSearch(search);
        card.setOnClickListener(openListener);
        btnOpen.setOnClickListener(openListener);

        btnDelete.setOnClickListener(v -> confirmDelete(search));

        savedSearchContainer.addView(card);
    }

    private String getSavedSearchTitle(SavedSearchData search) {
        String keyword = search.keyword.trim();
        String category = search.category.trim();

        if (keyword.isEmpty() && category.equalsIgnoreCase("All")) {
            return "All Items";
        }

        if (!keyword.isEmpty() && category.equalsIgnoreCase("All")) {
            return "\"" + keyword + "\"";
        }

        if (keyword.isEmpty()) {
            return category;
        }

        return "\"" + keyword + "\" in " + category;
    }

    private String getSavedSearchMeta(SavedSearchData search) {
        String meta = "Category: " + valueOrDash(search.category);

        if (!search.keyword.trim().isEmpty()) {
            meta += "\nKeyword: " + search.keyword;
        }

        meta += "\nSaved: " + formatDate(search.createdAt);

        return meta;
    }

    private void openSavedSearch(SavedSearchData search) {
        Intent intent = new Intent(SavedSearchesActivity.this, ExploreActivity.class);
        intent.putExtra("searchQuery", search.keyword);
        intent.putExtra("category", search.category);
        startActivity(intent);
        finish();
    }

    private void confirmDelete(SavedSearchData search) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Saved Search");
        builder.setMessage("Delete this saved search?");

        builder.setPositiveButton("Delete", (dialog, which) -> deleteSavedSearch(search));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteSavedSearch(SavedSearchData search) {
        firestore.collection("savedSearches")
                .document(search.searchId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Saved search deleted", Toast.LENGTH_SHORT).show();
                    loadSavedSearches();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void showSimpleState(String title, String subtitle) {
        savedSearchContainer.removeAllViews();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(22), dp(34), dp(22), dp(34));

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        titleText.setTextSize(20);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setGravity(Gravity.CENTER);
        card.addView(titleText);

        TextView subtitleText = new TextView(this);
        subtitleText.setText(subtitle != null ? subtitle : "");
        subtitleText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        subtitleText.setTextSize(14);
        subtitleText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.setMargins(0, dp(8), 0, 0);
        subtitleText.setLayoutParams(subParams);

        card.addView(subtitleText);
        savedSearchContainer.addView(card);
    }

    private String formatDate(long time) {
        if (time <= 0) {
            return "-";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date(time));
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