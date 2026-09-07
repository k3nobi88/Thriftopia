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

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatListActivity extends AppCompatActivity {

    TextView tvMessageSubtitle;
    LinearLayout chatContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    int currentLoadToken = 0;

    static class ChatData {
        String chatId;
        String itemId;
        String itemTitle;
        String buyerId;
        String sellerId;
        String otherUserId;
        String lastMessage;

        String offerId;
        String offerStatus;
        String offerBuyerId;
        double offerAmount;

        long updatedAt;
        long createdAt;

        ChatData(String chatId, String itemId, String itemTitle, String buyerId,
                 String sellerId, String otherUserId, String lastMessage,
                 String offerId, String offerStatus, String offerBuyerId, double offerAmount,
                 long updatedAt, long createdAt) {
            this.chatId = chatId;
            this.itemId = itemId;
            this.itemTitle = itemTitle;
            this.buyerId = buyerId;
            this.sellerId = sellerId;
            this.otherUserId = otherUserId;
            this.lastMessage = lastMessage;

            this.offerId = offerId;
            this.offerStatus = offerStatus;
            this.offerBuyerId = offerBuyerId;
            this.offerAmount = offerAmount;

            this.updatedAt = updatedAt;
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
        getWindow().setNavigationBarColor(getResources().getColor(R.color.white));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        setContentView(R.layout.activity_chat_list);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();

        BottomNavHelper.setup(this, BottomNavHelper.PAGE_MESSAGES);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChats();
    }

    private void bindViews() {
        tvMessageSubtitle = findViewById(R.id.tvMessageSubtitle);
        chatContainer = findViewById(R.id.chatContainer);
    }

    private void loadChats() {
        int loadToken = ++currentLoadToken;

        chatContainer.removeAllViews();
        tvMessageSubtitle.setText("Loading chats...");
        showLoadingState();

        if (firebaseAuth.getCurrentUser() == null) {
            chatContainer.removeAllViews();
            tvMessageSubtitle.setText("0 chat(s)");
            showEmptyState("Please login first", "Login to view your buyer and seller chats.");
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    chatContainer.removeAllViews();

                    ArrayList<ChatData> chatList = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String chatId = document.getString("chatId");

                        if (chatId == null || chatId.trim().isEmpty()) {
                            chatId = document.getId();
                        }

                        String itemId = safe(document.getString("itemId"));
                        String itemTitle = safe(document.getString("itemTitle"));
                        String buyerId = safe(document.getString("buyerId"));
                        String sellerId = safe(document.getString("sellerId"));
                        String lastMessage = safe(document.getString("lastMessage"));

                        String offerId = safe(document.getString("offerId"));
                        String offerStatus = safe(document.getString("offerStatus"));
                        String offerBuyerId = safe(document.getString("offerBuyerId"));

                        Double offerAmountValue = document.getDouble("offerAmount");

                        if (offerAmountValue == null) {
                            offerAmountValue = document.getDouble("amount");
                        }

                        double offerAmount = offerAmountValue != null ? offerAmountValue : 0;

                        Long updatedAtValue = document.getLong("updatedAt");
                        long updatedAt = updatedAtValue != null ? updatedAtValue : 0;

                        Long createdAtValue = document.getLong("createdAt");
                        long createdAt = createdAtValue != null ? createdAtValue : 0;

                        String otherUserId;

                        if (currentUserId.equals(buyerId)) {
                            otherUserId = sellerId;
                        } else {
                            otherUserId = buyerId;
                        }

                        chatList.add(new ChatData(
                                chatId,
                                itemId,
                                itemTitle,
                                buyerId,
                                sellerId,
                                otherUserId,
                                lastMessage,
                                offerId,
                                offerStatus,
                                offerBuyerId,
                                offerAmount,
                                updatedAt,
                                createdAt
                        ));
                    }

                    chatList.sort((a, b) -> Long.compare(getSortTime(b), getSortTime(a)));

                    if (chatList.isEmpty()) {
                        tvMessageSubtitle.setText("0 chat(s)");
                        showEmptyState("No messages yet", "Chats with buyers and sellers will appear here.");
                        return;
                    }

                    tvMessageSubtitle.setText(chatList.size() + " chat(s)");

                    for (ChatData chat : chatList) {
                        addChatCard(chat);
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadToken != currentLoadToken) {
                        return;
                    }

                    chatContainer.removeAllViews();
                    tvMessageSubtitle.setText("0 chat(s)");
                    showEmptyState("Failed to load chats", e.getMessage());
                });
    }

    private void showLoadingState() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(28), dp(18), dp(28));

        TextView title = new TextView(this);
        title.setText("Loading messages...");
        title.setTextColor(getResources().getColor(R.color.text_primary_light));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Please wait while we fetch your chats.");
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
        chatContainer.addView(card);
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
        cardParams.setMargins(0, dp(6), 0, 0);
        card.setLayoutParams(cardParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_messages);
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

        LinearLayout.LayoutParams subTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subTextParams.setMargins(0, dp(8), 0, 0);
        subtitle.setLayoutParams(subTextParams);
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
            Intent intent = new Intent(ChatListActivity.this, ExploreActivity.class);
            startActivity(intent);
            finish();
        });

        card.addView(btnExplore);
        chatContainer.addView(card);
    }

    private void addChatCard(ChatData chat) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.TOP);
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

        TextView avatar = new TextView(this);
        avatar.setText("?");
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(getResources().getColor(R.color.white));
        avatar.setTextSize(24);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setBackgroundResource(R.drawable.bg_circle_dark);

        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(58), dp(58));
        avatar.setLayoutParams(avatarParams);
        card.addView(avatar);

        LinearLayout middleArea = new LinearLayout(this);
        middleArea.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams middleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        middleParams.setMargins(dp(14), 0, dp(12), 0);
        middleArea.setLayoutParams(middleParams);

        TextView nameText = new TextView(this);
        nameText.setText("Loading...");
        nameText.setTextColor(getResources().getColor(R.color.text_primary_light));
        nameText.setTextSize(17);
        nameText.setTypeface(null, Typeface.BOLD);
        nameText.setMaxLines(1);
        middleArea.addView(nameText);

        TextView itemTitleText = new TextView(this);
        itemTitleText.setText(!chat.itemTitle.trim().isEmpty() ? chat.itemTitle : "Thriftopia item");
        itemTitleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        itemTitleText.setTextSize(16);
        itemTitleText.setTypeface(null, Typeface.BOLD);
        itemTitleText.setMaxLines(2);

        LinearLayout.LayoutParams itemTitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemTitleParams.setMargins(0, dp(4), 0, 0);
        itemTitleText.setLayoutParams(itemTitleParams);
        middleArea.addView(itemTitleText);

        TextView lastMessageText = new TextView(this);

        if (!chat.lastMessage.trim().isEmpty()) {
            lastMessageText.setText(chat.lastMessage);
        } else {
            lastMessageText.setText("Tap to continue conversation");
        }

        lastMessageText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        lastMessageText.setTextSize(14);
        lastMessageText.setMaxLines(2);

        LinearLayout.LayoutParams lastParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lastParams.setMargins(0, dp(5), 0, 0);
        lastMessageText.setLayoutParams(lastParams);
        middleArea.addView(lastMessageText);

        TextView offerText = new TextView(this);
        offerText.setTextSize(13);
        offerText.setTypeface(null, Typeface.BOLD);
        offerText.setMaxLines(1);

        LinearLayout.LayoutParams offerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        offerParams.setMargins(0, dp(12), 0, 0);
        offerText.setLayoutParams(offerParams);

        if (chat.offerAmount > 0 && !chat.offerStatus.trim().isEmpty()) {
            offerText.setText(getOfferLine(chat));
            offerText.setTextColor(getResources().getColor(getOfferColor(chat.offerStatus)));
            offerText.setVisibility(View.VISIBLE);
        } else {
            offerText.setVisibility(View.GONE);
        }

        middleArea.addView(offerText);

        card.addView(middleArea);

        LinearLayout rightArea = new LinearLayout(this);
        rightArea.setOrientation(LinearLayout.VERTICAL);
        rightArea.setGravity(Gravity.END);

        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(dp(82), LinearLayout.LayoutParams.WRAP_CONTENT);
        rightArea.setLayoutParams(rightParams);

        TextView dateText = new TextView(this);
        dateText.setText(formatDate(getSortTime(chat)));
        dateText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        dateText.setTextSize(13);
        dateText.setGravity(Gravity.END);

        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateText.setLayoutParams(dateParams);
        rightArea.addView(dateText);

        ImageView itemImage = new ImageView(this);
        itemImage.setBackgroundResource(R.drawable.bg_hero);
        itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        imageParams.setMargins(0, dp(12), 0, 0);
        imageParams.gravity = Gravity.END;
        itemImage.setLayoutParams(imageParams);
        rightArea.addView(itemImage);

        card.addView(rightArea);

        loadUserName(chat.otherUserId, nameText, avatar);
        loadItemPreview(chat.itemId, itemTitleText, itemImage);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, ChatRoomActivity.class);
            intent.putExtra("chatId", chat.chatId);
            intent.putExtra("itemId", chat.itemId);
            intent.putExtra("itemTitle", chat.itemTitle);
            intent.putExtra("sellerId", chat.sellerId);
            intent.putExtra("buyerId", chat.buyerId);
            startActivity(intent);
        });

        chatContainer.addView(card);
    }

    private String getOfferLine(ChatData chat) {
        String status = chat.offerStatus.toUpperCase(Locale.getDefault());
        String amount = "RM " + String.format(Locale.getDefault(), "%.2f", chat.offerAmount);

        boolean currentUserIsBuyer = chat.offerBuyerId != null && chat.offerBuyerId.equals(firebaseAuth.getCurrentUser().getUid());

        if (status.equals("PENDING")) {
            if (currentUserIsBuyer) {
                return "PENDING   You offered " + amount;
            } else {
                return "PENDING   Buyer offered " + amount;
            }
        }

        if (status.equals("ACCEPTED")) {
            if (currentUserIsBuyer) {
                return "ACCEPTED   Buy for " + amount;
            } else {
                return "ACCEPTED   Offer " + amount;
            }
        }

        if (status.equals("REJECTED")) {
            return "REJECTED   Offer " + amount;
        }

        if (status.equals("COMPLETED")) {
            return "PURCHASED   " + amount;
        }

        return status + "   Offer " + amount;
    }

    private int getOfferColor(String status) {
        if (status.equalsIgnoreCase("pending")) {
            return R.color.accent_lime;
        }

        if (status.equalsIgnoreCase("accepted")) {
            return R.color.accent_lime;
        }

        if (status.equalsIgnoreCase("completed")) {
            return R.color.accent_lime;
        }

        return R.color.text_secondary_light;
    }

    private void loadUserName(String userId, TextView nameText, TextView avatar) {
        if (userId == null || userId.trim().isEmpty()) {
            nameText.setText("Unknown User");
            avatar.setText("?");
            return;
        }

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        nameText.setText("Unknown User");
                        avatar.setText("?");
                        return;
                    }

                    String fullName = documentSnapshot.getString("fullName");
                    String email = documentSnapshot.getString("email");

                    String displayName;

                    if (fullName != null && !fullName.trim().isEmpty()) {
                        displayName = fullName;
                    } else if (email != null && !email.trim().isEmpty()) {
                        displayName = email;
                    } else {
                        displayName = "Thriftopia User";
                    }

                    nameText.setText(displayName);
                    avatar.setText(displayName.substring(0, 1).toUpperCase(Locale.getDefault()));
                })
                .addOnFailureListener(e -> {
                    nameText.setText("Unknown User");
                    avatar.setText("?");
                });
    }

    private void loadItemPreview(String itemId, TextView itemTitleText, ImageView itemImage) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return;
        }

        firestore.collection("items")
                .document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        findItemPreviewByField(itemId, itemTitleText, itemImage);
                        return;
                    }

                    String title = safe(documentSnapshot.getString("title"));
                    String imageUrl = safe(documentSnapshot.getString("imageUrl"));

                    if (!title.trim().isEmpty()) {
                        itemTitleText.setText(title);
                    }

                    if (!imageUrl.trim().isEmpty()) {
                        Glide.with(ChatListActivity.this)
                                .load(imageUrl)
                                .transform(new CenterCrop(), new RoundedCorners(dp(18)))
                                .into(itemImage);
                    }
                })
                .addOnFailureListener(e -> {
                });
    }

    private void findItemPreviewByField(String itemId, TextView itemTitleText, ImageView itemImage) {
        firestore.collection("items")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        return;
                    }

                    QueryDocumentSnapshot document = queryDocumentSnapshots.iterator().next();

                    String title = safe(document.getString("title"));
                    String imageUrl = safe(document.getString("imageUrl"));

                    if (!title.trim().isEmpty()) {
                        itemTitleText.setText(title);
                    }

                    if (!imageUrl.trim().isEmpty()) {
                        Glide.with(ChatListActivity.this)
                                .load(imageUrl)
                                .transform(new CenterCrop(), new RoundedCorners(dp(18)))
                                .into(itemImage);
                    }
                })
                .addOnFailureListener(e -> {
                });
    }

    private long getSortTime(ChatData chat) {
        if (chat.updatedAt > 0) {
            return chat.updatedAt;
        }

        return chat.createdAt;
    }

    private String formatDate(long time) {
        if (time <= 0) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}