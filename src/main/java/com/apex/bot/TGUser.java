package com.apex.bot;

import org.dizitart.no2.IndexType;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;

@Indices({
        @Index(value = "username", type = IndexType.NonUnique),
        @Index(value = "address", type = IndexType.NonUnique),
        @Index(value = "telegramId", type = IndexType.Unique),
        @Index(value = "nextRequest", type = IndexType.NonUnique),
        @Index(value = "paid", type = IndexType.NonUnique)
})
public class TGUser {

    @Id
    private NitriteId nitriteId;

    private String username;

    private String address;

    private int telegramId;

    private long nextRequest;

    private int paid;

    public TGUser(String username, String address, int telegramId, long nextRequest, int paid) {
        this.username = username;
        this.address = address;
        this.telegramId = telegramId;
        this.nextRequest = nextRequest;
        this.paid = paid;
    }

    public NitriteId getNitriteId() {
        return nitriteId;
    }

    public void setNitriteId(NitriteId nitriteId) {
        this.nitriteId = nitriteId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(int telegramId) {
        this.telegramId = telegramId;
    }

    public long getNextRequest() {
        return nextRequest;
    }

    public void setNextRequest(long nextRequest) {
        this.nextRequest = nextRequest;
    }

    public int getPaid() {
        return paid;
    }

    public void setPaid(int paid) {
        this.paid = paid;
    }

}
