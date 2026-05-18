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
public final class GroupDao_Impl implements GroupDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CachedGroup> __insertionAdapterOfCachedGroup;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public GroupDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCachedGroup = new EntityInsertionAdapter<CachedGroup>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_groups` (`id`,`name`,`cachedAt`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final CachedGroup entity) {
        statement.bindLong(1, entity.id);
        if (entity.name == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.name);
        }
        statement.bindLong(3, entity.cachedAt);
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_groups";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(final List<CachedGroup> groups) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfCachedGroup.insert(groups);
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
  public List<CachedGroup> getAllGroups() {
    final String _sql = "SELECT * FROM cached_groups ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
      final List<CachedGroup> _result = new ArrayList<CachedGroup>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final CachedGroup _item;
        _item = new CachedGroup();
        _item.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfName)) {
          _item.name = null;
        } else {
          _item.name = _cursor.getString(_cursorIndexOfName);
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
