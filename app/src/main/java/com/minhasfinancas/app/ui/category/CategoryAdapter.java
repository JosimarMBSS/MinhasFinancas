package com.minhasfinancas.app.ui.category;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minhasfinancas.app.data.entity.CategoryEntity;
import com.minhasfinancas.app.databinding.ItemCategoryBinding;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(CategoryEntity item);
    }

    private final List<CategoryEntity> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public CategoryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CategoryEntity> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryBinding binding;

        ViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CategoryEntity item) {
            binding.textName.setText(item.name);
            binding.textType.setText(labelType(item.type));
            binding.textInitial.setText(item.name != null && !item.name.isEmpty() ? item.name.substring(0, 1).toUpperCase() : "C");
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(item.colorHex != null ? item.colorHex : "#546E7A"));
            binding.textInitial.setBackground(drawable);
            binding.textState.setText(item.isActive ? "Ativa" : "Inativa");
            binding.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    private String labelType(String type) {
        if ("INCOME".equals(type)) return "Receita";
        if ("BOTH".equals(type)) return "Receita e despesa";
        return "Despesa";
    }
}
