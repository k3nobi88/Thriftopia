package com.example.thriftopia;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SuspendedUserGuard {

    public interface AllowedCallback {
        void onAllowed();
    }

    public static void checkCurrentSession(Activity activity) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isSuspended(documentSnapshot)) {
                        forceLogout(activity);
                    }
                });
    }

    public static void checkAfterLogin(Activity activity, AllowedCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isSuspended(documentSnapshot)) {
                        auth.signOut();
                        Toast.makeText(activity, "Your account has been suspended by admin.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    callback.onAllowed();
                })
                .addOnFailureListener(e -> {
                    callback.onAllowed();
                });
    }

    private static boolean isSuspended(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) {
            return false;
        }

        Boolean isSuspended = documentSnapshot.getBoolean("isSuspended");
        String status = documentSnapshot.getString("status");

        return (isSuspended != null && isSuspended)
                || (status != null && status.equalsIgnoreCase("suspended"));
    }

    private static void forceLogout(Activity activity) {
        FirebaseAuth.getInstance().signOut();

        Toast.makeText(activity, "Your account has been suspended by admin.", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}