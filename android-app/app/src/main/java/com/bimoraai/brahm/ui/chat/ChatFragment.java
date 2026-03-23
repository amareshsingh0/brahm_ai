package com.bimoraai.brahm.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bimoraai.brahm.api.SseManager;
import com.bimoraai.brahm.databinding.FragmentChatBinding;
import com.bimoraai.brahm.model.ChatMessage;
import com.bimoraai.brahm.ui.main.MainActivity;
import com.bimoraai.brahm.utils.ChatAdapter;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Chat screen — streams responses from the Brahm AI backend via SSE.
 */
public class ChatFragment extends Fragment {

    private FragmentChatBinding b;
    private ChatAdapter adapter;
    private SseManager sseManager;
    private StringBuilder currentAiContent = new StringBuilder();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentChatBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sseManager = new SseManager(requireContext());
        setupRecyclerView();
        setupSendButton();
        setupHeader();
        updateEmptyState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sseManager != null) sseManager.close();
        b = null;
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupHeader() {
        if (b.btnOpenDrawer != null) {
            b.btnOpenDrawer.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openDrawer();
                }
            });
        }

        if (b.btnClearChat != null) {
            b.btnClearChat.setOnClickListener(v -> {
                adapter.clearMessages();
                updateEmptyState();
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter();

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);

        b.rvMessages.setLayoutManager(lm);
        b.rvMessages.setAdapter(adapter);

        b.rvMessages.addOnLayoutChangeListener(
            (v, l, t, r, bottom, ol, ot, or, ob) -> {
                if (bottom < ob) {
                    b.rvMessages.postDelayed(() -> {
                        if (adapter.getItemCount() > 0) {
                            b.rvMessages.smoothScrollToPosition(
                                adapter.getItemCount() - 1);
                        }
                    }, 100);
                }
            });
    }

    private void setupSendButton() {
        b.btnSend.setOnClickListener(v -> sendMessage());

        b.etMessage.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void updateEmptyState() {
        if (b == null) return;
        boolean hasMessages = adapter.getItemCount() > 0;
        b.emptyState.setVisibility(hasMessages ? View.GONE : View.VISIBLE);
        b.rvMessages.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
    }

    // ── Chat logic ────────────────────────────────────────────────────────────

    private void sendMessage() {
        if (b == null) return;

        String text = b.etMessage.getText() != null
                      ? b.etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        // 1. Add user message & hide empty state
        adapter.addMessage(new ChatMessage(text, true));
        b.etMessage.setText("");
        updateEmptyState();
        scrollToBottom();

        // 2. Show typing indicator
        setTypingIndicator(true);

        // 3. Build birth data JSON from prefs
        PrefsHelper prefs = new PrefsHelper(requireContext());
        JsonObject birthData = new JsonObject();
        if (!prefs.getBirthDate().isEmpty()) {
            birthData.addProperty("birth_date",  prefs.getBirthDate());
            birthData.addProperty("birth_time",  prefs.getBirthTime());
            birthData.addProperty("birth_place", prefs.getBirthPlace());
            birthData.addProperty("lat",         prefs.getLat());
            birthData.addProperty("lon",         prefs.getLon());
            birthData.addProperty("tz",          prefs.getTz());
            birthData.addProperty("name",        prefs.getName());
        }

        // 4. Build history from current adapter messages
        List<ChatMessage> messages = adapter.getMessages();
        List<JsonObject>  history  = new ArrayList<>();
        int start = Math.max(0, messages.size() - 11);
        for (int i = start; i < messages.size() - 1; i++) {
            ChatMessage msg = messages.get(i);
            JsonObject turn = new JsonObject();
            turn.addProperty("role",    msg.isUser() ? "user" : "assistant");
            turn.addProperty("content", msg.getContent());
            history.add(turn);
        }

        // 5. Reset buffer and add empty AI placeholder
        currentAiContent = new StringBuilder();
        adapter.addMessage(new ChatMessage("", false));
        scrollToBottom();

        // 6. Start SSE stream
        sseManager.close();
        sseManager.postChat(requireContext(), text, birthData, history,
            new SseManager.SseListener() {
                @Override public void onOpen() {}

                @Override
                public void onChunk(String chunk) {
                    if (b == null) return;
                    currentAiContent.append(chunk);
                    adapter.updateLastMessage(currentAiContent.toString());
                    scrollToBottom();
                }

                @Override
                public void onDone(List<String> sources, String confidence) {
                    if (b == null) return;
                    setTypingIndicator(false);
                    String finalText = stripConfidenceTag(currentAiContent.toString());
                    String finalConf = confidence.isEmpty()
                        ? parseConfidence(currentAiContent.toString())
                        : confidence;
                    adapter.replaceLastMessage(new ChatMessage(finalText, false, finalConf));
                    scrollToBottom();
                }

                @Override
                public void onError(String message) {
                    if (b == null) return;
                    setTypingIndicator(false);
                    adapter.replaceLastMessage(
                        new ChatMessage("Sorry, something went wrong: " + message, false, ""));
                    scrollToBottom();
                }
            });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String parseConfidence(String response) {
        if (response == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\\[CONFIDENCE:\\s*(HIGH|MEDIUM|LOW)\\]",
                     java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(response);
        return m.find() ? m.group(1).toUpperCase() : "";
    }

    private static String stripConfidenceTag(String response) {
        if (response == null) return "";
        return response
            .replaceAll("\\[CONFIDENCE:\\s*(HIGH|MEDIUM|LOW)\\]", "")
            .trim();
    }

    private void setTypingIndicator(boolean show) {
        if (b == null) return;
        if (b.tvTyping != null) b.tvTyping.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void scrollToBottom() {
        if (b != null && adapter.getItemCount() > 0) {
            b.rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }
}
