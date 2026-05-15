package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class Group {
    @SerializedName("id")       public int id;
    @SerializedName("group_id") public int groupId;   // from find-or-create
    @SerializedName("name")     public String name;
    @SerializedName("created")  public boolean created;

    // Convenience getter handles both field names
    public int getId() { return groupId > 0 ? groupId : id; }
}
