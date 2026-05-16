package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("user_id")  public int userId;
    @SerializedName("username") public String username;
    @SerializedName("message")  public String message;
    @SerializedName("detail")   public String detail;
}
