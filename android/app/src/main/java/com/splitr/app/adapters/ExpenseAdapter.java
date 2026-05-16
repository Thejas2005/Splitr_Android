package com.splitr.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.splitr.app.R;
import com.splitr.app.models.Expense;

import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.VH> {

    public interface OnExpenseClick { void onClick(Expense e); }

    private List<Expense> data;
    private final OnExpenseClick listener;

    public ExpenseAdapter(List<Expense> data, OnExpenseClick listener) {
        this.data     = data;
        this.listener = listener;
    }

    public void setData(List<Expense> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Expense e = data.get(pos);
        h.tvDesc.setText(e.description != null ? e.description : "—");
        h.tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", e.amount));

        String meta = (e.groupId != null)
                ? "Group #" + e.groupId + " · " + (e.splitType != null ? e.splitType : "equal")
                : "Personal";
        h.tvMeta.setText(meta);

        if (e.latitude != null && e.longitude != null) {
            h.tvLoc.setText(String.format(Locale.getDefault(),
                    "📍 %.4f, %.4f", e.latitude, e.longitude));
            h.tvLoc.setVisibility(View.VISIBLE);
        } else {
            h.tvLoc.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(e));
    }

    @Override public int getItemCount() { return data == null ? 0 : data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDesc, tvAmount, tvMeta, tvLoc;
        VH(View v) {
            super(v);
            tvDesc   = v.findViewById(R.id.tvExpenseDesc);
            tvAmount = v.findViewById(R.id.tvExpenseAmount);
            tvMeta   = v.findViewById(R.id.tvExpenseMeta);
            tvLoc    = v.findViewById(R.id.tvExpenseLoc);
        }
    }
}
