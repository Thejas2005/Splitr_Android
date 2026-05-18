package com.splitr.app.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedGroup> groups);

    @Query("SELECT * FROM cached_groups ORDER BY id DESC")
    List<CachedGroup> getAllGroups();

    @Query("DELETE FROM cached_groups")
    void clearAll();
}