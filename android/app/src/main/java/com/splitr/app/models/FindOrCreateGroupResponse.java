package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class FindOrCreateGroupResponse {
    @SerializedName("group_id") public int groupId;
    @SerializedName("name")     public String name;
    @SerializedName("created")  public boolean created;
    @SerializedName("message")  public String message;
}
