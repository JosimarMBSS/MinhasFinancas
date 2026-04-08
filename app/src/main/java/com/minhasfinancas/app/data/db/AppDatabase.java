package com.minhasfinancas.app.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.minhasfinancas.app.data.entity.AccountEntity;
import com.minhasfinancas.app.data.entity.CategoryEntity;

public class AppDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "minhas_financas.db";
    private static final int DATABASE_VERSION = 4;

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppDatabase(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private AppDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(false);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "color_hex TEXT," +
                "icon_name TEXT," +
                "is_default INTEGER NOT NULL DEFAULT 0," +
                "is_active INTEGER NOT NULL DEFAULT 1," +
                "created_at INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "initial_balance REAL NOT NULL DEFAULT 0," +
                "color_hex TEXT," +
                "is_active INTEGER NOT NULL DEFAULT 1," +
                "created_at INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "description TEXT," +
                "amount REAL NOT NULL," +
                "type TEXT NOT NULL," +
                "category_id INTEGER NOT NULL," +
                "transaction_date TEXT NOT NULL," +
                "due_date TEXT," +
                "paid_date TEXT," +
                "status TEXT NOT NULL," +
                "recurrence_id INTEGER," +
                "account_id INTEGER," +
                "destination_account_id INTEGER," +
                "account_name TEXT," +
                "notes TEXT," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE recurrences (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "category_id INTEGER NOT NULL," +
                "frequency TEXT NOT NULL," +
                "interval_value INTEGER NOT NULL DEFAULT 1," +
                "start_date TEXT NOT NULL," +
                "end_date TEXT," +
                "max_occurrences INTEGER," +
                "is_active INTEGER NOT NULL DEFAULT 1," +
                "account_name TEXT," +
                "notes TEXT," +
                "created_at INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE recurrence_exclusions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "recurrence_id INTEGER NOT NULL," +
                "occurrence_date TEXT NOT NULL," +
                "created_at INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE INDEX idx_categories_name ON categories(name)");
        db.execSQL("CREATE INDEX idx_accounts_name ON accounts(name)");
        db.execSQL("CREATE INDEX idx_transactions_date ON transactions(transaction_date)");
        db.execSQL("CREATE INDEX idx_transactions_due_date ON transactions(due_date)");
        db.execSQL("CREATE INDEX idx_transactions_recurrence ON transactions(recurrence_id)");
        db.execSQL("CREATE INDEX idx_transactions_account ON transactions(account_id)");
        db.execSQL("CREATE INDEX idx_transactions_destination_account ON transactions(destination_account_id)");
        db.execSQL("CREATE INDEX idx_recurrences_start_date ON recurrences(start_date)");
        db.execSQL("CREATE UNIQUE INDEX idx_recurrence_exclusions_unique ON recurrence_exclusions(recurrence_id, occurrence_date)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            safeExec(db, "ALTER TABLE transactions ADD COLUMN account_name TEXT");
            safeExec(db, "ALTER TABLE recurrences ADD COLUMN account_name TEXT");
        }
        if (oldVersion < 3) {
            safeExec(db, "CREATE TABLE accounts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "type TEXT NOT NULL," +
                    "initial_balance REAL NOT NULL DEFAULT 0," +
                    "color_hex TEXT," +
                    "is_active INTEGER NOT NULL DEFAULT 1," +
                    "created_at INTEGER NOT NULL" +
                    ")");
            safeExec(db, "CREATE INDEX idx_accounts_name ON accounts(name)");
            safeExec(db, "ALTER TABLE transactions ADD COLUMN account_id INTEGER");
            safeExec(db, "ALTER TABLE transactions ADD COLUMN destination_account_id INTEGER");
            safeExec(db, "CREATE INDEX idx_transactions_account ON transactions(account_id)");
            safeExec(db, "CREATE INDEX idx_transactions_destination_account ON transactions(destination_account_id)");
        }
        if (oldVersion < 4) {
            safeExec(db, "CREATE TABLE recurrence_exclusions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "recurrence_id INTEGER NOT NULL," +
                    "occurrence_date TEXT NOT NULL," +
                    "created_at INTEGER NOT NULL" +
                    ")");
            safeExec(db, "CREATE UNIQUE INDEX idx_recurrence_exclusions_unique ON recurrence_exclusions(recurrence_id, occurrence_date)");
        }
    }

    private void safeExec(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (Exception ignored) {
        }
    }

    public int countCategories() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM categories", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public int countAccounts() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM accounts", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public long insertCategory(CategoryEntity entity) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", entity.name);
        values.put("type", entity.type);
        values.put("color_hex", entity.colorHex);
        values.put("icon_name", entity.iconName);
        values.put("is_default", entity.isDefault ? 1 : 0);
        values.put("is_active", entity.isActive ? 1 : 0);
        values.put("created_at", entity.createdAt);
        return db.insert("categories", null, values);
    }

    public long insertAccount(AccountEntity entity) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", entity.name);
        values.put("type", entity.type);
        values.put("initial_balance", entity.initialBalance);
        values.put("color_hex", entity.colorHex);
        values.put("is_active", entity.isActive ? 1 : 0);
        values.put("created_at", entity.createdAt);
        return db.insert("accounts", null, values);
    }
}
