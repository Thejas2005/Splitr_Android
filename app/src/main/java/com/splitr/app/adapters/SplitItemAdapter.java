package com.splitr.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.splitr.app.R;
import com.splitr.app.models.SplitItem;

import java.util.List;
import java.util.Locale;

public class SplitItemAdapter extends RecyclerView.Adapter<SplitItemAdapter.VH> {

    public interface OnSettle { void settle(SplitItem item); }

    private List<SplitItem> data;
    private final boolean isIOwe;   // true = "I Owe" tab; false = "Owed to Me" tab
    private final OnSettle listener;

    public SplitItemAdapter(List<SplitItem> data, boolean isIOwe, OnSettle listener) {
        this.data     = data;
        this.isIOwe   = isIOwe;
        this.listener = listener;
    }

    public void setData(List<SplitItem> d) {
        this.data = d;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_split, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SplitItem s = data.get(pos);

        h.tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", s.amount));
        h.tvDesc.setText(s.description != null ? s.description : "");

        if (isIOwe) {
            h.tvPerson.setText("→ " + (s.toUsername != null ? s.toUsername : "Unknown"));
            h.tvAmount.setTextColor(h.itemView.getContext().getColor(R.color.red));
            h.btnSettle.setText("Mark Paid");
        } else {
            String from = s.fromUsername != null ? s.fromUsername : "Guest";
            h.tvPerson.setText("← " + from + (s.isGuest ? " (guest)" : ""));
            h.tvAmount.setTextColor(h.itemView.getContext().getColor(R.color.green));
            h.btnSettle.setText("Mark Received");
        }

        h.btnSettle.setOnClickListener(v -> listener.settle(s));
    }

    @Override public int getItemCount() { return data == null ? 0 : data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAmount, tvPerson, tvDesc;
        Button   btnSettle;
        VH(View v) {
            super(v);
            tvAmount  = v.findViewById(R.id.tvSplitAmount);
            tvPerson  = v.findViewById(R.id.tvSplitPerson);
            tvDesc    = v.findViewById(R.id.tvSplitDesc);
            btnSettle = v.findViewById(R.id.btnSettle);
        }
    }
}
