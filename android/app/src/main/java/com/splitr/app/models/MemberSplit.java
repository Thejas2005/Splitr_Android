package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class MemberSplit {
    @SerializedName("user_id")    public Integer userId;
    @SerializedName("guest_name") public String guestName;
    @SerializedName("amount")     public double amount; // for dutch: owed; for pct: %

    public MemberSplit(Integer userId, String guestName, double amount) {
        this.userId = userId;
        this.guestName = guestName;
        this.amount = amount;
    }
}
