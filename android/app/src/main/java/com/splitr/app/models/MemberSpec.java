package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class MemberSpec {
    @SerializedName("user_id")    public Integer userId;
    @SerializedName("guest_name") public String guestName;

    public MemberSpec(Integer userId, String guestName) {
        this.userId = userId;
        this.guestName = guestName;
    }
}
