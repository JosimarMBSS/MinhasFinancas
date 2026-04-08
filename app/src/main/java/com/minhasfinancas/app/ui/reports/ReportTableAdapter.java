package com.minhasfinancas.app.ui.reports;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minhasfinancas.app.data.model.TransactionListItem;
import com.minhasfinancas.app.databinding.ItemReportRowBinding;
import com.minhasfinancas.app.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class ReportTableAdapter extends RecyclerView.Adapter<ReportTableAdapter.ViewHolder> {

    private final List<TransactionListItem> items = new ArrayList<>();

    public void submitList(List<TransactionListItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReportRowBinding binding = ItemReportRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReportRowBinding binding;

        ViewHolder(ItemReportRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TransactionListItem item) {
            String title = item.description != null && !item.description.trim().isEmpty() ? item.description : item.title;
            binding.textDescription.setText(title);
            binding.textAmount.setText(("INCOME".equals(item.type) ? "+ " : "- ") + DateUtils.currency(item.amount));
            binding.textAmount.setTextColor(Color.parseColor("INCOME".equals(item.type) ? "#198754" : "#D9485F"));
            binding.textStatus.setText(statusLabel(item));
            binding.getRoot().setBackgroundColor(Color.parseColor("PAID".equals(item.status) ? "#F0FAF3" : "#FFFFFF"));
        }

        private String statusLabel(TransactionListItem item) {
            if ("INCOME".equals(item.type)) {
                return "PAID".equals(item.status) ? "Recebido" : "A receber";
            }
            return "PAID".equals(item.status) ? "Pago" : "A pagar";
        }
    }
}
