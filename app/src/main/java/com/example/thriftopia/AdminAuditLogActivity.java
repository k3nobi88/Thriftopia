package com.example.thriftopia;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AdminAuditLogActivity extends AppCompatActivity {

    TextView btnBack, tvLogCount;
    TextView filterAll, filterReports, filterUsers, filterListings;
    LinearLayout logContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String currentFilter = "all";
    ArrayList<AuditLogData> logList = new ArrayList<>();

    static class AuditLogData {
        String logId;
        String action;
        String message;
        String targetType;
        String targetId;
        String actorId;
        String actorEmail;
        long createdAt;

        AuditLogData(String logId, String action, String message, String targetType,
                     String targetId, String actorId, String actorEmail, long createdAt) {
            this.logId = logId;
            this.action = action;
            this.message = message;
            this.targetType = targetType;
            this.targetId = targetId;
            this.actorId = actorId;
            this.actorEmail = actorEmail;
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

        setContentView(R.layout.activity_admin_audit_log);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        bindViews();
        setupClicks();
        loadAuditLogs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAuditLogs();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvLogCount = findViewById(R.id.tvLogCount);

        filterAll = findViewById(R.id.filterAll);
        filterReports = findViewById(R.id.filterReports);
        filterUsers = findViewById(R.id.filterUsers);
        filterListings = findViewById(R.id.filterListings);

        logContainer = findViewById(R.id.logContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        filterAll.setOnClickListener(v -> {
            currentFilter = "all";
            updateFilterButtons();
            renderLogs();
        });

        filterReports.setOnClickListener(v -> {
            currentFilter = "report";
            updateFilterButtons();
            renderLogs();
        });

        filterUsers.setOnClickListener(v -> {
            currentFilter = "user";
            updateFilterButtons();
            renderLogs();
        });

        filterListings.setOnClickListener(v -> {
            currentFilter = "listing";
            updateFilterButtons();
            renderLogs();
        });
    }

    private void updateFilterButtons() {
        filterAll.setBackgroundResource(currentFilter.equals("all") ? R.drawable.bg_primary_button : R.drawable.bg_card);
        filterReports.setBackgroundResource(currentFilter.equals("report") ? R.drawable.bg_primary_button : R.drawable.bg_card);
        filterUsers.setBackgroundResource(currentFilter.equals("user") ? R.drawable.bg_primary_button : R.drawable.bg_card);
        filterListings.setBackgroundResource(currentFilter.equals("listing") ? R.drawable.bg_primary_button : R.drawable.bg_card);
    }

    private void loadAuditLogs() {
        logContainer.removeAllViews();
        tvLogCount.setText("Loading logs...");
        showSimpleState("Loading audit logs...", "Please wait while admin actions are being fetched.");

        firestore.collection("auditLogs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    logList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String logId = safe(document.getString("logId"));

                        if (logId.trim().isEmpty()) {
                            logId = document.getId();
                        }

                        String action = safe(document.getString("action"));
                        String message = safe(document.getString("message"));
                        String targetType = safe(document.getString("targetType"));
                        String targetId = safe(document.getString("targetId"));
                        String actorId = safe(document.getString("actorId"));
                        String actorEmail = safe(document.getString("actorEmail"));

                        Long createdAtValue = document.getLong("createdAt");
                        long createdAt = createdAtValue != null ? createdAtValue : 0;

                        logList.add(new AuditLogData(
                                logId,
                                action,
                                message,
                                targetType,
                                targetId,
                                actorId,
                                actorEmail,
                                createdAt
                        ));
                    }

                    logList.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

                    updateFilterButtons();
                    renderLogs();
                })
                .addOnFailureListener(e -> {
                    tvLogCount.setText("0 log(s)");
                    logContainer.removeAllViews();
                    showSimpleState("Failed to load audit logs", e.getMessage());
                });
    }

    private void renderLogs() {
        logContainer.removeAllViews();

        int count = 0;

        for (AuditLogData log : logList) {
            if (!matchesFilter(log)) {
                continue;
            }

            count++;
            addLogCard(log);
        }

        tvLogCount.setText(count + " log(s)");

        if (count == 0) {
            showSimpleState("No audit logs", "Admin actions will appear here.");
        }
    }

    private boolean matchesFilter(AuditLogData log) {
        if (currentFilter.equals("all")) {
            return true;
        }

        if (currentFilter.equals("report")) {
            return log.targetType.equalsIgnoreCase("report")
                    || log.action.toLowerCase(Locale.getDefault()).contains("report");
        }

        if (currentFilter.equals("user")) {
            return log.targetType.equalsIgnoreCase("user")
                    || log.action.toLowerCase(Locale.getDefault()).contains("user");
        }

        if (currentFilter.equals("listing")) {
            return log.targetType.equalsIgnoreCase("listing")
                    || log.action.toLowerCase(Locale.getDefault()).contains("listing");
        }

        return true;
    }

    private void addLogCard(AuditLogData log) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        TextView actionText = new TextView(this);
        actionText.setText(formatAction(log.action));
        actionText.setTextColor(getResources().getColor(R.color.accent_lime));
        actionText.setTextSize(13);
        actionText.setTypeface(null, Typeface.BOLD);
        card.addView(actionText);

        TextView messageText = new TextView(this);
        messageText.setText(!log.message.trim().isEmpty() ? log.message : "Admin action recorded.");
        messageText.setTextColor(getResources().getColor(R.color.text_primary_light));
        messageText.setTextSize(17);
        messageText.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, dp(8), 0, 0);
        messageText.setLayoutParams(messageParams);
        card.addView(messageText);

        TextView metaText = new TextView(this);
        metaText.setText(getMeta(log));
        metaText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        metaText.setTextSize(13);

        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metaParams.setMargins(0, dp(10), 0, 0);
        metaText.setLayoutParams(metaParams);
        card.addView(metaText);

        logContainer.addView(card);
    }

    private String getMeta(AuditLogData log) {
        String meta = "";

        meta += "Target type: " + valueOrDash(log.targetType);

        if (!log.targetId.trim().isEmpty()) {
            meta += "\nTarget ID: " + log.targetId;
        }

        if (!log.actorEmail.trim().isEmpty()) {
            meta += "\nAdmin: " + log.actorEmail;
        }

        if (!log.actorId.trim().isEmpty()) {
            meta += "\nActor ID: " + log.actorId;
        }

        meta += "\nDate: " + formatDate(log.createdAt);

        return meta;
    }

    private String formatAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            return "AUDIT LOG";
        }

        return action.replace("_", " ").toUpperCase(Locale.getDefault());
    }

    private void showSimpleState(String title, String subtitle) {
        logContainer.removeAllViews();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(20), dp(32), dp(20), dp(32));

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        titleText.setTextSize(19);
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
        logContainer.addView(card);
    }

    private String formatDate(long time) {
        if (time <= 0) {
            return "-";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}