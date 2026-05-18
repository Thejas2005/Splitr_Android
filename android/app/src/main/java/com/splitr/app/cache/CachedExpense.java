package com.splitr.app.cache;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_expenses")
public class CachedExpense {
    @PrimaryKey
    public int id;
    public double amount;
    public String description;
    public Double latitude;
    public Double longitude;
    public int userId;
    public Integer groupId;
    public String splitType;
    public String username;
    public long cachedAt;
}