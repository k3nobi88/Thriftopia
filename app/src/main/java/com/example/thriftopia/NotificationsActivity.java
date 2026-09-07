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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {

    TextView btnBack, btnReadAll, tvNotificationSubtitle;
    LinearLayout notificationsContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    int currentLoadToken = 0;

    static class NotificationData {
        String notificationId;
        String userId;
        String title;
        String message;
        String type;
        String itemId;
        String orderId;
        boolean isRead;
        long createdAt;

        NotificationData(String notificationId, String userId, String title, String message,
                         String type, String itemId, String orderId, boolean isRead, long createdAt) {
            this.notificationId = notificationId;
            this.userId = userId;
            this.title = title;
            this.message = message;
            this.type = type;
            this.itemId = itemId;
            this.orderId = orderId;
            this.isRead = isRead;
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

        setContentView(R.layout.activity_notifications);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnReadAll = findViewById(R.id.btnReadAll);
        tvNotificationSubtitle = findViewById(R.id.tvNotificationSubtitle);
        notificationsContainer = findViewById(R.id.notificationsContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        btnReadAll.setOnClickListener(v -> markAllAsRead());
    }

    private void loadNotifications() {
        int loadToken = ++currentLoadToken;

        notificationsContainer.removeAllViews();
        showLoadingState();

        if (firebaseAuth.getCurrentUser() == null) {
            notificationsContainer.removeAllViews();
            tvNotificationSubtitle.setText("0 notification(s)");
            showEmptyState("Please login first", "Login to view your Thriftopia notifications.");
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    notificationsContainer.removeAllViews();

                    ArrayList<NotificationData> notificationList = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String notificationId = document.getString("notificationId");

                        if (notificationId == null || notificationId.trim().isEmpty()) {
                            notificationId = document.getId();
                        }

                        String userId = safe(document.getString("userId"));
                        String title = safe(document.getString("title"));
                        String message = safe(document.getString("message"));
                        String type = safe(document.getString("type"));
                        String itemId = safe(document.getString("itemId"));
                        String orderId = safe(document.getString("orderId"));

                        Boolean isReadValue = document.getBoolean("isRead");
                        boolean isRead = isReadValue != null && isReadValue;

                        Long createdAtValue = document.getLong("createdAt");
                        long createdAt = createdAtValue != null ? createdAtValue : 0;

                        notificationList.add(new NotificationData(
                                notificationId,
                                userId,
                                title,
                                message,
                                type,
                                itemId,
                                orderId,
                                isRead,
                                createdAt
                        ));
                    }

                    notificationList.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

                    int unreadCount = 0;

                    for (NotificationData notification : notificationList) {
                        if (!notification.isRead) {
                            unreadCount++;
                        }
                    }

                    if (notificationList.isEmpty()) {
                        tvNotificationSubtitle.setText("0 notification(s)");
                        btnReadAll.setVisibility(View.GONE);
                        showEmptyState("No notifications yet", "Updates about purchases, auctions, chats, and cart items will appear here.");
                        return;
                    }

                    btnReadAll.setVisibility(View.VISIBLE);
                    tvNotificationSubtitle.setText(notificationList.size() + " notification(s) • " + unreadCount + " unread");

                    for (NotificationData notification : notificationList) {
                        addNotificationCard(notification);
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    notificationsContainer.removeAllViews();
                    tvNotificationSubtitle.setText("0 notification(s)");
                    showEmptyState("Failed to load notifications", e.getMessage());
                });
    }

    private void showLoadingState() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(28), dp(18), dp(28));

        TextView title = new TextView(this);
        title.setText("Loading notifications...");
        title.setTextColor(getResources().getColor(R.color.text_primary_light));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Please wait while we fetch your latest updates.");
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
        notificationsContainer.addView(card);
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
        icon.setImageResource(R.drawable.ic_notifications);
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
        btnExplore.setText("BACK TO HOME");
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
            Intent intent = new Intent(NotificationsActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        card.addView(btnExplore);
        notificationsContainer.addView(card);
    }

    private void addNotificationCard(NotificationData notification) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
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

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_notifications);
        icon.setColorFilter(getResources().getColor(notification.isRead ? R.color.text_secondary_light : R.color.accent_lime));

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        icon.setLayoutParams(iconParams);
        topRow.addView(icon);

        LinearLayout textArea = new LinearLayout(this);
        textArea.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        textParams.setMargins(dp(12), 0, 0, 0);
        textArea.setLayoutParams(textParams);

        TextView titleText = new TextView(this);
        titleText.setText(!notification.title.trim().isEmpty() ? notification.title : "Notification");
        titleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setMaxLines(2);
        textArea.addView(titleText);

        TextView timeText = new TextView(this);
        timeText.setText(formatDate(notification.createdAt));
        timeText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        timeText.setTextSize(12);

        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.setMargins(0, dp(3), 0, 0);
        timeText.setLayoutParams(timeParams);
        textArea.addView(timeText);

        topRow.addView(textArea);

        TextView badgeText = new TextView(this);
        badgeText.setText(notification.isRead ? "READ" : "NEW");
        badgeText.setTextColor(getResources().getColor(notification.isRead ? R.color.text_secondary_light : R.color.accent_lime));
        badgeText.setTextSize(11);
        badgeText.setTypeface(null, Typeface.BOLD);
        badgeText.setGravity(Gravity.CENTER);
        topRow.addView(badgeText);

        card.addView(topRow);

        TextView messageText = new TextView(this);
        messageText.setText(!notification.message.trim().isEmpty() ? notification.message : "-");
        messageText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        messageText.setTextSize(14);

        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        msgParams.setMargins(0, dp(12), 0, 0);
        messageText.setLayoutParams(msgParams);
        card.addView(messageText);

        card.setOnClickListener(v -> {
            markSingleAsRead(notification);
            openNotificationTarget(notification);
        });

        notificationsContainer.addView(card);
    }

    private void markSingleAsRead(NotificationData notification) {
        if (notification.isRead) {
            return;
        }

        firestore.collection("notifications")
                .document(notification.notificationId)
                .update("isRead", true);
    }

    private void openNotificationTarget(NotificationData notification) {
        if (!notification.orderId.trim().isEmpty()) {
            Intent intent = new Intent(NotificationsActivity.this, OrderDetailsActivity.class);
            intent.putExtra("orderId", notification.orderId);
            startActivity(intent);
            return;
        }

        if (!notification.itemId.trim().isEmpty()) {
            Intent intent = new Intent(NotificationsActivity.this, ItemDetailsActivity.class);
            intent.putExtra("itemId", notification.itemId);
            startActivity(intent);
            return;
        }

        Toast.makeText(this, "Notification marked as read", Toast.LENGTH_SHORT).show();
        loadNotifications();
    }

    private void markAllAsRead() {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        btnReadAll.setEnabled(false);
        btnReadAll.setText("...");

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        resetReadAllButton();
                        loadNotifications();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        firestore.collection("notifications")
                                .document(document.getId())
                                .update("isRead", true);
                    }

                    Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
                    resetReadAllButton();
                    loadNotifications();
                })
                .addOnFailureListener(e -> {
                    resetReadAllButton();
                    Toast.makeText(this, "Failed to mark all as read", Toast.LENGTH_SHORT).show();
                });
    }

    private void resetReadAllButton() {
        btnReadAll.setEnabled(true);
        btnReadAll.setText("READ ALL");
    }

    private String formatDate(long time) {
        if (time <= 0) {
            return "Unknown time";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}