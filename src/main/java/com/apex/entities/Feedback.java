package com.apex.entities;

import javax.persistence.*;

@Entity
public class Feedback {

    public Feedback() {

    }

    public Feedback(final int userId, final long chatId, final String data) {
        this.userId = userId;
        this.chatId = chatId;
        this.data = data;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long feedbackId;

    private int userId;

    private long chatId;

    private String data;

    public long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(long feedbackId) {
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
