package com.minhasfinancas.app.util;

import android.content.Context;

import com.minhasfinancas.app.data.db.AppDatabase;
import com.minhasfinancas.app.data.entity.CategoryEntity;

import java.util.concurrent.Executors;

public final class DatabaseSeeder {

    private DatabaseSeeder() {}

    public static void seed(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            if (db.countCategories() == 0) {
                insertCategory(db, "Receitas", "INCOME", "#198754", "payments");
                insertCategory(db, "Despesas", "EXPENSE", "#D9485F", "receipt_long");
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
}
