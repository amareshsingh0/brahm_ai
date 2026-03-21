package com.bimoraai.brahm.model;

/**
 * A single message in the AI chat conversation.
 *
 * confidence is only meaningful for AI (isUser=false) messages.
 * Valid values: "HIGH", "MEDIUM", "LOW", or "" (not provided / user message).
 * The ChatAdapter uses this to render the colored confidence badge.
 */
public class ChatMessage {

    private final String content;
    private final boolean isUser;
    private final String confidence;

    /**
     * Convenience constructor for user messages (no confidence badge).
     */
    public ChatMessage(String content, boolean isUser) {
        this(content, isUser, "");
    }

    /**
     * Full constructor for AI messages that carry a confidence rating.
     *
     * @param content    Display text of the message (confidence tag already stripped).
     * @param isUser     true = sent by user, false = sent by AI.
     * @param confidence "HIGH", "MEDIUM", "LOW", or "" for none.
     */
    public ChatMessage(String content, boolean isUser, String confidence) {
        this.content    = content;
        this.isUser     = isUser;
        this.confidence = confidence != null ? confidence : "";
    }

    public String  getContent()    { return content; }
    public boolean isUser()        { return isUser; }
    public String  getConfidence() { return confidence; }

    /** Returns true if the confidence badge should be shown. */
    public boolean hasConfidence() { return !confidence.isEmpty(); }
}
