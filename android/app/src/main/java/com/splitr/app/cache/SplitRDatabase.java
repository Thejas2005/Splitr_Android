package com.splitr.app.cache;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                CachedExpense.class,
                CachedGroup.class,
                CachedBalance.class,
                PendingExpense.class
        },
        version = 1,
        exportSchema = false
)
public abstract class SplitRDatabase extends RoomDatabase {

    public abstract ExpenseDao expenseDao();
    public abstract GroupDao groupDao();
    public abstract BalanceDao balanceDao();
    public abstract PendingExpenseDao pendingExpenseDao();

    private static volatile SplitRDatabase INSTANCE;

    public static SplitRDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (SplitRDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SplitRDatabase.class,
                            "splitr_cache"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}