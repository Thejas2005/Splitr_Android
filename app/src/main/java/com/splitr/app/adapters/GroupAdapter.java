package com.splitr.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.splitr.app.R;
import com.splitr.app.models.Group;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.VH> {

    public interface OnGroupClick { void onClick(Group g); }

    private List<Group> data;
    private final OnGroupClick listener;

    public GroupAdapter(List<Group> data, OnGroupClick listener) {
        this.data     = data;
        this.listener = listener;
    }

    public void setData(List<Group> d) {
        this.data = d;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Group g = data.get(pos);
        h.tvName.setText(g.name != null ? g.name : "Group");
        h.tvId.setText("ID: " + g.getId());
        String initial = (g.name != null && !g.name.isEmpty())
                ? String.valueOf(g.name.charAt(0)).toUpperCase() : "G";
        h.tvIcon.setText(initial);
        h.itemView.setOnClickListener(v -> listener.onClick(g));
    }

    @Override public int getItemCount() { return data == null ? 0 : data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvId, tvIcon;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvGroupName);
            tvId   = v.findViewById(R.id.tvGroupId);
            tvIcon = v.findViewById(R.id.tvGroupIcon);
        }
    }
}
