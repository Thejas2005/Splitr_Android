package com.splitr.app.cache;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
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
public final class PendingExpenseDao_Impl implements PendingExpenseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PendingExpense> __insertionAdapterOfPendingExpense;

  private final EntityDeletionOrUpdateAdapter<PendingExpense> __updateAdapterOfPendingExpense;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public PendingExpenseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPendingExpense = new EntityInsertionAdapter<PendingExpense>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pending_expenses` (`localId`,`amount`,`description`,`latitude`,`longitude`,`userId`,`groupId`,`splitType`,`splitsJson`,`membersJson`,`status`,`retryCount`,`errorMessage`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final PendingExpense entity) {
        statement.bindLong(1, entity.localId);
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
        if (entity.splitsJson == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.splitsJson);
        }
        if (entity.membersJson == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.membersJson);
        }
        if (entity.status == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.status);
        }
        statement.bindLong(12, entity.retryCount);
        if (entity.errorMessage == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.errorMessage);
        }
        statement.bindLong(14, entity.createdAt);
      }
    };
    this.__updateAdapterOfPendingExpense = new EntityDeletionOrUpdateAdapter<PendingExpense>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `pending_expenses` SET `localId` = ?,`amount` = ?,`description` = ?,`latitude` = ?,`longitude` = ?,`userId` = ?,`groupId` = ?,`splitType` = ?,`splitsJson` = ?,`membersJson` = ?,`status` = ?,`retryCount` = ?,`errorMessage` = ?,`createdAt` = ? WHERE `localId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final PendingExpense entity) {
        statement.bindLong(1, entity.localId);
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
        if (entity.splitsJson == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.splitsJson);
        }
        if (entity.membersJson == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.membersJson);
        }
        if (entity.status == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.status);
        }
        statement.bindLong(12, entity.retryCount);
        if (entity.errorMessage == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.errorMessage);
        }
        statement.bindLong(14, entity.createdAt);
        statement.bindLong(15, entity.localId);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pending_expenses WHERE localId = ?";
        return _query;
      }
    };
  }

  @Override
  public long insert(final PendingExpense expense) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfPendingExpense.insertAndReturnId(expense);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final PendingExpense expense) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfPendingExpense.handle(expense);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteById(final int localId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, localId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteById.release(_stmt);
    }
  }

  @Override
  public List<PendingExpense> getAllPending() {
    final String _sql = "SELECT * FROM pending_expenses WHERE status = 'pending' ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfLocalId = CursorUtil.getColumnIndexOrThrow(_cursor, "localId");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
      final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
      final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
      final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
      final int _cursorIndexOfSplitType = CursorUtil.getColumnIndexOrThrow(_cursor, "splitType");
      final int _cursorIndexOfSplitsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "splitsJson");
      final int _cursorIndexOfMembersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "membersJson");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
      final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final List<PendingExpense> _result = new ArrayList<PendingExpense>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final PendingExpense _item;
        _item = new PendingExpense();
        _item.localId = _cursor.getInt(_cursorIndexOfLocalId);
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
        if (_cursor.isNull(_cursorIndexOfSplitsJson)) {
          _item.splitsJson = null;
        } else {
          _item.splitsJson = _cursor.getString(_cursorIndexOfSplitsJson);
        }
        if (_cursor.isNull(_cursorIndexOfMembersJson)) {
          _item.membersJson = null;
        } else {
          _item.membersJson = _cursor.getString(_cursorIndexOfMembersJson);
        }
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _item.status = null;
        } else {
          _item.status = _cursor.getString(_cursorIndexOfStatus);
        }
        _item.retryCount = _cursor.getInt(_cursorIndexOfRetryCount);
        if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
          _item.errorMessage = null;
        } else {
          _item.errorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
        }
        _item.createdAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int getPendingCount() {
    final String _sql = "SELECT COUNT(*) FROM pending_expenses WHERE status = 'pending'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
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
