package com.minhasfinancas.app.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.minhasfinancas.app.data.db.AppDatabase;
import com.minhasfinancas.app.data.entity.AccountEntity;
import com.minhasfinancas.app.data.entity.CategoryEntity;
import com.minhasfinancas.app.data.entity.RecurrenceEntity;
import com.minhasfinancas.app.data.entity.TransactionEntity;
import com.minhasfinancas.app.data.model.AccountListItem;
import com.minhasfinancas.app.data.model.DashboardSummary;
import com.minhasfinancas.app.data.model.RecurrenceListItem;
import com.minhasfinancas.app.data.model.ReportFilter;
import com.minhasfinancas.app.data.model.ReportSummary;
import com.minhasfinancas.app.data.model.TransactionListItem;
import com.minhasfinancas.app.util.RecurrenceEngine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinanceRepository {

    public interface DataCallback<T> {
        void onResult(T data);
    }

    public interface ActionCallback {
        void onComplete();
    }

    private static volatile FinanceRepository INSTANCE;

    private final AppDatabase db;
    private final ExecutorService io;
    private final Handler mainHandler;

    private FinanceRepository(Context context) {
        db = AppDatabase.getInstance(context);
        io = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static FinanceRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FinanceRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FinanceRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void getAllCategories(DataCallback<List<CategoryEntity>> callback) {
        io.execute(() -> post(callback, queryAllCategories()));
    }

    public void getCategoriesForType(String type, DataCallback<List<CategoryEntity>> callback) {
        io.execute(() -> post(callback, queryCategoriesForType(type)));
    }

    public void getCategory(long id, DataCallback<CategoryEntity> callback) {
        io.execute(() -> post(callback, queryCategoryById(id)));
    }

    public void saveCategory(CategoryEntity entity, DataCallback<Long> callback) {
        io.execute(() -> {
            SQLiteDatabase writable = db.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", entity.name);
            values.put("type", entity.type);
            values.put("color_hex", entity.colorHex);
            values.put("icon_name", entity.iconName);
            values.put("is_default", entity.isDefault ? 1 : 0);
            values.put("is_active", entity.isActive ? 1 : 0);
            values.put("created_at", entity.createdAt);
            long id;
            if (entity.id == 0L) {
                id = writable.insert("categories", null, values);
            } else {
                writable.update("categories", values, "id=?", new String[]{String.valueOf(entity.id)});
                id = entity.id;
            }
            post(callback, id);
        });
    }

    public void deleteCategory(CategoryEntity entity, ActionCallback callback) {
        io.execute(() -> {
            db.getWritableDatabase().delete("categories", "id=?", new String[]{String.valueOf(entity.id)});
            postAction(callback);
        });
    }

    public void getAccounts(DataCallback<List<AccountListItem>> callback) {
        io.execute(() -> post(callback, queryAllAccounts()));
    }

    public void getActiveAccounts(DataCallback<List<AccountEntity>> callback) {
        io.execute(() -> post(callback, queryActiveAccounts()));
    }

    public void getAccount(long id, DataCallback<AccountEntity> callback) {
        io.execute(() -> post(callback, queryAccountById(id)));
    }

    public void saveAccount(AccountEntity entity, DataCallback<Long> callback) {
        io.execute(() -> {
            SQLiteDatabase writable = db.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", entity.name);
            values.put("type", entity.type);
            values.put("initial_balance", entity.initialBalance);
            values.put("color_hex", entity.colorHex);
            values.put("is_active", entity.isActive ? 1 : 0);
            values.put("created_at", entity.createdAt);
            long id;
            if (entity.id == 0L) {
                id = writable.insert("accounts", null, values);
            } else {
                writable.update("accounts", values, "id=?", new String[]{String.valueOf(entity.id)});
                id = entity.id;
            }
            post(callback, id);
        });
    }

    public void deleteAccount(AccountEntity entity, ActionCallback callback) {
        io.execute(() -> {
            db.getWritableDatabase().delete("accounts", "id=?", new String[]{String.valueOf(entity.id)});
            postAction(callback);
        });
    }

    public void getOrCreateTransferCategoryId(DataCallback<Long> callback) {
        io.execute(() -> {
            SQLiteDatabase writable = db.getWritableDatabase();
            CategoryEntity existing = queryCategoryByName("Transferência");
            if (existing != null) {
                post(callback, existing.id);
                return;
            }
            ContentValues values = new ContentValues();
            values.put("name", "Transferência");
            values.put("type", "BOTH");
            values.put("color_hex", "#0F766E");
            values.put("icon_name", "swap_horiz");
            values.put("is_default", 1);
            values.put("is_active", 1);
            values.put("created_at", System.currentTimeMillis());
            long id = writable.insert("categories", null, values);
            post(callback, id);
        });
    }

    public void getTransactions(DataCallback<List<TransactionListItem>> callback) {
        io.execute(() -> post(callback, queryAllTransactions()));
    }

    public void getTransactionsForPeriod(String startDate, String endDate, DataCallback<List<TransactionListItem>> callback) {
        io.execute(() -> post(callback, queryTransactionsBetween(startDate, endDate)));
    }

    public void getTransactionsFiltered(ReportFilter filter, DataCallback<List<TransactionListItem>> callback) {
        io.execute(() -> {
            List<TransactionListItem> source = queryTransactionsBetween(filter.startDate, filter.endDate);
            List<TransactionListItem> filtered = new ArrayList<>();
            for (TransactionListItem item : source) {
                if (filter.type != null && !filter.type.isEmpty() && !"ALL".equals(filter.type) && !filter.type.equals(item.type)) {
                    continue;
                }
                if (filter.status != null && !filter.status.isEmpty() && !"ALL".equals(filter.status) && !filter.status.equals(item.status)) {
                    continue;
                }
                if (filter.categoryName != null && !filter.categoryName.isEmpty() && !"Todas".equals(filter.categoryName) && !filter.categoryName.equals(item.categoryName)) {
                    continue;
                }
                filtered.add(item);
            }
            post(callback, filtered);
        });
    }

    public void getReportSummary(ReportFilter filter, DataCallback<ReportSummary> callback) {
        getTransactionsFiltered(filter, items -> {
            ReportSummary summary = new ReportSummary();
            summary.count = items.size();
            for (TransactionListItem item : items) {
                if ("INCOME".equals(item.type)) {
                    summary.income += item.amount;
                } else if ("EXPENSE".equals(item.type)) {
                    summary.expense += item.amount;
                }
            }
            callback.onResult(summary);
        });
    }

    public void getUpcomingTransactions(String today, DataCallback<List<TransactionListItem>> callback) {
        io.execute(() -> post(callback, queryUpcomingTransactions(today)));
    }

    public void getTransaction(long id, DataCallback<TransactionEntity> callback) {
        io.execute(() -> post(callback, queryTransactionById(id)));
    }

    public void saveTransaction(TransactionEntity entity, DataCallback<Long> callback) {
        io.execute(() -> {
            SQLiteDatabase writable = db.getWritableDatabase();
            ContentValues values = buildTransactionValues(entity);
            long id;
            if (entity.id == 0L) {
                id = writable.insert("transactions", null, values);
            } else {
                writable.update("transactions", values, "id=?", new String[]{String.valueOf(entity.id)});
                id = entity.id;
            }
            post(callback, id);
        });
    }


    public void updateTransactionStatus(long transactionId, String status, String paidDate, ActionCallback callback) {
        io.execute(() -> {
            ContentValues values = new ContentValues();
            values.put("status", status);
            if (paidDate == null || paidDate.trim().isEmpty()) {
                values.putNull("paid_date");
            } else {
                values.put("paid_date", paidDate);
            }
            values.put("updated_at", System.currentTimeMillis());
            db.getWritableDatabase().update("transactions", values, "id=?", new String[]{String.valueOf(transactionId)});
            postAction(callback);
        });
    }

    public void deleteTransactionById(long transactionId, ActionCallback callback) {
        io.execute(() -> {
            db.getWritableDatabase().delete("transactions", "id=?", new String[]{String.valueOf(transactionId)});
            postAction(callback);
        });
    }

    public void deleteRecurringOccurrence(long recurrenceId, String occurrenceDate, ActionCallback callback) {
        io.execute(() -> {
            SQLiteDatabase writable = db.getWritableDatabase();
            writable.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put("recurrence_id", recurrenceId);
                values.put("occurrence_date", occurrenceDate);
                values.put("created_at", System.currentTimeMillis());
                writable.insertWithOnConflict("recurrence_exclusions", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                writable.delete("transactions", "recurrence_id=? AND transaction_date=?", new String[]{String.valueOf(recurrenceId), occurrenceDate});
                writable.setTransactionSuccessful();
            } finally {
                writable.endTransaction();
            }
            postAction(callback);
        });
    }

    public void deleteRecurringFromDate(long recurrenceId, String startDate, ActionCallback callback) {
        io.execute(() -> {
            SQLiteDatabase writable = db.getWritableDatabase();
            writable.beginTransaction();
            try {
                writable.delete("recurrences", "id=?", new String[]{String.valueOf(recurrenceId)});
                writable.delete("transactions", "recurrence_id=? AND transaction_date>=?", new String[]{String.valueOf(recurrenceId), startDate});
                writable.delete("recurrence_exclusions", "recurrence_id=? AND occurrence_date>=?", new String[]{String.valueOf(recurrenceId), startDate});
                writable.setTransactionSuccessful();
            } finally {
                writable.endTransaction();
            }
            postAction(callback);
        });
    }

    public void deleteRecurringSeries(long recurrenceId, ActionCallback callback) {
        io.execute(() -> {
            SQLiteDatabase writable = db.getWritableDatabase();
            writable.beginTransaction();
            try {
                writable.delete("recurrences", "id=?", new String[]{String.valueOf(recurrenceId)});
                writable.delete("transactions", "recurrence_id=?", new String[]{String.valueOf(recurrenceId)});
                writable.delete("recurrence_exclusions", "recurrence_id=?", new String[]{String.valueOf(recurrenceId)});
                writable.setTransactionSuccessful();
            } finally {
                writable.endTransaction();
            }
            postAction(callback);
        });
    }

    public void deleteTransaction(TransactionEntity entity, ActionCallback callback) {
        io.execute(() -> {
            db.getWritableDatabase().delete("transactions", "id=?", new String[]{String.valueOf(entity.id)});
            postAction(callback);
        });
    }

    public void getDashboardSummary(String startDate, String endDate, DataCallback<DashboardSummary> callback) {
        io.execute(() -> {
            double income = sumTransactionsBetween("INCOME", startDate, endDate);
            double expense = sumTransactionsBetween("EXPENSE", startDate, endDate);
            post(callback, new DashboardSummary(income, expense));
        });
    }

    public void getRecurrences(DataCallback<List<RecurrenceListItem>> callback) {
        io.execute(() -> post(callback, queryAllRecurrences()));
    }

    public void getRecurrence(long id, DataCallback<RecurrenceEntity> callback) {
        io.execute(() -> post(callback, queryRecurrenceById(id)));
    }

    public void saveRecurrence(RecurrenceEntity entity, DataCallback<Long> callback) {
        io.execute(() -> {
            SQLiteDatabase writable = db.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("title", entity.title);
            values.put("type", entity.type);
            values.put("amount", entity.amount);
            values.put("category_id", entity.categoryId);
            values.put("frequency", entity.frequency);
            values.put("interval_value", entity.intervalValue);
            values.put("start_date", entity.startDate);
            values.put("end_date", entity.endDate);
            if (entity.maxOccurrences == null) {
                values.putNull("max_occurrences");
            } else {
                values.put("max_occurrences", entity.maxOccurrences);
            }
            values.put("is_active", entity.isActive ? 1 : 0);
            values.put("account_name", entity.accountName);
            values.put("notes", entity.notes);
            values.put("created_at", entity.createdAt);
            long id;
            if (entity.id == 0L) {
                id = writable.insert("recurrences", null, values);
            } else {
                writable.update("recurrences", values, "id=?", new String[]{String.valueOf(entity.id)});
                id = entity.id;
            }
            post(callback, id);
        });
    }

    public void deleteRecurrence(RecurrenceEntity entity, ActionCallback callback) {
        io.execute(() -> {
            db.getWritableDatabase().delete("recurrences", "id=?", new String[]{String.valueOf(entity.id)});
            postAction(callback);
        });
    }

    public void removeRecurrenceFromDate(long recurrenceId, String startDate, ActionCallback callback) {
        io.execute(() -> {
            SQLiteDatabase writable = db.getWritableDatabase();
            writable.delete("recurrences", "id=?", new String[]{String.valueOf(recurrenceId)});
            ContentValues values = new ContentValues();
            values.putNull("recurrence_id");
            writable.update("transactions", values, "recurrence_id=? AND transaction_date>=?", new String[]{String.valueOf(recurrenceId), startDate});
            postAction(callback);
        });
    }

    public void generateRecurrencesForCurrentMonth(ActionCallback callback) {
        io.execute(() -> {
            LocalDate today = LocalDate.now();
            generateRecurrences(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
            postAction(callback);
        });
    }

    public void generateRecurrencesForDashboard() {
        io.execute(() -> {
            LocalDate today = LocalDate.now();
            LocalDate nextMonth = today.plusMonths(1);
            generateRecurrences(today.withDayOfMonth(1), nextMonth.withDayOfMonth(nextMonth.lengthOfMonth()));
        });
    }

    public void generateRecurrencesForPeriod(String startDate, String endDate, ActionCallback callback) {
        io.execute(() -> {
            generateRecurrences(LocalDate.parse(startDate), LocalDate.parse(endDate));
            postAction(callback);
        });
    }

    private void generateRecurrences(LocalDate start, LocalDate end) {
        List<RecurrenceEntity> recurrences = queryRawRecurrences();
        SQLiteDatabase writable = db.getWritableDatabase();
        for (RecurrenceEntity recurrence : recurrences) {
            if (!recurrence.isActive) {
                continue;
            }
            List<LocalDate> dates = RecurrenceEngine.generateDates(recurrence, start, end);
            for (LocalDate date : dates) {
                String occurrenceDate = date.toString();
                if (isExcludedOccurrence(recurrence.id, occurrenceDate)) {
                    continue;
                }
                if (queryTransactionByRecurrenceAndDate(recurrence.id, occurrenceDate) != null) {
                    continue;
                }
                TransactionEntity entity = new TransactionEntity();
                entity.title = recurrence.title;
                entity.description = recurrence.title;
                entity.amount = recurrence.amount;
                entity.type = recurrence.type;
                entity.categoryId = recurrence.categoryId;
                entity.transactionDate = date.toString();
                entity.dueDate = date.toString();
                entity.paidDate = null;
                entity.status = "PENDING";
                entity.recurrenceId = recurrence.id;
                entity.accountName = recurrence.accountName;
                entity.notes = recurrence.notes;
                entity.createdAt = System.currentTimeMillis();
                entity.updatedAt = System.currentTimeMillis();
                writable.insert("transactions", null, buildTransactionValues(entity));
            }
        }
    }

    private ContentValues buildTransactionValues(TransactionEntity entity) {
        ContentValues values = new ContentValues();
        values.put("title", entity.title);
        values.put("description", entity.description);
        values.put("amount", entity.amount);
        values.put("type", entity.type);
        values.put("category_id", entity.categoryId);
        values.put("transaction_date", entity.transactionDate);
        values.put("due_date", entity.dueDate);
        values.put("paid_date", entity.paidDate);
        values.put("status", entity.status);
        if (entity.recurrenceId == null) {
            values.putNull("recurrence_id");
        } else {
            values.put("recurrence_id", entity.recurrenceId);
        }
        if (entity.accountId == null) {
            values.putNull("account_id");
        } else {
            values.put("account_id", entity.accountId);
        }
        if (entity.destinationAccountId == null) {
            values.putNull("destination_account_id");
        } else {
            values.put("destination_account_id", entity.destinationAccountId);
        }
        values.put("account_name", entity.accountName);
        values.put("notes", entity.notes);
        values.put("created_at", entity.createdAt);
        values.put("updated_at", entity.updatedAt);
        return values;
    }

    private List<CategoryEntity> queryAllCategories() {
        List<CategoryEntity> items = new ArrayList<>();
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT * FROM categories ORDER BY name ASC", null)) {
            while (cursor.moveToNext()) {
                items.add(mapCategory(cursor));
            }
        }
        return items;
    }

    private List<CategoryEntity> queryCategoriesForType(String type) {
        List<CategoryEntity> items = new ArrayList<>();
        SQLiteDatabase readable = db.getReadableDatabase();
        String sql = "SELECT * FROM categories WHERE is_active = 1 AND (type = ? OR type = 'BOTH') ORDER BY name ASC";
        try (Cursor cursor = readable.rawQuery(sql, new String[]{type})) {
            while (cursor.moveToNext()) {
                items.add(mapCategory(cursor));
            }
        }
        return items;
    }

    private CategoryEntity queryCategoryById(long id) {
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT * FROM categories WHERE id = ? LIMIT 1", new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst() ? mapCategory(cursor) : null;
        }
    }

    private CategoryEntity queryCategoryByName(String name) {
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT * FROM categories WHERE name = ? LIMIT 1", new String[]{name})) {
            return cursor.moveToFirst() ? mapCategory(cursor) : null;
        }
    }

    private List<AccountListItem> queryAllAccounts() {
        List<AccountListItem> items = new ArrayList<>();
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT * FROM accounts ORDER BY is_active DESC, name ASC", null)) {
            while (cursor.moveToNext()) {
                AccountEntity entity = mapAccount(cursor);
                AccountListItem item = new AccountListItem();
                item.id = entity.id;
                item.name = entity.name;
                item.type = entity.type;
                item.initialBalance = entity.initialBalance;
                item.currentBalance = calculateAccountBalance(entity.id, entity.initialBalance);
                item.colorHex = entity.colorHex;
                item.isActive = entity.isActive;
                items.add(item);
            }
        }
        return items;
    }

    private List<AccountEntity> queryActiveAccounts() {
        List<AccountEntity> items = new ArrayList<>();
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT * FROM accounts WHERE is_active = 1 ORDER BY name ASC", null)) {
            while (cursor.moveToNext()) {
                items.add(mapAccount(cursor));
            }
        }
        return items;
    }

    private AccountEntity queryAccountById(long id) {
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT * FROM accounts WHERE id = ? LIMIT 1", new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst() ? mapAccount(cursor) : null;
        }
    }

    private double calculateAccountBalance(long accountId, double initialBalance) {
        double balance = initialBalance;
        SQLiteDatabase readable = db.getReadableDatabase();
        String sql = "SELECT type, amount, account_id, destination_account_id FROM transactions WHERE status = 'PAID' AND (account_id = ? OR destination_account_id = ?)";
        try (Cursor cursor = readable.rawQuery(sql, new String[]{String.valueOf(accountId), String.valueOf(accountId)})) {
            while (cursor.moveToNext()) {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                int sourceIndex = cursor.getColumnIndexOrThrow("account_id");
                int destinationIndex = cursor.getColumnIndexOrThrow("destination_account_id");
                Long sourceId = cursor.isNull(sourceIndex) ? null : cursor.getLong(sourceIndex);
                Long destinationId = cursor.isNull(destinationIndex) ? null : cursor.getLong(destinationIndex);

                if ("INCOME".equals(type) && sourceId != null && sourceId == accountId) {
                    balance += amount;
                } else if ("EXPENSE".equals(type) && sourceId != null && sourceId == accountId) {
                    balance -= amount;
                } else if ("TRANSFER".equals(type)) {
                    if (sourceId != null && sourceId == accountId) {
                        balance -= amount;
                    }
                    if (destinationId != null && destinationId == accountId) {
                        balance += amount;
                    }
                }
            }
        }
        return balance;
    }

    private List<TransactionListItem> queryAllTransactions() {
        return queryTransactionsBetween(null, null);
    }

    private List<TransactionListItem> queryTransactionsBetween(String startDate, String endDate) {
        List<TransactionListItem> items = new ArrayList<>();
        SQLiteDatabase readable = db.getReadableDatabase();
        StringBuilder sql = new StringBuilder();
        List<String> args = new ArrayList<>();

        sql.append("SELECT t.id, t.title, t.description, t.amount, t.type, t.transaction_date, t.due_date, t.status, ")
                .append("COALESCE(a.name, t.account_name) AS account_name, ")
                .append("da.name AS destination_account_name, ")
                .append("t.notes, t.recurrence_id, c.name AS category_name, c.color_hex AS category_color ")
                .append("FROM transactions t ")
                .append("INNER JOIN categories c ON c.id = t.category_id ")
                .append("LEFT JOIN accounts a ON a.id = t.account_id ")
                .append("LEFT JOIN accounts da ON da.id = t.destination_account_id ");

        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            sql.append("WHERE t.transaction_date BETWEEN ? AND ? ");
            args.add(startDate);
            args.add(endDate);
        }

        sql.append("ORDER BY CASE t.type WHEN 'EXPENSE' THEN 0 WHEN 'INCOME' THEN 1 ELSE 2 END, LOWER(COALESCE(t.description, t.title)) ASC, t.id DESC");

        try (Cursor cursor = readable.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                items.add(mapTransactionListItem(cursor));
            }
        }
        return items;
    }

    private List<TransactionListItem> queryUpcomingTransactions(String today) {
        List<TransactionListItem> items = new ArrayList<>();
        SQLiteDatabase readable = db.getReadableDatabase();
        String sql = "SELECT t.id, t.title, t.description, t.amount, t.type, t.transaction_date, t.due_date, t.status, " +
                "COALESCE(a.name, t.account_name) AS account_name, da.name AS destination_account_name, t.notes, t.recurrence_id, " +
                "c.name AS category_name, c.color_hex AS category_color " +
                "FROM transactions t INNER JOIN categories c ON c.id = t.category_id " +
                "LEFT JOIN accounts a ON a.id = t.account_id " +
                "LEFT JOIN accounts da ON da.id = t.destination_account_id " +
                "WHERE t.status = 'PENDING' AND t.due_date IS NOT NULL AND t.due_date >= ? " +
                "ORDER BY t.due_date ASC, t.id DESC LIMIT 5";
        try (Cursor cursor = readable.rawQuery(sql, new String[]{today})) {
            while (cursor.moveToNext()) {
                items.add(mapTransactionListItem(cursor));
            }
        }
        return items;
    }

    private TransactionEntity queryTransactionById(long id) {
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT * FROM transactions WHERE id = ? LIMIT 1", new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst() ? mapTransactionEntity(cursor) : null;
        }
    }

    private TransactionEntity queryTransactionByRecurrenceAndDate(long recurrenceId, String date) {
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT * FROM transactions WHERE recurrence_id = ? AND transaction_date = ? LIMIT 1", new String[]{String.valueOf(recurrenceId), date})) {
            return cursor.moveToFirst() ? mapTransactionEntity(cursor) : null;
        }
    }

    private boolean isExcludedOccurrence(long recurrenceId, String occurrenceDate) {
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT 1 FROM recurrence_exclusions WHERE recurrence_id = ? AND occurrence_date = ? LIMIT 1", new String[]{String.valueOf(recurrenceId), occurrenceDate})) {
            return cursor.moveToFirst();
        }
    }

    private double sumTransactionsBetween(String type, String startDate, String endDate) {
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = ? AND transaction_date BETWEEN ? AND ?", new String[]{type, startDate, endDate})) {
            return cursor.moveToFirst() ? cursor.getDouble(0) : 0d;
        }
    }

    private List<RecurrenceListItem> queryAllRecurrences() {
        List<RecurrenceListItem> items = new ArrayList<>();
        SQLiteDatabase readable = db.getReadableDatabase();
        String sql = "SELECT r.id, r.title, r.type, r.amount, r.frequency, r.interval_value, r.start_date, r.end_date, r.is_active, r.notes, r.account_name, c.name AS category_name, c.color_hex AS category_color FROM recurrences r INNER JOIN categories c ON c.id = r.category_id ORDER BY r.is_active DESC, r.created_at DESC";
        try (Cursor cursor = readable.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                items.add(mapRecurrenceListItem(cursor));
            }
        }
        return items;
    }

    private List<RecurrenceEntity> queryRawRecurrences() {
        List<RecurrenceEntity> items = new ArrayList<>();
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT * FROM recurrences ORDER BY created_at DESC", null)) {
            while (cursor.moveToNext()) {
                items.add(mapRecurrenceEntity(cursor));
            }
        }
        return items;
    }

    private RecurrenceEntity queryRecurrenceById(long id) {
        SQLiteDatabase readable = db.getReadableDatabase();
        try (Cursor cursor = readable.rawQuery("SELECT * FROM recurrences WHERE id = ? LIMIT 1", new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst() ? mapRecurrenceEntity(cursor) : null;
        }
    }

    private CategoryEntity mapCategory(Cursor cursor) {
        CategoryEntity item = new CategoryEntity();
        item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        item.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        item.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        item.colorHex = cursor.getString(cursor.getColumnIndexOrThrow("color_hex"));
        item.iconName = cursor.getString(cursor.getColumnIndexOrThrow("icon_name"));
        item.isDefault = cursor.getInt(cursor.getColumnIndexOrThrow("is_default")) == 1;
        item.isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1;
        item.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        return item;
    }

    private AccountEntity mapAccount(Cursor cursor) {
        AccountEntity item = new AccountEntity();
        item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        item.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        item.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        item.initialBalance = cursor.getDouble(cursor.getColumnIndexOrThrow("initial_balance"));
        item.colorHex = cursor.getString(cursor.getColumnIndexOrThrow("color_hex"));
        item.isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1;
        item.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        return item;
    }

    private TransactionEntity mapTransactionEntity(Cursor cursor) {
        TransactionEntity item = new TransactionEntity();
        item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        item.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        item.description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        item.amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
        item.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        item.categoryId = cursor.getLong(cursor.getColumnIndexOrThrow("category_id"));
        item.transactionDate = cursor.getString(cursor.getColumnIndexOrThrow("transaction_date"));
        item.dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
        item.paidDate = cursor.getString(cursor.getColumnIndexOrThrow("paid_date"));
        item.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
        int recurrenceIndex = cursor.getColumnIndex("recurrence_id");
        item.recurrenceId = cursor.isNull(recurrenceIndex) ? null : cursor.getLong(recurrenceIndex);
        int accountIdIndex = cursor.getColumnIndex("account_id");
        item.accountId = accountIdIndex >= 0 && !cursor.isNull(accountIdIndex) ? cursor.getLong(accountIdIndex) : null;
        int destinationIndex = cursor.getColumnIndex("destination_account_id");
        item.destinationAccountId = destinationIndex >= 0 && !cursor.isNull(destinationIndex) ? cursor.getLong(destinationIndex) : null;
        int accountIndex = cursor.getColumnIndex("account_name");
        item.accountName = accountIndex >= 0 ? cursor.getString(accountIndex) : null;
        item.notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
        item.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        item.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
        return item;
    }

    private TransactionListItem mapTransactionListItem(Cursor cursor) {
        TransactionListItem item = new TransactionListItem();
        item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        item.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        item.description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        item.amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
        item.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        item.transactionDate = cursor.getString(cursor.getColumnIndexOrThrow("transaction_date"));
        item.dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
        item.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
        int accountIndex = cursor.getColumnIndex("account_name");
        item.accountName = accountIndex >= 0 ? cursor.getString(accountIndex) : null;
        int destinationIndex = cursor.getColumnIndex("destination_account_name");
        item.destinationAccountName = destinationIndex >= 0 ? cursor.getString(destinationIndex) : null;
        item.notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
        int recurrenceIndex = cursor.getColumnIndex("recurrence_id");
        item.recurrenceId = cursor.isNull(recurrenceIndex) ? null : cursor.getLong(recurrenceIndex);
        item.categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
        item.categoryColor = cursor.getString(cursor.getColumnIndexOrThrow("category_color"));
        return item;
    }

    private RecurrenceEntity mapRecurrenceEntity(Cursor cursor) {
        RecurrenceEntity item = new RecurrenceEntity();
        item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        item.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        item.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        item.amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
        item.categoryId = cursor.getLong(cursor.getColumnIndexOrThrow("category_id"));
        item.frequency = cursor.getString(cursor.getColumnIndexOrThrow("frequency"));
        item.intervalValue = cursor.getInt(cursor.getColumnIndexOrThrow("interval_value"));
        item.startDate = cursor.getString(cursor.getColumnIndexOrThrow("start_date"));
        item.endDate = cursor.getString(cursor.getColumnIndexOrThrow("end_date"));
        int maxIndex = cursor.getColumnIndex("max_occurrences");
        item.maxOccurrences = cursor.isNull(maxIndex) ? null : cursor.getInt(maxIndex);
        item.isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1;
        int accountIndex = cursor.getColumnIndex("account_name");
        item.accountName = accountIndex >= 0 ? cursor.getString(accountIndex) : null;
        item.notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
        item.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        return item;
    }

    private RecurrenceListItem mapRecurrenceListItem(Cursor cursor) {
        RecurrenceListItem item = new RecurrenceListItem();
        item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        item.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        item.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        item.amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
        item.frequency = cursor.getString(cursor.getColumnIndexOrThrow("frequency"));
        item.intervalValue = cursor.getInt(cursor.getColumnIndexOrThrow("interval_value"));
        item.startDate = cursor.getString(cursor.getColumnIndexOrThrow("start_date"));
        item.endDate = cursor.getString(cursor.getColumnIndexOrThrow("end_date"));
        item.isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1;
        item.notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
        item.categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
        item.categoryColor = cursor.getString(cursor.getColumnIndexOrThrow("category_color"));
        return item;
    }

    private <T> void post(DataCallback<T> callback, T data) {
        mainHandler.post(() -> callback.onResult(data));
    }

    private void postAction(ActionCallback callback) {
        mainHandler.post(callback::onComplete);
    }
}
