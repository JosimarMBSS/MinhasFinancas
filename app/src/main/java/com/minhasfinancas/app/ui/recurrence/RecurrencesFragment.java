package com.minhasfinancas.app.ui.recurrence;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.FragmentRecurrencesBinding;

public class RecurrencesFragment extends Fragment {

    private FragmentRecurrencesBinding binding;
    private RecurrenceAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRecurrencesBinding.inflate(inflater, container, false);
        adapter = new RecurrenceAdapter(item -> {
            Intent intent = new Intent(requireContext(), RecurrenceFormActivity.class);
            intent.putExtra(RecurrenceFormActivity.EXTRA_RECURRENCE_ID, item.id);
            startActivity(intent);
        });
        binding.recyclerRecurrences.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerRecurrences.setAdapter(adapter);
        binding.buttonGenerate.setOnClickListener(v -> FinanceRepository.getInstance(requireContext()).generateRecurrencesForCurrentMonth(() -> {
            Toast.makeText(requireContext(), "Lançamentos recorrentes gerados para o mês atual.", Toast.LENGTH_SHORT).show();
        }));
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        FinanceRepository.getInstance(requireContext()).getRecurrences(items -> {
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
