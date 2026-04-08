package com.minhasfinancas.app.ui.recurrence;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minhasfinancas.app.data.model.RecurrenceListItem;
import com.minhasfinancas.app.databinding.ItemRecurrenceBinding;
import com.minhasfinancas.app.util.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecurrenceAdapter extends RecyclerView.Adapter<RecurrenceAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(RecurrenceListItem item);
    }

    private final List<RecurrenceListItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public RecurrenceAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<RecurrenceListItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecurrenceBinding binding = ItemRecurrenceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ItemRecurrenceBinding binding;

        ViewHolder(ItemRecurrenceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RecurrenceListItem item) {
            binding.textTitle.setText(item.title);
            binding.textAmount.setText(("INCOME".equals(item.type) ? "+ " : "- ") + DateUtils.currency(item.amount));
            binding.textAmount.setTextColor(Color.parseColor("INCOME".equals(item.type) ? "#198754" : "#D9485F"));
            binding.textMeta.setText(item.categoryName + " • " + labelFrequency(item.frequency, item.intervalValue) + " • início " + DateUtils.formatIsoToHuman(item.startDate));
            binding.textState.setText(item.isActive ? "Ativa" : "Pausada");
            binding.textBadge.setText(item.title != null && !item.title.isEmpty() ? item.title.substring(0, 1).toUpperCase(Locale.ROOT) : "R");
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(item.categoryColor != null ? item.categoryColor : "#0F766E"));
            binding.textBadge.setBackground(drawable);
            binding.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    private String labelFrequency(String frequency, int interval) {
        if ("WEEKLY".equals(frequency)) return interval > 1 ? "a cada " + interval + " semanas" : "semanal";
        if ("BIWEEKLY".equals(frequency)) return "quinzenal";
        if ("YEARLY".equals(frequency)) return interval > 1 ? "a cada " + interval + " anos" : "anual";
        if ("CUSTOM_DAYS".equals(frequency)) return "a cada " + interval + " dias";
        if ("INSTALLMENT".equals(frequency)) return "parcelado";
        return interval > 1 ? "a cada " + interval + " meses" : "mensal";
    }
}
