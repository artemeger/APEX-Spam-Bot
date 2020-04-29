package com.apex.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Blacklist {

    public Blacklist() {
    }

    public Blacklist(final String hash) {
        this.hash = hash;
    }

    @Id
    @GeneratedValue(generator="UUID")
    private String blackListId;

    private String hash;

    public String getBlackListId() {
        return blackListId;
    }

    public void setBlackListId(String blackListId) {
        this.blackListId = blackListId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

}
