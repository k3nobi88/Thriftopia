package com.example.thriftopia;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminReportsActivity extends AppCompatActivity {

    TextView btnBack, tvReportCount;
    TextView btnFilterPending, btnFilterResolved, btnFilterDismissed;
    LinearLayout reportContainer;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String currentFilter = "pending";

    ArrayList<ReportData> reportList = new ArrayList<>();
    Map<String, Boolean> userSuspendedMap = new HashMap<>();
    Map<String, Boolean> listingHiddenMap = new HashMap<>();

    static class ReportData {
        String reportId, targetType, itemId, itemTitle, chatId;
        String reportedUserId, reportedUserName, reporterId, reporterEmail;
        String reason, details, status, adminNote;
        long createdAt, updatedAt;

        ReportData(String reportId, String targetType, String itemId, String itemTitle,
                   String chatId, String reportedUserId, String reportedUserName,
                   String reporterId, String reporterEmail, String reason, String details,
                   String status, String adminNote, long createdAt, long updatedAt) {
            this.reportId = reportId;
            this.targetType = targetType;
            this.itemId = itemId;
            this.itemTitle = itemTitle;
            this.chatId = chatId;
            this.reportedUserId = reportedUserId;
            this.reportedUserName = reportedUserName;
            this.reporterId = reporterId;
            this.reporterEmail = reporterEmail;
            this.reason = reason;
            this.details = details;
            this.status = status;
            this.adminNote = adminNote;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    interface DoneCallback {
        void done();
    }

    interface ItemUpdateCallback {
        void success();

        void failure(String message);
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

        setContentView(R.layout.activity_admin_reports);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        bindViews();
        setupClicks();
        loadReports();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvReportCount = findViewById(R.id.tvReportCount);
        btnFilterPending = findViewById(R.id.btnFilterPending);
        btnFilterResolved = findViewById(R.id.btnFilterResolved);
        btnFilterDismissed = findViewById(R.id.btnFilterDismissed);
        reportContainer = findViewById(R.id.reportContainer);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        btnFilterPending.setOnClickListener(v -> {
            currentFilter = "pending";
            updateFilterButtons();
            renderReports();
        });

        btnFilterResolved.setOnClickListener(v -> {
            currentFilter = "resolved";
            updateFilterButtons();
            renderReports();
        });

        btnFilterDismissed.setOnClickListener(v -> {
            currentFilter = "dismissed";
            updateFilterButtons();
            renderReports();
        });
    }

    private void updateFilterButtons() {
        btnFilterPending.setBackgroundResource(currentFilter.equals("pending") ? R.drawable.bg_primary_button : R.drawable.bg_card);
        btnFilterResolved.setBackgroundResource(currentFilter.equals("resolved") ? R.drawable.bg_primary_button : R.drawable.bg_card);
        btnFilterDismissed.setBackgroundResource(currentFilter.equals("dismissed") ? R.drawable.bg_primary_button : R.drawable.bg_card);
    }

    private void loadReports() {
        reportContainer.removeAllViews();
        tvReportCount.setText("Loading reports...");
        showSimpleState("Loading reports...", "Please wait while reports are being fetched.");

        firestore.collection("reports")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reportList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String reportId = safe(document.getString("reportId"));

                        if (reportId.isEmpty()) {
                            reportId = document.getId();
                        }

                        Long createdAtValue = document.getLong("createdAt");
                        Long updatedAtValue = document.getLong("updatedAt");

                        reportList.add(new ReportData(
                                reportId,
                                safe(document.getString("targetType")),
                                safe(document.getString("itemId")),
                                safe(document.getString("itemTitle")),
                                safe(document.getString("chatId")),
                                safe(document.getString("reportedUserId")),
                                safe(document.getString("reportedUserName")),
                                safe(document.getString("reporterId")),
                                safe(document.getString("reporterEmail")),
                                safe(document.getString("reason")),
                                safe(document.getString("details")),
                                safe(document.getString("status")),
                                safe(document.getString("adminNote")),
                                createdAtValue != null ? createdAtValue : 0,
                                updatedAtValue != null ? updatedAtValue : 0
                        ));
                    }

                    reportList.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                    loadActionStatesThenRender();
                })
                .addOnFailureListener(e -> {
                    tvReportCount.setText("0 report(s)");
                    reportContainer.removeAllViews();
                    showSimpleState("Failed to load reports", e.getMessage());
                });
    }

    private void loadActionStatesThenRender() {
        userSuspendedMap.clear();
        listingHiddenMap.clear();

        ArrayList<String> userIds = new ArrayList<>();
        ArrayList<String> itemIds = new ArrayList<>();

        for (ReportData report : reportList) {
            if (isListingReport(report)) {
                if (!report.itemId.trim().isEmpty() && !itemIds.contains(report.itemId)) {
                    itemIds.add(report.itemId);
                }
            } else {
                if (!report.reportedUserId.trim().isEmpty() && !userIds.contains(report.reportedUserId)) {
                    userIds.add(report.reportedUserId);
                }
            }
        }

        int total = userIds.size() + itemIds.size();

        if (total == 0) {
            updateFilterButtons();
            renderReports();
            return;
        }

        int[] remaining = {total};

        for (String userId : userIds) {
            loadUserSuspendedState(userId, () -> {
                remaining[0]--;

                if (remaining[0] <= 0) {
                    updateFilterButtons();
                    renderReports();
                }
            });
        }

        for (String itemId : itemIds) {
            loadListingHiddenState(itemId, () -> {
                remaining[0]--;

                if (remaining[0] <= 0) {
                    updateFilterButtons();
                    renderReports();
                }
            });
        }
    }

    private void loadUserSuspendedState(String userId, DoneCallback callback) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean suspended = false;

                    if (documentSnapshot.exists()) {
                        Boolean isSuspended = documentSnapshot.getBoolean("isSuspended");
                        String status = documentSnapshot.getString("status");

                        suspended = (isSuspended != null && isSuspended)
                                || (status != null && status.equalsIgnoreCase("suspended"));
                    }

                    userSuspendedMap.put(userId, suspended);
                    callback.done();
                })
                .addOnFailureListener(e -> {
                    userSuspendedMap.put(userId, false);
                    callback.done();
                });
    }

    private void loadListingHiddenState(String itemId, DoneCallback callback) {
        firestore.collection("items")
                .document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listingHiddenMap.put(itemId, isListingHiddenDoc(documentSnapshot));
                        callback.done();
                        return;
                    }

                    firestore.collection("items")
                            .whereEqualTo("itemId", itemId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                boolean hidden = false;

                                if (!queryDocumentSnapshots.isEmpty()) {
                                    QueryDocumentSnapshot document = queryDocumentSnapshots.iterator().next();
                                    hidden = isListingHiddenDoc(document);
                                }

                                listingHiddenMap.put(itemId, hidden);
                                callback.done();
                            })
                            .addOnFailureListener(e -> {
                                listingHiddenMap.put(itemId, false);
                                callback.done();
                            });
                })
                .addOnFailureListener(e -> {
                    listingHiddenMap.put(itemId, false);
                    callback.done();
                });
    }

    private boolean isListingHiddenDoc(DocumentSnapshot documentSnapshot) {
        Boolean isHidden = documentSnapshot.getBoolean("isHidden");
        Boolean hiddenByAdmin = documentSnapshot.getBoolean("hiddenByAdmin");
        String status = documentSnapshot.getString("status");

        return (isHidden != null && isHidden)
                || (hiddenByAdmin != null && hiddenByAdmin)
                || (status != null && status.equalsIgnoreCase("hidden"));
    }

    private void renderReports() {
        reportContainer.removeAllViews();

        int count = 0;

        for (ReportData report : reportList) {
            if (report.status.equalsIgnoreCase(currentFilter)) {
                count++;
                addReportCard(report);
            }
        }

        tvReportCount.setText(count + " " + currentFilter + " report(s)");

        if (count == 0) {
            showSimpleState("No " + currentFilter + " reports", "Reports will appear here when users submit them.");
        }
    }

    private void addReportCard(ReportData report) {
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

        TextView statusText = new TextView(this);
        statusText.setText(report.status.toUpperCase(Locale.getDefault()) + " • " + formatDate(report.createdAt));
        statusText.setTextColor(getResources().getColor(R.color.accent_lime));
        statusText.setTextSize(13);
        statusText.setTypeface(null, Typeface.BOLD);
        card.addView(statusText);

        TextView titleText = new TextView(this);
        titleText.setText(getTitle(report));
        titleText.setTextColor(getResources().getColor(R.color.text_primary_light));
        titleText.setTextSize(19);
        titleText.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(8), 0, 0);
        titleText.setLayoutParams(titleParams);
        card.addView(titleText);

        TextView reasonText = new TextView(this);
        reasonText.setText("Reason: " + report.reason);
        reasonText.setTextColor(getResources().getColor(R.color.text_primary_light));
        reasonText.setTextSize(15);

        LinearLayout.LayoutParams reasonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        reasonParams.setMargins(0, dp(10), 0, 0);
        reasonText.setLayoutParams(reasonParams);
        card.addView(reasonText);

        TextView detailText = new TextView(this);
        detailText.setText(report.details);
        detailText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        detailText.setTextSize(14);

        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.setMargins(0, dp(8), 0, 0);
        detailText.setLayoutParams(detailParams);
        card.addView(detailText);

        TextView metaText = new TextView(this);
        metaText.setText(getMeta(report));
        metaText.setTextColor(getResources().getColor(R.color.text_secondary_light));
        metaText.setTextSize(13);

        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metaParams.setMargins(0, dp(12), 0, 0);
        metaText.setLayoutParams(metaParams);
        card.addView(metaText);

        if (!report.adminNote.trim().isEmpty()) {
            TextView noteText = new TextView(this);
            noteText.setText("Admin note: " + report.adminNote);
            noteText.setTextColor(getResources().getColor(R.color.text_primary_light));
            noteText.setTextSize(13);
            noteText.setTypeface(null, Typeface.BOLD);

            LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            noteParams.setMargins(0, dp(12), 0, 0);
            noteText.setLayoutParams(noteParams);
            card.addView(noteText);
        }

        addMainActionRow(card, report);
        addReportStatusRow(card, report);

        reportContainer.addView(card);
    }

    private void addMainActionRow(LinearLayout card, ReportData report) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        rowParams.setMargins(0, dp(16), 0, 0);
        row.setLayoutParams(rowParams);

        TextView btnViewTarget = actionButton(getViewButtonText(report), false);
        btnViewTarget.setOnClickListener(v -> openTarget(report));
        row.addView(btnViewTarget);

        if (isListingReport(report)) {
            boolean hidden = isListingCurrentlyHidden(report);

            TextView btnListingAction = actionButton(hidden ? "UNHIDE LISTING" : "HIDE LISTING", !hidden);

            if (hidden) {
                btnListingAction.setOnClickListener(v -> confirmUnhideListing(report));
            } else {
                btnListingAction.setOnClickListener(v -> confirmHideListing(report));
            }

            row.addView(btnListingAction);
        } else {
            boolean suspended = isUserCurrentlySuspended(report);

            TextView btnUserAction = actionButton(suspended ? "REACTIVATE USER" : "SUSPEND USER", !suspended);

            if (suspended) {
                btnUserAction.setOnClickListener(v -> confirmReactivateUser(report));
            } else {
                btnUserAction.setOnClickListener(v -> confirmSuspendUser(report));
            }

            row.addView(btnUserAction);
        }

        card.addView(row);
    }

    private void addReportStatusRow(LinearLayout card, ReportData report) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        rowParams.setMargins(0, dp(10), 0, 0);
        row.setLayoutParams(rowParams);

        if (report.status.equalsIgnoreCase("pending")) {
            TextView btnResolve = actionButton("RESOLVE", true);
            TextView btnDismiss = actionButton("DISMISS", false);

            btnResolve.setOnClickListener(v -> showUpdateDialog(report, "resolved"));
            btnDismiss.setOnClickListener(v -> showUpdateDialog(report, "dismissed"));

            row.addView(btnResolve);
            row.addView(btnDismiss);
        } else {
            TextView btnReopen = actionButton("REOPEN REPORT", false);
            btnReopen.setOnClickListener(v -> showUpdateDialog(report, "pending"));
            row.addView(btnReopen);
        }

        card.addView(row);
    }

    private TextView actionButton(String text, boolean primary) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(getResources().getColor(R.color.text_primary_light));
        button.setTextSize(10);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_outline_button);
        button.setClickable(true);
        button.setFocusable(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(params);

        return button;
    }

    private String getViewButtonText(ReportData report) {
        if (report.targetType.equalsIgnoreCase("chat") || !report.chatId.trim().isEmpty()) {
            return "VIEW CHAT";
        }

        if (report.targetType.equalsIgnoreCase("user")) {
            return "VIEW USER";
        }

        return "VIEW LISTING";
    }

    private void openTarget(ReportData report) {
        if (report.targetType.equalsIgnoreCase("chat") || !report.chatId.trim().isEmpty()) {
            Intent intent = new Intent(this, ChatRoomActivity.class);
            intent.putExtra("chatId", report.chatId);
            intent.putExtra("itemId", report.itemId);
            intent.putExtra("itemTitle", report.itemTitle);
            startActivity(intent);
            return;
        }

        if (!report.itemId.trim().isEmpty()) {
            Intent intent = new Intent(this, ItemDetailsActivity.class);
            intent.putExtra("itemId", report.itemId);
            startActivity(intent);
            return;
        }

        if (!report.reportedUserId.trim().isEmpty()) {
            Toast.makeText(this, "User ID: " + report.reportedUserId, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "No target page available", Toast.LENGTH_SHORT).show();
    }

    private void confirmSuspendUser(ReportData report) {
        if (report.reportedUserId.trim().isEmpty()) {
            Toast.makeText(this, "Reported user not found", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Suspension reason");
        input.setMinLines(3);
        input.setGravity(Gravity.TOP);

        new AlertDialog.Builder(this)
                .setTitle("Suspend User")
                .setMessage("This will mark the reported user as suspended.")
                .setView(input)
                .setPositiveButton("Suspend", (dialog, which) -> {
                    String note = input.getText().toString().trim();
                    suspendUser(report, note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void suspendUser(ReportData report, String note) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isSuspended", true);
        updates.put("status", "suspended");
        updates.put("suspendedReason", note);
        updates.put("suspendedBy", firebaseAuth.getCurrentUser().getUid());
        updates.put("suspendedAt", System.currentTimeMillis());
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("users")
                .document(report.reportedUserId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    createAuditLog("USER_SUSPENDED", "Admin suspended user: " + report.reportedUserName, report.reportedUserId, "user");

                    userSuspendedMap.put(report.reportedUserId, true);

                    Toast.makeText(this, "User suspended", Toast.LENGTH_SHORT).show();
                    renderReports();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to suspend user: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void confirmReactivateUser(ReportData report) {
        if (report.reportedUserId.trim().isEmpty()) {
            Toast.makeText(this, "Reported user not found", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Reactivate User")
                .setMessage("This will reactivate the reported user.")
                .setPositiveButton("Reactivate", (dialog, which) -> reactivateUser(report))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reactivateUser(ReportData report) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isSuspended", false);
        updates.put("status", "active");
        updates.put("reactivatedBy", firebaseAuth.getCurrentUser().getUid());
        updates.put("reactivatedAt", System.currentTimeMillis());
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("users")
                .document(report.reportedUserId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    createAuditLog("USER_REACTIVATED", "Admin reactivated user: " + report.reportedUserName, report.reportedUserId, "user");

                    userSuspendedMap.put(report.reportedUserId, false);

                    Toast.makeText(this, "User reactivated", Toast.LENGTH_SHORT).show();
                    renderReports();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to reactivate user: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void confirmHideListing(ReportData report) {
        if (report.itemId.trim().isEmpty()) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Reason for hiding listing");
        input.setMinLines(3);
        input.setGravity(Gravity.TOP);

        new AlertDialog.Builder(this)
                .setTitle("Hide Listing")
                .setMessage("This listing will be hidden by admin.")
                .setView(input)
                .setPositiveButton("Hide", (dialog, which) -> {
                    String note = input.getText().toString().trim();
                    hideListing(report, note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void hideListing(ReportData report, String note) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isHidden", true);
        updates.put("hiddenByAdmin", true);
        updates.put("status", "hidden");
        updates.put("adminHiddenReason", note);
        updates.put("hiddenAt", System.currentTimeMillis());
        updates.put("updatedAt", System.currentTimeMillis());

        updateItemWithFallback(report.itemId, updates, new ItemUpdateCallback() {
            @Override
            public void success() {
                createAuditLog("LISTING_HIDDEN", "Admin hid listing: " + report.itemTitle, report.itemId, "listing");

                listingHiddenMap.put(report.itemId, true);

                Toast.makeText(AdminReportsActivity.this, "Listing hidden", Toast.LENGTH_SHORT).show();
                renderReports();
            }

            @Override
            public void failure(String message) {
                Toast.makeText(AdminReportsActivity.this, "Failed to hide listing: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmUnhideListing(ReportData report) {
        if (report.itemId.trim().isEmpty()) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Unhide Listing")
                .setMessage("This will make the listing active again.")
                .setPositiveButton("Unhide", (dialog, which) -> unhideListing(report))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unhideListing(ReportData report) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isHidden", false);
        updates.put("hiddenByAdmin", false);
        updates.put("status", "active");
        updates.put("unhiddenAt", System.currentTimeMillis());
        updates.put("updatedAt", System.currentTimeMillis());

        updateItemWithFallback(report.itemId, updates, new ItemUpdateCallback() {
            @Override
            public void success() {
                createAuditLog("LISTING_UNHIDDEN", "Admin unhid listing: " + report.itemTitle, report.itemId, "listing");

                listingHiddenMap.put(report.itemId, false);

                Toast.makeText(AdminReportsActivity.this, "Listing unhidden", Toast.LENGTH_SHORT).show();
                renderReports();
            }

            @Override
            public void failure(String message) {
                Toast.makeText(AdminReportsActivity.this, "Failed to unhide listing: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateItemWithFallback(String itemId, Map<String, Object> updates, ItemUpdateCallback callback) {
        firestore.collection("items")
                .document(itemId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.success())
                .addOnFailureListener(firstError -> {
                    firestore.collection("items")
                            .whereEqualTo("itemId", itemId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (queryDocumentSnapshots.isEmpty()) {
                                    callback.failure("Item document not found");
                                    return;
                                }

                                QueryDocumentSnapshot document = queryDocumentSnapshots.iterator().next();

                                firestore.collection("items")
                                        .document(document.getId())
                                        .update(updates)
                                        .addOnSuccessListener(unused -> callback.success())
                                        .addOnFailureListener(secondError -> callback.failure(secondError.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.failure(e.getMessage()));
                });
    }

    private void showUpdateDialog(ReportData report, String newStatus) {
        EditText input = new EditText(this);
        input.setHint("Admin note");
        input.setMinLines(3);
        input.setGravity(Gravity.TOP);

        String title;

        if (newStatus.equals("resolved")) {
            title = "Resolve Report";
        } else if (newStatus.equals("dismissed")) {
            title = "Dismiss Report";
        } else {
            title = "Reopen Report";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Add an admin note for this action.")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String note = input.getText().toString().trim();
                    updateReportStatus(report, newStatus, note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateReportStatus(ReportData report, String newStatus, String adminNote) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("adminNote", adminNote);
        updates.put("updatedAt", System.currentTimeMillis());
        updates.put("reviewedBy", firebaseAuth.getCurrentUser().getUid());
        updates.put("reviewedByEmail", firebaseAuth.getCurrentUser().getEmail() != null ? firebaseAuth.getCurrentUser().getEmail() : "");

        firestore.collection("reports")
                .document(report.reportId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    String action;

                    if (newStatus.equals("resolved")) {
                        action = "REPORT_RESOLVED";
                    } else if (newStatus.equals("dismissed")) {
                        action = "REPORT_DISMISSED";
                    } else {
                        action = "REPORT_REOPENED";
                    }

                    createAuditLog(action, "Admin updated report to " + newStatus + ": " + report.reason, report.reportId, "report");

                    report.status = newStatus;
                    report.adminNote = adminNote;
                    report.updatedAt = System.currentTimeMillis();

                    Toast.makeText(this, "Report " + newStatus, Toast.LENGTH_SHORT).show();
                    renderReports();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update report: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void createAuditLog(String action, String message, String targetId, String targetType) {
        String logId = firestore.collection("auditLogs").document().getId();

        Map<String, Object> log = new HashMap<>();
        log.put("logId", logId);
        log.put("action", action);
        log.put("message", message);
        log.put("targetType", targetType);
        log.put("targetId", targetId);
        log.put("actorId", firebaseAuth.getCurrentUser().getUid());
        log.put("actorEmail", firebaseAuth.getCurrentUser().getEmail() != null ? firebaseAuth.getCurrentUser().getEmail() : "");
        log.put("createdAt", System.currentTimeMillis());

        firestore.collection("auditLogs").document(logId).set(log);
    }

    private void showSimpleState(String title, String subtitle) {
        reportContainer.removeAllViews();

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
        reportContainer.addView(card);
    }

    private boolean isListingReport(ReportData report) {
        return report.targetType.equalsIgnoreCase("listing") || report.chatId.trim().isEmpty();
    }

    private boolean isUserCurrentlySuspended(ReportData report) {
        Boolean value = userSuspendedMap.get(report.reportedUserId);
        return value != null && value;
    }

    private boolean isListingCurrentlyHidden(ReportData report) {
        Boolean value = listingHiddenMap.get(report.itemId);
        return value != null && value;
    }

    private String getTitle(ReportData report) {
        if (report.targetType.equalsIgnoreCase("user")) {
            return "User Report";
        }

        if (report.targetType.equalsIgnoreCase("chat")) {
            return "Chat/User Report";
        }

        return "Listing Report";
    }

    private String getMeta(ReportData report) {
        String meta = "";

        if (!report.itemTitle.trim().isEmpty()) {
            meta += "Item: " + report.itemTitle;
        }

        if (!report.reportedUserName.trim().isEmpty()) {
            meta += addLine(meta) + "Reported user: " + report.reportedUserName;
        }

        if (!report.reporterEmail.trim().isEmpty()) {
            meta += addLine(meta) + "Reported by: " + report.reporterEmail;
        }

        if (!report.itemId.trim().isEmpty()) {
            meta += addLine(meta) + "Item ID: " + report.itemId;
        }

        if (!report.chatId.trim().isEmpty()) {
            meta += addLine(meta) + "Chat ID: " + report.chatId;
        }

        if (isListingReport(report)) {
            meta += addLine(meta) + "Listing action status: " + (isListingCurrentlyHidden(report) ? "HIDDEN" : "ACTIVE");
        } else {
            meta += addLine(meta) + "User action status: " + (isUserCurrentlySuspended(report) ? "SUSPENDED" : "ACTIVE");
        }

        return meta;
    }

    private String addLine(String text) {
        return text.trim().isEmpty() ? "" : "\n";
    }

    private String formatDate(long time) {
        if (time <= 0) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}