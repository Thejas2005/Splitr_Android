package com.splitr.app.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedExpense> expenses);

    @Query("SELECT * FROM cached_expenses WHERE userId = :userId ORDER BY id DESC")
    List<CachedExpense> getExpensesForUser(int userId);

    @Query("DELETE FROM cached_expenses WHERE userId = :userId")
    void clearForUser(int userId);
}