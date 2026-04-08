package com.minhasfinancas.app.ui.recurrence;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.minhasfinancas.app.data.entity.CategoryEntity;
import com.minhasfinancas.app.data.entity.RecurrenceEntity;
import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.ActivityRecurrenceFormBinding;
import com.minhasfinancas.app.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class RecurrenceFormActivity extends AppCompatActivity {

    public static final String EXTRA_RECURRENCE_ID = "recurrence_id";

    private ActivityRecurrenceFormBinding binding;
    private final List<CategoryEntity> availableCategories = new ArrayList<>();
    private FinanceRepository repository;
    private long recurrenceId;
    private RecurrenceEntity editing;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecurrenceFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FinanceRepository.getInstance(this);
        recurrenceId = getIntent().getLongExtra(EXTRA_RECURRENCE_ID, 0L);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(recurrenceId == 0L ? "Nova recorrência" : "Editar recorrência");
        }

        binding.inputType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"Despesa", "Receita"}));
        binding.inputFrequency.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"Mensal", "Quinzenal", "Semanal", "Anual", "Parcelado", "Personalizado (dias)"}));
        binding.inputType.setText("Despesa", false);
        binding.inputFrequency.setText("Mensal", false);
        binding.inputInterval.setText("1");
        binding.inputStartDate.setText(DateUtils.todayIso());
        binding.switchActive.setChecked(true);

        binding.inputType.setOnItemClickListener((parent, view, position, id) -> loadCategories(position == 0 ? "EXPENSE" : "INCOME", 0L));
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> deleteCurrent());

        if (recurrenceId == 0L) {
            binding.buttonDelete.setEnabled(false);
            loadCategories("EXPENSE", 0L);
        } else {
            repository.getRecurrence(recurrenceId, entity -> {
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

    private void fillForm(RecurrenceEntity entity) {
        binding.inputTitle.setText(entity.title);
        binding.inputAmount.setText(String.valueOf(entity.amount));
        binding.inputType.setText("INCOME".equals(entity.type) ? "Receita" : "Despesa", false);
        binding.inputFrequency.setText(labelFrequency(entity.frequency), false);
        binding.inputInterval.setText(String.valueOf(entity.intervalValue <= 0 ? 1 : entity.intervalValue));
        binding.inputStartDate.setText(entity.startDate);
        binding.inputEndDate.setText(entity.endDate);
        binding.inputMaxOccurrences.setText(entity.maxOccurrences == null ? "" : String.valueOf(entity.maxOccurrences));
        binding.inputNotes.setText(entity.notes);
        binding.switchActive.setChecked(entity.isActive);
        loadCategories(entity.type, entity.categoryId);
    }

    private void loadCategories(String type, long selectedId) {
        repository.getCategoriesForType(type, categories -> {
            availableCategories.clear();
            availableCategories.addAll(categories);
            List<String> names = new ArrayList<>();
            int selectedIndex = 0;
            for (int i = 0; i < categories.size(); i++) {
                names.add(categories.get(i).name);
                if (categories.get(i).id == selectedId) {
                    selectedIndex = i;
                }
            }
            binding.inputCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
            if (!names.isEmpty()) {
                binding.inputCategory.setText(names.get(selectedIndex), false);
            }
        });
    }

    private void save() {
        String title = textOf(binding.inputTitle.getText());
        String amountText = textOf(binding.inputAmount.getText());
        String categoryName = textOf(binding.inputCategory.getText());
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(amountText) || TextUtils.isEmpty(categoryName)) {
            Toast.makeText(this, "Preencha título, valor e categoria.", Toast.LENGTH_SHORT).show();
            return;
        }

        CategoryEntity selectedCategory = null;
        for (CategoryEntity category : availableCategories) {
            if (category.name.equals(categoryName)) {
                selectedCategory = category;
                break;
            }
        }
        if (selectedCategory == null) {
            Toast.makeText(this, "Selecione uma categoria válida.", Toast.LENGTH_SHORT).show();
            return;
        }

        RecurrenceEntity entity = editing != null ? editing : new RecurrenceEntity();
        entity.title = title;
        entity.amount = Double.parseDouble(amountText.replace(',', '.'));
        entity.type = "Receita".contentEquals(binding.inputType.getText()) ? "INCOME" : "EXPENSE";
        entity.categoryId = selectedCategory.id;
        entity.frequency = mapFrequency(textOf(binding.inputFrequency.getText()));
        entity.intervalValue = parseInt(textOf(binding.inputInterval.getText()), 1);
        entity.startDate = textOf(binding.inputStartDate.getText());
        entity.endDate = emptyToNull(textOf(binding.inputEndDate.getText()));
        entity.maxOccurrences = textOf(binding.inputMaxOccurrences.getText()).isEmpty() ? null : parseInt(textOf(binding.inputMaxOccurrences.getText()), 0);
        entity.notes = textOf(binding.inputNotes.getText());
        entity.isActive = binding.switchActive.isChecked();
        entity.createdAt = editing != null ? editing.createdAt : System.currentTimeMillis();

        repository.saveRecurrence(entity, id -> {
            Toast.makeText(this, "Recorrência salva.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void deleteCurrent() {
        if (editing == null) return;
        repository.deleteRecurrence(editing, () -> {
            Toast.makeText(this, "Recorrência removida.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private String mapFrequency(String label) {
        if ("Semanal".equals(label)) return "WEEKLY";
        if ("Quinzenal".equals(label)) return "BIWEEKLY";
        if ("Anual".equals(label)) return "YEARLY";
        if ("Parcelado".equals(label)) return "INSTALLMENT";
        if (label != null && label.startsWith("Personalizado")) return "CUSTOM_DAYS";
        return "MONTHLY";
    }

    private String labelFrequency(String value) {
        if ("WEEKLY".equals(value)) return "Semanal";
        if ("BIWEEKLY".equals(value)) return "Quinzenal";
        if ("YEARLY".equals(value)) return "Anual";
        if ("INSTALLMENT".equals(value)) return "Parcelado";
        if ("CUSTOM_DAYS".equals(value)) return "Personalizado (dias)";
        return "Mensal";
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
