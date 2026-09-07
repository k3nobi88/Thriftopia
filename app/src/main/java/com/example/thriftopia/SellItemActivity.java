package com.example.thriftopia;

import android.content.Intent;
import android.content.res.Configuration;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class SellItemActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;

    TextView tvSellTitle, tvSellSubtitle;
    TextView btnUploadImage, btnGenerateDescription, btnSubmitListing;
    ImageView imgItemPreview;

    EditText etItemTitle, etItemBrand, etItemPrice, etItemDescription;

    Spinner spinnerCategory, spinnerCondition, spinnerSize;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    Uri selectedImageUri = null;

    String sellMode = "normal";

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

        setupSystemBars();

        setContentView(R.layout.activity_sell_item);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        sellMode = getIntent().getStringExtra("sellMode");

        if (sellMode == null || sellMode.trim().isEmpty()) {
            sellMode = "normal";
        }

        tvSellTitle = findViewById(R.id.tvSellTitle);
        tvSellSubtitle = findViewById(R.id.tvSellSubtitle);

        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnGenerateDescription = findViewById(R.id.btnGenerateDescription);
        btnSubmitListing = findViewById(R.id.btnSubmitListing);

        imgItemPreview = findViewById(R.id.imgItemPreview);

        etItemTitle = findViewById(R.id.etItemTitle);
        etItemBrand = findViewById(R.id.etItemBrand);
        etItemPrice = findViewById(R.id.etItemPrice);
        etItemDescription = findViewById(R.id.etItemDescription);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        spinnerSize = findViewById(R.id.spinnerSize);

        BottomNavHelper.setup(this, BottomNavHelper.PAGE_SELL);

        setupDropdowns();
        setupSellModeUi();

        btnUploadImage.setOnClickListener(v -> openImagePicker());

        imgItemPreview.setOnClickListener(v -> openImagePicker());

        btnGenerateDescription.setOnClickListener(v -> generateDescription());

        btnSubmitListing.setOnClickListener(v -> submitListing());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupSystemBars();
        BottomNavHelper.setup(this, BottomNavHelper.PAGE_SELL);
    }

    private void setupSystemBars() {
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_light));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.bg_light));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean isDarkMode = (getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

            if (isDarkMode) {
                getWindow().getDecorView().setSystemUiVisibility(0);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    private void setupDropdowns() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                categoryOptions
        );
        categoryAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                conditionOptions
        );
        conditionAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerCondition.setAdapter(conditionAdapter);

        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                sizeOptions
        );
        sizeAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerSize.setAdapter(sizeAdapter);
    }

    private void setupSellModeUi() {
        if (sellMode.equals("auction")) {
            tvSellTitle.setText("Sell by Auction");
            tvSellSubtitle.setText("Create a listing first, then set auction details");
            btnSubmitListing.setText("CONTINUE TO AUCTION");
        } else {
            tvSellTitle.setText("Sell Item");
            tvSellSubtitle.setText("List your thrift item for buyers");
            btnSubmitListing.setText("SUBMIT LISTING");
        }
    }

    private void generateDescription() {
        String title = etItemTitle.getText().toString().trim();
        String brand = etItemBrand.getText().toString().trim();
        String priceText = etItemPrice.getText().toString().trim();

        int categoryPosition = spinnerCategory.getSelectedItemPosition();
        int conditionPosition = spinnerCondition.getSelectedItemPosition();
        int sizePosition = spinnerSize.getSelectedItemPosition();

        if (title.isEmpty()) {
            etItemTitle.setError("Enter item title first");
            etItemTitle.requestFocus();
            Toast.makeText(this, "Fill in item title first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (categoryPosition == 0) {
            Toast.makeText(this, "Select category first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (conditionPosition == 0) {
            Toast.makeText(this, "Select condition first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sizePosition == 0) {
            Toast.makeText(this, "Select size first", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = categoryOptions[categoryPosition];
        String condition = conditionOptions[conditionPosition];
        String size = sizeOptions[sizePosition];

        if (brand.isEmpty()) {
            brand = "unbranded";
        }

        String conditionSentence = getConditionSentence(condition);
        String categorySentence = getCategorySentence(category);

        String priceSentence = "";
        if (!priceText.isEmpty()) {
            priceSentence = " Selling at RM" + priceText + ".";
        }

        String generatedDescription =
                title + " by " + brand + " in " + condition.toLowerCase(Locale.getDefault()) + " condition. "
                        + "Size " + size + ". "
                        + conditionSentence + " "
                        + categorySentence + " "
                        + "Great choice for anyone looking for affordable thrift fashion with a clean and stylish look."
                        + priceSentence + " "
                        + "Please refer to the photos for the actual item condition.";

        etItemDescription.setText(generatedDescription);
        etItemDescription.setSelection(generatedDescription.length());

        Toast.makeText(this, "AI description generated", Toast.LENGTH_SHORT).show();
    }

    private String getConditionSentence(String condition) {
        if (condition.equalsIgnoreCase("New")) {
            return "The item is new and has not been worn.";
        } else if (condition.equalsIgnoreCase("Like New")) {
            return "The item is still in excellent condition with very minimal signs of use.";
        } else if (condition.equalsIgnoreCase("Good")) {
            return "The item is in good condition and suitable for daily wear.";
        } else if (condition.equalsIgnoreCase("Used")) {
            return "The item has been used before, but it is still wearable and functional.";
        } else {
            return "The item condition is shown clearly in the uploaded photos.";
        }
    }

    private String getCategorySentence(String category) {
        if (category.equalsIgnoreCase("Tops")) {
            return "Easy to match with jeans, cargo pants, or casual outfits.";
        } else if (category.equalsIgnoreCase("Bottoms")) {
            return "Suitable for casual styling, streetwear looks, or everyday outfits.";
        } else if (category.equalsIgnoreCase("Outerwear")) {
            return "Perfect for layering and adding character to an outfit.";
        } else if (category.equalsIgnoreCase("Shoes")) {
            return "Suitable for casual wear and daily use.";
        } else if (category.equalsIgnoreCase("Accessories")) {
            return "A nice accessory to complete your outfit.";
        } else {
            return "Suitable for buyers who enjoy unique thrift finds.";
        }
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
                    .into(imgItemPreview);

            btnUploadImage.setText("IMAGE SELECTED");
        }
    }

    private void submitListing() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(SellItemActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        String title = etItemTitle.getText().toString().trim();
        String brand = etItemBrand.getText().toString().trim();
        String priceText = etItemPrice.getText().toString().trim();
        String description = etItemDescription.getText().toString().trim();

        int categoryPosition = spinnerCategory.getSelectedItemPosition();
        int conditionPosition = spinnerCondition.getSelectedItemPosition();
        int sizePosition = spinnerSize.getSelectedItemPosition();

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please upload item image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty()) {
            etItemTitle.setError("Title required");
            etItemTitle.requestFocus();
            return;
        }

        if (categoryPosition == 0) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show();
            spinnerCategory.requestFocus();
            return;
        }

        if (conditionPosition == 0) {
            Toast.makeText(this, "Please select condition", Toast.LENGTH_SHORT).show();
            spinnerCondition.requestFocus();
            return;
        }

        if (brand.isEmpty()) {
            etItemBrand.setError("Brand required");
            etItemBrand.requestFocus();
            return;
        }

        if (sizePosition == 0) {
            Toast.makeText(this, "Please select size", Toast.LENGTH_SHORT).show();
            spinnerSize.requestFocus();
            return;
        }

        if (priceText.isEmpty()) {
            etItemPrice.setError("Price required");
            etItemPrice.requestFocus();
            return;
        }

        double priceValue;

        try {
            priceValue = Double.parseDouble(priceText);
        } catch (Exception e) {
            etItemPrice.setError("Enter valid price");
            etItemPrice.requestFocus();
            return;
        }

        if (priceValue <= 0) {
            etItemPrice.setError("Price must be more than 0");
            etItemPrice.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etItemDescription.setError("Description required");
            etItemDescription.requestFocus();
            return;
        }

        String category = categoryOptions[categoryPosition];
        String condition = conditionOptions[conditionPosition];
        String size = sizeOptions[sizePosition];

        btnSubmitListing.setEnabled(false);
        btnSubmitListing.setText(sellMode.equals("auction") ? "PREPARING AUCTION..." : "SUBMITTING...");

        createListing(title, category, condition, brand, size, priceValue, description);
    }

    private void createListing(String title, String category, String condition, String brand,
                               String size, double priceValue, String description) {

        String sellerId = firebaseAuth.getCurrentUser().getUid();

        DocumentReference itemRef = firestore.collection("items").document();
        String itemId = itemRef.getId();

        String imagePath = "item_images/" + itemId + ".jpg";

        StorageReference imageRef = storageReference.child(imagePath);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();

                                saveListingToFirestore(
                                        itemRef,
                                        itemId,
                                        sellerId,
                                        title,
                                        category,
                                        condition,
                                        brand,
                                        size,
                                        priceValue,
                                        description,
                                        imageUrl,
                                        imagePath
                                );
                            })
                            .addOnFailureListener(e -> {
                                resetSubmitButton();
                                Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    resetSubmitButton();
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveListingToFirestore(DocumentReference itemRef, String itemId, String sellerId,
                                        String title, String category, String condition, String brand,
                                        String size, double priceValue, String description,
                                        String imageUrl, String imagePath) {

        Map<String, Object> item = new HashMap<>();
        item.put("itemId", itemId);
        item.put("sellerId", sellerId);
        item.put("title", title);
        item.put("searchTitle", title.toLowerCase());
        item.put("category", category);
        item.put("condition", condition);
        item.put("brand", brand);
        item.put("size", size);
        item.put("price", priceValue);
        item.put("description", description);
        item.put("imageUrl", imageUrl);
        item.put("imagePath", imagePath);
        item.put("status", "active");
        item.put("buyerId", "");
        item.put("saleType", sellMode.equals("auction") ? "auction" : "normal");
        item.put("listingType", sellMode.equals("auction") ? "auction" : "normal");
        item.put("type", sellMode.equals("auction") ? "auction" : "normal");
        item.put("isSold", false);
        item.put("createdAt", System.currentTimeMillis());
        item.put("updatedAt", System.currentTimeMillis());

        itemRef.set(item)
                .addOnSuccessListener(unused -> {
                    if (sellMode.equals("auction")) {
                        goToAuctionCreation(itemId);
                    } else {
                        showNormalSellSuccessDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    resetSubmitButton();
                    Toast.makeText(this, "Failed to save listing: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void goToAuctionCreation(String itemId) {
        Toast.makeText(this, "Listing created. Set auction details now.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(SellItemActivity.this, AuctionCreationActivity.class);
        intent.putExtra("itemId", itemId);
        startActivity(intent);
        finish();
    }

    private void showNormalSellSuccessDialog() {
        btnSubmitListing.setEnabled(true);
        btnSubmitListing.setText("SUBMIT LISTING");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Listing Created");
        builder.setMessage("Your item has been listed successfully.");

        builder.setPositiveButton("Done", (dialog, which) -> {
            Intent intent = new Intent(SellItemActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("My Listings", (dialog, which) -> {
            Intent intent = new Intent(SellItemActivity.this, MyListingsActivity.class);
            startActivity(intent);
            finish();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void resetSubmitButton() {
        btnSubmitListing.setEnabled(true);

        if (sellMode.equals("auction")) {
            btnSubmitListing.setText("CONTINUE TO AUCTION");
        } else {
            btnSubmitListing.setText("SUBMIT LISTING");
        }
    }
}