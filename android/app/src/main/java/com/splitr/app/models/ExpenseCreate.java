package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class ExpenseCreate {
    @SerializedName("amount")      public double amount;
    @SerializedName("description") public String description;
    @SerializedName("latitude")    public Double latitude;
    @SerializedName("longitude")   public Double longitude;
    @SerializedName("user_id")     public int userId;

    public ExpenseCreate(double amount, String description, Double lat, Double lng, int userId) {
        this.amount = amount;
        this.description = description;
        this.latitude = lat;
        this.longitude = lng;
        this.userId = userId;
    }
}
