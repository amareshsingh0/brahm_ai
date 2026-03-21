package com.bimoraai.brahm.ui.secondary;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityPalmistryBinding;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Palmistry (Hand Reading) screen using Gemini Vision.
 *
 * Flow:
 *   1. Camera / Gallery → select palm photo.
 *   2. Analyze button → read bytes on background thread → base64 encode →
 *      POST /api/palmistry via OkHttp → parse result → display.
 */
public class PalmistryActivity extends AppCompatActivity {

    private static final String PALMISTRY_PATH = "api/palmistry";
    private static final MediaType JSON_TYPE    = MediaType.parse("application/json; charset=utf-8");

    private ActivityPalmistryBinding b;
    private PrefsHelper prefs;
    private Uri currentPhotoUri;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ── Activity result launchers ─────────────────────────────────────────────

    private final ActivityResultLauncher<Uri> cameraLauncher =
        registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && currentPhotoUri != null) showImage(currentPhotoUri);
        });

    private final ActivityResultLauncher<String> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) { currentPhotoUri = uri; showImage(uri); }
        });

    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) openCamera();
            else Toast.makeText(this, "Camera permission required.", Toast.LENGTH_SHORT).show();
        });

    // ── Activity lifecycle ────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPalmistryBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);
        setupToolbar();
        setupButtons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupToolbar() {
        if (b.btnBack != null) b.btnBack.setOnClickListener(v -> finish());
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupButtons() {
        b.btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        b.btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        b.btnAnalyzePalm.setEnabled(false);
        b.btnAnalyzePalm.setOnClickListener(v -> analyzePalm());
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            currentPhotoUri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", photoFile);
            cameraLauncher.launch(currentPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Cannot create image file: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir  = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile("PALM_" + timestamp + "_", ".jpg", storageDir);
    }

    private void showImage(Uri uri) {
        b.ivPalmImage.setImageURI(uri);
        b.ivPalmImage.setVisibility(View.VISIBLE);
        b.btnAnalyzePalm.setEnabled(true);
        if (b.cardResult != null) b.cardResult.setVisibility(View.GONE);
    }

    // ── Analysis ──────────────────────────────────────────────────────────────

    /**
     * Reads the selected image on a background thread, base64-encodes it,
     * then POSTs to /api/palmistry using OkHttp directly.
     */
    private void analyzePalm() {
        if (currentPhotoUri == null) {
            Toast.makeText(this, "Please select or capture a palm photo first",
                Toast.LENGTH_SHORT).show();
            return;
        }

        b.btnAnalyzePalm.setEnabled(false);
        b.btnAnalyzePalm.setText("Analysing palm…");
        if (b.progressBar != null) b.progressBar.setVisibility(View.VISIBLE);

        // Read and encode the image off the main thread
        executor.execute(() -> {
            try {
                byte[] imageBytes = readUri(currentPhotoUri);
                String base64     = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                JsonObject body = new JsonObject();
                body.addProperty("image_base64", base64);

                Request request = new Request.Builder()
                    .url(ApiClient.BASE_URL + PALMISTRY_PATH)
                    .post(RequestBody.create(body.toString(), JSON_TYPE))
                    .build();

                OkHttpClient client = ApiClient.getOkHttpClient(PalmistryActivity.this);
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onResponse(@NonNull Call call,
                                           @NonNull Response response) throws IOException {
                        runOnUiThread(() -> {
                            if (b == null) return;
                            b.btnAnalyzePalm.setEnabled(true);
                            b.btnAnalyzePalm.setText("Analyse Palm");
                            if (b.progressBar != null) b.progressBar.setVisibility(View.GONE);
                        });

                        ResponseBody rb = response.body();
                        if (response.isSuccessful() && rb != null) {
                            String raw = rb.string();
                            try {
                                JsonObject result = new com.google.gson.Gson()
                                    .fromJson(raw, JsonObject.class);
                                String analysis = result.has("analysis")
                                    ? result.get("analysis").getAsString()
                                    : result.has("interpretation")
                                        ? result.get("interpretation").getAsString()
                                        : raw;
                                runOnUiThread(() -> showResult(analysis));
                            } catch (Exception e) {
                                runOnUiThread(() -> showResult(raw));
                            }
                        } else {
                            String errBody = rb != null ? rb.string() : "Unknown error";
                            runOnUiThread(() ->
                                Toast.makeText(PalmistryActivity.this,
                                    "Analysis failed: " + errBody,
                                    Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            if (b == null) return;
                            b.btnAnalyzePalm.setEnabled(true);
                            b.btnAnalyzePalm.setText("Analyse Palm");
                            if (b.progressBar != null) b.progressBar.setVisibility(View.GONE);
                            Toast.makeText(PalmistryActivity.this,
                                "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    if (b == null) return;
                    b.btnAnalyzePalm.setEnabled(true);
                    b.btnAnalyzePalm.setText("Analyse Palm");
                    if (b.progressBar != null) b.progressBar.setVisibility(View.GONE);
                    Toast.makeText(PalmistryActivity.this,
                        "Could not read image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /** Reads all bytes from the given content URI. */
    private byte[] readUri(Uri uri) throws IOException {
        try (InputStream in  = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IOException("Cannot open URI: " + uri);
            byte[] buf = new byte[8192];
            int    n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private void showResult(String interpretation) {
        if (b.cardResult != null) {
            b.cardResult.setVisibility(View.VISIBLE);
            if (b.tvInterpretation != null) b.tvInterpretation.setText(interpretation);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
