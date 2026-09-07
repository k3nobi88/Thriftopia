package com.example.thriftopia;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    ImageView imgProfilePhoto;
    TextView tvProfileName, tvProfileEmail, tvSellerRating;
    TextView tvListingCount, tvWatchlistCount;
    TextView menuMyListings, menuPurchases, menuWatchlist, menuSavedSearches;
    TextView menuAnalytics, menuEditProfile, menuAppearance;
    TextView btnLogout;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String currentUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupSystemBars();

        setContentView(R.layout.activity_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        currentUserId = firebaseAuth.getCurrentUser().getUid();

        bindViews();
        setupClicks();
        updateAppearanceLabel();

        BottomNavHelper.setup(this, BottomNavHelper.PAGE_PROFILE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupSystemBars();
        updateAppearanceLabel();
        loadProfile();
        loadCounts();
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
        imgProfilePhoto = findViewById(R.id.imgProfilePhoto);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvSellerRating = findViewById(R.id.tvSellerRating);

        tvListingCount = findViewById(R.id.tvListingCount);
        tvWatchlistCount = findViewById(R.id.tvWatchlistCount);

        menuMyListings = findViewById(R.id.menuMyListings);
        menuPurchases = findViewById(R.id.menuPurchases);
        menuWatchlist = findViewById(R.id.menuWatchlist);
        menuSavedSearches = findViewById(R.id.menuSavedSearches);
        menuAnalytics = findViewById(R.id.menuAnalytics);
        menuEditProfile = findViewById(R.id.menuEditProfile);
        menuAppearance = findViewById(R.id.menuAppearance);

        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupClicks() {
        menuMyListings.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, MyListingsActivity.class))
        );

        menuPurchases.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, MyPurchasesActivity.class))
        );

        menuWatchlist.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, WatchlistActivity.class))
        );

        menuSavedSearches.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, SavedSearchesActivity.class))
        );

        menuAnalytics.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, AnalyticsDashboardActivity.class))
        );

        menuEditProfile.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class))
        );

        menuAppearance.setOnClickListener(v -> showAppearanceDialog());

        btnLogout.setOnClickListener(v -> {
            firebaseAuth.signOut();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showAppearanceDialog() {
        String savedMode = ThemeManager.getSavedThemeMode(this);

        String[] options = {
                "System Default",
                "Light Mode",
                "Dark Mode"
        };

        int checkedIndex = 0;

        if (ThemeManager.MODE_LIGHT.equals(savedMode)) {
            checkedIndex = 1;
        } else if (ThemeManager.MODE_DARK.equals(savedMode)) {
            checkedIndex = 2;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Appearance");

        builder.setSingleChoiceItems(options, checkedIndex, (dialog, which) -> {
            String selectedMode = ThemeManager.MODE_SYSTEM;

            if (which == 1) {
                selectedMode = ThemeManager.MODE_LIGHT;
            } else if (which == 2) {
                selectedMode = ThemeManager.MODE_DARK;
            }

            ThemeManager.saveThemeMode(ProfileActivity.this, selectedMode);
            updateAppearanceLabel();

            Toast.makeText(ProfileActivity.this, "Appearance updated", Toast.LENGTH_SHORT).show();

            dialog.dismiss();
            recreate();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateAppearanceLabel() {
        if (menuAppearance == null) {
            return;
        }

        String mode = ThemeManager.getSavedThemeMode(this);

        if (ThemeManager.MODE_LIGHT.equals(mode)) {
            menuAppearance.setText("Appearance • Light Mode");
        } else if (ThemeManager.MODE_DARK.equals(mode)) {
            menuAppearance.setText("Appearance • Dark Mode");
        } else {
            menuAppearance.setText("Appearance • System Default");
        }
    }

    private void loadProfile() {
        firestore.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        if (firebaseAuth.getCurrentUser() != null && firebaseAuth.getCurrentUser().getEmail() != null) {
                            tvProfileEmail.setText(firebaseAuth.getCurrentUser().getEmail());
                        }

                        tvProfileName.setText("Thriftopia User");
                        tvSellerRating.setText("Rating: No ratings yet");
                        return;
                    }

                    String fullName = documentSnapshot.getString("fullName");
                    String email = documentSnapshot.getString("email");
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                    if (fullName != null && !fullName.trim().isEmpty()) {
                        tvProfileName.setText(fullName);
                    } else {
                        tvProfileName.setText("Thriftopia User");
                    }

                    if (email != null && !email.trim().isEmpty()) {
                        tvProfileEmail.setText(email);
                    } else if (firebaseAuth.getCurrentUser() != null && firebaseAuth.getCurrentUser().getEmail() != null) {
                        tvProfileEmail.setText(firebaseAuth.getCurrentUser().getEmail());
                    }

                    if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(profileImageUrl)
                                .transform(new CircleCrop())
                                .into(imgProfilePhoto);
                    }

                    Double average = documentSnapshot.getDouble("ratingAverage");
                    Long count = documentSnapshot.getLong("ratingCount");

                    if (average != null && count != null && count > 0) {
                        tvSellerRating.setText("Rating: " + String.format(Locale.getDefault(), "%.1f", average) + " ★ (" + count + ")");
                    } else {
                        tvSellerRating.setText("Rating: No ratings yet");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadCounts() {
        firestore.collection("items")
                .whereEqualTo("sellerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;

                    for (QueryDocumentSnapshot ignored : queryDocumentSnapshots) {
                        count++;
                    }

                    tvListingCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> tvListingCount.setText("0"));

        firestore.collection("watchlist")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;

                    for (QueryDocumentSnapshot ignored : queryDocumentSnapshots) {
                        count++;
                    }

                    tvWatchlistCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> tvWatchlistCount.setText("0"));
    }
}