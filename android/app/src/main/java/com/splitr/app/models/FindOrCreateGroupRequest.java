package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FindOrCreateGroupRequest {
    @SerializedName("payer_id")   public int payerId;
    @SerializedName("members")    public List<MemberSpec> members;
    @SerializedName("group_name") public String groupName;

    public FindOrCreateGroupRequest(int payerId, List<MemberSpec> members, String groupName) {
        this.payerId = payerId;
        this.members = members;
        this.groupName = groupName;
    }
}
