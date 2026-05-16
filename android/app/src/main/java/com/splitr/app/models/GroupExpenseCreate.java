package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GroupExpenseCreate {
    @SerializedName("group_id")    public int groupId;
    @SerializedName("amount")      public double amount;
    @SerializedName("description") public String description;
    @SerializedName("latitude")    public Double latitude;
    @SerializedName("longitude")   public Double longitude;
    @SerializedName("user_id")     public int userId;
    @SerializedName("split_type")  public String splitType; // equal | percentage | dutch
    @SerializedName("splits")      public List<MemberSplit> splits;

    public GroupExpenseCreate(int groupId, double amount, String description,
                               Double lat, Double lng, int userId,
                               String splitType, List<MemberSplit> splits) {
        this.groupId = groupId;
        this.amount = amount;
        this.description = description;
        this.latitude = lat;
        this.longitude = lng;
        this.userId = userId;
        this.splitType = splitType;
        this.splits = splits;
    }
}
