package com.example.forkit.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.forkit.BuildConfig;
import com.example.forkit.R;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.utils.ApiClient;
import com.example.forkit.utils.CaloriesNinjaApi;
import com.example.forkit.utils.DateUtils;
import com.example.forkit.utils.GeminiApi;
import com.example.forkit.utils.GeminiClient;
import com.example.forkit.utils.GeminiNutritionResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraFragment extends Fragment {

    private TextInputEditText etFoodQuery;
    private TextView tvResult, tvCaloriesFound, tvMacrosFound;
    private MaterialButton btnSearch, btnAddFood, btnCamera, btnGallery;
    private ImageView ivMealPreview;
    private Spinner spinnerMealType;
    private ProgressBar progressBar;

    private CaloriesNinjaApi.NutritionItem foundItem = null;
    private Uri photoUri;
    private File photoFile;
    private static final String NUTRITION_PROMPT = "analyze this food image. return only valid json with: {\"name\":\"food name\",\"calories\":number,\"protein_g\":number,\"carbs_g\":number,\"fat_g\":number}. no markdown. estimate portions and total nutrition.";

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && photoUri != null) {
                    processImageUri(photoUri);
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processImageUri(uri);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Snackbar.make(requireView(), "camera permission needed", Snackbar.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etFoodQuery = view.findViewById(R.id.et_food_query);
        tvResult = view.findViewById(R.id.tv_result_label);
        tvCaloriesFound = view.findViewById(R.id.tv_calories_found);
        tvMacrosFound = view.findViewById(R.id.tv_macros_found);
        btnSearch = view.findViewById(R.id.btn_search_food);
        btnAddFood = view.findViewById(R.id.btn_add_food);
        btnCamera = view.findViewById(R.id.btn_camera);
        btnGallery = view.findViewById(R.id.btn_gallery);
        ivMealPreview = view.findViewById(R.id.iv_meal_preview);
        spinnerMealType = view.findViewById(R.id.spinner_meal_type);
        progressBar = view.findViewById(R.id.progress_search);

        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, mealTypes);
        spinnerMealType.setAdapter(mealAdapter);

        String suggested = DateUtils.getMealTypeForHour();
        for (int i = 0; i < mealTypes.length; i++) {
            if (mealTypes[i].equals(suggested)) spinnerMealType.setSelection(i);
        }

        btnSearch.setOnClickListener(v -> searchFood());
        btnAddFood.setOnClickListener(v -> addFoodToLog());
        btnCamera.setOnClickListener(v -> openCamera());
        btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnAddFood.setVisibility(View.GONE);
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        try {
            photoFile = File.createTempFile("meal_", ".jpg", requireContext().getCacheDir());
            photoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(photoUri);
        } catch (Exception e) {
            Snackbar.make(requireView(), "cannot create photo file", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void processImageUri(Uri uri) {
        ivMealPreview.setVisibility(View.VISIBLE);
        ivMealPreview.setImageURI(uri);
        analyzeWithGemini(uri);
    }

    private void analyzeWithGemini(Uri uri) {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            Snackbar.make(requireView(), "add GEMINI_API_KEY to local.properties", Snackbar.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnAddFood.setVisibility(View.GONE);
        foundItem = null;

        new Thread(() -> {
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                if (is == null) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Snackbar.make(requireView(), "cannot read image", Snackbar.LENGTH_SHORT).show();
                    });
                    return;
                }
                byte[] bytes = new byte[is.available()];
                is.read(bytes);
                is.close();

                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp == null) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Snackbar.make(requireView(), "cannot decode image", Snackbar.LENGTH_SHORT).show();
                    });
                    return;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                GeminiApi.GeminiRequest.Part.InlineData imgData = new GeminiApi.GeminiRequest.Part.InlineData();
                imgData.mimeType = "image/jpeg";
                imgData.data = base64;
                GeminiApi.GeminiRequest.Part[] parts = new GeminiApi.GeminiRequest.Part[]{
                        new GeminiApi.GeminiRequest.Part(imgData),
                        new GeminiApi.GeminiRequest.Part(NUTRITION_PROMPT)
                };

                GeminiApi.GeminiRequest.Content content = new GeminiApi.GeminiRequest.Content();
                content.parts = parts;

                GeminiApi.GeminiRequest request = new GeminiApi.GeminiRequest(
                        new GeminiApi.GeminiRequest.Content[]{content},
                        new GeminiApi.GeminiRequest.GenerationConfig()
                );

                GeminiClient.getApi().generateContent(apiKey, request).enqueue(new Callback<GeminiApi.GeminiResponse>() {
                    @Override
                    public void onResponse(Call<GeminiApi.GeminiResponse> call, Response<GeminiApi.GeminiResponse> response) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().candidates != null && response.body().candidates.length > 0
                                    && response.body().candidates[0].content != null
                                    && response.body().candidates[0].content.parts != null
                                    && response.body().candidates[0].content.parts.length > 0) {
                                String text = response.body().candidates[0].content.parts[0].text;
                                if (text != null) {
                                    text = text.trim().replaceAll("^```json\\s*|\\s*```$", "");
                                    try {
                                        GeminiNutritionResult result = new Gson().fromJson(text, GeminiNutritionResult.class);
                                        if (result != null && result.name != null) {
                                            foundItem = new CaloriesNinjaApi.NutritionItem();
                                            foundItem.name = result.name;
                                            foundItem.calories = result.calories;
                                            foundItem.protein_g = result.proteinG;
                                            foundItem.carbohydrates_total_g = result.carbsG;
                                            foundItem.fat_total_g = result.fatG;
                                            tvResult.setText("found: " + capitalize(foundItem.name));
                                            tvCaloriesFound.setText((int) foundItem.calories + " kcal");
                                            tvMacrosFound.setText(String.format("protein: %.1fg   carbs: %.1fg   fat: %.1fg",
                                                    foundItem.protein_g, foundItem.carbohydrates_total_g, foundItem.fat_total_g));
                                            btnAddFood.setVisibility(View.VISIBLE);
                                            return;
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                            Snackbar.make(requireView(), "gemini could not analyze image", Snackbar.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(Call<GeminiApi.GeminiResponse> call, Throwable t) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Snackbar.make(requireView(), "api error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Snackbar.make(requireView(), "error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void searchFood() {
        String query = etFoodQuery.getText() != null ? etFoodQuery.getText().toString().trim() : "";
        if (TextUtils.isEmpty(query)) {
            etFoodQuery.setError("enter a food name");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);
        tvResult.setText("searching...");
        btnAddFood.setVisibility(View.GONE);
        ivMealPreview.setVisibility(View.GONE);
        foundItem = null;

        ApiClient.getApi().getNutrition(ApiClient.API_KEY, query)
                .enqueue(new Callback<CaloriesNinjaApi.NutritionResponse>() {
                    @Override
                    public void onResponse(Call<CaloriesNinjaApi.NutritionResponse> call,
                                           Response<CaloriesNinjaApi.NutritionResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSearch.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            List<CaloriesNinjaApi.NutritionItem> items = response.body().items;
                            if (items != null && !items.isEmpty()) {
                                foundItem = items.get(0);
                                tvResult.setText("found: " + capitalize(foundItem.name));
                                tvCaloriesFound.setText((int) foundItem.calories + " kcal");
                                tvMacrosFound.setText(String.format(
                                        "protein: %.1fg   carbs: %.1fg   fat: %.1fg",
                                        foundItem.protein_g,
                                        foundItem.carbohydrates_total_g,
                                        foundItem.fat_total_g));
                                btnAddFood.setVisibility(View.VISIBLE);
                            } else {
                                tvResult.setText("no results. try different name.");
                                tvCaloriesFound.setText("");
                                tvMacrosFound.setText("");
                            }
                        } else {
                            showDemoResult(query);
                        }
                    }

                    @Override
                    public void onFailure(Call<CaloriesNinjaApi.NutritionResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSearch.setEnabled(true);
                        showDemoResult(query);
                    }
                });
    }

    private void showDemoResult(String query) {
        foundItem = new CaloriesNinjaApi.NutritionItem();
        foundItem.name = query;
        foundItem.calories = 250;
        foundItem.protein_g = 15;
        foundItem.carbohydrates_total_g = 30;
        foundItem.fat_total_g = 8;

        tvResult.setText("demo: " + capitalize(query) + " (estimate)");
        tvCaloriesFound.setText("~250 kcal");
        tvMacrosFound.setText("protein: 15g   carbs: 30g   fat: 8g");
        btnAddFood.setVisibility(View.VISIBLE);
    }

    private void addFoodToLog() {
        if (foundItem == null) return;

        String mealType = spinnerMealType.getSelectedItem().toString();
        FoodEntry entry = new FoodEntry(
                capitalize(foundItem.name),
                (int) foundItem.calories,
                (float) foundItem.protein_g,
                (float) foundItem.carbohydrates_total_g,
                (float) foundItem.fat_total_g,
                mealType
        );

        HomeFragment.foodEntries.add(entry);

        Snackbar.make(requireView(),
                capitalize(foundItem.name) + " added to " + mealType,
                Snackbar.LENGTH_SHORT).show();

        etFoodQuery.setText("");
        tvResult.setText("search or scan a food above");
        tvCaloriesFound.setText("");
        tvMacrosFound.setText("");
        btnAddFood.setVisibility(View.GONE);
        ivMealPreview.setVisibility(View.GONE);
        foundItem = null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
