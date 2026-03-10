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
import android.app.AlertDialog;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
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
import com.example.forkit.utils.GeminiApi;
import com.example.forkit.utils.GeminiClient;
import com.example.forkit.utils.GeminiNutritionResult;
import com.example.forkit.utils.PrefsHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraFragment extends Fragment {

    private MaterialButton btnCamera, btnGallery;
    private FrameLayout root, inputView, scanningOverlay, resultContainer;
    private Uri lastPhotoUri;
    private GeminiNutritionResult lastResult;
    private static final String NUTRITION_PROMPT = "analyze this food image. return only valid json: {\"name\":\"meal name\",\"ingredients\":\"comma separated ingredients\",\"calories\":number,\"protein_g\":number,\"carbs_g\":number,\"fat_g\":number,\"portion_g\":number,\"confidence\":0-100}. no markdown. estimate total nutrition.";

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (lastPhotoUri != null && isAdded()) {
                    processImage(lastPhotoUri);
                } else if (isAdded() && getView() != null) {
                    Snackbar.make(getView(), "camera failed, try gallery", Snackbar.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) processImage(uri);
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) launchCamera();
                else if (getView() != null) Snackbar.make(getView(), "camera permission needed", Snackbar.LENGTH_SHORT).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view.findViewById(R.id.camera_root);
        inputView = view.findViewById(R.id.camera_input_view);
        scanningOverlay = view.findViewById(R.id.scanning_overlay);
        resultContainer = view.findViewById(R.id.scan_result_container);
        btnCamera = view.findViewById(R.id.btn_camera);
        btnGallery = view.findViewById(R.id.btn_gallery);

        if (scanningOverlay.getChildCount() == 0) {
            getLayoutInflater().inflate(R.layout.overlay_scanning, scanningOverlay);
        }

        view.findViewById(R.id.btn_camera_menu).setOnClickListener(v -> {
            if (getActivity() instanceof com.example.forkit.MainActivity)
                ((com.example.forkit.MainActivity) getActivity()).openDrawer();
        });
        btnCamera.setOnClickListener(v -> openCamera());
        btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        view.findViewById(R.id.iv_scan_placeholder).setOnClickListener(v -> showSampleScanResult());
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCamera();
    }

    /** Shows the scan result screen with sample data (for preview/demo). */
    private void showSampleScanResult() {
        GeminiNutritionResult sample = new GeminiNutritionResult();
        sample.name = "Grilled chicken salad";
        sample.ingredients = "Chicken breast, mixed greens, cherry tomatoes, cucumber, olive oil";
        sample.calories = 380;
        sample.proteinG = 35;
        sample.carbsG = 12;
        sample.fatG = 22;
        sample.portionG = 350;
        sample.confidence = 92;
        Uri sampleUri = null;
        try {
            Drawable d = getResources().getDrawable(R.drawable.natural_food, null);
            if (d instanceof BitmapDrawable) {
                android.graphics.Bitmap bmp = ((BitmapDrawable) d).getBitmap();
                if (bmp != null) {
                    File f = File.createTempFile("sample_", ".jpg", requireContext().getCacheDir());
                    FileOutputStream fos = new FileOutputStream(f);
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();
                    sampleUri = FileProvider.getUriForFile(requireContext(),
                            requireContext().getPackageName() + ".fileprovider", f);
                }
            }
        } catch (Exception ignored) {}
        showScanResult(sampleUri != null ? sampleUri : Uri.EMPTY, sample);
    }

    private void launchCamera() {
        try {
            File photoFile = File.createTempFile("meal_", ".jpg", requireContext().getCacheDir());
            lastPhotoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(lastPhotoUri);
        } catch (Exception e) {
            Snackbar.make(requireView(), "cannot create photo file", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void processImage(Uri uri) {
        if (getContext() == null || getView() == null) return;
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.isEmpty()) {
            Snackbar.make(getView(), "add GEMINI_API_KEY to local.properties", Snackbar.LENGTH_LONG).show();
            return;
        }
        inputView.setVisibility(View.GONE);
        resultContainer.setVisibility(View.GONE);
        scanningOverlay.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                InputStream is = getContext().getContentResolver().openInputStream(uri);
                if (is == null) {
                    showError("cannot read image");
                    return;
                }
                byte[] bytes = new byte[is.available()];
                is.read(bytes);
                is.close();
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp == null) {
                    showError("cannot decode image");
                    return;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                GeminiApi.GeminiRequest.Part.InlineData imgData = new GeminiApi.GeminiRequest.Part.InlineData();
                imgData.mimeType = "image/jpeg";
                imgData.data = base64;
                GeminiApi.GeminiRequest.Part[] parts = {
                        new GeminiApi.GeminiRequest.Part(imgData),
                        new GeminiApi.GeminiRequest.Part(NUTRITION_PROMPT)
                };
                GeminiApi.GeminiRequest.Content content = new GeminiApi.GeminiRequest.Content();
                content.parts = parts;
                GeminiApi.GeminiRequest request = new GeminiApi.GeminiRequest(
                        new GeminiApi.GeminiRequest.Content[]{content},
                        new GeminiApi.GeminiRequest.GenerationConfig()
                );

                GeminiClient.getApi().generateContent(BuildConfig.GEMINI_API_KEY, request).enqueue(new Callback<GeminiApi.GeminiResponse>() {
                    @Override
                    public void onResponse(Call<GeminiApi.GeminiResponse> call, Response<GeminiApi.GeminiResponse> response) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            scanningOverlay.setVisibility(View.GONE);
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
                                            lastResult = result;
                                            lastPhotoUri = uri;
                                            showScanResult(uri, result);
                                            return;
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                            inputView.setVisibility(View.VISIBLE);
                            if (getView() != null) Snackbar.make(getView(), "gemini could not analyze image", Snackbar.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(Call<GeminiApi.GeminiResponse> call, Throwable t) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            scanningOverlay.setVisibility(View.GONE);
                            inputView.setVisibility(View.VISIBLE);
                            if (getView() != null) Snackbar.make(getView(), "api error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                showError(e.getMessage());
            }
        }).start();
    }

    private void showError(String msg) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (!isAdded()) return;
            scanningOverlay.setVisibility(View.GONE);
            inputView.setVisibility(View.VISIBLE);
            if (getView() != null) Snackbar.make(getView(), msg, Snackbar.LENGTH_SHORT).show();
        });
    }

    private void showScanResult(Uri uri, GeminiNutritionResult r) {
        if (resultContainer.getChildCount() == 0) {
            getLayoutInflater().inflate(R.layout.fragment_scan_result, resultContainer);
        }
        View v = resultContainer.getChildAt(0);
        ((TextView) v.findViewById(R.id.tv_meal_name)).setText(capitalize(r.name));
        ((TextView) v.findViewById(R.id.tv_meal_ingredients)).setText(r.ingredients != null ? r.ingredients : "");
        ((TextView) v.findViewById(R.id.tv_meal_kcal)).setText(r.calories + "kcal");
        ((TextView) v.findViewById(R.id.tv_meal_macros)).setText(String.format("Protein: %.0fg  Carbs: %.0fg  Fat: %.0fg", r.proteinG, r.carbsG, r.fatG));
        int portion = r.portionG > 0 ? r.portionG : 350;
        ((TextView) v.findViewById(R.id.tv_portion)).setText("Estimated portion: " + portion + "g");
        ((TextView) v.findViewById(R.id.tv_confidence)).setText((r.confidence > 0 ? r.confidence : 90) + "%");
        ImageView ivMeal = v.findViewById(R.id.iv_meal_result);
        if (uri != null && !Uri.EMPTY.equals(uri)) ivMeal.setImageURI(uri);
        else ivMeal.setImageResource(R.drawable.natural_food);

        int basePortion = r.portionG > 0 ? r.portionG : 350;
        TextView tvPortion = v.findViewById(R.id.tv_portion);
        TextView tvConfidence = v.findViewById(R.id.tv_confidence);
        int conf = r.confidence > 0 ? r.confidence : 90;

        SeekBar seek = v.findViewById(R.id.seek_portion);
        seek.setProgress(100);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float mult = Math.max(0.5f, progress / 100f);
                ((TextView) v.findViewById(R.id.tv_meal_kcal)).setText((int)(r.calories * mult) + "kcal");
                ((TextView) v.findViewById(R.id.tv_meal_macros)).setText(String.format("Protein: %.0fg  Carbs: %.0fg  Fat: %.0fg",
                        r.proteinG * mult, r.carbsG * mult, r.fatG * mult));
                tvPortion.setText("Estimated portion: " + Math.round(basePortion * mult) + "g");
                int scaledConf = Math.round(conf * mult);
                tvConfidence.setText(Math.min(99, Math.max(50, scaledConf)) + "%");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        v.findViewById(R.id.btn_back).setOnClickListener(x -> {
            resultContainer.setVisibility(View.GONE);
            inputView.setVisibility(View.VISIBLE);
        });

        v.findViewById(R.id.btn_add_meal).setOnClickListener(x -> showMealTypePickerAndAdd(v, seek, r, uri));
        v.findViewById(R.id.tv_confirm_portion).setOnClickListener(x -> showMealTypePickerAndAdd(v, seek, r, uri));

        resultContainer.setVisibility(View.VISIBLE);
    }

    private void showMealTypePickerAndAdd(View v, SeekBar seek, GeminiNutritionResult r, Uri uri) {
        if (getContext() == null) return;
        android.view.View dlgView = android.view.LayoutInflater.from(new android.view.ContextThemeWrapper(getContext(), R.style.AlertDialogLight)).inflate(R.layout.dialog_meal_type, null);
        AlertDialog d = new AlertDialog.Builder(getContext(), R.style.AlertDialogLight).setView(dlgView).create();
        String[] meals = {"Breakfast", "Lunch", "Dinner", "Tea", "Snack"};
        for (String meal : meals) {
            int id = meal.equals("Breakfast") ? R.id.option_breakfast : meal.equals("Lunch") ? R.id.option_lunch : meal.equals("Dinner") ? R.id.option_dinner : meal.equals("Tea") ? R.id.option_tea : R.id.option_snack;
            dlgView.findViewById(id).setOnClickListener(x -> {
                d.dismiss();
                addMealFromResult(v, seek, r, uri, meal);
            });
        }
        d.show();
    }

    private void addMealFromResult(View v, SeekBar seek, GeminiNutritionResult r, Uri uri, String mealType) {
        float mult = seek.getProgress() / 100f;
        int cal = (int)(r.calories * mult);
        float protein = r.proteinG * mult;
        float carbs = r.carbsG * mult;
        float fat = r.fatG * mult;
        int portion = (int)((r.portionG > 0 ? r.portionG : 350) * mult);
        String ing = r.ingredients != null ? r.ingredients : "";
        FoodEntry entry = new FoodEntry(capitalize(r.name), cal, protein, carbs, fat, mealType, ing, portion);
        if (uri != null && !Uri.EMPTY.equals(uri)) entry.setImagePath(uri.toString());
        HomeFragment.foodEntries.add(entry);
        new PrefsHelper(requireContext()).onFoodLogged();
        resultContainer.setVisibility(View.GONE);
        inputView.setVisibility(View.VISIBLE);
        Snackbar.make(requireView(), capitalize(r.name) + " added", Snackbar.LENGTH_SHORT).show();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
