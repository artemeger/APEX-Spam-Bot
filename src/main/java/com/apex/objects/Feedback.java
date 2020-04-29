package com.apex.objects;

import java.io.Serializable;

public class Feedback implements Serializable {

    public static final long serialVersionUID = 4453534524667546L;

    public Feedback(String dataToBan, int userId, long chatId, long messageId){
        this.dataToBan = dataToBan;
        this.userId = userId;
        this.chatId = chatId;
        this.messageId = messageId;
    }

    private String dataToBan;

    private int userId;

    private long chatId;

    private long messageId;

    public String getDataToBan() {
        return dataToBan;
    }

    public void setDataToBan(String dataToBan) {
        this.dataToBan = dataToBan;
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

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

}
