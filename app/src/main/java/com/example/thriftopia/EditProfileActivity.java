package com.example.thriftopia;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 2001;

    TextView btnBack, btnChoosePhoto, btnSave;
    ImageView imgProfilePhoto;
    EditText etFullName, etEmail;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    FirebaseStorage storage;

    Bitmap croppedBitmap = null;

    String currentUserId = "";
    String currentPhotoUrl = "";

    boolean isSaving = false;

    Handler timeoutHandler = new Handler(Looper.getMainLooper());
    Runnable timeoutRunnable;

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

        setContentView(R.layout.activity_edit_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = firebaseAuth.getCurrentUser().getUid();

        bindViews();
        setupClicks();
        loadProfile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnChoosePhoto = findViewById(R.id.btnChoosePhoto);
        btnSave = findViewById(R.id.btnSave);

        imgProfilePhoto = findViewById(R.id.imgProfilePhoto);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        btnChoosePhoto.setOnClickListener(v -> openImagePicker());

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        if (firebaseAuth.getCurrentUser() != null && firebaseAuth.getCurrentUser().getEmail() != null) {
            etEmail.setText(firebaseAuth.getCurrentUser().getEmail());
        }

        firestore.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    String fullName = documentSnapshot.getString("fullName");
                    String email = documentSnapshot.getString("email");
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                    if (fullName != null) {
                        etFullName.setText(fullName);
                    }

                    if (email != null) {
                        etEmail.setText(email);
                    }

                    if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
                        currentPhotoUrl = profileImageUrl;

                        Glide.with(EditProfileActivity.this)
                                .load(profileImageUrl)
                                .centerCrop()
                                .into(imgProfilePhoto);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Bitmap bitmap = uriToBitmap(data.getData());

            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            showCropDialog(bitmap);
        }
    }

    private Bitmap uriToBitmap(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            return null;
        }
    }

    private void showCropDialog(Bitmap bitmap) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getResources().getColor(R.color.bg_light));
        root.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView title = new TextView(this);
        title.setText("Crop Profile Photo");
        title.setTextColor(getResources().getColor(R.color.text_primary_light));
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Drag or pinch to adjust, then tap DONE.");
        hint.setTextColor(getResources().getColor(R.color.text_secondary_light));
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.setMargins(0, dp(6), 0, dp(12));
        hint.setLayoutParams(hintParams);

        root.addView(hint);

        SquareCropImageView cropView = new SquareCropImageView(this);
        cropView.setBitmap(bitmap);

        LinearLayout.LayoutParams cropParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(380)
        );
        cropParams.setMargins(0, dp(8), 0, dp(16));
        cropView.setLayoutParams(cropParams);

        root.addView(cropView);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("CANCEL");
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextSize(14);
        btnCancel.setTypeface(null, android.graphics.Typeface.BOLD);
        btnCancel.setTextColor(getResources().getColor(R.color.text_primary_light));
        btnCancel.setBackgroundResource(R.drawable.bg_outline_button);
        btnCancel.setClickable(true);
        btnCancel.setFocusable(true);

        TextView btnDone = new TextView(this);
        btnDone.setText("DONE");
        btnDone.setGravity(Gravity.CENTER);
        btnDone.setTextSize(14);
        btnDone.setTypeface(null, android.graphics.Typeface.BOLD);
        btnDone.setTextColor(getResources().getColor(R.color.text_primary_light));
        btnDone.setBackgroundResource(R.drawable.bg_primary_button);
        btnDone.setClickable(true);
        btnDone.setFocusable(true);

        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(54), 1);
        cancelParams.setMargins(0, 0, dp(8), 0);
        btnCancel.setLayoutParams(cancelParams);

        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(0, dp(54), 1);
        doneParams.setMargins(dp(8), 0, 0, 0);
        btnDone.setLayoutParams(doneParams);

        buttonRow.addView(btnCancel);
        buttonRow.addView(btnDone);

        root.addView(buttonRow);

        dialog.setContentView(root);

        Window window = dialog.getWindow();

        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDone.setOnClickListener(v -> {
            Bitmap result = cropView.getCroppedBitmap(700);

            if (result == null) {
                Toast.makeText(this, "Failed to crop image", Toast.LENGTH_SHORT).show();
                return;
            }

            croppedBitmap = result;

            Glide.with(this)
                    .load(croppedBitmap)
                    .centerCrop()
                    .into(imgProfilePhoto);

            btnChoosePhoto.setText("PHOTO CROPPED");
            dialog.dismiss();
        });

        dialog.show();

        Window shownWindow = dialog.getWindow();

        if (shownWindow != null) {
            shownWindow.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
    }

    private void saveProfile() {
        if (isSaving) {
            return;
        }

        String fullName = etFullName.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Full name required");
            etFullName.requestFocus();
            return;
        }

        setSavingState(true);

        if (croppedBitmap != null) {
            uploadCroppedProfileImage(fullName);
        } else {
            updateProfileData(fullName, currentPhotoUrl);
        }
    }

    private void uploadCroppedProfileImage(String fullName) {
        byte[] imageBytes = bitmapToBytes(croppedBitmap);

        if (imageBytes == null) {
            setSavingState(false);
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference imageRef = storage.getReference()
                .child("profile_images")
                .child(currentUserId + ".jpg");

        imageRef.putBytes(imageBytes)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        updateProfileData(fullName, uri.toString())
                                )
                                .addOnFailureListener(e -> {
                                    setSavingState(false);
                                    Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                })
                )
                .addOnFailureListener(e -> {
                    setSavingState(false);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private byte[] bitmapToBytes(Bitmap bitmap) {
        try {
            Bitmap finalBitmap = resizeBitmap(bitmap, 700);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void updateProfileData(String fullName, String profileImageUrl) {
        String email = "";

        if (firebaseAuth.getCurrentUser() != null && firebaseAuth.getCurrentUser().getEmail() != null) {
            email = firebaseAuth.getCurrentUser().getEmail();
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("email", email);
        updates.put("profileImageUrl", profileImageUrl);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("users")
                .document(currentUserId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    setSavingState(false);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    firestore.collection("users")
                            .document(currentUserId)
                            .set(updates)
                            .addOnSuccessListener(unused -> {
                                setSavingState(false);
                                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(error -> {
                                setSavingState(false);
                                Toast.makeText(this, "Failed to update profile: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void setSavingState(boolean saving) {
        isSaving = saving;

        if (saving) {
            btnSave.setText("SAVING...");
            btnSave.setEnabled(false);
            btnChoosePhoto.setEnabled(false);

            if (timeoutRunnable != null) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
            }

            timeoutRunnable = () -> {
                if (isSaving) {
                    setSavingState(false);
                    Toast.makeText(EditProfileActivity.this, "Saving took too long. Check internet/Firebase Storage rules.", Toast.LENGTH_LONG).show();
                }
            };

            timeoutHandler.postDelayed(timeoutRunnable, 25000);
        } else {
            btnSave.setText("SAVE CHANGES");
            btnSave.setEnabled(true);
            btnChoosePhoto.setEnabled(true);

            if (timeoutRunnable != null) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
            }
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}