package com.example.thriftopia;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuctionCreationActivity extends AppCompatActivity {

    TextView btnBack, btnCreateAuction, tvSelectedItem, tvItemPrice, tvDuration;
    EditText etStartingBid, etBidIncrement;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String itemId = "";
    String imageUrl = "";
    String itemTitle = "";
    String itemCategory = "";
    String itemCondition = "";
    String itemDescription = "";
    String itemBrand = "";
    String itemSize = "";
    double itemPrice = 0;
    int durationHours = 0;

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

        setContentView(R.layout.activity_auction_creation);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        itemId = getIntent().getStringExtra("itemId");

        btnBack = findViewById(R.id.btnBack);
        btnCreateAuction = findViewById(R.id.btnCreateAuction);
        tvSelectedItem = findViewById(R.id.tvSelectedItem);
        tvItemPrice = findViewById(R.id.tvItemPrice);
        tvDuration = findViewById(R.id.tvDuration);

        etStartingBid = findViewById(R.id.etStartingBid);
        etBidIncrement = findViewById(R.id.etBidIncrement);

        btnBack.setOnClickListener(v -> finish());

        tvDuration.setOnClickListener(v -> {
            showDurationDialog();
        });

        btnCreateAuction.setOnClickListener(v -> {
            createAuction();
        });

        loadSelectedItem();
    }

    private void loadSelectedItem() {
        if (itemId == null || itemId.isEmpty()) {
            tvSelectedItem.setText("No item selected");
            return;
        }

        firestore.collection("items")
                .document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        itemTitle = documentSnapshot.getString("title");
                        itemCategory = documentSnapshot.getString("category");
                        itemCondition = documentSnapshot.getString("condition");
                        itemDescription = documentSnapshot.getString("description");
                        itemBrand = documentSnapshot.getString("brand");
                        itemSize = documentSnapshot.getString("size");
                        imageUrl = documentSnapshot.getString("imageUrl");

                        Double priceValue = documentSnapshot.getDouble("price");
                        if (priceValue != null) {
                            itemPrice = priceValue;
                        }

                        tvSelectedItem.setText(itemTitle != null ? itemTitle : "Untitled Item");
                        tvItemPrice.setText("Item Price: RM" + (int) itemPrice);
                        etStartingBid.setText(String.valueOf((int) itemPrice));
                    } else {
                        tvSelectedItem.setText("Item not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showDurationDialog() {
        String[] durations = {"1 Hour", "3 Hours", "6 Hours", "12 Hours", "24 Hours"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Auction Duration");
        builder.setItems(durations, (dialog, which) -> {
            if (which == 0) durationHours = 1;
            if (which == 1) durationHours = 3;
            if (which == 2) durationHours = 6;
            if (which == 3) durationHours = 12;
            if (which == 4) durationHours = 24;

            tvDuration.setText(durations[which]);
            tvDuration.setTextColor(getResources().getColor(R.color.text_primary_light));
        });
        builder.show();
    }

    private void createAuction() {
        String startingBidText = etStartingBid.getText().toString().trim();
        String incrementText = etBidIncrement.getText().toString().trim();

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startingBidText.isEmpty()) {
            etStartingBid.setError("Starting bid is required");
            etStartingBid.requestFocus();
            return;
        }

        if (incrementText.isEmpty()) {
            etBidIncrement.setError("Bid increment is required");
            etBidIncrement.requestFocus();
            return;
        }

        if (durationHours == 0) {
            Toast.makeText(this, "Please select auction duration", Toast.LENGTH_SHORT).show();
            return;
        }

        double startingBid = Double.parseDouble(startingBidText);
        double bidIncrement = Double.parseDouble(incrementText);

        if (startingBid <= 0) {
            etStartingBid.setError("Starting bid must be more than 0");
            etStartingBid.requestFocus();
            return;
        }

        if (bidIncrement <= 0) {
            etBidIncrement.setError("Bid increment must be more than 0");
            etBidIncrement.requestFocus();
            return;
        }

        btnCreateAuction.setEnabled(false);
        btnCreateAuction.setText("CREATING...");

        String auctionId = firestore.collection("auctions").document().getId();
        String sellerId = firebaseAuth.getCurrentUser().getUid();

        long startAt = System.currentTimeMillis();
        long endAt = startAt + (durationHours * 60L * 60L * 1000L);

        Map<String, Object> auction = new HashMap<>();
        auction.put("auctionId", auctionId);
        auction.put("itemId", itemId);
        auction.put("imageUrl", imageUrl);
        auction.put("sellerId", sellerId);
        auction.put("title", itemTitle);
        auction.put("category", itemCategory);
        auction.put("condition", itemCondition);
        auction.put("description", itemDescription);
        auction.put("brand", itemBrand);
        auction.put("size", itemSize);
        auction.put("startingBid", startingBid);
        auction.put("currentBid", startingBid);
        auction.put("bidIncrement", bidIncrement);
        auction.put("highestBidderId", "");
        auction.put("bidCount", 0);
        auction.put("durationHours", durationHours);
        auction.put("status", "live");
        auction.put("createdAt", startAt);
        auction.put("endAt", endAt);

        firestore.collection("auctions")
                .document(auctionId)
                .set(auction)
                .addOnSuccessListener(unused -> {
                    firestore.collection("items")
                            .document(itemId)
                            .update("auctionId", auctionId);

                    Toast.makeText(this, "Auction created successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AuctionCreationActivity.this, AuctionDetailActivity.class);
                    intent.putExtra("auctionId", auctionId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreateAuction.setEnabled(true);
                    btnCreateAuction.setText("CREATE AUCTION");
                    Toast.makeText(this, "Failed to create auction: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}