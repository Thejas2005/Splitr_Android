package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class GroupExpenseResponse {
    @SerializedName("message")          public String message;
    @SerializedName("split_type")       public String splitType;
    @SerializedName("split_per_person") public Double splitPerPerson;
    @SerializedName("expense_id")       public int expenseId;
    @SerializedName("detail")           public String detail;
}
