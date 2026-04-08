package com.minhasfinancas.app.ui.account;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minhasfinancas.app.data.model.AccountListItem;
import com.minhasfinancas.app.databinding.ItemAccountBinding;
import com.minhasfinancas.app.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(AccountListItem item);
    }

    private final List<AccountListItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public AccountAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AccountListItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAccountBinding binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ItemAccountBinding binding;

        ViewHolder(ItemAccountBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AccountListItem item) {
            binding.textName.setText(item.name);
            binding.textType.setText(labelType(item.type));
            binding.textBalance.setText(DateUtils.currency(item.currentBalance));
            binding.textInitial.setText(item.name != null && !item.name.isEmpty() ? item.name.substring(0, 1).toUpperCase() : "C");
            binding.textState.setText(item.isActive ? "Ativa" : "Inativa");
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(item.colorHex != null ? item.colorHex : "#0F766E"));
            binding.textInitial.setBackground(drawable);
            binding.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    private String labelType(String type) {
        if ("CARTAO".equals(type)) return "Cartão";
        if ("POUPANCA".equals(type)) return "Poupança";
        if ("DINHEIRO".equals(type)) return "Dinheiro";
        return "Banco";
    }
}
