package com.minhasfinancas.app.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.minhasfinancas.app.data.model.DashboardSummary;
import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.FragmentDashboardBinding;
import com.minhasfinancas.app.ui.transaction.TransactionAdapter;
import com.minhasfinancas.app.util.DateUtils;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private TransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        adapter = new TransactionAdapter(item -> { });
        binding.recyclerUpcoming.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerUpcoming.setAdapter(adapter);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        FinanceRepository repo = FinanceRepository.getInstance(requireContext());
        repo.generateRecurrencesForDashboard();
        repo.getDashboardSummary(DateUtils.monthStartIso(), DateUtils.monthEndIso(), this::renderSummary);
        repo.getUpcomingTransactions(DateUtils.todayIso(), items -> {
            adapter.submitList(items);
            binding.textEmptyUpcoming.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            binding.textPendingCount.setText(String.valueOf(items.size()));
        });
    }

    private void renderSummary(DashboardSummary summary) {
        binding.textIncome.setText(DateUtils.currency(summary.income));
        binding.textExpense.setText(DateUtils.currency(summary.expense));
        binding.textBalance.setText(DateUtils.currency(summary.getBalance()));
        binding.textReference.setText(DateUtils.formatMonthYear(DateUtils.monthStartIso()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
