package com.bimoraai.brahm.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages SSE (Server-Sent Events) streaming for the AI chat endpoint.
 *
 * Usage:
 * <pre>
 *   sseManager = new SseManager(context);
 *   sseManager.postChat(message, birthData, history, new SseManager.SseListener() {
 *       {@literal @}Override public void onChunk(String chunk) {
 *           // append chunk to current AI message
 *       }
 *       {@literal @}Override public void onDone(List<String> sources, String confidence) {
 *           // finalise message, hide typing indicator
 *       }
 *       {@literal @}Override public void onError(String message) {
 *           // show error
 *       }
 *       {@literal @}Override public void onOpen() {
 *           // optional: first byte received
 *       }
 *   });
 *
 *   // Cancel (e.g. in onDestroyView):
 *   sseManager.close();
 * </pre>
 *
 * All {@link SseListener} callbacks are delivered on the main thread.
 */
public class SseManager {

    // ── SSE listener interface ─────────────────────────────────────────────────

    public interface SseListener {
        /** Called for each streamed text chunk (type == "chunk"). */
        void onChunk(String chunk);

        /**
         * Called when the stream ends (type == "done").
         *
         * @param sources    List of source names cited by the AI (may be empty).
         * @param confidence "HIGH", "MEDIUM", "LOW", or "" if not present.
         */
        void onDone(List<String> sources, String confidence);

        /** Called when the backend sends type == "error" or on a transport failure. */
        void onError(String message);

        /** Called when the SSE connection is first established. */
        void onOpen();
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String CHAT_PATH    = "api/chat";
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    // SSE streams can run for a long time; use extended read timeout.
    private static final int SSE_READ_TIMEOUT_SECONDS = 120;

    // ── Fields ────────────────────────────────────────────────────────────────

    private final OkHttpClient sseClient;
    private final Handler      mainHandler = new Handler(Looper.getMainLooper());

    private EventSource activeSource; // non-null while a stream is open

    // ── Constructor ───────────────────────────────────────────────────────────

    public SseManager(Context context) {
        // Build a dedicated OkHttpClient with a longer read timeout for SSE.
        // Re-uses the auth interceptor from the shared client but overrides timeouts.
        sseClient = ApiClient.getOkHttpClient(context)
            .newBuilder()
            .readTimeout(SSE_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Opens an SSE connection to POST /api/chat and streams the AI response.
     *
     * @param context    Used for token retrieval via ApiClient.
     * @param message    User's question / message text.
     * @param birthData  JsonObject with birth_date, birth_time, lat, lon, tz, name.
     *                   Pass null / empty object if not available.
     * @param history    Previous chat turns as a list of JsonObjects, each with
     *                   "role" ("user"/"assistant") and "content" fields.
     * @param callback   Listener that receives streaming events on the main thread.
     */
    public void postChat(
            Context        context,
            String         message,
            JsonObject     birthData,
            List<JsonObject> history,
            SseListener    callback) {

        // Cancel any active stream before starting a new one.
        close();

        // Build JSON request body.
        JsonObject body = new JsonObject();
        body.addProperty("message", message);
        if (birthData != null && birthData.size() > 0) {
            body.add("birth_data", birthData);
        }
        if (history != null && !history.isEmpty()) {
            JsonArray historyArray = new JsonArray();
            for (JsonObject turn : history) {
                historyArray.add(turn);
            }
            body.add("history", historyArray);
        }
        body.addProperty("page_context", "mobile");

        Request request = new Request.Builder()
            .url(ApiClient.BASE_URL + CHAT_PATH)
            .post(RequestBody.create(body.toString(), JSON_TYPE))
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build();

        EventSourceListener listener = new EventSourceListener() {

            @Override
            public void onOpen(EventSource eventSource, Response response) {
                mainHandler.post(callback::onOpen);
            }

            @Override
            public void onEvent(EventSource eventSource,
                                String id,
                                String type,
                                String data) {
                if (data == null || data.isEmpty()) return;

                try {
                    JsonObject event = JsonParser.parseString(data).getAsJsonObject();
                    String eventType = event.has("type")
                        ? event.get("type").getAsString() : "";

                    switch (eventType) {
                        case "chunk": {
                            String content = event.has("content")
                                ? event.get("content").getAsString() : "";
                            if (!content.isEmpty()) {
                                mainHandler.post(() -> callback.onChunk(content));
                            }
                            break;
                        }

                        case "done": {
                            List<String> sources    = parseSources(event);
                            String       confidence = parseConfidence(event);
                            mainHandler.post(() -> callback.onDone(sources, confidence));
                            eventSource.cancel();
                            break;
                        }

                        case "error": {
                            String errMsg = event.has("message")
                                ? event.get("message").getAsString()
                                : "Unknown error from server";
                            mainHandler.post(() -> callback.onError(errMsg));
                            eventSource.cancel();
                            break;
                        }

                        default:
                            // Unknown event type — ignore
                            break;
                    }
                } catch (Exception e) {
                    // Malformed JSON event — treat as a non-fatal chunk if possible
                    if (!data.startsWith("{")) {
                        mainHandler.post(() -> callback.onChunk(data));
                    }
                }
            }

            @Override
            public void onClosed(EventSource eventSource) {
                // Stream closed without an explicit "done" event — still notify done.
                mainHandler.post(() -> callback.onDone(new ArrayList<>(), ""));
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                String msg = t != null ? t.getMessage() : "Connection failed";
                if (msg == null) msg = "Connection failed";
                final String errorMsg = msg;
                mainHandler.post(() -> callback.onError(errorMsg));
            }
        };

        activeSource = EventSources.createFactory(sseClient)
            .newEventSource(request, listener);
    }

    /**
     * Cancels the active SSE stream, if any.
     * Safe to call even when no stream is open.
     */
    public void close() {
        if (activeSource != null) {
            activeSource.cancel();
            activeSource = null;
        }
    }

    // ── JSON parsing helpers ──────────────────────────────────────────────────

    private static List<String> parseSources(JsonObject event) {
        List<String> sources = new ArrayList<>();
        if (!event.has("sources")) return sources;
        JsonElement el = event.get("sources");
        if (el.isJsonArray()) {
            for (JsonElement src : el.getAsJsonArray()) {
                sources.add(src.getAsString());
            }
        }
        return sources;
    }

    private static String parseConfidence(JsonObject event) {
        if (!event.has("confidence")) return "";
        JsonElement el = event.get("confidence");
        return el.isJsonPrimitive() ? el.getAsString().toUpperCase() : "";
    }
}
