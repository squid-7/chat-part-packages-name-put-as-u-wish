package com.yourpackagename.models;

/**
 * Model for each chat message, for Firebase.
 * All variable names must match Firebase keys.
 */
public class ModelChat {
    private String messageId;
    private String messageType;
    private String message;
    private String fromUid;
    private String toUid;
    private long timestamp;

    // Required empty constructor for Firebase
    public ModelChat() {}

    // Full constructor
    public ModelChat(String messageId, String messageType, String message, String fromUid, String toUid, long timestamp) {
        this.messageId = messageId;
        this.messageType = messageType;
        this.message = message;
        this.fromUid = fromUid;
        this.toUid = toUid;
        this.timestamp = timestamp;
    }
    // Getters
    public String getMessageId() { return messageId; }
    public String getMessageType() { return messageType; }
    public String getMessage() { return message; }
    public String getFromUid() { return fromUid; }
    public String getToUid() { return toUid; }
    public long getTimestamp() { return timestamp; }
    // Setters
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public void setMessage(String message) { this.message = message; }
    public void setFromUid(String fromUid) { this.fromUid = fromUid; }
    public void setToUid(String toUid) { this.toUid = toUid; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
