package com.example.forkit.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.forkit.BuildConfig;
import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.utils.CustomFoodHelper;
import com.example.forkit.utils.GeminiApi;
import com.example.forkit.utils.GeminiClient;
import com.example.forkit.utils.GeminiNutritionResult;
import com.example.forkit.utils.FoodStore;
import com.example.forkit.utils.PrefsHelper;
import com.example.forkit.utils.SupabaseApi;
import com.example.forkit.utils.SupabaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
//DO NOT TOUCH ANYTHING IDK HOW IT WORKS AND IT JUST DOES
public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";
    private MaterialButton btnCamera, btnGallery;
    private FrameLayout root, inputView, scanningOverlay, resultContainer;
    private Uri lastPhotoUri;
    private GeminiNutritionResult lastResult;
    private ScanResultState scanResultState;
    private boolean applyingScanFieldUpdate;
    private static final String NUTRITION_PROMPT =
            "Analyze this food image and estimate total nutrition for the full portion shown.\n"
                    + "Return ONLY a single JSON object with EXACT keys:\n"
                    + "{\"name\":\"meal name\",\"ingredients\":\"comma separated ingredients\",\"calories\":number,\"protein_g\":number,\"carbs_g\":number,\"fat_g\":number,\"portion_g\":number,\"confidence\":0-100}\n"
                    + "Rules: numbers must be plain numbers (no units), no markdown, no extra keys, no surrounding text.";

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                Log.d(TAG, "TakePicture result success=" + success + " uri=" + lastPhotoUri);
                if (Boolean.TRUE.equals(success) && lastPhotoUri != null && isAdded()) {
                    processImage(lastPhotoUri);
                } else if (isAdded() && getView() != null) {
                    Snackbar.make(getView(), "camera failed, try gallery", Snackbar.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                Log.d(TAG, "Gallery pick uri=" + uri);
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
        view.findViewById(R.id.btn_add_custom_food).setOnClickListener(v -> CustomFoodHelper.show(this));
        view.findViewById(R.id.iv_scan_placeholder).setOnClickListener(v -> showSampleScanResult());
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCamera(); //show camera operation
    }

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchCamera() {
        try {
            File photoFile = File.createTempFile("meal_", ".jpg", requireContext().getCacheDir());
            lastPhotoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            Log.d(TAG, "Launching camera with uri=" + lastPhotoUri + " file=" + photoFile.getAbsolutePath() + " exists=" + photoFile.exists());
            takePictureLauncher.launch(lastPhotoUri);
        } catch (Exception e) {
            Log.e(TAG, "launchCamera failed", e);
            Snackbar.make(requireView(), "cannot create photo file", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void processImage(Uri uri) {
        if (getContext() == null || getView() == null) return;
        Log.d(TAG, "processImage uri=" + uri);
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.isEmpty()) {
            Log.w(TAG, "Missing BuildConfig.GEMINI_API_KEY");
            Snackbar.make(getView(), "add GEMINI_API_KEY to local.properties", Snackbar.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, "GEMINI_API_KEY present len=" + BuildConfig.GEMINI_API_KEY.length()
                + " prefix=" + BuildConfig.GEMINI_API_KEY.substring(0, Math.min(4, BuildConfig.GEMINI_API_KEY.length())) + "***");
        inputView.setVisibility(View.GONE);
        resultContainer.setVisibility(View.GONE);
        scanningOverlay.setVisibility(View.VISIBLE);
        ImageView scanningPreview = scanningOverlay.findViewById(R.id.iv_scanning_icon);
        if (scanningPreview != null) {
            try {
                scanningPreview.clearColorFilter();
                scanningPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                scanningPreview.setImageDrawable(null);
                scanningPreview.setImageURI(uri);
            } catch (Exception ignored) {}
        }

        new Thread(() -> {
            try {
                InputStream is = getContext().getContentResolver().openInputStream(uri);
                if (is == null) {
                    Log.e(TAG, "openInputStream returned null for uri=" + uri);
                    showError("cannot read image");
                    return;
                }
                byte[] bytes = readAllBytes(is);
                Log.d(TAG, "Read image bytes=" + bytes.length);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp == null) {
                    Log.e(TAG, "Bitmap decode failed bytes=" + bytes.length + " uri=" + uri);
                    showError("cannot decode image");
                    return;
                }
                Log.d(TAG, "Bitmap decoded w=" + bmp.getWidth() + " h=" + bmp.getHeight() + " config=" + bmp.getConfig());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] jpegBytes = baos.toByteArray();
                Log.d(TAG, "Compressed jpeg bytes=" + jpegBytes.length);
                String base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
                Log.d(TAG, "Base64 length=" + (base64 != null ? base64.length() : -1));

                GeminiApi.InlineData imgData = new GeminiApi.InlineData();
                imgData.mimeType = "image/jpeg";
                imgData.data = base64;
                GeminiApi.Part[] parts = {
                        new GeminiApi.Part(imgData),
                        new GeminiApi.Part(NUTRITION_PROMPT)
                };
                GeminiApi.Content content = new GeminiApi.Content();
                content.parts = parts;
                GeminiApi.GeminiRequest request = new GeminiApi.GeminiRequest(
                        new GeminiApi.Content[]{content},
                        new GeminiApi.GenerationConfig()
                );
                Log.d(TAG, "Calling Gemini model with responseMimeType=" + request.generationConfig.responseMimeType);

                GeminiClient.getApi().generateContent(BuildConfig.GEMINI_API_KEY, request).enqueue(new Callback<GeminiApi.GeminiResponse>() {
                    @Override
                    public void onResponse(Call<GeminiApi.GeminiResponse> call, Response<GeminiApi.GeminiResponse> response) {
                        if (getActivity() == null) return;
                        final boolean ok = response.isSuccessful();
                        final String responseText = ok ? extractCandidateText(response.body()) : null;
                        final String errorText;
                        if (!ok) {
                            String body = null;
                            try {
                                if (response.errorBody() != null) body = response.errorBody().string();
                            } catch (Exception ignored) {}
                            errorText = "gemini http " + response.code() + (body != null && !body.isEmpty() ? (": " + body) : "");
                        } else {
                            errorText = null;
                        }
                        Log.d(TAG, "Gemini response ok=" + ok + " code=" + response.code());
                        if (!ok) {
                            Log.e(TAG, errorText != null ? errorText : "gemini error");
                        } else {
                            logLong(TAG, "Gemini raw text", responseText);
                        }
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            scanningOverlay.setVisibility(View.GONE);
                            if (!ok) {
                                inputView.setVisibility(View.VISIBLE);
                                if (getView() != null) Snackbar.make(getView(), errorText != null ? errorText : "gemini error", Snackbar.LENGTH_LONG).show();
                                return;
                            }

                            String json = extractJsonObject(responseText);
                            logLong(TAG, "Gemini extracted json", json);
                            if (json != null) {
                                try {
                                    GeminiNutritionResult result = new Gson().fromJson(json, GeminiNutritionResult.class);
                                    if (result != null && result.name != null) {
                                        Log.d(TAG, "Parsed result name=" + result.name + " kcal=" + result.calories
                                                + " p=" + result.proteinG + " c=" + result.carbsG + " f=" + result.fatG
                                                + " portion_g=" + result.portionG + " conf=" + result.confidence);
                                        lastResult = result;
                                        lastPhotoUri = uri;
                                        showScanResult(uri, result);
                                        return;
                                    }
                                } catch (Exception ignored) {}
                            }
                            Log.e(TAG, "Parse failed. raw=" + safeSnippet(responseText));
                            inputView.setVisibility(View.VISIBLE);
                            if (getView() != null) Snackbar.make(getView(), "gemini parse failed. raw: " + safeSnippet(responseText), Snackbar.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onFailure(Call<GeminiApi.GeminiResponse> call, Throwable t) {
                        if (getActivity() == null) return;
                        Log.e(TAG, "Gemini call failed: " + t.getMessage(), t);
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            scanningOverlay.setVisibility(View.GONE);
                            inputView.setVisibility(View.VISIBLE);
                            if (getView() != null) Snackbar.make(getView(), "api error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "processImage exception: " + e.getMessage(), e);
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
        resultContainer.removeAllViews();
        getLayoutInflater().inflate(R.layout.fragment_scan_result, resultContainer);
        View v = resultContainer.getChildAt(0);

        float basePortion = r.portionG > 0 ? r.portionG : 350f;
        scanResultState = new ScanResultState(basePortion, r.calories, r.proteinG, r.carbsG, r.fatG);
        applyingScanFieldUpdate = true;
        ((EditText) v.findViewById(R.id.et_meal_name)).setText(capitalize(r.name));
        ((EditText) v.findViewById(R.id.et_meal_ingredients)).setText(r.ingredients != null ? r.ingredients : "");
        ((EditText) v.findViewById(R.id.et_meal_kcal)).setText(String.valueOf(Math.round(r.calories)));
        ((EditText) v.findViewById(R.id.et_protein_g)).setText(formatMacro(r.proteinG));
        ((EditText) v.findViewById(R.id.et_carbs_g)).setText(formatMacro(r.carbsG));
        ((EditText) v.findViewById(R.id.et_fat_g)).setText(formatMacro(r.fatG));
        ((EditText) v.findViewById(R.id.et_portion_g)).setText(String.valueOf(Math.round(basePortion)));
        ((TextView) v.findViewById(R.id.tv_confidence)).setText((r.confidence > 0 ? r.confidence : 90) + "%");
        applyingScanFieldUpdate = false;

        ImageView ivMeal = v.findViewById(R.id.iv_meal_result);
        ivMeal.setImageDrawable(null);
        if (uri != null && !Uri.EMPTY.equals(uri)) {
            ivMeal.setImageURI(uri);
        } else {
            ivMeal.setImageResource(R.drawable.natural_food);
        }

        EditText etPortion = v.findViewById(R.id.et_portion_g);
        EditText etKcal = v.findViewById(R.id.et_meal_kcal);
        EditText etP = v.findViewById(R.id.et_protein_g);
        EditText etC = v.findViewById(R.id.et_carbs_g);
        EditText etF = v.findViewById(R.id.et_fat_g);

        TextWatcher portionWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (applyingScanFieldUpdate || scanResultState == null) return;
                if (!scanResultState.syncMacrosFromPortion) return;
                float grams = parseFloatSafe(s != null ? s.toString() : "", scanResultState.baselinePortionG);
                if (grams <= 0) return;
                float mult = grams / scanResultState.baselinePortionG;
                applyingScanFieldUpdate = true;
                etKcal.setText(String.valueOf(Math.round(scanResultState.baselineCal * mult)));
                etP.setText(formatMacro(scanResultState.baselineP * mult));
                etC.setText(formatMacro(scanResultState.baselineC * mult));
                etF.setText(formatMacro(scanResultState.baselineF * mult));
                applyingScanFieldUpdate = false;
            }
        };
        TextWatcher macroEditWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (applyingScanFieldUpdate || scanResultState == null) return;
                scanResultState.syncMacrosFromPortion = false;
            }
        };
        etPortion.addTextChangedListener(portionWatcher);
        etKcal.addTextChangedListener(macroEditWatcher);
        etP.addTextChangedListener(macroEditWatcher);
        etC.addTextChangedListener(macroEditWatcher);
        etF.addTextChangedListener(macroEditWatcher);

        v.findViewById(R.id.btn_back).setOnClickListener(x -> {
            resultContainer.setVisibility(View.GONE);
            inputView.setVisibility(View.VISIBLE);
        });

        v.findViewById(R.id.btn_add_meal).setOnClickListener(x -> showMealTypePickerAndAdd(v, uri));

        resultContainer.setVisibility(View.VISIBLE);
    }

    private void showMealTypePickerAndAdd(View v, Uri uri) {
        if (getContext() == null) return;
        android.view.View dlgView = android.view.LayoutInflater.from(new android.view.ContextThemeWrapper(getContext(), R.style.AlertDialogLight)).inflate(R.layout.dialog_meal_type, null);
        AlertDialog d = new AlertDialog.Builder(getContext(), R.style.AlertDialogLight).setView(dlgView).create();
        String[] meals = {"Breakfast", "Lunch", "Dinner", "Tea", "Snack"};
        for (String meal : meals) {
            int id = meal.equals("Breakfast") ? R.id.option_breakfast : meal.equals("Lunch") ? R.id.option_lunch : meal.equals("Dinner") ? R.id.option_dinner : meal.equals("Tea") ? R.id.option_tea : R.id.option_snack;
            dlgView.findViewById(id).setOnClickListener(x -> {
                d.dismiss();
                addMealFromResult(v, uri, meal);
            });
        }
        d.show();
    }

    private void addMealFromResult(View v, Uri uri, String mealType) {
        EditText etName = v.findViewById(R.id.et_meal_name);
        EditText etIng = v.findViewById(R.id.et_meal_ingredients);

        String name = etName.getText().toString().trim();
        if (name.isEmpty()) name = "Meal";

        String ing = etIng.getText().toString().trim();

        int cal = Math.round(parseFloatSafe(textOf(v, R.id.et_meal_kcal), 0));
        float protein = parseFloatSafe(textOf(v, R.id.et_protein_g), 0);
        float carbs = parseFloatSafe(textOf(v, R.id.et_carbs_g), 0);
        float fat = parseFloatSafe(textOf(v, R.id.et_fat_g), 0);
        int portion = Math.round(parseFloatSafe(textOf(v, R.id.et_portion_g),
                scanResultState != null ? scanResultState.baselinePortionG : 0));
        if (portion < 0) {
            portion = 0;
        }

        FoodEntry entry = new FoodEntry(capitalize(name), cal, protein, carbs, fat, mealType, ing, portion);
        if (uri != null && !Uri.EMPTY.equals(uri)) {
            entry.setImagePath(uri.toString());
        }

        addEntryAndSync(entry);
        new PrefsHelper(requireContext()).onFoodLogged();

        resultContainer.setVisibility(View.GONE);
        inputView.setVisibility(View.VISIBLE);

        Snackbar.make(requireView(), capitalize(name) + " added", Snackbar.LENGTH_SHORT).show();
    }


    private static String textOf(View v, int id) {
        EditText et = v.findViewById(id);
        if (et == null) return "";
        return et.getText().toString().trim();
    }

    private static float parseFloatSafe(String s, float def) {
        if (s == null || s.isEmpty()) return def;
        try {
            return Float.parseFloat(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String formatMacro(float v) {
        if (Float.isNaN(v) || Float.isInfinite(v)) return "0";
        if (Math.abs(v - Math.round(v)) < 0.05f) return String.valueOf(Math.round(v));
        return String.format(Locale.US, "%.1f", v);
    }

    private static final class ScanResultState {
        final float baselinePortionG;
        final int baselineCal;
        final float baselineP;
        final float baselineC;
        final float baselineF;
        boolean syncMacrosFromPortion = true;

        ScanResultState(float baselinePortionG, int baselineCal, float baselineP, float baselineC, float baselineF) {
            this.baselinePortionG = baselinePortionG > 0 ? baselinePortionG : 350f;
            this.baselineCal = baselineCal;
            this.baselineP = baselineP;
            this.baselineC = baselineC;
            this.baselineF = baselineF;
        }
    }

    private void addEntryAndSync(FoodEntry entry) {
        FoodStore.add(entry);
        PrefsHelper prefs = new PrefsHelper(requireContext());
        String userId = prefs.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "insertFoodEntry skipped: no user_id in prefs (meal saved locally only)");
            if (getView() != null) {
                Snackbar.make(getView(), "Meal saved on device; sign in again to sync to your account", Snackbar.LENGTH_LONG).show();
            }
            return;
        }

        entry.setUserId(userId);
        entry.setUserAdded(true);
        String token = prefs.getAccessToken();
        if (token != null && !token.isEmpty()) {
            SupabaseClient.setAccessToken(token);
        }
        Log.d(TAG, "insertFoodEntry userId=" + userId + " food=" + entry.getFoodName());
        SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);
        api.insertFoodEntry(entry).enqueue(new Callback<java.util.List<FoodEntry>>() {
            @Override
            public void onResponse(Call<java.util.List<FoodEntry>> call, Response<java.util.List<FoodEntry>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && !response.body().isEmpty()) {
                        entry.setId(response.body().get(0).getId());
                        Log.d(TAG, "insertFoodEntry ok id=" + entry.getId());
                    } else {
                        Log.d(TAG, "insertFoodEntry ok HTTP " + response.code() + " (empty body — row may still be inserted)");
                    }
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).refreshFoodEntriesFromCloud();
                    }
                    return;
                }
                StringBuilder msg = new StringBuilder("insertFoodEntry failed HTTP ").append(response.code());
                try {
                    if (response.errorBody() != null) msg.append(" ").append(response.errorBody().string());
                } catch (Exception ignored) {}
                Log.e(TAG, msg.toString());
                if (getView() != null) {
                    Snackbar.make(getView(), "Could not save meal to cloud", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<FoodEntry>> call, Throwable t) {
                Log.e(TAG, "insertFoodEntry failed", t);
                if (getView() != null) {
                    Snackbar.make(getView(), "Could not save meal to cloud: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[16 * 1024];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } finally {
            try { is.close(); } catch (Exception ignored) {}
        }
    }

    private static String extractCandidateText(GeminiApi.GeminiResponse body) {
        if (body == null || body.candidates == null || body.candidates.length == 0) return null;
        GeminiApi.Candidate c = body.candidates[0];
        if (c == null || c.content == null || c.content.parts == null || c.content.parts.length == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (GeminiApi.Part2 p : c.content.parts) {
            if (p != null && p.text != null) sb.append(p.text);
        }
        return sb.toString();
    }

    private static String extractJsonObject(String text) {
        if (text == null) return null;
        String t = text.trim();
        t = t.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "").trim();
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return t.substring(start, end + 1);
    }

    private static String safeSnippet(String text) {
        if (text == null) return "(empty)";
        String t = text.replace('\n', ' ').replace('\r', ' ').trim();
        int max = 120;
        if (t.length() <= max) return t;
        return t.substring(0, max) + "...";
    }

    private static void logLong(String tag, String label, String text) {
        if (text == null) {
            Log.d(tag, label + ": (null)");
            return;
        }
        final int chunkSize = 3500; // keep under Logcat limit
        int len = text.length();
        Log.d(tag, label + " length=" + len);
        for (int i = 0; i < len; i += chunkSize) {
            int end = Math.min(len, i + chunkSize);
            Log.d(tag, label + " [" + i + ":" + end + "] " + text.substring(i, end));
        }
    }
}
