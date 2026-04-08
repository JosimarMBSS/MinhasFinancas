package com.minhasfinancas.app.ui.transaction;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.minhasfinancas.app.R;
import com.minhasfinancas.app.data.entity.RecurrenceEntity;
import com.minhasfinancas.app.data.entity.TransactionEntity;
import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.ActivityTransactionFormBinding;
import com.minhasfinancas.app.util.DateUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

public class TransactionFormActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = "transaction_id";

    private ActivityTransactionFormBinding binding;
    private FinanceRepository repository;
    private long transactionId;
    private TransactionEntity editing;
    private RecurrenceEntity editingRecurrence;
    private String currentType = "EXPENSE";
    private boolean isFormattingDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransactionFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FinanceRepository.getInstance(this);
        transactionId = getIntent().getLongExtra(EXTRA_TRANSACTION_ID, 0L);

        setupToolbar();
        setupDropdowns();
        setupInteractions();
        setupMasks();

        binding.inputDate.setText(DateUtils.todayDisplay());
        binding.inputRecurrenceType.setText("Mensal", false);
        binding.inputInstallments.setText("2");
        setTransactionType("EXPENSE");

        if (transactionId == 0L) {
            binding.buttonDelete.setEnabled(false);
            binding.buttonDelete.setVisibility(android.view.View.GONE);
        } else {
            repository.getTransaction(transactionId, entity -> {
                editing = entity;
                if (entity != null) {
                    fillForm(entity);
                }
            });
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.top_app_bar_on));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(transactionId == 0L ? "Novo lançamento" : "Editar lançamento");
        }
        if (binding.toolbar.getNavigationIcon() != null) {
            binding.toolbar.getNavigationIcon().setTint(ContextCompat.getColor(this, R.color.top_app_bar_on));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupDropdowns() {
        binding.inputRecurrenceType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new String[]{"Mensal", "Quinzenal", "Anual", "Parcelado"}));
    }

    private void setupInteractions() {
        binding.buttonExpense.setOnClickListener(v -> setTransactionType("EXPENSE"));
        binding.buttonIncome.setOnClickListener(v -> setTransactionType("INCOME"));
        binding.checkboxRecurrence.setOnCheckedChangeListener((buttonView, isChecked) -> updateDynamicSections());
        binding.inputRecurrenceType.setOnItemClickListener((parent, view, position, id) -> updateDynamicSections());
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> deleteCurrent());
        binding.inputDate.setOnClickListener(v -> showDatePicker());
        binding.inputDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker();
            }
        });
    }

    private void setupMasks() {
        binding.inputDate.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isFormattingDate) {
                    return;
                }
                isFormattingDate = true;
                String digits = editable.toString().replaceAll("\\D", "");
                if (digits.length() > 8) {
                    digits = digits.substring(0, 8);
                }
                StringBuilder value = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    value.append(digits.charAt(i));
                    if ((i == 1 || i == 3) && i < digits.length() - 1) {
                        value.append('/');
                    }
                }
                editable.replace(0, editable.length(), value.toString());
                isFormattingDate = false;
            }
        });

        binding.inputAmount.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String formatted = formatAmount(binding.inputAmount.getText() == null ? "" : binding.inputAmount.getText().toString());
                if (!formatted.isEmpty()) {
                    binding.inputAmount.setText(formatted);
                }
            }
        });
    }

    private void setTransactionType(String type) {
        currentType = type;
        if ("INCOME".equals(type)) {
            binding.toggleType.check(R.id.button_income);
            binding.checkboxPaid.setText("Marcar como recebido");
        } else {
            binding.toggleType.check(R.id.button_expense);
            binding.checkboxPaid.setText("Marcar como pago");
        }
        applyTypeButtonStyles();
        updateDynamicSections();
    }

    private void fillForm(TransactionEntity entity) {
        binding.inputDescription.setText(entity.description);
        binding.inputAmount.setText(formatAmount(String.valueOf(entity.amount)));
        binding.inputDate.setText(DateUtils.isoToDisplay(entity.transactionDate));
        binding.checkboxPaid.setChecked("PAID".equals(entity.status));
        setTransactionType(entity.type);

        if (entity.recurrenceId != null) {
            repository.getRecurrence(entity.recurrenceId, recurrence -> {
                editingRecurrence = recurrence;
                if (recurrence != null) {
                    binding.checkboxRecurrence.setChecked(true);
                    binding.inputRecurrenceType.setText(labelRecurrence(recurrence.frequency), false);
                    if ("INSTALLMENT".equals(recurrence.frequency) && recurrence.maxOccurrences != null) {
                        binding.inputInstallments.setText(String.valueOf(recurrence.maxOccurrences));
                    }
                    updateDynamicSections();
                }
            });
        } else {
            binding.checkboxRecurrence.setChecked(false);
            updateDynamicSections();
        }
    }

    private void updateDynamicSections() {
        boolean isRecurring = binding.checkboxRecurrence.isChecked();
        binding.layoutRecurrence.setVisibility(isRecurring ? android.view.View.VISIBLE : android.view.View.GONE);
        boolean isInstallment = "Parcelado".contentEquals(binding.inputRecurrenceType.getText());
        binding.inputLayoutInstallments.setVisibility(isRecurring && isInstallment ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void showDatePicker() {
        LocalDate initialDate = DateUtils.parseDisplayOrIsoOrToday(textOf(binding.inputDate.getText()));
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    LocalDate selected = LocalDate.of(year, month + 1, dayOfMonth);
                    binding.inputDate.setText(DateUtils.isoToDisplay(selected.toString()));
                },
                initialDate.getYear(),
                initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        );
        dialog.show();
    }

    private void save() {
        String description = textOf(binding.inputDescription.getText());
        String amountText = textOf(binding.inputAmount.getText());
        String dateDisplay = textOf(binding.inputDate.getText());
        String dateIso = DateUtils.displayToIso(dateDisplay);

        if (TextUtils.isEmpty(description) || TextUtils.isEmpty(amountText) || TextUtils.isEmpty(dateIso)) {
            Toast.makeText(this, "Preencha descrição, valor e data.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = parseAmount(amountText);
        } catch (Exception ex) {
            Toast.makeText(this, "Informe um valor válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.getOrCreateBaseCategoryId(currentType, categoryId -> {
            TransactionEntity entity = editing != null ? editing : new TransactionEntity();
            entity.title = description;
            entity.description = description;
            entity.amount = amount;
            entity.type = currentType;
            entity.categoryId = categoryId;
            entity.transactionDate = dateIso;
            entity.dueDate = dateIso;
            entity.accountId = null;
            entity.destinationAccountId = null;
            entity.accountName = null;
            entity.paidDate = binding.checkboxPaid.isChecked() ? dateIso : null;
            entity.status = binding.checkboxPaid.isChecked() ? "PAID" : "PENDING";
            entity.notes = null;
            entity.createdAt = editing != null ? editing.createdAt : System.currentTimeMillis();
            entity.updatedAt = System.currentTimeMillis();

            if (binding.checkboxRecurrence.isChecked()) {
                saveRecurringTransaction(entity);
                return;
            }

            if (editing != null && editing.recurrenceId != null) {
                repository.removeRecurrenceFromDate(editing.recurrenceId, editing.transactionDate, () -> {
                    entity.recurrenceId = null;
                    repository.saveTransaction(entity, id -> finishSave());
                });
            } else {
                entity.recurrenceId = null;
                repository.saveTransaction(entity, id -> finishSave());
            }
        });
    }

    private void saveRecurringTransaction(TransactionEntity entity) {
        String label = textOf(binding.inputRecurrenceType.getText());
        if (TextUtils.isEmpty(label)) {
            label = "Mensal";
        }

        RecurrenceEntity recurrence = editingRecurrence != null ? editingRecurrence : new RecurrenceEntity();
        recurrence.title = entity.description;
        recurrence.type = entity.type;
        recurrence.amount = entity.amount;
        recurrence.categoryId = entity.categoryId;
        recurrence.accountName = null;
        recurrence.startDate = entity.transactionDate;
        recurrence.endDate = null;
        recurrence.notes = null;
        recurrence.isActive = true;
        recurrence.createdAt = editingRecurrence != null ? editingRecurrence.createdAt : System.currentTimeMillis();
        recurrence.intervalValue = 1;
        recurrence.maxOccurrences = null;

        if ("Quinzenal".equals(label)) {
            recurrence.frequency = "BIWEEKLY";
        } else if ("Anual".equals(label)) {
            recurrence.frequency = "YEARLY";
        } else if ("Parcelado".equals(label)) {
            recurrence.frequency = "INSTALLMENT";
            recurrence.maxOccurrences = Math.max(parseInt(textOf(binding.inputInstallments.getText()), 2), 2);
        } else {
            recurrence.frequency = "MONTHLY";
        }

        repository.saveRecurrence(recurrence, recurrenceId -> {
            entity.recurrenceId = recurrenceId;
            repository.saveTransaction(entity, id -> finishSave());
        });
    }

    private void applyTypeButtonStyles() {
        styleTypeButton(binding.buttonExpense, "EXPENSE".equals(currentType), 0xFFD9485F);
        styleTypeButton(binding.buttonIncome, "INCOME".equals(currentType), 0xFF198754);
    }

    private void styleTypeButton(MaterialButton button, boolean selected, int color) {
        int textColor = selected ? 0xFFFFFFFF : color;
        button.setTextColor(textColor);
        button.setStrokeColor(ColorStateList.valueOf(color));
        button.setBackgroundTintList(ColorStateList.valueOf(selected ? color : 0x00000000));
    }

    private void finishSave() {
        Toast.makeText(this, "Lançamento salvo.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void deleteCurrent() {
        if (editing == null) {
            return;
        }
        if (editing.recurrenceId != null) {
            String[] options = new String[]{"Somente este lançamento", "Este e próximos", "Toda a recorrência"};
            new AlertDialog.Builder(this)
                    .setTitle("Excluir recorrência")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            repository.deleteRecurringOccurrence(editing.recurrenceId, editing.transactionDate, () -> finishDelete("Ocorrência removida."));
                        } else if (which == 1) {
                            repository.deleteRecurringFromDate(editing.recurrenceId, editing.transactionDate, () -> finishDelete("Recorrência removida deste lançamento em diante."));
                        } else if (which == 2) {
                            repository.deleteRecurringSeries(editing.recurrenceId, () -> finishDelete("Recorrência excluída por completo."));
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }
        repository.deleteTransaction(editing, () -> finishDelete("Lançamento removido."));
    }

    private void finishDelete(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String labelRecurrence(String frequency) {
        if ("BIWEEKLY".equals(frequency)) return "Quinzenal";
        if ("YEARLY".equals(frequency)) return "Anual";
        if ("INSTALLMENT".equals(frequency)) return "Parcelado";
        return "Mensal";
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double parseAmount(String amountText) {
        String normalized = amountText.replace("R$", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(',', '.');
        return Double.parseDouble(normalized);
    }

    private String formatAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        try {
            double value = parseAmount(raw);
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
            symbols.setDecimalSeparator(',');
            symbols.setGroupingSeparator('.');
            return new DecimalFormat("#,##0.00", symbols).format(value);
        } catch (Exception ex) {
            return raw;
        }
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
