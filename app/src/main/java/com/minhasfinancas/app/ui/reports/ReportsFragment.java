package com.minhasfinancas.app.ui.reports;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.ChipGroup;
import com.minhasfinancas.app.R;
import com.minhasfinancas.app.data.model.ReportFilter;
import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.FragmentReportsBinding;
import com.minhasfinancas.app.ui.transaction.TransactionAdapter;
import com.minhasfinancas.app.util.DateUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private TransactionAdapter adapter;
    private LocalDate selectedMonth;
    private String currentTypeFilter = "ALL";
    private String currentStatusFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        adapter = new TransactionAdapter(item -> { }, false);
        selectedMonth = LocalDate.now().withDayOfMonth(1);

        binding.recyclerReport.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerReport.setAdapter(adapter);

        setupCategoryFilter();
        setupChips();

        binding.buttonPreviousMonth.setOnClickListener(v -> {
            selectedMonth = selectedMonth.minusMonths(1);
            loadReport();
        });
        binding.buttonNextMonth.setOnClickListener(v -> {
            selectedMonth = selectedMonth.plusMonths(1);
            loadReport();
        });
        binding.buttonClearFilters.setOnClickListener(v -> resetFilters());
        binding.inputCategoryFilter.setOnItemClickListener((parent, view, position, id) -> loadReport());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReport();
    }

    private void setupCategoryFilter() {
        FinanceRepository.getInstance(requireContext()).getAllCategories(categories -> {
            List<String> names = new ArrayList<>();
            names.add("Todas");
            for (int i = 0; i < categories.size(); i++) {
                names.add(categories.get(i).name);
            }
            binding.inputCategoryFilter.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, names));
            if (textOf(binding.inputCategoryFilter.getText()).isEmpty()) {
                binding.inputCategoryFilter.setText("Todas", false);
            }
        });
    }

    private void setupChips() {
        binding.chipGroupType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentTypeFilter = "ALL";
            } else if (checkedIds.get(0) == R.id.chip_type_income) {
                currentTypeFilter = "INCOME";
            } else if (checkedIds.get(0) == R.id.chip_type_expense) {
                currentTypeFilter = "EXPENSE";
            } else if (checkedIds.get(0) == R.id.chip_type_transfer) {
                currentTypeFilter = "TRANSFER";
            } else {
                currentTypeFilter = "ALL";
            }
            loadReport();
        });

        binding.chipGroupStatus.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                if (checkedIds.isEmpty()) {
                    currentStatusFilter = "ALL";
                } else if (checkedIds.get(0) == R.id.chip_report_status_paid) {
                    currentStatusFilter = "PAID";
                } else if (checkedIds.get(0) == R.id.chip_report_status_pending) {
                    currentStatusFilter = "PENDING";
                } else {
                    currentStatusFilter = "ALL";
                }
                loadReport();
            }
        });
    }

    private void resetFilters() {
        currentTypeFilter = "ALL";
        currentStatusFilter = "ALL";
        binding.chipTypeAll.setChecked(true);
        binding.chipReportStatusAll.setChecked(true);
        binding.inputCategoryFilter.setText("Todas", false);
        loadReport();
    }

    private void loadReport() {
        if (binding == null) {
            return;
        }

        binding.textReportMonth.setText(DateUtils.formatMonthYear(selectedMonth));
        ReportFilter filter = new ReportFilter();
        filter.startDate = DateUtils.monthStartIso(selectedMonth);
        filter.endDate = DateUtils.monthEndIso(selectedMonth);
        filter.type = currentTypeFilter;
        filter.status = currentStatusFilter;
        filter.categoryName = textOf(binding.inputCategoryFilter.getText()).isEmpty() ? "Todas" : textOf(binding.inputCategoryFilter.getText());

        FinanceRepository repo = FinanceRepository.getInstance(requireContext());
        repo.generateRecurrencesForPeriod(filter.startDate, filter.endDate, () -> {
            repo.getTransactionsFiltered(filter, items -> {
                adapter.submitList(items);
                binding.textEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
            repo.getReportSummary(filter, summary -> {
                binding.textIncomeTotal.setText(DateUtils.currency(summary.income));
                binding.textExpenseTotal.setText(DateUtils.currency(summary.expense));
                binding.textBalanceTotal.setText(DateUtils.currency(summary.getBalance()));
                binding.textBalanceTotal.setTextColor(Color.parseColor(summary.getBalance() >= 0 ? "#198754" : "#D9485F"));
                binding.textCount.setText(summary.count + " lançamento(s)");
            });
        });
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
