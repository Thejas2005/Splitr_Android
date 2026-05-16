package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class LocationExpense {
    @SerializedName("lat")         public double lat;
    @SerializedName("lng")         public double lng;
    @SerializedName("description") public String description;
    @SerializedName("amount")      public double amount;
    @SerializedName("my_amount")   public double myAmount;
    @SerializedName("user_id")     public int userId;
    @SerializedName("group_id")    public Integer groupId;
    @SerializedName("is_mine")     public boolean isMine;
    @SerializedName("split_type")  public String splitType;
}
