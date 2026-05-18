package com.splitr.app.cache;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class SplitRDatabase_Impl extends SplitRDatabase {
  private volatile ExpenseDao _expenseDao;

  private volatile GroupDao _groupDao;

  private volatile BalanceDao _balanceDao;

  private volatile PendingExpenseDao _pendingExpenseDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_expenses` (`id` INTEGER NOT NULL, `amount` REAL NOT NULL, `description` TEXT, `latitude` REAL, `longitude` REAL, `userId` INTEGER NOT NULL, `groupId` INTEGER, `splitType` TEXT, `username` TEXT, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_groups` (`id` INTEGER NOT NULL, `name` TEXT, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_balances` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `splitId` INTEGER NOT NULL, `toUsername` TEXT, `toUserId` INTEGER, `fromUsername` TEXT, `fromUserId` INTEGER, `isGuest` INTEGER NOT NULL, `amount` REAL NOT NULL, `description` TEXT, `groupId` INTEGER, `isIOwe` INTEGER NOT NULL, `cachedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pending_expenses` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `description` TEXT, `latitude` REAL, `longitude` REAL, `userId` INTEGER NOT NULL, `groupId` INTEGER, `splitType` TEXT, `splitsJson` TEXT, `membersJson` TEXT, `status` TEXT, `retryCount` INTEGER NOT NULL, `errorMessage` TEXT, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a385c4bac0b641e37f9d0a04e7bcc7e8')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `cached_expenses`");
        db.execSQL("DROP TABLE IF EXISTS `cached_groups`");
        db.execSQL("DROP TABLE IF EXISTS `cached_balances`");
        db.execSQL("DROP TABLE IF EXISTS `pending_expenses`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCachedExpenses = new HashMap<String, TableInfo.Column>(10);
        _columnsCachedExpenses.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedExpenses.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedExpenses.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedExpenses.put("latitude", new TableInfo.Column("latitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedExpenses.put("longitude", new TableInfo.Column("longitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedExpenses.put("userId", new TableInfo.Column("userId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedExpenses.put("groupId", new TableInfo.Column("groupId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedExpenses.put("splitType", new TableInfo.Column("splitType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedExpenses.put("username", new TableInfo.Column("username", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedExpenses.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedExpenses = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedExpenses = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedExpenses = new TableInfo("cached_expenses", _columnsCachedExpenses, _foreignKeysCachedExpenses, _indicesCachedExpenses);
        final TableInfo _existingCachedExpenses = TableInfo.read(db, "cached_expenses");
        if (!_infoCachedExpenses.equals(_existingCachedExpenses)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_expenses(com.splitr.app.cache.CachedExpense).\n"
                  + " Expected:\n" + _infoCachedExpenses + "\n"
                  + " Found:\n" + _existingCachedExpenses);
        }
        final HashMap<String, TableInfo.Column> _columnsCachedGroups = new HashMap<String, TableInfo.Column>(3);
        _columnsCachedGroups.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedGroups.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedGroups.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedGroups = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedGroups = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedGroups = new TableInfo("cached_groups", _columnsCachedGroups, _foreignKeysCachedGroups, _indicesCachedGroups);
        final TableInfo _existingCachedGroups = TableInfo.read(db, "cached_groups");
        if (!_infoCachedGroups.equals(_existingCachedGroups)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_groups(com.splitr.app.cache.CachedGroup).\n"
                  + " Expected:\n" + _infoCachedGroups + "\n"
                  + " Found:\n" + _existingCachedGroups);
        }
        final HashMap<String, TableInfo.Column> _columnsCachedBalances = new HashMap<String, TableInfo.Column>(12);
        _columnsCachedBalances.put("localId", new TableInfo.Column("localId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("splitId", new TableInfo.Column("splitId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("toUsername", new TableInfo.Column("toUsername", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("toUserId", new TableInfo.Column("toUserId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("fromUsername", new TableInfo.Column("fromUsername", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("fromUserId", new TableInfo.Column("fromUserId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("isGuest", new TableInfo.Column("isGuest", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("groupId", new TableInfo.Column("groupId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("isIOwe", new TableInfo.Column("isIOwe", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBalances.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedBalances = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedBalances = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedBalances = new TableInfo("cached_balances", _columnsCachedBalances, _foreignKeysCachedBalances, _indicesCachedBalances);
        final TableInfo _existingCachedBalances = TableInfo.read(db, "cached_balances");
        if (!_infoCachedBalances.equals(_existingCachedBalances)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_balances(com.splitr.app.cache.CachedBalance).\n"
                  + " Expected:\n" + _infoCachedBalances + "\n"
                  + " Found:\n" + _existingCachedBalances);
        }
        final HashMap<String, TableInfo.Column> _columnsPendingExpenses = new HashMap<String, TableInfo.Column>(14);
        _columnsPendingExpenses.put("localId", new TableInfo.Column("localId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("latitude", new TableInfo.Column("latitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("longitude", new TableInfo.Column("longitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("userId", new TableInfo.Column("userId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("groupId", new TableInfo.Column("groupId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("splitType", new TableInfo.Column("splitType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("splitsJson", new TableInfo.Column("splitsJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("membersJson", new TableInfo.Column("membersJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("status", new TableInfo.Column("status", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("retryCount", new TableInfo.Column("retryCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("errorMessage", new TableInfo.Column("errorMessage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingExpenses.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPendingExpenses = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPendingExpenses = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPendingExpenses = new TableInfo("pending_expenses", _columnsPendingExpenses, _foreignKeysPendingExpenses, _indicesPendingExpenses);
        final TableInfo _existingPendingExpenses = TableInfo.read(db, "pending_expenses");
        if (!_infoPendingExpenses.equals(_existingPendingExpenses)) {
          return new RoomOpenHelper.ValidationResult(false, "pending_expenses(com.splitr.app.cache.PendingExpense).\n"
                  + " Expected:\n" + _infoPendingExpenses + "\n"
                  + " Found:\n" + _existingPendingExpenses);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "a385c4bac0b641e37f9d0a04e7bcc7e8", "5bf688b6fd8081997941934424773288");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "cached_expenses","cached_groups","cached_balances","pending_expenses");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `cached_expenses`");
      _db.execSQL("DELETE FROM `cached_groups`");
      _db.execSQL("DELETE FROM `cached_balances`");
      _db.execSQL("DELETE FROM `pending_expenses`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ExpenseDao.class, ExpenseDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GroupDao.class, GroupDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BalanceDao.class, BalanceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PendingExpenseDao.class, PendingExpenseDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ExpenseDao expenseDao() {
    if (_expenseDao != null) {
      return _expenseDao;
    } else {
      synchronized(this) {
        if(_expenseDao == null) {
          _expenseDao = new ExpenseDao_Impl(this);
        }
        return _expenseDao;
      }
    }
  }

  @Override
  public GroupDao groupDao() {
    if (_groupDao != null) {
      return _groupDao;
    } else {
      synchronized(this) {
        if(_groupDao == null) {
          _groupDao = new GroupDao_Impl(this);
        }
        return _groupDao;
      }
    }
  }

  @Override
  public BalanceDao balanceDao() {
    if (_balanceDao != null) {
      return _balanceDao;
    } else {
      synchronized(this) {
        if(_balanceDao == null) {
          _balanceDao = new BalanceDao_Impl(this);
        }
        return _balanceDao;
      }
    }
  }

  @Override
  public PendingExpenseDao pendingExpenseDao() {
    if (_pendingExpenseDao != null) {
      return _pendingExpenseDao;
    } else {
      synchronized(this) {
        if(_pendingExpenseDao == null) {
          _pendingExpenseDao = new PendingExpenseDao_Impl(this);
        }
        return _pendingExpenseDao;
      }
    }
  }
}
