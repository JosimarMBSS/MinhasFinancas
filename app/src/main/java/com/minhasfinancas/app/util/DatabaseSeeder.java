package com.minhasfinancas.app.util;

import android.content.Context;

import com.minhasfinancas.app.data.db.AppDatabase;
import com.minhasfinancas.app.data.entity.AccountEntity;
import com.minhasfinancas.app.data.entity.CategoryEntity;

import java.util.concurrent.Executors;

public final class DatabaseSeeder {

    private DatabaseSeeder() {}

    public static void seed(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);

            if (db.countCategories() == 0) {
                insertCategory(db, "Salário", "INCOME", "#2E7D32", "payments");
                insertCategory(db, "Freelance", "INCOME", "#1565C0", "payments");
                insertCategory(db, "Investimentos", "INCOME", "#6A1B9A", "savings");
                insertCategory(db, "Alimentação", "EXPENSE", "#EF6C00", "restaurant");
                insertCategory(db, "Transporte", "EXPENSE", "#00897B", "directions_car");
                insertCategory(db, "Moradia", "EXPENSE", "#5D4037", "home");
                insertCategory(db, "Saúde", "EXPENSE", "#C62828", "favorite");
                insertCategory(db, "Educação", "EXPENSE", "#283593", "school");
                insertCategory(db, "Lazer", "EXPENSE", "#AD1457", "sports_esports");
                insertCategory(db, "Contas", "EXPENSE", "#455A64", "receipt_long");
                insertCategory(db, "Transferência", "BOTH", "#0F766E", "swap_horiz");
                insertCategory(db, "Outros", "BOTH", "#546E7A", "category");
            }

            if (db.countAccounts() == 0) {
                insertAccount(db, "Carteira", "DINHEIRO", 0d, "#0F766E");
                insertAccount(db, "Conta corrente", "BANCO", 0d, "#1565C0");
            }
        });
    }

    private static void insertCategory(AppDatabase db, String name, String type, String color, String icon) {
        CategoryEntity entity = new CategoryEntity();
        entity.name = name;
        entity.type = type;
        entity.colorHex = color;
        entity.iconName = icon;
        entity.isDefault = true;
        entity.isActive = true;
        entity.createdAt = System.currentTimeMillis();
        db.insertCategory(entity);
    }

    private static void insertAccount(AppDatabase db, String name, String type, double initialBalance, String color) {
        AccountEntity entity = new AccountEntity();
        entity.name = name;
        entity.type = type;
        entity.initialBalance = initialBalance;
        entity.colorHex = color;
        entity.isActive = true;
        entity.createdAt = System.currentTimeMillis();
        db.insertAccount(entity);
    }
}
