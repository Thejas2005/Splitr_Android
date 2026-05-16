package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class GroupMember {
    @SerializedName("member_id") public int memberId;
    @SerializedName("user_id")   public Integer userId;
    @SerializedName("group_id")  public int groupId;
    @SerializedName("username")  public String username;
    @SerializedName("is_guest")  public boolean isGuest;
}
