package com.splitr.app.cache;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_balances")
public class CachedBalance {
    @PrimaryKey(autoGenerate = true)
    public int localId;
    public int splitId;
    public String toUsername;
    public Integer toUserId;
    public String fromUsername;
    public Integer fromUserId;
    public boolean isGuest;
    public double amount;
    public String description;
    public Integer groupId;
    public boolean isIOwe;
    public long cachedAt;
}