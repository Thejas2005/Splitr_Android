package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class AddMemberRequest {
    @SerializedName("group_id")   public int groupId;
    @SerializedName("user_id")    public Integer userId;
    @SerializedName("guest_name") public String guestName;

    public AddMemberRequest(int groupId, Integer userId, String guestName) {
        this.groupId = groupId;
        this.userId = userId;
        this.guestName = guestName;
    }
}
