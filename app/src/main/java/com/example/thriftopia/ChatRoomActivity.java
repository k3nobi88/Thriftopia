package com.example.thriftopia;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity {

    TextView btnBack, btnReport, btnMore;
    TextView tvChatName, tvChatStatus;
    TextView tvItemTitle, tvItemPrice;
    TextView tvSellerName, tvSellerMeta, tvSellerRating;
    TextView btnActionOne, btnActionTwo, btnActionThree;
    TextView btnSend;
    EditText etMessage;
    ImageView imgItemImage;
    LinearLayout messageContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    ListenerRegistration messageListener;

    String currentUserId = "";
    String currentUserEmail = "";

    String chatId = "";
    String itemId = "";
    String currentItemDocId = "";
    String itemTitle = "";
    String itemImageUrl = "";
    String sellerId = "";
    String buyerId = "";

    String sellerDisplayName = "Seller";
    String buyerDisplayName = "Buyer";

    double itemPrice = 0;

    String currentOfferId = "";
    String currentOfferStatus = "";
    String currentOfferBuyerId = "";
    String currentOfferSellerId = "";
    double currentOfferAmount = 0;
    long currentOfferCreatedAt = 0;

    static class MessageData {
        String messageId;
        String senderId;
        String text;
        String type;
        long createdAt;
        boolean seenByOther;
        boolean seenByCurrent;

        MessageData(String messageId, String senderId, String text, String type, long createdAt,
                    boolean seenByOther, boolean seenByCurrent) {
            this.messageId = messageId;
            this.senderId = senderId;
            this.text = text;
            this.type = type;
            this.createdAt = createdAt;
            this.seenByOther = seenByOther;
            this.seenByCurrent = seenByCurrent;
        }
    }

    static class OfferData {
        String offerId;
        String status;
        String buyerId;
        String sellerId;
        double amount;
        long createdAt;

        OfferData(String offerId, String status, String buyerId, String sellerId, double amount, long createdAt) {
            this.offerId = offerId;
            this.status = status;
            this.buyerId = buyerId;
            this.sellerId = sellerId;
            this.amount = amount;
            this.createdAt = createdAt;
        }
    }

    interface UserCallback {
        void onLoaded(String name, String email);
    }

    interface OrderCallback {
        void onCreated(String orderId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        setContentView(R.layout.activity_chat_room);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = firebaseAuth.getCurrentUser().getUid();
        currentUserEmail = firebaseAuth.getCurrentUser().getEmail() != null
                ? firebaseAuth.getCurrentUser().getEmail()
                : "";

        getIntentData();
        bindViews();
        setupClicks();
        setupQuickChips();
        setupInitialUi();
        setupKeyboardBehavior();
        scrollMessagesToBottom();

        ensureChatReady();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (messageListener != null) {
            messageListener.remove();
        }
    }

    private void getIntentData() {
        chatId = safe(getIntent().getStringExtra("chatId"));
        itemId = safe(getIntent().getStringExtra("itemId"));
        itemTitle = safe(getIntent().getStringExtra("itemTitle"));
        sellerId = safe(getIntent().getStringExtra("sellerId"));
        buyerId = safe(getIntent().getStringExtra("buyerId"));

        if (sellerId.trim().isEmpty()) {
            sellerId = safe(getIntent().getStringExtra("sellerID"));
        }

        if (buyerId.trim().isEmpty()) {
            buyerId = safe(getIntent().getStringExtra("buyerID"));
        }
    }

    private void bindViews() {
        btnBack = findFirstText("btnBack");
        btnReport = findFirstText("btnReport");
        btnMore = findFirstText("btnMore");

        tvChatName = findFirstText("tvChatName", "tvHeaderName", "tvReceiverName", "tvConversationName");
        tvChatStatus = findFirstText("tvChatStatus", "tvOnlineStatus", "tvSubtitle");

        tvItemTitle = findFirstText("tvItemTitle", "tvProductTitle", "tvChatItemTitle");
        tvItemPrice = findFirstText("tvItemPrice", "tvProductPrice", "tvChatItemPrice");

        tvSellerName = findFirstText("tvSellerName", "tvSellerInfoName");
        tvSellerMeta = findFirstText("tvSellerMeta", "tvSellerEmail", "tvSellerInfo");
        tvSellerRating = findFirstText("tvSellerRating", "tvRating");

        btnActionOne = findFirstText("btnBuy", "btnBuyNow", "btnViewItem", "btnPrimaryAction", "btnActionPrimary", "btnActionOne");
        btnActionTwo = findFirstText("btnAddToCart", "btnCart", "btnEditListing", "btnSecondaryAction", "btnActionSecondary", "btnActionTwo");
        btnActionThree = findFirstText("btnMakeOffer", "btnOffer", "btnMyListings", "btnThirdAction", "btnActionThird", "btnActionThree");

        btnSend = findFirstText("btnSend", "btnSendMessage");
        etMessage = findFirstEditText("etMessage", "etMessageInput", "inputMessage", "messageInput", "etChatMessage");

        imgItemImage = findFirstImage("imgItemImage", "ivItemImage", "itemImage", "imgChatItem");

        messageContainer = findFirstLinear("messageContainer", "messagesContainer", "chatMessageContainer", "chatMessagesContainer");
    }

    private void setupClicks() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> sendTextMessage());
        }

        if (btnReport != null) {
            btnReport.setOnClickListener(v -> openReportPage());
        }

        if (btnMore != null) {
            btnMore.setOnClickListener(v -> showMoreDialog());
        }
    }

    private void setupKeyboardBehavior() {
        if (etMessage == null) {
            return;
        }

        etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollMessagesToBottom();
            }
        });

        etMessage.setOnClickListener(v -> scrollMessagesToBottom());
    }

    private void setupQuickChips() {
        setupChip("chipMeetup", "Can we meet up?");
        setupChip("chipAvailable", "Hi, is this still available?");
        setupChip("chipPrice", "Can you lower the price?");
        setupChip("chipCondition", "What is the item condition?");
        setupChip("chipPickup", "Where can I pick this up?");
    }

    private void setupChip(String idName, String text) {
        TextView chip = findFirstText(idName);

        if (chip != null) {
            chip.setOnClickListener(v -> {
                if (etMessage != null) {
                    etMessage.setText(text);
                    etMessage.setSelection(etMessage.getText().length());
                }
            });
        }
    }

    private void setupInitialUi() {
        if (tvChatName != null) {
            tvChatName.setText("Chat");
        }

        if (tvChatStatus != null) {
            tvChatStatus.setText("Online");
        }

        if (tvItemTitle != null) {
            tvItemTitle.setText(!itemTitle.trim().isEmpty() ? itemTitle : "Thriftopia item");
        }

        if (tvItemPrice != null) {
            tvItemPrice.setText("RM 0.00");
        }

        if (tvSellerName != null) {
            tvSellerName.setText("Seller");
        }

        if (tvSellerMeta != null) {
            tvSellerMeta.setText("Thriftopia seller");
        }

        if (tvSellerRating != null) {
            tvSellerRating.setText("5.0 ★★★★★ (0)");
        }

        updateActionButtons();
    }

    private void ensureChatReady() {
        if (!chatId.trim().isEmpty()) {
            loadChatData();
            return;
        }

        if (buyerId.trim().isEmpty() && !currentUserId.equals(sellerId)) {
            buyerId = currentUserId;
        }

        if (!itemId.trim().isEmpty() && !sellerId.trim().isEmpty() && !buyerId.trim().isEmpty()) {
            chatId = createChatId(itemId, buyerId, sellerId);
            createOrMergeChat();
            return;
        }

        loadItemInfo();
        loadNames();
        updateActionButtons();
    }

    private void loadChatData() {
        firestore.collection("chats")
                .document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String loadedChatId = documentSnapshot.getString("chatId");
                        String loadedItemId = documentSnapshot.getString("itemId");
                        String loadedItemTitle = documentSnapshot.getString("itemTitle");
                        String loadedBuyerId = documentSnapshot.getString("buyerId");
                        String loadedSellerId = documentSnapshot.getString("sellerId");

                        if (loadedChatId != null && !loadedChatId.trim().isEmpty()) {
                            chatId = loadedChatId;
                        }

                        if (loadedItemId != null && !loadedItemId.trim().isEmpty()) {
                            itemId = loadedItemId;
                        }

                        if (loadedItemTitle != null && !loadedItemTitle.trim().isEmpty()) {
                            itemTitle = loadedItemTitle;
                        }

                        if (loadedBuyerId != null && !loadedBuyerId.trim().isEmpty()) {
                            buyerId = loadedBuyerId;
                        }

                        if (loadedSellerId != null && !loadedSellerId.trim().isEmpty()) {
                            sellerId = loadedSellerId;
                        }
                    } else {
                        createOrMergeChat();
                    }

                    loadItemInfo();
                    loadNames();
                    loadLatestOffer();
                    startMessagesListener();
                    updateActionButtons();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadItemInfo();
                    loadNames();
                    loadLatestOffer();
                    startMessagesListener();
                    updateActionButtons();
                });
    }

    private void createOrMergeChat() {
        if (chatId.trim().isEmpty()) {
            if (buyerId.trim().isEmpty() && !currentUserId.equals(sellerId)) {
                buyerId = currentUserId;
            }

            chatId = createChatId(itemId, buyerId, sellerId);
        }

        ArrayList<String> participants = new ArrayList<>();

        if (!buyerId.trim().isEmpty()) {
            participants.add(buyerId);
        }

        if (!sellerId.trim().isEmpty() && !participants.contains(sellerId)) {
            participants.add(sellerId);
        }

        Map<String, Object> chat = new HashMap<>();
        chat.put("chatId", chatId);
        chat.put("itemId", itemId);
        chat.put("itemTitle", itemTitle);
        chat.put("buyerId", buyerId);
        chat.put("sellerId", sellerId);
        chat.put("participants", participants);
        chat.put("updatedAt", System.currentTimeMillis());

        if (participants.size() > 0) {
            chat.put("createdAt", System.currentTimeMillis());
        }

        firestore.collection("chats")
                .document(chatId)
                .set(chat, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    loadItemInfo();
                    loadNames();
                    loadLatestOffer();
                    startMessagesListener();
                    updateActionButtons();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create chat: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadItemInfo() {
        if (itemId.trim().isEmpty()) {
            updateItemUi();
            return;
        }

        firestore.collection("items")
                .document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        applyItemDocument(documentSnapshot);
                    } else {
                        findItemByField();
                    }
                })
                .addOnFailureListener(e -> findItemByField());
    }

    private void findItemByField() {
        firestore.collection("items")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = queryDocumentSnapshots.iterator().next();
                        applyItemDocument(document);
                    } else {
                        updateItemUi();
                    }
                })
                .addOnFailureListener(e -> updateItemUi());
    }

    private void applyItemDocument(DocumentSnapshot document) {
        currentItemDocId = document.getId();

        String title = document.getString("title");
        String imageUrl = document.getString("imageUrl");
        String seller = document.getString("sellerId");

        if (title != null && !title.trim().isEmpty()) {
            itemTitle = title;
        }

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            itemImageUrl = imageUrl;
        }

        if (seller != null && !seller.trim().isEmpty()) {
            sellerId = seller;
        }

        double price = readDouble(document, "price", "priceValue", "itemPrice");

        if (price > 0) {
            itemPrice = price;
        }

        if (buyerId.trim().isEmpty() && !currentUserId.equals(sellerId)) {
            buyerId = currentUserId;
        }

        updateItemUi();
        loadNames();
        updateActionButtons();
    }

    private void updateItemUi() {
        if (tvItemTitle != null) {
            tvItemTitle.setText(!itemTitle.trim().isEmpty() ? itemTitle : "Thriftopia item");
        }

        if (tvItemPrice != null) {
            tvItemPrice.setText("RM " + String.format(Locale.getDefault(), "%.2f", itemPrice));
        }

        if (imgItemImage != null && !itemImageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(itemImageUrl)
                    .centerCrop()
                    .into(imgItemImage);
        }
    }

    private void loadNames() {
        String otherUserId = getOtherUserId();

        if (!otherUserId.trim().isEmpty()) {
            loadUser(otherUserId, (name, email) -> {
                if (tvChatName != null) {
                    tvChatName.setText(name);
                }
            });
        }

        if (!sellerId.trim().isEmpty()) {
            loadUser(sellerId, (name, email) -> {
                sellerDisplayName = name;

                if (tvSellerName != null) {
                    tvSellerName.setText(name);
                }

                if (tvSellerMeta != null) {
                    tvSellerMeta.setText(!email.trim().isEmpty() ? email : "Thriftopia seller");
                }
            });

            loadSellerRating();
        }

        if (!buyerId.trim().isEmpty()) {
            loadUser(buyerId, (name, email) -> buyerDisplayName = name);
        }
    }

    private void loadSellerRating() {
        firestore.collection("users")
                .document(sellerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists() || tvSellerRating == null) {
                        return;
                    }

                    Double average = documentSnapshot.getDouble("ratingAverage");
                    Long count = documentSnapshot.getLong("ratingCount");

                    if (average != null && count != null && count > 0) {
                        tvSellerRating.setText(String.format(Locale.getDefault(), "%.1f", average) + " ★★★★★ (" + count + ")");
                    } else {
                        tvSellerRating.setText("5.0 ★★★★★ (0)");
                    }
                });
    }

    private void loadUser(String userId, UserCallback callback) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = "Thriftopia User";
                    String email = "";

                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String userEmail = documentSnapshot.getString("email");

                        if (fullName != null && !fullName.trim().isEmpty()) {
                            name = fullName;
                        } else if (userEmail != null && !userEmail.trim().isEmpty()) {
                            name = userEmail;
                        }

                        if (userEmail != null) {
                            email = userEmail;
                        }
                    }

                    callback.onLoaded(name, email);
                })
                .addOnFailureListener(e -> callback.onLoaded("Thriftopia User", ""));
    }

    private void loadLatestOffer() {
        if (chatId.trim().isEmpty()) {
            resetOffer();
            updateActionButtons();
            return;
        }

        firestore.collection("offers")
                .whereEqualTo("chatId", chatId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    OfferData latest = null;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String offerId = safe(document.getString("offerId"));

                        if (offerId.trim().isEmpty()) {
                            offerId = document.getId();
                        }

                        String status = safe(document.getString("status"));
                        String offerBuyerId = safe(document.getString("buyerId"));
                        String offerSellerId = safe(document.getString("sellerId"));
                        double amount = readDouble(document, "offerAmount", "amount");

                        Long createdAtValue = document.getLong("createdAt");
                        long createdAt = createdAtValue != null ? createdAtValue : 0;

                        OfferData data = new OfferData(offerId, status, offerBuyerId, offerSellerId, amount, createdAt);

                        if (latest == null || data.createdAt > latest.createdAt) {
                            latest = data;
                        }
                    }

                    if (latest == null) {
                        resetOffer();
                    } else {
                        currentOfferId = latest.offerId;
                        currentOfferStatus = latest.status;
                        currentOfferBuyerId = latest.buyerId;
                        currentOfferSellerId = latest.sellerId;
                        currentOfferAmount = latest.amount;
                        currentOfferCreatedAt = latest.createdAt;
                    }

                    updateActionButtons();
                })
                .addOnFailureListener(e -> {
                    resetOffer();
                    updateActionButtons();
                });
    }

    private void resetOffer() {
        currentOfferId = "";
        currentOfferStatus = "";
        currentOfferBuyerId = "";
        currentOfferSellerId = "";
        currentOfferAmount = 0;
        currentOfferCreatedAt = 0;
    }

    private void startMessagesListener() {
        if (chatId.trim().isEmpty()) {
            return;
        }

        if (messageListener != null) {
            messageListener.remove();
        }

        messageListener = firestore.collection("messages")
                .whereEqualTo("chatId", chatId)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null || queryDocumentSnapshots == null) {
                        return;
                    }

                    ArrayList<MessageData> messages = new ArrayList<>();
                    String otherUserId = getOtherUserId();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String messageId = safe(document.getString("messageId"));

                        if (messageId.trim().isEmpty()) {
                            messageId = document.getId();
                        }

                        String senderId = safe(document.getString("senderId"));
                        String text = safe(document.getString("text"));
                        String type = safe(document.getString("type"));

                        Long createdAtValue = document.getLong("createdAt");
                        long createdAt = createdAtValue != null ? createdAtValue : 0;

                        boolean seenByOther = false;
                        boolean seenByCurrent = false;

                        Object seenByObject = document.get("seenBy");

                        if (seenByObject instanceof java.util.List) {
                            java.util.List<?> seenByList = (java.util.List<?>) seenByObject;

                            seenByCurrent = seenByList.contains(currentUserId);

                            if (!otherUserId.trim().isEmpty()) {
                                seenByOther = seenByList.contains(otherUserId);
                            }
                        }

                        messages.add(new MessageData(
                                messageId,
                                senderId,
                                text,
                                type,
                                createdAt,
                                seenByOther,
                                seenByCurrent
                        ));
                    }

                    messages.sort((a, b) -> Long.compare(a.createdAt, b.createdAt));

                    markIncomingMessagesAsSeen(messages);
                    renderMessages(messages);
                });
    }

    private void renderMessages(ArrayList<MessageData> messages) {
        if (messageContainer == null) {
            return;
        }

        messageContainer.removeAllViews();

        if (messages.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Start a message or try your own offer.");
            empty.setTextColor(Color.parseColor("#888888"));
            empty.setTextSize(13);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(18), dp(12), dp(18));
            messageContainer.addView(empty);
            return;
        }

        String lastDayKey = "";

        for (MessageData message : messages) {
            String dayKey = getDayKey(message.createdAt);

            if (!dayKey.equals(lastDayKey)) {
                addDateSeparator(formatDayLabel(message.createdAt));
                lastDayKey = dayKey;
            }

            addMessageBubble(message);
        }

        scrollMessagesToBottom();
    }

    private void addMessageBubble(MessageData message) {
        if (messageContainer == null) {
            return;
        }

        boolean isMine = message.senderId.equals(currentUserId);
        boolean isSystem = message.type.equalsIgnoreCase("system");

        LinearLayout outerRow = new LinearLayout(this);
        outerRow.setOrientation(LinearLayout.HORIZONTAL);
        outerRow.setGravity(isSystem ? Gravity.CENTER : (isMine ? Gravity.END : Gravity.START));

        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        outerParams.setMargins(0, dp(4), 0, dp(6));
        outerRow.setLayoutParams(outerParams);

        LinearLayout bubbleColumn = new LinearLayout(this);
        bubbleColumn.setOrientation(LinearLayout.VERTICAL);
        bubbleColumn.setGravity(isMine ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bubbleColumn.setLayoutParams(columnParams);

        TextView bubble = new TextView(this);
        bubble.setText(message.text);
        bubble.setTextSize(isSystem ? 12 : 14);
        bubble.setIncludeFontPadding(false);
        bubble.setMinWidth(0);
        bubble.setMinEms(0);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));

        int maxBubbleWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.68f);
        bubble.setMaxWidth(maxBubbleWidth);

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bubble.setLayoutParams(bubbleParams);

        if (isSystem) {
            bubble.setTextColor(Color.parseColor("#C6FF00"));
            bubble.setGravity(Gravity.CENTER);
            bubble.setTypeface(null, Typeface.BOLD);
            bubble.setBackground(roundedBg(Color.parseColor("#202020"), dp(16)));
        } else if (isMine) {
            bubble.setTextColor(Color.BLACK);
            bubble.setGravity(Gravity.START);
            bubble.setBackground(roundedBg(Color.parseColor("#C6FF00"), dp(18)));
        } else {
            bubble.setTextColor(Color.WHITE);
            bubble.setGravity(Gravity.START);
            bubble.setBackground(roundedBg(Color.parseColor("#262626"), dp(18)));
        }

        bubbleColumn.addView(bubble);

        TextView meta = new TextView(this);
        meta.setTextSize(11);
        meta.setTextColor(Color.parseColor("#8A8A8A"));
        meta.setIncludeFontPadding(false);

        String metaText = formatMessageTime(message.createdAt);

        if (isMine && !isSystem) {
            metaText = metaText + " • " + (message.seenByOther ? "Seen" : "Sent");
        }

        meta.setText(metaText);
        meta.setGravity(isMine ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metaParams.setMargins(dp(4), dp(4), dp(4), 0);
        meta.setLayoutParams(metaParams);

        if (!isSystem) {
            bubbleColumn.addView(meta);
        }

        outerRow.addView(bubbleColumn);
        messageContainer.addView(outerRow);
    }

    private void sendTextMessage() {
        if (etMessage == null) {
            return;
        }

        String text = etMessage.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        if (chatId.trim().isEmpty()) {
            Toast.makeText(this, "Chat is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        etMessage.setText("");
        sendMessage(text, "text");
    }

    private void sendMessage(String text, String type) {
        String messageId = firestore.collection("messages").document().getId();

        ArrayList<String> seenBy = new ArrayList<>();
        seenBy.add(currentUserId);

        Map<String, Object> message = new HashMap<>();
        message.put("messageId", messageId);
        message.put("chatId", chatId);
        message.put("itemId", itemId);
        message.put("senderId", currentUserId);
        message.put("senderEmail", currentUserEmail);
        message.put("text", text);
        message.put("type", type);
        message.put("createdAt", System.currentTimeMillis());
        message.put("seenBy", seenBy);

        firestore.collection("messages")
                .document(messageId)
                .set(message)
                .addOnSuccessListener(unused -> updateChatLastMessage(text))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void sendSystemMessage(String text) {
        String messageId = firestore.collection("messages").document().getId();

        Map<String, Object> message = new HashMap<>();
        message.put("messageId", messageId);
        message.put("chatId", chatId);
        message.put("itemId", itemId);
        message.put("senderId", "system");
        message.put("senderEmail", "");
        message.put("text", text);
        message.put("type", "system");
        message.put("createdAt", System.currentTimeMillis());

        firestore.collection("messages")
                .document(messageId)
                .set(message)
                .addOnSuccessListener(unused -> updateChatLastMessage(text));
    }

    private void updateChatLastMessage(String text) {
        if (chatId.trim().isEmpty()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", text);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("chats")
                .document(chatId)
                .set(updates, SetOptions.merge());
    }

    private void updateActionButtons() {
        boolean isSeller = currentUserId.equals(sellerId);
        boolean isBuyer = currentUserId.equals(buyerId) || buyerId.trim().isEmpty();

        if (isSeller) {
            setButton(btnActionOne, "View", true, v -> openItemDetails());
            setButton(btnActionTwo, "Edit", true, v -> openEditListing());

            if (currentOfferStatus.equalsIgnoreCase("pending") && currentOfferSellerId.equals(currentUserId)) {
                setButton(btnActionThree, "Respond Offer", true, v -> showRespondOfferDialog());
            } else {
                setButton(btnActionThree, "My Listings", true, v ->
                        startActivity(new Intent(ChatRoomActivity.this, MyListingsActivity.class))
                );
            }

            return;
        }

        if (isBuyer) {
            if (currentOfferStatus.equalsIgnoreCase("accepted") && currentOfferBuyerId.equals(currentUserId)) {
                setButton(btnActionOne, "Buy Offer", true, v -> buyAcceptedOffer());
            } else {
                setButton(btnActionOne, "Buy", true, v -> buyNow());
            }

            setButton(btnActionTwo, "Add to Cart", true, v -> addToCart());
            setButton(btnActionThree, "Make Offer", true, v -> showMakeOfferDialog());
            return;
        }

        setButton(btnActionOne, "View", true, v -> openItemDetails());
        setButton(btnActionTwo, "Add to Cart", false, null);
        setButton(btnActionThree, "Make Offer", false, null);
    }

    private void setButton(TextView button, String text, boolean enabled, View.OnClickListener listener) {
        if (button == null) {
            return;
        }

        button.setVisibility(View.VISIBLE);
        button.setText(text);
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.45f);
        button.setOnClickListener(listener);
    }

    private void openItemDetails() {
        Intent intent = new Intent(this, ItemDetailsActivity.class);
        intent.putExtra("itemId", !currentItemDocId.trim().isEmpty() ? currentItemDocId : itemId);
        startActivity(intent);
    }

    private void openEditListing() {
        Intent intent = new Intent(this, EditListingActivity.class);
        intent.putExtra("itemId", !currentItemDocId.trim().isEmpty() ? currentItemDocId : itemId);
        startActivity(intent);
    }

    private void addToCart() {
        if (currentUserId.equals(sellerId)) {
            Toast.makeText(this, "You cannot add your own item to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        if (itemId.trim().isEmpty()) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String cartId = currentUserId + "_" + itemId;

        Map<String, Object> cart = new HashMap<>();
        cart.put("cartId", cartId);
        cart.put("userId", currentUserId);
        cart.put("itemId", itemId);
        cart.put("itemTitle", itemTitle);
        cart.put("title", itemTitle);
        cart.put("price", itemPrice);
        cart.put("priceValue", itemPrice);
        cart.put("imageUrl", itemImageUrl);
        cart.put("sellerId", sellerId);
        cart.put("createdAt", System.currentTimeMillis());
        cart.put("updatedAt", System.currentTimeMillis());

        firestore.collection("cart")
                .document(cartId)
                .set(cart, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add cart: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void buyNow() {
        if (currentUserId.equals(sellerId)) {
            Toast.makeText(this, "You cannot buy your own item", Toast.LENGTH_SHORT).show();
            return;
        }

        if (itemPrice <= 0) {
            Toast.makeText(this, "Item price not available", Toast.LENGTH_SHORT).show();
            return;
        }

        createOrder("normal", "", itemPrice, orderId -> {
            sendSystemMessage("Item purchased for RM " + String.format(Locale.getDefault(), "%.2f", itemPrice));
            openOrderDetails(orderId);
        });
    }

    private void showMakeOfferDialog() {
        if (currentUserId.equals(sellerId)) {
            Toast.makeText(this, "You cannot make offer on your own item", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chatId.trim().isEmpty()) {
            Toast.makeText(this, "Chat is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Enter offer amount");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Make Offer");
        builder.setMessage("Enter your offer price for this item.");
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String amountText = input.getText().toString().trim();

            if (amountText.isEmpty()) {
                Toast.makeText(this, "Amount required", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;

            try {
                amount = Double.parseDouble(amountText);
            } catch (Exception e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount <= 0) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            submitOffer(amount);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void submitOffer(double amount) {
        String offerId = firestore.collection("offers").document().getId();

        Map<String, Object> offer = new HashMap<>();
        offer.put("offerId", offerId);
        offer.put("chatId", chatId);
        offer.put("itemId", itemId);
        offer.put("itemTitle", itemTitle);
        offer.put("buyerId", currentUserId);
        offer.put("sellerId", sellerId);
        offer.put("offerAmount", amount);
        offer.put("amount", amount);
        offer.put("status", "pending");
        offer.put("createdAt", System.currentTimeMillis());
        offer.put("updatedAt", System.currentTimeMillis());

        firestore.collection("offers")
                .document(offerId)
                .set(offer)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> chatUpdates = new HashMap<>();
                    chatUpdates.put("offerId", offerId);
                    chatUpdates.put("offerAmount", amount);
                    chatUpdates.put("offerStatus", "pending");
                    chatUpdates.put("offerBuyerId", currentUserId);
                    chatUpdates.put("updatedAt", System.currentTimeMillis());

                    firestore.collection("chats")
                            .document(chatId)
                            .set(chatUpdates, SetOptions.merge());

                    currentOfferId = offerId;
                    currentOfferStatus = "pending";
                    currentOfferBuyerId = currentUserId;
                    currentOfferSellerId = sellerId;
                    currentOfferAmount = amount;
                    currentOfferCreatedAt = System.currentTimeMillis();

                    String msg = "Offered RM " + String.format(Locale.getDefault(), "%.2f", amount);
                    sendSystemMessage(msg);

                    createNotification(
                            sellerId,
                            "New Offer",
                            "Buyer offered RM " + String.format(Locale.getDefault(), "%.2f", amount) + " for " + itemTitle,
                            "offer",
                            chatId,
                            itemId,
                            ""
                    );

                    Toast.makeText(this, "Offer sent", Toast.LENGTH_SHORT).show();
                    updateActionButtons();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send offer: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void showRespondOfferDialog() {
        if (currentOfferId.trim().isEmpty()) {
            Toast.makeText(this, "No pending offer", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountText = "RM " + String.format(Locale.getDefault(), "%.2f", currentOfferAmount);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Respond Offer");
        builder.setMessage("Buyer offered " + amountText);

        builder.setPositiveButton("Accept", (dialog, which) -> updateOfferStatus("accepted"));
        builder.setNegativeButton("Reject", (dialog, which) -> updateOfferStatus("rejected"));
        builder.setNeutralButton("Cancel", null);

        builder.show();
    }

    private void updateOfferStatus(String newStatus) {
        if (currentOfferId.trim().isEmpty()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("offers")
                .document(currentOfferId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> chatUpdates = new HashMap<>();
                    chatUpdates.put("offerStatus", newStatus);
                    chatUpdates.put("updatedAt", System.currentTimeMillis());

                    firestore.collection("chats")
                            .document(chatId)
                            .set(chatUpdates, SetOptions.merge());

                    currentOfferStatus = newStatus;

                    String amount = "RM " + String.format(Locale.getDefault(), "%.2f", currentOfferAmount);

                    if (newStatus.equalsIgnoreCase("accepted")) {
                        sendSystemMessage("Offer accepted: " + amount);
                        createNotification(
                                currentOfferBuyerId,
                                "Offer Accepted",
                                "Your offer " + amount + " for " + itemTitle + " was accepted.",
                                "offer",
                                chatId,
                                itemId,
                                ""
                        );
                        Toast.makeText(this, "Offer accepted", Toast.LENGTH_SHORT).show();
                    } else {
                        sendSystemMessage("Offer rejected: " + amount);
                        createNotification(
                                currentOfferBuyerId,
                                "Offer Rejected",
                                "Your offer " + amount + " for " + itemTitle + " was rejected.",
                                "offer",
                                chatId,
                                itemId,
                                ""
                        );
                        Toast.makeText(this, "Offer rejected", Toast.LENGTH_SHORT).show();
                    }

                    updateActionButtons();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update offer: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void buyAcceptedOffer() {
        if (!currentOfferStatus.equalsIgnoreCase("accepted")) {
            Toast.makeText(this, "Offer is not accepted yet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!currentOfferBuyerId.equals(currentUserId)) {
            Toast.makeText(this, "Only buyer can purchase this offer", Toast.LENGTH_SHORT).show();
            return;
        }

        createOrder("offer", currentOfferId, currentOfferAmount, orderId -> {
            Map<String, Object> offerUpdates = new HashMap<>();
            offerUpdates.put("status", "completed");
            offerUpdates.put("orderId", orderId);
            offerUpdates.put("updatedAt", System.currentTimeMillis());

            firestore.collection("offers")
                    .document(currentOfferId)
                    .update(offerUpdates);

            Map<String, Object> chatUpdates = new HashMap<>();
            chatUpdates.put("offerStatus", "completed");
            chatUpdates.put("updatedAt", System.currentTimeMillis());

            firestore.collection("chats")
                    .document(chatId)
                    .set(chatUpdates, SetOptions.merge());

            currentOfferStatus = "completed";

            sendSystemMessage("Offer purchased for RM " + String.format(Locale.getDefault(), "%.2f", currentOfferAmount));

            createNotification(
                    sellerId,
                    "Offer Purchased",
                    "Buyer purchased " + itemTitle + " for RM " + String.format(Locale.getDefault(), "%.2f", currentOfferAmount),
                    "order",
                    chatId,
                    itemId,
                    orderId
            );

            openOrderDetails(orderId);
        });
    }

    private void createOrder(String purchaseType, String offerId, double amount, OrderCallback callback) {
        if (sellerId.trim().isEmpty()) {
            Toast.makeText(this, "Seller not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String orderId = firestore.collection("orders").document().getId();

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("buyerId", currentUserId);
        order.put("buyerEmail", currentUserEmail);
        order.put("buyerName", buyerDisplayName);
        order.put("sellerId", sellerId);
        order.put("sellerName", sellerDisplayName);
        order.put("itemId", itemId);
        order.put("itemTitle", itemTitle);
        order.put("title", itemTitle);
        order.put("imageUrl", itemImageUrl);
        order.put("price", amount);
        order.put("priceValue", amount);
        order.put("totalPrice", amount);
        order.put("purchaseType", purchaseType);
        order.put("offerId", offerId);
        order.put("paymentMethod", "Demo Payment");
        order.put("orderStatus", "Order Placed");
        order.put("deliveryStatus", "Order Placed");
        order.put("trackingStatus", "Order Placed");
        order.put("trackingStep", 0);
        order.put("createdAt", System.currentTimeMillis());
        order.put("updatedAt", System.currentTimeMillis());

        firestore.collection("orders")
                .document(orderId)
                .set(order)
                .addOnSuccessListener(unused -> {
                    updateItemSold(orderId, purchaseType);
                    removeFromCartAfterPurchase();

                    createNotification(
                            sellerId,
                            "Item Sold",
                            itemTitle + " has been purchased.",
                            "order",
                            chatId,
                            itemId,
                            orderId
                    );

                    notifyCartUsersItemSold(orderId);

                    Toast.makeText(this, "Purchase successful", Toast.LENGTH_SHORT).show();

                    if (callback != null) {
                        callback.onCreated(orderId);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create order: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void updateItemSold(String orderId, String purchaseType) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isSold", true);
        updates.put("sold", true);
        updates.put("status", "sold");
        updates.put("soldAt", System.currentTimeMillis());
        updates.put("soldTo", currentUserId);
        updates.put("soldBy", purchaseType);
        updates.put("orderId", orderId);
        updates.put("updatedAt", System.currentTimeMillis());

        String docId = !currentItemDocId.trim().isEmpty() ? currentItemDocId : itemId;

        if (docId.trim().isEmpty()) {
            return;
        }

        firestore.collection("items")
                .document(docId)
                .update(updates)
                .addOnFailureListener(e -> {
                    if (!itemId.trim().isEmpty()) {
                        firestore.collection("items")
                                .whereEqualTo("itemId", itemId)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        QueryDocumentSnapshot document = queryDocumentSnapshots.iterator().next();
                                        firestore.collection("items").document(document.getId()).update(updates);
                                    }
                                });
                    }
                });
    }

    private void removeFromCartAfterPurchase() {
        String cartId = currentUserId + "_" + itemId;

        firestore.collection("cart")
                .document(cartId)
                .delete();
    }

    private void notifyCartUsersItemSold(String orderId) {
        if (itemId.trim().isEmpty()) {
            return;
        }

        firestore.collection("cart")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = safe(document.getString("userId"));

                        if (!userId.trim().isEmpty()
                                && !userId.equals(currentUserId)
                                && !userId.equals(sellerId)) {
                            createNotification(
                                    userId,
                                    "Item Sold",
                                    itemTitle + " has been sold.",
                                    "item_sold",
                                    chatId,
                                    itemId,
                                    orderId
                            );
                        }
                    }
                });
    }

    private void openOrderDetails(String orderId) {
        Intent intent = new Intent(this, OrderDetailsActivity.class);
        intent.putExtra("orderId", orderId);
        startActivity(intent);
    }

    private void createNotification(String userId, String title, String message, String type,
                                    String targetChatId, String targetItemId, String orderId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        String notificationId = firestore.collection("notifications").document().getId();

        Map<String, Object> notification = new HashMap<>();
        notification.put("notificationId", notificationId);
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("chatId", targetChatId);
        notification.put("itemId", targetItemId);
        notification.put("orderId", orderId);
        notification.put("isRead", false);
        notification.put("createdAt", System.currentTimeMillis());

        firestore.collection("notifications")
                .document(notificationId)
                .set(notification);
    }

    private void openReportPage() {
        String reportedUserId = getOtherUserId();

        Intent intent = new Intent(ChatRoomActivity.this, ReportActivity.class);
        intent.putExtra("targetType", "chat");
        intent.putExtra("chatId", chatId);
        intent.putExtra("itemId", !currentItemDocId.trim().isEmpty() ? currentItemDocId : itemId);
        intent.putExtra("itemTitle", itemTitle);
        intent.putExtra("reportedUserId", reportedUserId);

        startActivity(intent);
    }

    private void showMoreDialog() {
        String[] options = {"Report Chat/User", "View Item"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("More Options");

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openReportPage();
            } else {
                openItemDetails();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String getOtherUserId() {
        if (currentUserId.equals(buyerId)) {
            return sellerId;
        }

        if (currentUserId.equals(sellerId)) {
            return buyerId;
        }

        if (!sellerId.trim().isEmpty()) {
            return sellerId;
        }

        if (!buyerId.trim().isEmpty()) {
            return buyerId;
        }

        return "";
    }

    private String createChatId(String itemId, String buyerId, String sellerId) {
        String raw = itemId + "_" + buyerId + "_" + sellerId;
        return raw.replace("/", "_")
                .replace("\\", "_")
                .replace("#", "_")
                .replace("?", "_");
    }

    private double readDouble(DocumentSnapshot document, String... fields) {
        for (String field : fields) {
            Object value = document.get(field);

            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }

            if (value instanceof String) {
                try {
                    String clean = ((String) value)
                            .replace("RM", "")
                            .replace(",", "")
                            .trim();

                    return Double.parseDouble(clean);
                } catch (Exception ignored) {
                }
            }
        }

        return 0;
    }

    private TextView findFirstText(String... ids) {
        for (String idName : ids) {
            int id = getResources().getIdentifier(idName, "id", getPackageName());

            if (id != 0) {
                View view = findViewById(id);

                if (view instanceof TextView) {
                    return (TextView) view;
                }
            }
        }

        return null;
    }

    private EditText findFirstEditText(String... ids) {
        for (String idName : ids) {
            int id = getResources().getIdentifier(idName, "id", getPackageName());

            if (id != 0) {
                View view = findViewById(id);

                if (view instanceof EditText) {
                    return (EditText) view;
                }
            }
        }

        return null;
    }

    private ImageView findFirstImage(String... ids) {
        for (String idName : ids) {
            int id = getResources().getIdentifier(idName, "id", getPackageName());

            if (id != 0) {
                View view = findViewById(id);

                if (view instanceof ImageView) {
                    return (ImageView) view;
                }
            }
        }

        return null;
    }

    private LinearLayout findFirstLinear(String... ids) {
        for (String idName : ids) {
            int id = getResources().getIdentifier(idName, "id", getPackageName());

            if (id != 0) {
                View view = findViewById(id);

                if (view instanceof LinearLayout) {
                    return (LinearLayout) view;
                }
            }
        }

        return null;
    }

    private GradientDrawable roundedBg(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private String formatDate(long time) {
        if (time <= 0) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private void markIncomingMessagesAsSeen(ArrayList<MessageData> messages) {
        for (MessageData message : messages) {
            if (message.messageId == null || message.messageId.trim().isEmpty()) {
                continue;
            }

            if (message.senderId.equals(currentUserId)) {
                continue;
            }

            if (message.senderId.equalsIgnoreCase("system")) {
                continue;
            }

            if (message.seenByCurrent) {
                continue;
            }

            firestore.collection("messages")
                    .document(message.messageId)
                    .update("seenBy", FieldValue.arrayUnion(currentUserId));
        }
    }

    private void addDateSeparator(String label) {
        if (messageContainer == null || label.trim().isEmpty()) {
            return;
        }

        TextView dateText = new TextView(this);
        dateText.setText(label);
        dateText.setTextColor(Color.parseColor("#A3A3A3"));
        dateText.setTextSize(11);
        dateText.setTypeface(null, Typeface.BOLD);
        dateText.setGravity(Gravity.CENTER);
        dateText.setIncludeFontPadding(false);
        dateText.setBackground(roundedBg(Color.parseColor("#1A1A1A"), dp(14)));
        dateText.setPadding(dp(12), dp(7), dp(12), dp(7));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.setMargins(0, dp(12), 0, dp(10));
        dateText.setLayoutParams(params);

        messageContainer.addView(dateText);
    }

    private String formatMessageTime(long time) {
        if (time <= 0) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private String getDayKey(long time) {
        if (time <= 0) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private String formatDayLabel(long time) {
        if (time <= 0) {
            return "";
        }

        Calendar messageCalendar = Calendar.getInstance();
        messageCalendar.setTimeInMillis(time);

        Calendar today = Calendar.getInstance();

        if (isSameDay(messageCalendar, today)) {
            return "Today";
        }

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        if (isSameDay(messageCalendar, yesterday)) {
            return "Yesterday";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private boolean isSameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private void scrollMessagesToBottom() {
        if (messageContainer == null) {
            return;
        }

        messageContainer.postDelayed(() -> {
            View parent = (View) messageContainer.getParent();

            while (parent != null && !(parent instanceof ScrollView)) {
                if (!(parent.getParent() instanceof View)) {
                    break;
                }

                parent = (View) parent.getParent();
            }

            if (parent instanceof ScrollView) {
                ((ScrollView) parent).fullScroll(View.FOCUS_DOWN);
            }
        }, 120);
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}