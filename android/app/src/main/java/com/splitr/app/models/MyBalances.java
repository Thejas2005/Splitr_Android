package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MyBalances {
    @SerializedName("i_owe")           public List<SplitItem> iOwe;
    @SerializedName("owed_to_me")      public List<SplitItem> owedToMe;
    @SerializedName("total_i_owe")     public double totalIOwe;
    @SerializedName("total_owed_to_me")public double totalOwedToMe;
    @SerializedName("net")             public double net;
}
