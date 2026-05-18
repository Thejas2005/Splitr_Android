package com.splitr.app.cache;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_expenses")
public class PendingExpense {
    @PrimaryKey(autoGenerate = true)
    public int localId;
    public double amount;
    public String description;
    public Double latitude;
    public Double longitude;
    public int userId;
    public Integer groupId;
    public String splitType;
    public String splitsJson;
    public String membersJson;
    public String status;
    public int retryCount;
    public String errorMessage;
    public long createdAt;

    public boolean isGroupExpense() {
        return membersJson != null && !membersJson.isEmpty();
    }
}