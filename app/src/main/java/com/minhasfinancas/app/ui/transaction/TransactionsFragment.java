package com.minhasfinancas.app.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.minhasfinancas.app.data.model.TransactionListItem;
import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.FragmentTransactionsBinding;
import com.minhasfinancas.app.util.DateUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionsFragment extends Fragment {

    private FragmentTransactionsBinding binding;
    private TransactionAdapter adapter;
    private LocalDate selectedMonth;
    private final List<TransactionListItem> monthItems = new ArrayList<>();
    private String currentStatusFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        selectedMonth = LocalDate.now().withDayOfMonth(1);

        adapter = new TransactionAdapter(new TransactionAdapter.OnItemClickListener() {
            @Override
            public void onEdit(TransactionListItem item) {
                Intent intent = new Intent(requireContext(), TransactionFormActivity.class);
                intent.putExtra(TransactionFormActivity.EXTRA_TRANSACTION_ID, item.id);
                startActivity(intent);
            }

            @Override
            public void onDelete(TransactionListItem item) {
                confirmDelete(item);
            }

            @Override
            public void onQuickStatus(TransactionListItem item, View anchor) {
                showQuickStatusMenu(item, anchor);
            }
        }, true);

        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTransactions.setAdapter(adapter);

        binding.buttonPreviousMonth.setOnClickListener(v -> {
            selectedMonth = selectedMonth.minusMonths(1);
            loadMonth();
        });
        binding.buttonNextMonth.setOnClickListener(v -> {
            selectedMonth = selectedMonth.plusMonths(1);
            loadMonth();
        });
        binding.buttonFilter.setOnClickListener(v -> showFilterMenu());

        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { applyLocalFilters(); }
        });

        updateFilterIndicator();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMonth();
    }

    private void showFilterMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), binding.buttonFilter);
        popupMenu.getMenu().add(0, 1, 0, "Todos");
        popupMenu.getMenu().add(0, 2, 1, "Pago");
        popupMenu.getMenu().add(0, 3, 2, "A Pagar");
        popupMenu.getMenu().add(0, 4, 3, "Recebido");
        popupMenu.getMenu().add(0, 5, 4, "A Receber");
        popupMenu.setOnMenuItemClickListener(this::onFilterSelected);
        popupMenu.show();
    }

    private boolean onFilterSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == 2) {
            currentStatusFilter = "EXPENSE_PAID";
        } else if (id == 3) {
            currentStatusFilter = "EXPENSE_PENDING";
        } else if (id == 4) {
            currentStatusFilter = "INCOME_PAID";
        } else if (id == 5) {
            currentStatusFilter = "INCOME_PENDING";
        } else {
            currentStatusFilter = "ALL";
        }
        updateFilterIndicator();
        applyLocalFilters();
        return true;
    }

    private void loadMonth() {
        if (binding == null) {
            return;
        }
        binding.textMonth.setText(DateUtils.formatMonthYear(selectedMonth));
        String startDate = DateUtils.monthStartIso(selectedMonth);
        String endDate = DateUtils.monthEndIso(selectedMonth);
        FinanceRepository repository = FinanceRepository.getInstance(requireContext());
        repository.generateRecurrencesForPeriod(startDate, endDate, () ->
                repository.getTransactionsForPeriod(startDate, endDate, items -> {
                    monthItems.clear();
                    monthItems.addAll(items);
                    applyLocalFilters();
                })
        );
    }

    private void applyLocalFilters() {
        if (binding == null) {
            return;
        }
        String query = textOf(binding.inputSearch.getText()).toLowerCase(Locale.ROOT);
        List<TransactionListItem> filtered = new ArrayList<>();
        double income = 0d;
        double expense = 0d;

        for (TransactionListItem item : monthItems) {
            if (!matchesStatus(item)) {
                continue;
            }
            if (!query.isEmpty() && !matchesQuery(item, query)) {
                continue;
            }
            filtered.add(item);
            if ("INCOME".equals(item.type)) {
                income += item.amount;
            } else if ("EXPENSE".equals(item.type)) {
                expense += item.amount;
            }
        }

        filtered.sort((first, second) -> {
            int typeCompare = Integer.compare(typeOrder(first.type), typeOrder(second.type));
            if (typeCompare != 0) {
                return typeCompare;
            }
            String firstLabel = displayLabel(first);
            String secondLabel = displayLabel(second);
            return firstLabel.compareToIgnoreCase(secondLabel);
        });

        adapter.submitList(filtered);
        binding.textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.textIncomeTotal.setText(DateUtils.currency(income));
        binding.textExpenseTotal.setText(DateUtils.currency(expense));
        binding.textBalanceTotal.setText(DateUtils.currency(income - expense));
    }

    private int typeOrder(String type) {
        if ("EXPENSE".equals(type)) {
            return 0;
        }
        if ("INCOME".equals(type)) {
            return 1;
        }
        return 2;
    }

    private String displayLabel(TransactionListItem item) {
        String label = item.description != null && !item.description.trim().isEmpty() ? item.description : item.title;
        return label == null ? "" : label.trim();
    }

    private boolean matchesStatus(TransactionListItem item) {
        switch (currentStatusFilter) {
            case "EXPENSE_PAID":
                return "EXPENSE".equals(item.type) && "PAID".equals(item.status);
            case "EXPENSE_PENDING":
                return "EXPENSE".equals(item.type) && "PENDING".equals(item.status);
            case "INCOME_PAID":
                return "INCOME".equals(item.type) && "PAID".equals(item.status);
            case "INCOME_PENDING":
                return "INCOME".equals(item.type) && "PENDING".equals(item.status);
            default:
                return true;
        }
    }

    private void showQuickStatusMenu(TransactionListItem item, View anchor) {
        if ("PAID".equals(item.status)) {
            Toast.makeText(requireContext(), labelAlreadyDone(item.type), Toast.LENGTH_SHORT).show();
            return;
        }

        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add(labelQuickAction(item.type));
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            FinanceRepository.getInstance(requireContext()).updateTransactionStatus(item.id, "PAID", DateUtils.todayIso(), () -> {
                Toast.makeText(requireContext(), labelDone(item.type), Toast.LENGTH_SHORT).show();
                loadMonth();
            });
            return true;
        });
        popupMenu.show();
    }

    private void confirmDelete(TransactionListItem item) {
        FinanceRepository repository = FinanceRepository.getInstance(requireContext());
        if (item.recurrenceId != null) {
            String[] options = new String[]{"Somente este lançamento", "Este e próximos", "Toda a recorrência"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Excluir recorrência")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            repository.deleteRecurringOccurrence(item.recurrenceId, item.transactionDate, () -> {
                                Toast.makeText(requireContext(), "Ocorrência excluída.", Toast.LENGTH_SHORT).show();
                                loadMonth();
                            });
                        } else if (which == 1) {
                            repository.deleteRecurringFromDate(item.recurrenceId, item.transactionDate, () -> {
                                Toast.makeText(requireContext(), "Recorrência removida deste lançamento em diante.", Toast.LENGTH_SHORT).show();
                                loadMonth();
                            });
                        } else if (which == 2) {
                            repository.deleteRecurringSeries(item.recurrenceId, () -> {
                                Toast.makeText(requireContext(), "Recorrência excluída por completo.", Toast.LENGTH_SHORT).show();
                                loadMonth();
                            });
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Excluir lançamento")
                .setMessage("Deseja excluir este lançamento?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) ->
                        repository.deleteTransactionById(item.id, () -> {
                            Toast.makeText(requireContext(), "Lançamento excluído.", Toast.LENGTH_SHORT).show();
                            loadMonth();
                        }))
                .show();
    }

    private String labelQuickAction(String type) {
        if ("INCOME".equals(type)) {
            return "Marcar como recebido";
        }
        if ("TRANSFER".equals(type)) {
            return "Concluir transferência";
        }
        return "Marcar como pago";
    }

    private String labelDone(String type) {
        if ("INCOME".equals(type)) {
            return "Receita marcada como recebida.";
        }
        if ("TRANSFER".equals(type)) {
            return "Transferência concluída.";
        }
        return "Despesa marcada como paga.";
    }

    private String labelAlreadyDone(String type) {
        if ("INCOME".equals(type)) {
            return "Esta receita já foi recebida.";
        }
        if ("TRANSFER".equals(type)) {
            return "Esta transferência já está concluída.";
        }
        return "Esta despesa já foi paga.";
    }

    private void updateFilterIndicator() {
        if (binding == null) {
            return;
        }
        String label;
        switch (currentStatusFilter) {
            case "EXPENSE_PAID":
                label = "Pago";
                break;
            case "EXPENSE_PENDING":
                label = "A Pagar";
                break;
            case "INCOME_PAID":
                label = "Recebido";
                break;
            case "INCOME_PENDING":
                label = "A Receber";
                break;
            default:
                label = "Todos";
                break;
        }
        binding.textFilterIndicator.setText("Filtro: " + label);
    }

    private boolean matchesQuery(TransactionListItem item, String query) {
        return contains(item.description, query)
                || contains(item.title, query)
                || contains(item.accountName, query)
                || contains(item.destinationAccountName, query)
                || contains(item.categoryName, query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
