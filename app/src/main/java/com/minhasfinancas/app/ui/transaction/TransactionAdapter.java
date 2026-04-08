package com.minhasfinancas.app.ui.transaction;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minhasfinancas.app.data.model.TransactionListItem;
import com.minhasfinancas.app.databinding.ItemTransactionBinding;
import com.minhasfinancas.app.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onEdit(TransactionListItem item);
        default void onDelete(TransactionListItem item) { }
        default void onQuickStatus(TransactionListItem item, View anchor) { }
    }

    private final List<TransactionListItem> items = new ArrayList<>();
    private final OnItemClickListener listener;
    private final boolean inlineActions;
    private long expandedItemId = -1L;

    public TransactionAdapter(OnItemClickListener listener) {
        this(listener, true);
    }

    public TransactionAdapter(OnItemClickListener listener, boolean inlineActions) {
        this.listener = listener;
        this.inlineActions = inlineActions;
    }

    public void submitList(List<TransactionListItem> newItems) {
        items.clear();
        items.addAll(newItems);
        boolean stillExists = false;
        for (TransactionListItem item : newItems) {
            if (item.id == expandedItemId) {
                stillExists = true;
                break;
            }
        }
        if (!stillExists) {
            expandedItemId = -1L;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ItemTransactionBinding binding;

        ViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TransactionListItem item) {
            binding.textTitle.setText(item.description != null && !item.description.isEmpty() ? item.description : item.title);
            binding.textAmount.setText(formattedAmount(item));
            binding.textAmount.setTextColor(Color.parseColor(colorForType(item.type)));

            binding.textBadge.setText(letterForType(item.type));
            binding.textBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorForType(item.type))));

            boolean isPaid = "PAID".equals(item.status);
            binding.cardContent.setCardBackgroundColor(Color.parseColor(isPaid ? paidBackgroundForType(item.type) : "#FFFFFF"));
            binding.cardContent.setStrokeColor(Color.parseColor(isPaid ? paidStrokeForType(item.type) : "#D8E1E8"));

            boolean expanded = inlineActions && item.id == expandedItemId;
            binding.layoutActions.setVisibility(inlineActions ? View.VISIBLE : View.GONE);
            binding.layoutActions.setAlpha(expanded ? 1f : 0f);
            binding.cardContent.setTranslationX(expanded ? -dp(104) : 0f);

            binding.cardContent.setOnClickListener(v -> {
                if (!inlineActions) {
                    listener.onEdit(item);
                    return;
                }
                long previousId = expandedItemId;
                expandedItemId = expanded ? -1L : item.id;
                int previousIndex = indexOf(previousId);
                int currentPosition = getBindingAdapterPosition();
                if (previousIndex != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousIndex);
                }
                if (currentPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(currentPosition);
                }
            });

            binding.buttonEdit.setOnClickListener(v -> listener.onEdit(item));
            binding.buttonDelete.setOnClickListener(v -> listener.onDelete(item));
            binding.textBadge.setOnClickListener(v -> listener.onQuickStatus(item, binding.textBadge));
        }

        private float dp(int value) {
            return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    value,
                    binding.getRoot().getResources().getDisplayMetrics()
            );
        }
    }

    private int indexOf(long id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == id) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private String paidBackgroundForType(String type) {
        if ("TRANSFER".equals(type)) return "#EEF4FF";
        if ("INCOME".equals(type)) return "#ECFDF3";
        return "#FFF1F3";
    }

    private String paidStrokeForType(String type) {
        if ("TRANSFER".equals(type)) return "#BFD3FF";
        if ("INCOME".equals(type)) return "#B7E4C7";
        return "#F6C4CD";
    }

    private String formattedAmount(TransactionListItem item) {
        String prefix = "INCOME".equals(item.type) ? "+ " : "EXPENSE".equals(item.type) ? "- " : "↔ ";
        return prefix + DateUtils.currency(item.amount);
    }

    private String letterForType(String type) {
        if ("INCOME".equals(type)) return "R";
        if ("TRANSFER".equals(type)) return "T";
        return "D";
    }

    private String colorForType(String type) {
        if ("INCOME".equals(type)) return "#198754";
        if ("TRANSFER".equals(type)) return "#2F6DF6";
        return "#D9485F";
    }
}
