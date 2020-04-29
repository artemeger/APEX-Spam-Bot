package com.apex.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TGUser {

    public TGUser(){}

    public TGUser(final int userId, final int count, final boolean trusted) {
        this.userId = userId;
        this.count = count;
        this.trusted = trusted;
    }

    @Id
    private int userId;

    private int count;

    private boolean trusted;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

}
