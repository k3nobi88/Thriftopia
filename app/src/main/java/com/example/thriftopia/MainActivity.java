package com.example.thriftopia;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

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

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        showSplashUi();

        new Handler(Looper.getMainLooper()).postDelayed(this::routeUser, 1200);
    }

    private void showSplashUi() {
        LinearLayout root = new LinearLayout(this);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(getResources().getColor(R.color.bg_light));
        root.setGravity(Gravity.CENTER);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.thriftopia_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(170), dp(170));
        logo.setLayoutParams(logoParams);

        TextView appName = new TextView(this);
        appName.setText("THRIFTOPIA");
        appName.setTextColor(getResources().getColor(R.color.text_primary_light));
        appName.setTextSize(28);
        appName.setGravity(Gravity.CENTER);
        appName.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nameParams.setMargins(0, dp(18), 0, 0);
        appName.setLayoutParams(nameParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("Thrift • Bid • Buy");
        subtitle.setTextColor(getResources().getColor(R.color.text_secondary_light));
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dp(6), 0, 0);
        subtitle.setLayoutParams(subtitleParams);

        root.addView(logo);
        root.addView(appName);
        root.addView(subtitle);

        setContentView(root);
    }

    private void routeUser() {
        if (firebaseAuth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        firebaseAuth.signOut();
                        goToLogin();
                        return;
                    }

                    Boolean isSuspendedValue = documentSnapshot.getBoolean("isSuspended");
                    String status = documentSnapshot.getString("status");

                    boolean isSuspended = (isSuspendedValue != null && isSuspendedValue)
                            || (status != null && status.equalsIgnoreCase("suspended"));

                    if (isSuspended) {
                        firebaseAuth.signOut();
                        Toast.makeText(this, "Your account has been suspended by admin.", Toast.LENGTH_LONG).show();
                        goToLogin();
                        return;
                    }

                    String role = documentSnapshot.getString("role");

                    if (role != null && role.equalsIgnoreCase("admin")) {
                        Intent intent = new Intent(MainActivity.this, AdminDashboardActivity.class);

                        String email = firebaseAuth.getCurrentUser() != null
                                ? firebaseAuth.getCurrentUser().getEmail()
                                : "";

                        intent.putExtra("adminEmail", email);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    firebaseAuth.signOut();
                    goToLogin();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}