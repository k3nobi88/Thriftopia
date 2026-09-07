package com.example.thriftopia;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    TextView btnBack, btnSubmitReport, tvReportType, tvTargetInfo;
    Spinner spReason;
    EditText etDetails;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String targetType = "";
    String itemId = "";
    String itemTitle = "";
    String reportedUserId = "";
    String reportedUserName = "";
    String chatId = "";

    boolean isSubmitting = false;

    String[] reasons = {
            "Fake or misleading listing",
            "Inappropriate item",
            "Scam or suspicious activity",
            "Harassment or rude behaviour",
            "Item already sold but still listed",
            "Wrong category or wrong information",
            "Other"
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

        setContentView(R.layout.activity_report);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        getIntentData();
        bindViews();
        setupSpinner();
        setupClicks();
        setupPageInfo();
        loadMissingTargetInfo();
    }

    private void getIntentData() {
        targetType = safe(getIntent().getStringExtra("targetType"));
        itemId = safe(getIntent().getStringExtra("itemId"));
        itemTitle = safe(getIntent().getStringExtra("itemTitle"));
        reportedUserId = safe(getIntent().getStringExtra("reportedUserId"));
        reportedUserName = safe(getIntent().getStringExtra("reportedUserName"));
        chatId = safe(getIntent().getStringExtra("chatId"));

        if (targetType.trim().isEmpty()) {
            targetType = "listing";
        }
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        tvReportType = findViewById(R.id.tvReportType);
        tvTargetInfo = findViewById(R.id.tvTargetInfo);
        spReason = findViewById(R.id.spReason);
        etDetails = findViewById(R.id.etDetails);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                reasons
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReason.setAdapter(adapter);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());
        btnSubmitReport.setOnClickListener(v -> submitReport());
    }

    private void setupPageInfo() {
        if (targetType.equalsIgnoreCase("user")) {
            tvReportType.setText("Report User");
        } else if (targetType.equalsIgnoreCase("chat")) {
            tvReportType.setText("Report Chat/User");
        } else {
            tvReportType.setText("Report Listing");
        }

        updateTargetText();
    }

    private void updateTargetText() {
        String info = "";

        if (!itemTitle.trim().isEmpty()) {
            info += "Item: " + itemTitle;
        }

        if (!reportedUserName.trim().isEmpty()) {
            if (!info.trim().isEmpty()) {
                info += "\n";
            }
            info += "User: " + reportedUserName;
        }

        if (!itemId.trim().isEmpty()) {
            if (!info.trim().isEmpty()) {
                info += "\n";
            }
            info += "Item ID: " + itemId;
        }

        if (!chatId.trim().isEmpty()) {
            if (!info.trim().isEmpty()) {
                info += "\n";
            }
            info += "Chat ID: " + chatId;
        }

        if (info.trim().isEmpty()) {
            info = "Report target will be attached automatically.";
        }

        tvTargetInfo.setText(info);
    }

    private void loadMissingTargetInfo() {
        if (!itemId.trim().isEmpty() && itemTitle.trim().isEmpty()) {
            firestore.collection("items")
                    .document(itemId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            findItemByField();
                            return;
                        }

                        String title = documentSnapshot.getString("title");
                        String sellerId = documentSnapshot.getString("sellerId");

                        if (title != null) {
                            itemTitle = title;
                        }

                        if (sellerId != null && reportedUserId.trim().isEmpty()) {
                            reportedUserId = sellerId;
                            loadReportedUserName();
                        }

                        updateTargetText();
                    })
                    .addOnFailureListener(e -> {
                    });
        }

        if (!reportedUserId.trim().isEmpty() && reportedUserName.trim().isEmpty()) {
            loadReportedUserName();
        }
    }

    private void findItemByField() {
        firestore.collection("items")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        return;
                    }

                    com.google.firebase.firestore.QueryDocumentSnapshot document =
                            queryDocumentSnapshots.iterator().next();

                    String title = document.getString("title");
                    String sellerId = document.getString("sellerId");

                    if (title != null) {
                        itemTitle = title;
                    }

                    if (sellerId != null && reportedUserId.trim().isEmpty()) {
                        reportedUserId = sellerId;
                        loadReportedUserName();
                    }

                    updateTargetText();
                });
    }

    private void loadReportedUserName() {
        firestore.collection("users")
                .document(reportedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        updateTargetText();
                        return;
                    }

                    String fullName = documentSnapshot.getString("fullName");
                    String email = documentSnapshot.getString("email");

                    if (fullName != null && !fullName.trim().isEmpty()) {
                        reportedUserName = fullName;
                    } else if (email != null && !email.trim().isEmpty()) {
                        reportedUserName = email;
                    }

                    updateTargetText();
                });
    }

    private void submitReport() {
        if (isSubmitting) {
            return;
        }

        String reason = spReason.getSelectedItem() != null ? spReason.getSelectedItem().toString() : "";
        String details = etDetails.getText().toString().trim();

        if (details.isEmpty()) {
            etDetails.setError("Please enter report details");
            etDetails.requestFocus();
            return;
        }

        isSubmitting = true;
        btnSubmitReport.setText("SUBMITTING...");
        btnSubmitReport.setEnabled(false);

        String reporterId = firebaseAuth.getCurrentUser().getUid();
        String reporterEmail = firebaseAuth.getCurrentUser().getEmail() != null
                ? firebaseAuth.getCurrentUser().getEmail()
                : "";

        String reportId = firestore.collection("reports").document().getId();

        Map<String, Object> report = new HashMap<>();
        report.put("reportId", reportId);
        report.put("targetType", targetType.toLowerCase(Locale.getDefault()));
        report.put("itemId", itemId);
        report.put("itemTitle", itemTitle);
        report.put("chatId", chatId);
        report.put("reportedUserId", reportedUserId);
        report.put("reportedUserName", reportedUserName);
        report.put("reporterId", reporterId);
        report.put("reporterEmail", reporterEmail);
        report.put("reason", reason);
        report.put("details", details);
        report.put("status", "pending");
        report.put("adminNote", "");
        report.put("createdAt", System.currentTimeMillis());
        report.put("updatedAt", System.currentTimeMillis());

        firestore.collection("reports")
                .document(reportId)
                .set(report)
                .addOnSuccessListener(unused -> {
                    createAuditLog("REPORT_CREATED", "New report submitted: " + reason, reportId);

                    Toast.makeText(this, "Report submitted. Admin will review it.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    isSubmitting = false;
                    btnSubmitReport.setText("SUBMIT REPORT");
                    btnSubmitReport.setEnabled(true);
                    Toast.makeText(this, "Failed to submit report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void createAuditLog(String action, String message, String targetId) {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String logId = firestore.collection("auditLogs").document().getId();

        Map<String, Object> log = new HashMap<>();
        log.put("logId", logId);
        log.put("action", action);
        log.put("message", message);
        log.put("targetType", "report");
        log.put("targetId", targetId);
        log.put("actorId", firebaseAuth.getCurrentUser().getUid());
        log.put("actorEmail", firebaseAuth.getCurrentUser().getEmail() != null ? firebaseAuth.getCurrentUser().getEmail() : "");
        log.put("createdAt", System.currentTimeMillis());

        firestore.collection("auditLogs").document(logId).set(log);
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}