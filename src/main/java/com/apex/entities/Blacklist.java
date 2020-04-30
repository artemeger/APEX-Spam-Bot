package com.apex.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Blacklist {

    public Blacklist() {
    }

    public Blacklist(final String hash) {
        this.hash = hash;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long blackListId;

    private String hash;

    public long getBlackListId() {
        return blackListId;
    }

    public void setBlackListId(long blackListId) {
        this.blackListId = blackListId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

}
