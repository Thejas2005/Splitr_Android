package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class GroupCreate {
    @SerializedName("name")    public String name;
    @SerializedName("user_id") public int userId;

    public GroupCreate(String name, int userId) {
        this.name = name;
        this.userId = userId;
    }
}
