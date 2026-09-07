package com.example.thriftopia;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AuctionDetailActivity extends AppCompatActivity {

    TextView btnBack, btnPlaceBid;
    TextView tvAuctionStatus, tvAuctionTitle, tvAuctionInfo;
    TextView tvCurrentBid, tvMinimumBid, tvCountdown;
    EditText etBidAmount;
    ImageView imgAuctionImage;
    LinearLayout bidsContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String auctionId = "";
    String itemId = "";
    String sellerId = "";
    String title = "";
    String category = "";
    String condition = "";
    String brand = "";
    String size = "";
    String imageUrl = "";
    String highestBidderId = "";
    String auctionStatus = "live";

    double startingBid = 0;
    double currentBid = 0;
    double bidIncrement = 1;
    long endAt = 0;
    long bidCount = 0;

    boolean isOwnAuction = false;
    boolean auctionEnded = false;
    boolean endingProcessStarted = false;

    Handler countdownHandler = new Handler(Looper.getMainLooper());

    Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            updateCountdown();
            countdownHandler.postDelayed(this, 1000);
        }
    };

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

        setContentView(R.layout.activity_auction_detail);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        auctionId = getIntent().getStringExtra("auctionId");

        if (auctionId == null || auctionId.isEmpty()) {
            Toast.makeText(this, "Invalid auction", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        btnPlaceBid = findViewById(R.id.btnPlaceBid);

        tvAuctionStatus = findViewById(R.id.tvAuctionStatus);
        tvAuctionTitle = findViewById(R.id.tvAuctionTitle);
        tvAuctionInfo = findViewById(R.id.tvAuctionInfo);
        tvCurrentBid = findViewById(R.id.tvCurrentBid);
        tvMinimumBid = findViewById(R.id.tvMinimumBid);
        tvCountdown = findViewById(R.id.tvCountdown);

        etBidAmount = findViewById(R.id.etBidAmount);
        imgAuctionImage = findViewById(R.id.imgAuctionImage);
        bidsContainer = findViewById(R.id.bidsContainer);

        btnBack.setOnClickListener(v -> finish());

        btnPlaceBid.setOnClickListener(v -> placeBid());

        loadAuctionDetails();
        loadBidHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAuctionDetails();
        loadBidHistory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        countdownHandler.removeCallbacks(countdownRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        countdownHandler.removeCallbacks(countdownRunnable);
    }

    private void loadAuctionDetails() {
        countdownHandler.removeCallbacks(countdownRunnable);

        firestore.collection("auctions")
                .document(auctionId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "Auction not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    itemId = safeString(document.getString("itemId"));
                    sellerId = safeString(document.getString("sellerId"));
                    title = safeString(document.getString("title"));
                    category = safeString(document.getString("category"));
                    condition = safeString(document.getString("condition"));
                    brand = safeString(document.getString("brand"));
                    size = safeString(document.getString("size"));
                    imageUrl = safeString(document.getString("imageUrl"));
                    highestBidderId = safeString(document.getString("highestBidderId"));
                    auctionStatus = safeString(document.getString("status"));

                    if (auctionStatus.isEmpty()) {
                        auctionStatus = "live";
                    }

                    Double startingBidValue = document.getDouble("startingBid");
                    Double currentBidValue = document.getDouble("currentBid");
                    Double bidIncrementValue = document.getDouble("bidIncrement");
                    Long endAtValue = document.getLong("endAt");
                    Long bidCountValue = document.getLong("bidCount");

                    startingBid = startingBidValue != null ? startingBidValue : 0;
                    currentBid = currentBidValue != null ? currentBidValue : startingBid;
                    bidIncrement = bidIncrementValue != null ? bidIncrementValue : 1;
                    endAt = endAtValue != null ? endAtValue : 0;
                    bidCount = bidCountValue != null ? bidCountValue : 0;

                    displayAuctionData();

                    if (auctionStatus.equals("ended") || endAt <= System.currentTimeMillis()) {
                        handleAuctionEnded();
                    } else {
                        auctionEnded = false;
                        endingProcessStarted = false;
                        checkBidPermission();
                        countdownHandler.post(countdownRunnable);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load auction: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void displayAuctionData() {
        tvAuctionStatus.setText(auctionStatus.equals("ended") ? "ENDED" : "LIVE");
        tvAuctionTitle.setText(!title.isEmpty() ? title : "Auction Item");

        String info = "";
        if (!category.isEmpty()) info += category;
        if (!condition.isEmpty()) info += info.isEmpty() ? condition : " • " + condition;
        if (!brand.isEmpty()) info += info.isEmpty() ? brand : " • " + brand;
        if (!size.isEmpty()) info += info.isEmpty() ? "Size " + size : " • Size " + size;

        tvAuctionInfo.setText(!info.isEmpty() ? info : "Auction item details");

        tvCurrentBid.setText(String.format(Locale.getDefault(), "RM%.0f", currentBid));

        double minimumBid = getMinimumBid();
        tvMinimumBid.setText(String.format(Locale.getDefault(), "Minimum bid: RM%.0f", minimumBid));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(imgAuctionImage);
        }
    }

    private double getMinimumBid() {
        if (bidCount <= 0) {
            return startingBid;
        }

        return currentBid + bidIncrement;
    }

    private void updateCountdown() {
        if (auctionEnded) {
            countdownHandler.removeCallbacks(countdownRunnable);
            return;
        }

        if (endAt == 0) {
            tvCountdown.setText("--:--:--");
            return;
        }

        long remaining = endAt - System.currentTimeMillis();

        if (remaining <= 0) {
            handleAuctionEnded();
            return;
        }

        long seconds = remaining / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs));
    }

    private void checkBidPermission() {
        if (firebaseAuth.getCurrentUser() == null) {
            etBidAmount.setEnabled(false);
            etBidAmount.setHint("Login to place a bid");

            btnPlaceBid.setEnabled(false);
            btnPlaceBid.setAlpha(0.6f);
            btnPlaceBid.setText("LOGIN TO BID");
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        isOwnAuction = sellerId != null && sellerId.equals(currentUserId);

        if (isOwnAuction) {
            etBidAmount.setEnabled(false);
            etBidAmount.setHint("You cannot bid on your own auction");

            btnPlaceBid.setEnabled(false);
            btnPlaceBid.setAlpha(0.6f);
            btnPlaceBid.setText("OWN AUCTION");
        } else {
            etBidAmount.setEnabled(true);
            etBidAmount.setHint(String.format(Locale.getDefault(), "Enter at least RM%.0f", getMinimumBid()));

            btnPlaceBid.setEnabled(true);
            btnPlaceBid.setAlpha(1f);
            btnPlaceBid.setText("PLACE BID");
        }
    }

    private void handleAuctionEnded() {
        auctionEnded = true;
        auctionStatus = "ended";

        countdownHandler.removeCallbacks(countdownRunnable);

        tvAuctionStatus.setText("ENDED");
        tvCountdown.setText("Auction ended");

        etBidAmount.setEnabled(false);
        etBidAmount.setHint("Auction has ended");

        btnPlaceBid.setEnabled(false);
        btnPlaceBid.setAlpha(0.6f);
        btnPlaceBid.setText("AUCTION ENDED");

        loadWinnerInfo();

        if (endingProcessStarted) {
            return;
        }

        endingProcessStarted = true;

        if (highestBidderId != null && !highestBidderId.isEmpty()) {
            createAuctionWinnerOrder();
        } else {
            firestore.collection("auctions")
                    .document(auctionId)
                    .update("status", "ended", "endedAt", System.currentTimeMillis());
        }
    }

    private void createAuctionWinnerOrder() {
        String orderId = "auction_" + auctionId;
        String winnerNotificationId = "auction_win_" + auctionId + "_" + highestBidderId;
        String sellerNotificationId = "auction_sold_" + auctionId + "_" + sellerId;

        DocumentReference auctionRef = firestore.collection("auctions").document(auctionId);
        DocumentReference itemRef = firestore.collection("items").document(itemId);
        DocumentReference orderRef = firestore.collection("orders").document(orderId);
        DocumentReference winnerNotificationRef = firestore.collection("notifications").document(winnerNotificationId);
        DocumentReference sellerNotificationRef = firestore.collection("notifications").document(sellerNotificationId);

        Map<String, Object> auctionUpdates = new HashMap<>();
        auctionUpdates.put("status", "ended");
        auctionUpdates.put("endedAt", System.currentTimeMillis());
        auctionUpdates.put("orderId", orderId);

        Map<String, Object> itemUpdates = new HashMap<>();
        itemUpdates.put("status", "sold");
        itemUpdates.put("buyerId", highestBidderId);
        itemUpdates.put("soldAt", System.currentTimeMillis());
        itemUpdates.put("soldBy", "auction");
        itemUpdates.put("orderId", orderId);

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("auctionId", auctionId);
        order.put("itemId", itemId);
        order.put("buyerId", highestBidderId);
        order.put("sellerId", sellerId);
        order.put("title", title);
        order.put("category", category);
        order.put("condition", condition);
        order.put("brand", brand);
        order.put("size", size);
        order.put("priceText", String.format(Locale.getDefault(), "RM%.0f", currentBid));
        order.put("priceValue", currentBid);
        order.put("imageUrl", imageUrl);
        order.put("status", "purchased");
        order.put("purchaseType", "auction");
        order.put("createdAt", System.currentTimeMillis());

        Map<String, Object> winnerNotification = new HashMap<>();
        winnerNotification.put("notificationId", winnerNotificationId);
        winnerNotification.put("userId", highestBidderId);
        winnerNotification.put("title", "You won an auction");
        winnerNotification.put("message", "You won the auction for " + title + ".");
        winnerNotification.put("type", "auction_win");
        winnerNotification.put("itemId", itemId);
        winnerNotification.put("auctionId", auctionId);
        winnerNotification.put("orderId", orderId);
        winnerNotification.put("isRead", false);
        winnerNotification.put("createdAt", System.currentTimeMillis());

        Map<String, Object> sellerNotification = new HashMap<>();
        sellerNotification.put("notificationId", sellerNotificationId);
        sellerNotification.put("userId", sellerId);
        sellerNotification.put("title", "Auction item sold");
        sellerNotification.put("message", title + " was sold through auction.");
        sellerNotification.put("type", "auction_sold");
        sellerNotification.put("itemId", itemId);
        sellerNotification.put("auctionId", auctionId);
        sellerNotification.put("orderId", orderId);
        sellerNotification.put("isRead", false);
        sellerNotification.put("createdAt", System.currentTimeMillis());

        WriteBatch batch = firestore.batch();
        batch.update(auctionRef, auctionUpdates);
        batch.update(itemRef, itemUpdates);
        batch.set(orderRef, order);
        batch.set(winnerNotificationRef, winnerNotification);

        if (sellerId != null && !sellerId.isEmpty()) {
            batch.set(sellerNotificationRef, sellerNotification);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    deleteWatchlistRecordsForSoldItem();
                    tvMinimumBid.setText("Winner order created");
                    loadWinnerInfo();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create auction order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteWatchlistRecordsForSoldItem() {
        if (itemId == null || itemId.isEmpty()) {
            return;
        }

        firestore.collection("watchlist")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        firestore.collection("watchlist")
                                .document(document.getId())
                                .delete();
                    }
                });
    }

    private void loadWinnerInfo() {
        if (highestBidderId == null || highestBidderId.isEmpty()) {
            tvMinimumBid.setText("Winner: No bids placed");
            return;
        }

        firestore.collection("users")
                .document(highestBidderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String email = documentSnapshot.getString("email");

                        if (fullName != null && !fullName.trim().isEmpty()) {
                            tvMinimumBid.setText("Winner: " + fullName);
                        } else if (email != null && !email.trim().isEmpty()) {
                            tvMinimumBid.setText("Winner: " + email);
                        } else {
                            tvMinimumBid.setText("Winner: User found");
                        }
                    } else {
                        tvMinimumBid.setText("Winner: User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    tvMinimumBid.setText("Winner: " + highestBidderId);
                });
    }

    private void placeBid() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (sellerId != null && sellerId.equals(currentUserId)) {
            Toast.makeText(this, "You cannot bid on your own auction", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auctionEnded) {
            Toast.makeText(this, "Auction has ended", Toast.LENGTH_SHORT).show();
            return;
        }

        String bidText = etBidAmount.getText().toString().trim();

        if (bidText.isEmpty()) {
            etBidAmount.setError("Enter bid amount");
            etBidAmount.requestFocus();
            return;
        }

        double bidAmount;

        try {
            bidAmount = Double.parseDouble(bidText);
        } catch (Exception e) {
            etBidAmount.setError("Invalid bid amount");
            etBidAmount.requestFocus();
            return;
        }

        double minimumBid = getMinimumBid();

        if (bidAmount < minimumBid) {
            etBidAmount.setError(String.format(Locale.getDefault(), "Minimum bid is RM%.0f", minimumBid));
            etBidAmount.requestFocus();
            return;
        }

        btnPlaceBid.setEnabled(false);
        btnPlaceBid.setText("PLACING BID...");

        DocumentReference auctionRef = firestore.collection("auctions").document(auctionId);
        DocumentReference bidRef = firestore.collection("bids").document();

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(auctionRef);

                    if (!snapshot.exists()) {
                        throw new FirebaseFirestoreException("Auction not found", FirebaseFirestoreException.Code.NOT_FOUND);
                    }

                    String transactionStatus = snapshot.getString("status");
                    String transactionSellerId = snapshot.getString("sellerId");
                    Long transactionEndAtValue = snapshot.getLong("endAt");
                    Long transactionBidCountValue = snapshot.getLong("bidCount");

                    Double transactionStartingBidValue = snapshot.getDouble("startingBid");
                    Double transactionCurrentBidValue = snapshot.getDouble("currentBid");
                    Double transactionBidIncrementValue = snapshot.getDouble("bidIncrement");

                    long transactionEndAt = transactionEndAtValue != null ? transactionEndAtValue : 0;
                    long transactionBidCount = transactionBidCountValue != null ? transactionBidCountValue : 0;

                    double transactionStartingBid = transactionStartingBidValue != null ? transactionStartingBidValue : 0;
                    double transactionCurrentBid = transactionCurrentBidValue != null ? transactionCurrentBidValue : transactionStartingBid;
                    double transactionBidIncrement = transactionBidIncrementValue != null ? transactionBidIncrementValue : 1;

                    if (transactionStatus == null || transactionStatus.equals("ended") || transactionEndAt <= System.currentTimeMillis()) {
                        throw new FirebaseFirestoreException("Auction has ended", FirebaseFirestoreException.Code.ABORTED);
                    }

                    if (transactionSellerId != null && transactionSellerId.equals(currentUserId)) {
                        throw new FirebaseFirestoreException("You cannot bid on your own auction", FirebaseFirestoreException.Code.PERMISSION_DENIED);
                    }

                    double transactionMinimumBid;

                    if (transactionBidCount <= 0) {
                        transactionMinimumBid = transactionStartingBid;
                    } else {
                        transactionMinimumBid = transactionCurrentBid + transactionBidIncrement;
                    }

                    if (bidAmount < transactionMinimumBid) {
                        throw new FirebaseFirestoreException(
                                String.format(Locale.getDefault(), "Minimum bid is RM%.0f", transactionMinimumBid),
                                FirebaseFirestoreException.Code.ABORTED
                        );
                    }

                    Map<String, Object> auctionUpdates = new HashMap<>();
                    auctionUpdates.put("currentBid", bidAmount);
                    auctionUpdates.put("highestBidderId", currentUserId);
                    auctionUpdates.put("bidCount", transactionBidCount + 1);
                    auctionUpdates.put("updatedAt", System.currentTimeMillis());

                    transaction.update(auctionRef, auctionUpdates);

                    Map<String, Object> bid = new HashMap<>();
                    bid.put("bidId", bidRef.getId());
                    bid.put("auctionId", auctionId);
                    bid.put("itemId", itemId);
                    bid.put("sellerId", sellerId);
                    bid.put("bidderId", currentUserId);
                    bid.put("bidAmount", bidAmount);
                    bid.put("createdAt", System.currentTimeMillis());

                    transaction.set(bidRef, bid);

                    return null;
                })
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Bid placed successfully", Toast.LENGTH_SHORT).show();
                    etBidAmount.setText("");
                    loadAuctionDetails();
                    loadBidHistory();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to place bid: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    checkBidPermission();
                });
    }

    private void loadBidHistory() {
        bidsContainer.removeAllViews();

        TextView loadingText = new TextView(this);
        loadingText.setText("Loading bid history...");
        loadingText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        loadingText.setTextSize(14);
        bidsContainer.addView(loadingText);

        firestore.collection("bids")
                .whereEqualTo("auctionId", auctionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bidsContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView emptyText = new TextView(this);
                        emptyText.setText("No bids yet.");
                        emptyText.setTextColor(getResources().getColor(R.color.text_secondary_light));
                        emptyText.setTextSize(14);
                        bidsContainer.addView(emptyText);
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String bidderId = document.getString("bidderId");
                        Double bidAmountValue = document.getDouble("bidAmount");
                        Long createdAtValue = document.getLong("createdAt");

                        double amount = bidAmountValue != null ? bidAmountValue : 0;
                        long createdAt = createdAtValue != null ? createdAtValue : 0;

                        addBidCard(bidderId, amount, createdAt);
                    }
                })
                .addOnFailureListener(e -> {
                    bidsContainer.removeAllViews();

                    TextView errorText = new TextView(this);
                    errorText.setText("Failed to load bid history.");
                    errorText.setTextColor(getResources().getColor(R.color.error));
                    errorText.setTextSize(14);
                    bidsContainer.addView(errorText);
                });
    }

    private void addBidCard(String bidderId, double amount, long createdAt) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);

        TextView amountText = new TextView(this);
        amountText.setText(String.format(Locale.getDefault(), "RM%.0f", amount));
        amountText.setTextColor(getResources().getColor(R.color.text_primary_light));
        amountText.setTextSize(18);
        amountText.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(amountText);

        TextView bidderText = new TextView(this);
        bidderText.setText("Bidder: " + shortId(bidderId));
        bidderText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        bidderText.setTextSize(12);

        LinearLayout.LayoutParams bidderParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bidderParams.setMargins(0, dp(4), 0, 0);
        bidderText.setLayoutParams(bidderParams);
        card.addView(bidderText);

        TextView timeText = new TextView(this);
        timeText.setText("Bid time: " + formatDateTime(createdAt));
        timeText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        timeText.setTextSize(12);
        timeText.setGravity(Gravity.START);

        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.setMargins(0, dp(4), 0, 0);
        timeText.setLayoutParams(timeParams);
        card.addView(timeText);

        bidsContainer.addView(card);
    }

    private String formatDateTime(long time) {
        if (time <= 0) {
            return "-";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private String shortId(String id) {
        if (id == null || id.isEmpty()) {
            return "-";
        }

        if (id.length() <= 8) {
            return id;
        }

        return id.substring(0, 8) + "...";
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}