package com.apex.entities;

import javax.persistence.*;
import java.util.UUID;

@Entity
public class Feedback {

    public Feedback() {
        this.feedbackId = UUID.randomUUID().toString();
    }

    public Feedback(final String uuid, final int userId, final long chatId, final String data) {
        this.feedbackId = uuid;
        this.userId = userId;
        this.chatId = chatId;
        this.data = data;
    }

    @Id
    private String feedbackId;

    private int userId;

    private long chatId;

    private String data;

    public String getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
