package com.splitr.app.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PendingExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PendingExpense expense);

    @Update
    void update(PendingExpense expense);

    @Query("SELECT * FROM pending_expenses WHERE status = 'pending' ORDER BY createdAt ASC")
    List<PendingExpense> getAllPending();

    @Query("SELECT COUNT(*) FROM pending_expenses WHERE status = 'pending'")
    int getPendingCount();

    @Query("DELETE FROM pending_expenses WHERE localId = :localId")
    void deleteById(int localId);
}