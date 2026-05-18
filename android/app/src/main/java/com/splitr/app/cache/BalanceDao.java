package com.splitr.app.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface BalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedBalance> balances);

    @Query("SELECT * FROM cached_balances WHERE isIOwe = 1")
    List<CachedBalance> getIOwe();

    @Query("SELECT * FROM cached_balances WHERE isIOwe = 0")
    List<CachedBalance> getOwedToMe();

    @Query("DELETE FROM cached_balances")
    void clearAll();
}