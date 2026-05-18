package com.splitr.app.cache;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class ExpenseDao_Impl implements ExpenseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CachedExpense> __insertionAdapterOfCachedExpense;

  private final SharedSQLiteStatement __preparedStmtOfClearForUser;

  public ExpenseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCachedExpense = new EntityInsertionAdapter<CachedExpense>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_expenses` (`id`,`amount`,`description`,`latitude`,`longitude`,`userId`,`groupId`,`splitType`,`username`,`cachedAt`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final CachedExpense entity) {
        statement.bindLong(1, entity.id);
        statement.bindDouble(2, entity.amount);
        if (entity.description == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.description);
        }
        if (entity.latitude == null) {
          statement.bindNull(4);
        } else {
          statement.bindDouble(4, entity.latitude);
        }
        if (entity.longitude == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.longitude);
        }
        statement.bindLong(6, entity.userId);
        if (entity.groupId == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.groupId);
        }
        if (entity.splitType == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.splitType);
        }
        if (entity.username == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.username);
        }
        statement.bindLong(10, entity.cachedAt);
      }
    };
    this.__preparedStmtOfClearForUser = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_expenses WHERE userId = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(final List<CachedExpense> expenses) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfCachedExpense.insert(expenses);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void clearForUser(final int userId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfClearForUser.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, userId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfClearForUser.release(_stmt);
    }
  }

  @Override
  public List<CachedExpense> getExpensesForUser(final int userId) {
    final String _sql = "SELECT * FROM cached_expenses WHERE userId = ? ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
      final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
      final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
      final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
      final int _cursorIndexOfSplitType = CursorUtil.getColumnIndexOrThrow(_cursor, "splitType");
      final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
      final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
      final List<CachedExpense> _result = new ArrayList<CachedExpense>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final CachedExpense _item;
        _item = new CachedExpense();
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _item.amount = _cursor.getDouble(_cursorIndexOfAmount);
        if (_cursor.isNull(_cursorIndexOfDescription)) {
          _item.description = null;
        } else {
          _item.description = _cursor.getString(_cursorIndexOfDescription);
        }
        if (_cursor.isNull(_cursorIndexOfLatitude)) {
          _item.latitude = null;
        } else {
          _item.latitude = _cursor.getDouble(_cursorIndexOfLatitude);
        }
        if (_cursor.isNull(_cursorIndexOfLongitude)) {
          _item.longitude = null;
        } else {
          _item.longitude = _cursor.getDouble(_cursorIndexOfLongitude);
        }
        _item.userId = _cursor.getInt(_cursorIndexOfUserId);
        if (_cursor.isNull(_cursorIndexOfGroupId)) {
          _item.groupId = null;
        } else {
          _item.groupId = _cursor.getInt(_cursorIndexOfGroupId);
        }
        if (_cursor.isNull(_cursorIndexOfSplitType)) {
          _item.splitType = null;
        } else {
          _item.splitType = _cursor.getString(_cursorIndexOfSplitType);
        }
        if (_cursor.isNull(_cursorIndexOfUsername)) {
          _item.username = null;
        } else {
          _item.username = _cursor.getString(_cursorIndexOfUsername);
        }
        _item.cachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
