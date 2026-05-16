package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class SplitItem {
    @SerializedName("split_id")     public int splitId;
    @SerializedName("to_username")  public String toUsername;
    @SerializedName("to_user_id")   public Integer toUserId;
    @SerializedName("from_username")public String fromUsername;
    @SerializedName("from_user_id") public Integer fromUserId;
    @SerializedName("is_guest")     public boolean isGuest;
    @SerializedName("amount")       public double amount;
    @SerializedName("description")  public String description;
    @SerializedName("group_id")     public Integer groupId;
}
