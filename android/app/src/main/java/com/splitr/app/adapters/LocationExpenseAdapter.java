package com.splitr.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.splitr.app.R;
import com.splitr.app.models.LocationExpense;

import java.util.List;
import java.util.Locale;

/**
 * Used inside the BottomSheet cluster popup.
 * Shows each expense in the cluster with owner info and amounts.
 */
public class LocationExpenseAdapter extends RecyclerView.Adapter<LocationExpenseAdapter.VH> {

    private final List<LocationExpense> data;
    private final int currentUserId;

    public LocationExpenseAdapter(List<LocationExpense> data, int currentUserId) {
        this.data          = data;
        this.currentUserId = currentUserId;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cluster_expense, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        LocationExpense e    = data.get(pos);
        boolean         isMe = (e.userId == currentUserId);

        h.tvDesc.setText(e.description != null ? e.description : "—");
        h.tvTotal.setText(String.format(Locale.getDefault(), "₹%.2f", e.amount));
        h.tvType.setText(e.splitType != null ? e.splitType : "personal");

        if (isMe) {
            h.tvOwnership.setText("You paid");
            h.tvOwnership.setTextColor(h.itemView.getContext().getColor(R.color.blue));
            h.tvOwnership.setBackground(
                    h.itemView.getContext().getDrawable(R.drawable.bg_pill_blue));
        } else if (e.myAmount > 0) {
            h.tvOwnership.setText(String.format(Locale.getDefault(),
                    "You owe ₹%.2f", e.myAmount));
            h.tvOwnership.setTextColor(h.itemView.getContext().getColor(R.color.red));
            h.tvOwnership.setBackground(
                    h.itemView.getContext().getDrawable(R.drawable.bg_chip));
        } else {
            h.tvOwnership.setText("Others");
            h.tvOwnership.setTextColor(h.itemView.getContext().getColor(R.color.muted));
            h.tvOwnership.setBackground(
                    h.itemView.getContext().getDrawable(R.drawable.bg_pill_muted));
        }
    }

    @Override public int getItemCount() { return data == null ? 0 : data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDesc, tvTotal, tvOwnership, tvType;
        VH(View v) {
            super(v);
            tvDesc      = v.findViewById(R.id.tvClusterDesc);
            tvTotal     = v.findViewById(R.id.tvClusterTotal);
            tvOwnership = v.findViewById(R.id.tvClusterOwnership);
            tvType      = v.findViewById(R.id.tvClusterType);
        }
    }
}
