package com.bimoraai.brahm.api;

import android.content.Context;
import com.bimoraai.brahm.BuildConfig;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Singleton Retrofit client for the Brahm AI backend.
 *
 * Usage:
 *   ApiClient.getApiService(context).getPanchang(...).enqueue(cb);
 *
 * For SSE (AI chat) use {@link #getOkHttpClient(Context)} directly with
 * {@link SseManager}.
 */
public class ApiClient {

    /** Backend base URL — must end with a trailing slash for Retrofit. */
    public static final String BASE_URL = "https://brahmasmi.bimoraai.com/";

    private static final int TIMEOUT_SECONDS = 30;

    private static volatile ApiClient instance;

    private final Retrofit      retrofit;
    private final OkHttpClient  okHttpClient;
    private final ApiService    apiService;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private ApiClient(Context context) {
        okHttpClient = buildOkHttpClient(context);

        Gson gson = new GsonBuilder()
            .setLenient()
            .create();

        retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

        apiService = retrofit.create(ApiService.class);
    }

    /** Returns the singleton instance, creating it if necessary. */
    public static ApiClient getInstance(Context context) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = new ApiClient(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ── Convenience accessors ─────────────────────────────────────────────────

    /** Returns the Retrofit-generated {@link ApiService} implementation. */
    public static ApiService getApiService(Context context) {
        return getInstance(context).apiService;
    }

    /**
     * Returns the shared {@link OkHttpClient}.
     * Use this for manual OkHttp calls (SSE, multipart uploads).
     */
    public static OkHttpClient getOkHttpClient(Context context) {
        return getInstance(context).okHttpClient;
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private static OkHttpClient buildOkHttpClient(Context context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Auth interceptor: reads JWT from PrefsHelper and attaches the Bearer header.
        // A fresh PrefsHelper is created each call so token rotation is automatic.
        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                String token = new PrefsHelper(context).getToken();
                Request original = chain.request();
                if (token.isEmpty()) {
                    return chain.proceed(original);
                }
                Request authenticated = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
                return chain.proceed(authenticated);
            }
        });

        // Logging interceptor: BODY level in debug builds, NONE in release.
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG
            ? HttpLoggingInterceptor.Level.BODY
            : HttpLoggingInterceptor.Level.NONE);
        builder.addInterceptor(logging);

        return builder.build();
    }

    // ── Error helper ──────────────────────────────────────────────────────────

    /**
     * Parses an error message from a Retrofit error response body.
     * Tries to parse as JSON and extract the "detail" field; falls back
     * to the raw body string; then to the provided fallback.
     *
     * @param errorBody  {@code response.errorBody()} — may be null.
     * @param fallback   String to return when parsing fails completely.
     * @return Human-readable error message.
     */
    public static String parseError(okhttp3.ResponseBody errorBody, String fallback) {
        if (errorBody == null) return fallback;
        try {
            String raw = errorBody.string();
            com.google.gson.JsonObject obj = new com.google.gson.Gson()
                .fromJson(raw, com.google.gson.JsonObject.class);
            if (obj != null && obj.has("detail")) {
                com.google.gson.JsonElement detail = obj.get("detail");
                if (detail.isJsonPrimitive()) return detail.getAsString();
                // FastAPI validation errors return detail as an array
                return detail.toString();
            }
            return raw.isEmpty() ? fallback : raw;
        } catch (Exception e) {
            return fallback;
        }
    }
}
