package com.splitr.app.cache;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_groups")
public class CachedGroup {
    @PrimaryKey
    public int id;
    public String name;
    public long cachedAt;
}