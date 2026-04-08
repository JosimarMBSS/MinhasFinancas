package com.minhasfinancas.app.ui.category;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.FragmentCategoriesBinding;

public class CategoriesFragment extends Fragment {

    private FragmentCategoriesBinding binding;
    private CategoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false);
        adapter = new CategoryAdapter(item -> {
            Intent intent = new Intent(requireContext(), CategoryFormActivity.class);
            intent.putExtra(CategoryFormActivity.EXTRA_CATEGORY_ID, item.id);
            startActivity(intent);
        });
        binding.recyclerCategories.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerCategories.setAdapter(adapter);
        binding.recyclerCategories.setHasFixedSize(true);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        FinanceRepository.getInstance(requireContext()).getAllCategories(items -> {
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
