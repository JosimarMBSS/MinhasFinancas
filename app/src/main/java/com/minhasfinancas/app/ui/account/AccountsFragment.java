package com.minhasfinancas.app.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.FragmentAccountsBinding;

public class AccountsFragment extends Fragment {

    private FragmentAccountsBinding binding;
    private AccountAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountsBinding.inflate(inflater, container, false);
        adapter = new AccountAdapter(item -> {
            Intent intent = new Intent(requireContext(), AccountFormActivity.class);
            intent.putExtra(AccountFormActivity.EXTRA_ACCOUNT_ID, item.id);
            startActivity(intent);
        });
        binding.recyclerAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAccounts.setAdapter(adapter);
        binding.recyclerAccounts.setHasFixedSize(true);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        FinanceRepository.getInstance(requireContext()).getAccounts(items -> {
            adapter.submitList(items);
            binding.textEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
