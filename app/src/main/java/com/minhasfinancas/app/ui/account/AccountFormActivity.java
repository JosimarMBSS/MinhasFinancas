package com.minhasfinancas.app.ui.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.minhasfinancas.app.R;
import com.minhasfinancas.app.data.entity.AccountEntity;
import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.ActivityAccountFormBinding;

public class AccountFormActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_ID = "account_id";

    private ActivityAccountFormBinding binding;
    private FinanceRepository repository;
    private AccountEntity editing;
    private long accountId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FinanceRepository.getInstance(this);
        accountId = getIntent().getLongExtra(EXTRA_ACCOUNT_ID, 0L);

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.top_app_bar_on));
        if (binding.toolbar.getNavigationIcon() != null) {
            binding.toolbar.getNavigationIcon().setTint(ContextCompat.getColor(this, R.color.top_app_bar_on));
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(accountId == 0L ? "Nova conta" : "Editar conta");
        }

        binding.inputType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new String[]{"Banco", "Dinheiro", "Poupança", "Cartão"}));
        binding.inputType.setText("Banco", false);
        binding.inputColor.setText("#0F766E");
        binding.switchActive.setChecked(true);

        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> deleteCurrent());

        if (accountId == 0L) {
            binding.buttonDelete.setEnabled(false);
        } else {
            repository.getAccount(accountId, entity -> {
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

    private void fillForm(AccountEntity entity) {
        binding.inputName.setText(entity.name);
        binding.inputType.setText(labelType(entity.type), false);
        binding.inputInitialBalance.setText(String.valueOf(entity.initialBalance));
        binding.inputColor.setText(entity.colorHex);
        binding.switchActive.setChecked(entity.isActive);
    }

    private void save() {
        String name = textOf(binding.inputName.getText());
        String initialBalanceText = textOf(binding.inputInitialBalance.getText());

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Informe o nome da conta.", Toast.LENGTH_SHORT).show();
            return;
        }

        double initialBalance;
        try {
            initialBalance = initialBalanceText.isEmpty() ? 0d : Double.parseDouble(initialBalanceText.replace(',', '.'));
        } catch (Exception ex) {
            Toast.makeText(this, "Informe um saldo inicial válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        AccountEntity entity = editing != null ? editing : new AccountEntity();
        entity.name = name;
        entity.type = mapType(textOf(binding.inputType.getText()));
        entity.initialBalance = initialBalance;
        entity.colorHex = textOf(binding.inputColor.getText()).isEmpty() ? "#0F766E" : textOf(binding.inputColor.getText());
        entity.isActive = binding.switchActive.isChecked();
        entity.createdAt = editing != null ? editing.createdAt : System.currentTimeMillis();

        repository.saveAccount(entity, id -> {
            Toast.makeText(this, "Conta salva.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void deleteCurrent() {
        if (editing == null) {
            return;
        }
        repository.deleteAccount(editing, () -> {
            Toast.makeText(this, "Conta removida.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private String mapType(String label) {
        if ("Dinheiro".equals(label)) return "DINHEIRO";
        if ("Poupança".equals(label)) return "POUPANCA";
        if ("Cartão".equals(label)) return "CARTAO";
        return "BANCO";
    }

    private String labelType(String type) {
        if ("DINHEIRO".equals(type)) return "Dinheiro";
        if ("POUPANCA".equals(type)) return "Poupança";
        if ("CARTAO".equals(type)) return "Cartão";
        return "Banco";
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
