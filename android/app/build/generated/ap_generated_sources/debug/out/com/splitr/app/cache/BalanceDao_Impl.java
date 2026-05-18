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
public final class BalanceDao_Impl implements BalanceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CachedBalance> __insertionAdapterOfCachedBalance;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public BalanceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCachedBalance = new EntityInsertionAdapter<CachedBalance>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_balances` (`localId`,`splitId`,`toUsername`,`toUserId`,`fromUsername`,`fromUserId`,`isGuest`,`amount`,`description`,`groupId`,`isIOwe`,`cachedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final CachedBalance entity) {
        statement.bindLong(1, entity.localId);
        statement.bindLong(2, entity.splitId);
        if (entity.toUsername == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.toUsername);
        }
        if (entity.toUserId == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.toUserId);
        }
        if (entity.fromUsername == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.fromUsername);
        }
        if (entity.fromUserId == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.fromUserId);
        }
        final int _tmp = entity.isGuest ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindDouble(8, entity.amount);
        if (entity.description == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.description);
        }
        if (entity.groupId == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.groupId);
        }
        final int _tmp_1 = entity.isIOwe ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        statement.bindLong(12, entity.cachedAt);
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_balances";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(final List<CachedBalance> balances) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfCachedBalance.insert(balances);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void clearAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfClearAll.release(_stmt);
    }
  }

  @Override
  public List<CachedBalance> getIOwe() {
    final String _sql = "SELECT * FROM cached_balances WHERE isIOwe = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfLocalId = CursorUtil.getColumnIndexOrThrow(_cursor, "localId");
      final int _cursorIndexOfSplitId = CursorUtil.getColumnIndexOrThrow(_cursor, "splitId");
      final int _cursorIndexOfToUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "toUsername");
      final int _cursorIndexOfToUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "toUserId");
      final int _cursorIndexOfFromUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "fromUsername");
      final int _cursorIndexOfFromUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "fromUserId");
      final int _cursorIndexOfIsGuest = CursorUtil.getColumnIndexOrThrow(_cursor, "isGuest");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
      final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
      final int _cursorIndexOfIsIOwe = CursorUtil.getColumnIndexOrThrow(_cursor, "isIOwe");
      final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
      final List<CachedBalance> _result = new ArrayList<CachedBalance>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final CachedBalance _item;
        _item = new CachedBalance();
        _item.localId = _cursor.getInt(_cursorIndexOfLocalId);
        _item.splitId = _cursor.getInt(_cursorIndexOfSplitId);
        if (_cursor.isNull(_cursorIndexOfToUsername)) {
          _item.toUsername = null;
        } else {
          _item.toUsername = _cursor.getString(_cursorIndexOfToUsername);
        }
        if (_cursor.isNull(_cursorIndexOfToUserId)) {
          _item.toUserId = null;
        } else {
          _item.toUserId = _cursor.getInt(_cursorIndexOfToUserId);
        }
        if (_cursor.isNull(_cursorIndexOfFromUsername)) {
          _item.fromUsername = null;
        } else {
          _item.fromUsername = _cursor.getString(_cursorIndexOfFromUsername);
        }
        if (_cursor.isNull(_cursorIndexOfFromUserId)) {
          _item.fromUserId = null;
        } else {
          _item.fromUserId = _cursor.getInt(_cursorIndexOfFromUserId);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsGuest);
        _item.isGuest = _tmp != 0;
        _item.amount = _cursor.getDouble(_cursorIndexOfAmount);
        if (_cursor.isNull(_cursorIndexOfDescription)) {
          _item.description = null;
        } else {
          _item.description = _cursor.getString(_cursorIndexOfDescription);
        }
        if (_cursor.isNull(_cursorIndexOfGroupId)) {
          _item.groupId = null;
        } else {
          _item.groupId = _cursor.getInt(_cursorIndexOfGroupId);
        }
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfIsIOwe);
        _item.isIOwe = _tmp_1 != 0;
        _item.cachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<CachedBalance> getOwedToMe() {
    final String _sql = "SELECT * FROM cached_balances WHERE isIOwe = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfLocalId = CursorUtil.getColumnIndexOrThrow(_cursor, "localId");
      final int _cursorIndexOfSplitId = CursorUtil.getColumnIndexOrThrow(_cursor, "splitId");
      final int _cursorIndexOfToUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "toUsername");
      final int _cursorIndexOfToUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "toUserId");
      final int _cursorIndexOfFromUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "fromUsername");
      final int _cursorIndexOfFromUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "fromUserId");
      final int _cursorIndexOfIsGuest = CursorUtil.getColumnIndexOrThrow(_cursor, "isGuest");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
      final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
      final int _cursorIndexOfIsIOwe = CursorUtil.getColumnIndexOrThrow(_cursor, "isIOwe");
      final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
      final List<CachedBalance> _result = new ArrayList<CachedBalance>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final CachedBalance _item;
        _item = new CachedBalance();
        _item.localId = _cursor.getInt(_cursorIndexOfLocalId);
        _item.splitId = _cursor.getInt(_cursorIndexOfSplitId);
        if (_cursor.isNull(_cursorIndexOfToUsername)) {
          _item.toUsername = null;
        } else {
          _item.toUsername = _cursor.getString(_cursorIndexOfToUsername);
        }
        if (_cursor.isNull(_cursorIndexOfToUserId)) {
          _item.toUserId = null;
        } else {
          _item.toUserId = _cursor.getInt(_cursorIndexOfToUserId);
        }
        if (_cursor.isNull(_cursorIndexOfFromUsername)) {
          _item.fromUsername = null;
        } else {
          _item.fromUsername = _cursor.getString(_cursorIndexOfFromUsername);
        }
        if (_cursor.isNull(_cursorIndexOfFromUserId)) {
          _item.fromUserId = null;
        } else {
          _item.fromUserId = _cursor.getInt(_cursorIndexOfFromUserId);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsGuest);
        _item.isGuest = _tmp != 0;
        _item.amount = _cursor.getDouble(_cursorIndexOfAmount);
        if (_cursor.isNull(_cursorIndexOfDescription)) {
          _item.description = null;
        } else {
          _item.description = _cursor.getString(_cursorIndexOfDescription);
        }
        if (_cursor.isNull(_cursorIndexOfGroupId)) {
          _item.groupId = null;
        } else {
          _item.groupId = _cursor.getInt(_cursorIndexOfGroupId);
        }
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfIsIOwe);
        _item.isIOwe = _tmp_1 != 0;
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
