package com.minhasfinancas.app.ui.category;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.minhasfinancas.app.data.entity.CategoryEntity;
import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.ActivityCategoryFormBinding;

public class CategoryFormActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";

    private ActivityCategoryFormBinding binding;
    private FinanceRepository repository;
    private CategoryEntity editing;
    private long categoryId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FinanceRepository.getInstance(this);
        categoryId = getIntent().getLongExtra(EXTRA_CATEGORY_ID, 0L);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(categoryId == 0L ? "Nova categoria" : "Editar categoria");
        }

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"Despesa", "Receita", "Ambos"});
        binding.inputType.setAdapter(typeAdapter);
        binding.inputType.setText("Despesa", false);
        binding.inputColor.setText("#546E7A");
        binding.inputIcon.setText("category");
        binding.switchActive.setChecked(true);

        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> deleteCurrent());

        if (categoryId == 0L) {
            binding.buttonDelete.setEnabled(false);
        } else {
            repository.getCategory(categoryId, entity -> {
                editing = entity;
                if (entity != null) {
                    fillForm(entity);
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void fillForm(CategoryEntity entity) {
        binding.inputName.setText(entity.name);
        binding.inputType.setText(labelType(entity.type), false);
        binding.inputColor.setText(entity.colorHex);
        binding.inputIcon.setText(entity.iconName);
        binding.switchActive.setChecked(entity.isActive);
    }

    private void save() {
        String name = textOf(binding.inputName.getText());
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Informe o nome da categoria.", Toast.LENGTH_SHORT).show();
            return;
        }

        CategoryEntity entity = editing != null ? editing : new CategoryEntity();
        entity.name = name;
        entity.type = mapType(textOf(binding.inputType.getText()));
        entity.colorHex = textOf(binding.inputColor.getText());
        entity.iconName = textOf(binding.inputIcon.getText());
        entity.isActive = binding.switchActive.isChecked();
        entity.isDefault = editing != null && editing.isDefault;
        entity.createdAt = editing != null ? editing.createdAt : System.currentTimeMillis();

        repository.saveCategory(entity, id -> {
            Toast.makeText(this, "Categoria salva.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void deleteCurrent() {
        if (editing == null) {
            return;
        }
        repository.deleteCategory(editing, () -> {
            Toast.makeText(this, "Categoria removida.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private String mapType(String label) {
        if ("Receita".equals(label)) return "INCOME";
        if ("Ambos".equals(label)) return "BOTH";
        return "EXPENSE";
    }

    private String labelType(String type) {
        if ("INCOME".equals(type)) return "Receita";
        if ("BOTH".equals(type)) return "Ambos";
        return "Despesa";
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
