package com.minhasfinancas.app.ui.transaction;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.minhasfinancas.app.R;
import com.minhasfinancas.app.data.entity.AccountEntity;
import com.minhasfinancas.app.data.entity.CategoryEntity;
import com.minhasfinancas.app.data.entity.RecurrenceEntity;
import com.minhasfinancas.app.data.entity.TransactionEntity;
import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.ActivityTransactionFormBinding;
import com.minhasfinancas.app.util.DateUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionFormActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = "transaction_id";

    private ActivityTransactionFormBinding binding;
    private final List<CategoryEntity> availableCategories = new ArrayList<>();
    private final List<AccountEntity> availableAccounts = new ArrayList<>();
    private FinanceRepository repository;
    private long transactionId;
    private TransactionEntity editing;
    private RecurrenceEntity editingRecurrence;
    private String currentType = "EXPENSE";

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

        binding.inputDate.setText(DateUtils.todayDisplay());
        binding.inputRecurrenceType.setText("Mensal", false);
        binding.inputInstallments.setText("2");
        setTransactionType("EXPENSE", true);

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

        repository.getActiveAccounts(accounts -> {
            availableAccounts.clear();
            availableAccounts.addAll(accounts);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < accounts.size(); i++) {
                names.add(accounts.get(i).name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
            binding.inputAccount.setAdapter(adapter);
            binding.inputDestinationAccount.setAdapter(adapter);
            applyEditingAccounts();
        });
    }

    private void setupInteractions() {
        binding.buttonExpense.setOnClickListener(v -> setTransactionType("EXPENSE", true));
        binding.buttonIncome.setOnClickListener(v -> setTransactionType("INCOME", true));
        binding.buttonTransfer.setOnClickListener(v -> setTransactionType("TRANSFER", false));
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

    private void setTransactionType(String type, boolean reloadCategories) {
        currentType = type;
        if ("INCOME".equals(type)) {
            binding.toggleType.check(R.id.button_income);
            binding.checkboxPaid.setText("Marcar como recebido");
        } else if ("TRANSFER".equals(type)) {
            binding.toggleType.check(R.id.button_transfer);
            binding.checkboxPaid.setText("Concluir transferência agora");
        } else {
            binding.toggleType.check(R.id.button_expense);
            binding.checkboxPaid.setText("Marcar como pago");
        }
        applyTypeButtonStyles();
        if (reloadCategories) {
            binding.inputCategory.setText("", false);
            loadCategories(type, 0L);
        }
        updateDynamicSections();
    }

    private void fillForm(TransactionEntity entity) {
        binding.inputDescription.setText(entity.description);
        binding.inputAmount.setText(String.valueOf(entity.amount));
        binding.inputDate.setText(DateUtils.isoToDisplay(entity.transactionDate));
        binding.checkboxPaid.setChecked("PAID".equals(entity.status));

        if ("TRANSFER".equals(entity.type)) {
            setTransactionType("TRANSFER", false);
            selectAccount(entity.accountId, binding.inputAccount);
            selectAccount(entity.destinationAccountId, binding.inputDestinationAccount);
            binding.checkboxRecurrence.setChecked(false);
            updateDynamicSections();
            applyEditingAccounts();
            return;
        }

        setTransactionType(entity.type, false);
        loadCategories(entity.type, entity.categoryId);
        applyEditingAccounts();

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


    private void applyEditingAccounts() {
        if (editing == null || availableAccounts.isEmpty()) {
            return;
        }
        selectAccount(editing.accountId, binding.inputAccount);
        selectAccount(editing.destinationAccountId, binding.inputDestinationAccount);
    }

    private void selectAccount(Long accountId, MaterialAutoCompleteTextView targetView) {
        if (accountId == null) {
            return;
        }
        for (AccountEntity account : availableAccounts) {
            if (account.id == accountId) {
                targetView.setText(account.name, false);
                return;
            }
        }
    }

    private void updateDynamicSections() {
        boolean isTransfer = "TRANSFER".equals(currentType);
        boolean isRecurring = binding.checkboxRecurrence.isChecked() && !isTransfer;
        binding.layoutCategory.setVisibility(isTransfer ? android.view.View.GONE : android.view.View.VISIBLE);
        binding.layoutDestinationAccount.setVisibility(isTransfer ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.checkboxRecurrence.setVisibility(isTransfer ? android.view.View.GONE : android.view.View.VISIBLE);
        binding.layoutRecurrence.setVisibility(isRecurring ? android.view.View.VISIBLE : android.view.View.GONE);
        boolean isInstallment = "Parcelado".contentEquals(binding.inputRecurrenceType.getText());
        binding.inputLayoutInstallments.setVisibility(isRecurring && isInstallment ? android.view.View.VISIBLE : android.view.View.GONE);
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
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
            binding.inputCategory.setAdapter(adapter);
            if (!names.isEmpty()) {
                binding.inputCategory.setText(names.get(selectedIndex), false);
            }
        });
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
        String date = textOf(binding.inputDate.getText());
        String dateIso = DateUtils.displayToIso(date);

        if (TextUtils.isEmpty(description) || TextUtils.isEmpty(amountText) || TextUtils.isEmpty(dateIso)) {
            Toast.makeText(this, "Preencha descrição, valor e data.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText.replace(',', '.'));
        } catch (Exception ex) {
            Toast.makeText(this, "Informe um valor válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        AccountEntity sourceAccount = findAccountByName(textOf(binding.inputAccount.getText()));
        if (sourceAccount == null) {
            Toast.makeText(this, "Selecione uma conta válida.", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("TRANSFER".equals(currentType)) {
            saveTransfer(description, amount, dateIso, sourceAccount);
            return;
        }

        String categoryName = textOf(binding.inputCategory.getText());
        if (TextUtils.isEmpty(categoryName)) {
            Toast.makeText(this, "Selecione uma categoria válida.", Toast.LENGTH_SHORT).show();
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

        TransactionEntity entity = editing != null ? editing : new TransactionEntity();
        entity.title = description;
        entity.description = description;
        entity.amount = amount;
        entity.type = currentType;
        entity.categoryId = selectedCategory.id;
        entity.transactionDate = dateIso;
        entity.dueDate = dateIso;
        entity.accountId = sourceAccount.id;
        entity.destinationAccountId = null;
        entity.accountName = sourceAccount.name;
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
    }

    private void saveTransfer(String description, double amount, String date, AccountEntity sourceAccount) {
        AccountEntity destinationAccount = findAccountByName(textOf(binding.inputDestinationAccount.getText()));
        if (destinationAccount == null) {
            Toast.makeText(this, "Selecione a conta de destino.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destinationAccount.id == sourceAccount.id) {
            Toast.makeText(this, "Origem e destino precisam ser diferentes.", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.getOrCreateTransferCategoryId(categoryId -> {
            TransactionEntity entity = editing != null ? editing : new TransactionEntity();
            entity.title = description;
            entity.description = description;
            entity.amount = amount;
            entity.type = "TRANSFER";
            entity.categoryId = categoryId;
            entity.transactionDate = date;
            entity.dueDate = date;
            entity.accountId = sourceAccount.id;
            entity.destinationAccountId = destinationAccount.id;
            entity.accountName = sourceAccount.name;
            entity.paidDate = binding.checkboxPaid.isChecked() ? date : null;
            entity.status = binding.checkboxPaid.isChecked() ? "PAID" : "PENDING";
            entity.recurrenceId = null;
            entity.notes = null;
            entity.createdAt = editing != null ? editing.createdAt : System.currentTimeMillis();
            entity.updatedAt = System.currentTimeMillis();
            repository.saveTransaction(entity, id -> finishSave());
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
        recurrence.accountName = entity.accountName;
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
        styleTypeButton(binding.buttonTransfer, "TRANSFER".equals(currentType), 0xFF2F6DF6);
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

    private AccountEntity findAccountByName(String name) {
        for (AccountEntity account : availableAccounts) {
            if (account.name.equals(name)) {
                return account;
            }
        }
        return null;
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
