package com.example.thriftopia;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditListingActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 2001;

    TextView btnBack, btnChangeImage, btnUpdateListing;
    ImageView imgEditItemPreview;

    EditText etEditTitle, etEditBrand, etEditPrice, etEditDescription;
    Spinner spinnerEditCategory, spinnerEditCondition, spinnerEditSize;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    String itemId = "";
    String imageUrl = "";
    String imagePath = "";
    String sellerId = "";

    Uri selectedImageUri = null;

    String[] categoryOptions = {
            "Select category",
            "Tops",
            "Bottoms",
            "Outerwear",
            "Shoes",
            "Accessories"
    };

    String[] conditionOptions = {
            "Select condition",
            "New",
            "Like New",
            "Good",
            "Used"
    };

    String[] sizeOptions = {
            "Select size",
            "XS",
            "S",
            "M",
            "L",
            "XL",
            "Free Size"
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

        setContentView(R.layout.activity_edit_listing);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        itemId = getIntent().getStringExtra("itemId");

        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(this, "Invalid listing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnUpdateListing = findViewById(R.id.btnUpdateListing);

        imgEditItemPreview = findViewById(R.id.imgEditItemPreview);

        etEditTitle = findViewById(R.id.etEditTitle);
        etEditBrand = findViewById(R.id.etEditBrand);
        etEditPrice = findViewById(R.id.etEditPrice);
        etEditDescription = findViewById(R.id.etEditDescription);

        spinnerEditCategory = findViewById(R.id.spinnerEditCategory);
        spinnerEditCondition = findViewById(R.id.spinnerEditCondition);
        spinnerEditSize = findViewById(R.id.spinnerEditSize);

        setupDropdowns();

        btnBack.setOnClickListener(v -> finish());

        btnChangeImage.setOnClickListener(v -> openImagePicker());

        btnUpdateListing.setOnClickListener(v -> updateListing());

        loadListingData();
    }

    private void setupDropdowns() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                categoryOptions
        );
        categoryAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerEditCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                conditionOptions
        );
        conditionAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerEditCondition.setAdapter(conditionAdapter);

        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                sizeOptions
        );
        sizeAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerEditSize.setAdapter(sizeAdapter);
    }

    private void loadListingData() {
        firestore.collection("items")
                .document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Listing not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    sellerId = documentSnapshot.getString("sellerId");

                    if (firebaseAuth.getCurrentUser() == null ||
                            sellerId == null ||
                            !sellerId.equals(firebaseAuth.getCurrentUser().getUid())) {
                        Toast.makeText(this, "You can only edit your own listing", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String title = documentSnapshot.getString("title");
                    String category = documentSnapshot.getString("category");
                    String condition = documentSnapshot.getString("condition");
                    String brand = documentSnapshot.getString("brand");
                    String size = documentSnapshot.getString("size");
                    String description = documentSnapshot.getString("description");

                    imageUrl = documentSnapshot.getString("imageUrl");
                    imagePath = documentSnapshot.getString("imagePath");

                    Double priceValue = documentSnapshot.getDouble("price");

                    etEditTitle.setText(title != null ? title : "");
                    etEditBrand.setText(brand != null ? brand : "");
                    etEditDescription.setText(description != null ? description : "");

                    if (priceValue != null) {
                        if (priceValue % 1 == 0) {
                            etEditPrice.setText(String.valueOf(priceValue.intValue()));
                        } else {
                            etEditPrice.setText(String.valueOf(priceValue));
                        }
                    } else {
                        etEditPrice.setText("");
                    }

                    spinnerEditCategory.setSelection(getOptionIndex(categoryOptions, category));
                    spinnerEditCondition.setSelection(getOptionIndex(conditionOptions, condition));
                    spinnerEditSize.setSelection(getOptionIndex(sizeOptions, size));

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(imageUrl)
                                .centerCrop()
                                .into(imgEditItemPreview);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load listing: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private int getOptionIndex(String[] options, String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(value.trim())) {
                return i;
            }
        }

        return 0;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(imgEditItemPreview);

            btnChangeImage.setText("IMAGE SELECTED");
        }
    }

    private void updateListing() {
        String title = etEditTitle.getText().toString().trim();
        String brand = etEditBrand.getText().toString().trim();
        String priceText = etEditPrice.getText().toString().trim();
        String description = etEditDescription.getText().toString().trim();

        int categoryPosition = spinnerEditCategory.getSelectedItemPosition();
        int conditionPosition = spinnerEditCondition.getSelectedItemPosition();
        int sizePosition = spinnerEditSize.getSelectedItemPosition();

        if (title.isEmpty()) {
            etEditTitle.setError("Title required");
            etEditTitle.requestFocus();
            return;
        }

        if (categoryPosition == 0) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show();
            spinnerEditCategory.requestFocus();
            return;
        }

        if (conditionPosition == 0) {
            Toast.makeText(this, "Please select condition", Toast.LENGTH_SHORT).show();
            spinnerEditCondition.requestFocus();
            return;
        }

        if (brand.isEmpty()) {
            etEditBrand.setError("Brand required");
            etEditBrand.requestFocus();
            return;
        }

        if (sizePosition == 0) {
            Toast.makeText(this, "Please select size", Toast.LENGTH_SHORT).show();
            spinnerEditSize.requestFocus();
            return;
        }

        if (priceText.isEmpty()) {
            etEditPrice.setError("Price required");
            etEditPrice.requestFocus();
            return;
        }

        double priceValue;

        try {
            priceValue = Double.parseDouble(priceText);
        } catch (Exception e) {
            etEditPrice.setError("Enter valid price");
            etEditPrice.requestFocus();
            return;
        }

        if (priceValue <= 0) {
            etEditPrice.setError("Price must be more than 0");
            etEditPrice.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etEditDescription.setError("Description required");
            etEditDescription.requestFocus();
            return;
        }

        String category = categoryOptions[categoryPosition];
        String condition = conditionOptions[conditionPosition];
        String size = sizeOptions[sizePosition];

        btnUpdateListing.setEnabled(false);
        btnUpdateListing.setText("UPDATING...");

        if (selectedImageUri != null) {
            uploadNewImage(title, category, condition, brand, size, priceValue, description);
        } else {
            updateFirestoreListing(title, category, condition, brand, size, priceValue, description, imageUrl, imagePath);
        }
    }

    private void uploadNewImage(String title, String category, String condition, String brand,
                                String size, double priceValue, String description) {

        String newImagePath = imagePath;

        if (newImagePath == null || newImagePath.isEmpty()) {
            newImagePath = "item_images/" + itemId + ".jpg";
        }

        StorageReference imageRef = storageReference.child(newImagePath);
        String finalNewImagePath = newImagePath;

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String newImageUrl = uri.toString();
                                updateFirestoreListing(
                                        title,
                                        category,
                                        condition,
                                        brand,
                                        size,
                                        priceValue,
                                        description,
                                        newImageUrl,
                                        finalNewImagePath
                                );
                            })
                            .addOnFailureListener(e -> {
                                resetUpdateButton();
                                Toast.makeText(this, "Failed to get new image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    resetUpdateButton();
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateFirestoreListing(String title, String category, String condition, String brand,
                                        String size, double priceValue, String description,
                                        String finalImageUrl, String finalImagePath) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("category", category);
        updates.put("condition", condition);
        updates.put("brand", brand);
        updates.put("size", size);
        updates.put("price", priceValue);
        updates.put("description", description);
        updates.put("imageUrl", finalImageUrl);
        updates.put("imagePath", finalImagePath);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("items")
                .document(itemId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    updateRelatedAuctions(title, category, condition, brand, size, finalImageUrl);
                    Toast.makeText(this, "Listing updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetUpdateButton();
                    Toast.makeText(this, "Failed to update listing: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateRelatedAuctions(String title, String category, String condition,
                                       String brand, String size, String finalImageUrl) {
        firestore.collection("auctions")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> auctionUpdates = new HashMap<>();
                        auctionUpdates.put("title", title);
                        auctionUpdates.put("category", category);
                        auctionUpdates.put("condition", condition);
                        auctionUpdates.put("brand", brand);
                        auctionUpdates.put("size", size);
                        auctionUpdates.put("imageUrl", finalImageUrl);
                        auctionUpdates.put("updatedAt", System.currentTimeMillis());

                        firestore.collection("auctions")
                                .document(document.getId())
                                .update(auctionUpdates);
                    }
                });
    }

    private void resetUpdateButton() {
        btnUpdateListing.setEnabled(true);
        btnUpdateListing.setText("UPDATE LISTING");
    }
}