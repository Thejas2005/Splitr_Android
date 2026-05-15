package com.splitr.app.models;

import com.google.gson.annotations.SerializedName;

public class GenericResponse {
    @SerializedName("message") public String message;
    @SerializedName("detail")  public String detail;
}
