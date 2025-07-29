package com.lumi.android.bicyclemap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RouteAdapter extends ListAdapter<Route, RouteAdapter.ViewHolder> {

    private final MainViewModel viewModel;
    private RecyclerView recyclerView;

    public RouteAdapter(MainViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (recyclerView == null && parent instanceof RecyclerView) {
            recyclerView = (RecyclerView) parent;
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Route route = getItem(position);

        holder.title.setText(route.title);
        holder.info.setText("약 " + route.dist_km + "km · " + route.time + "분");

        int imageResId = holder.image.getContext().getResources()
                .getIdentifier(route.image.replace(".jpg", "").replace(".png", ""),
                        "drawable", holder.image.getContext().getPackageName());
        holder.image.setImageResource(imageResId);

        holder.itemView.setOnClickListener(v -> {
            viewModel.setSelectedRoute(route);

            if (recyclerView != null) {
                int pos = holder.getAbsoluteAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                    if (layoutManager instanceof LinearLayoutManager) {
                        LinearLayoutManager llm = (LinearLayoutManager) layoutManager;
                        View viewAtPos = llm.findViewByPosition(pos);

                        if (viewAtPos != null) {
                            int itemCenter = viewAtPos.getLeft() + viewAtPos.getWidth() / 2;
                            int recyclerCenter = recyclerView.getWidth() / 2;
                            int scrollBy = itemCenter - recyclerCenter;

                            // 💡 중앙 정렬만 수행, 무한 순환 없음
                            recyclerView.smoothScrollBy(scrollBy, 0);
                        }
                    }
                }
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, info;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
            info = itemView.findViewById(R.id.info);
        }
    }

    public static final DiffUtil.ItemCallback<Route> DIFF_CALLBACK = new DiffUtil.ItemCallback<Route>() {
        @Override
        public boolean areItemsTheSame(@NonNull Route oldItem, @NonNull Route newItem) {
            return oldItem.title.equals(newItem.title);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Route oldItem, @NonNull Route newItem) {
            return oldItem.dist_km == newItem.dist_km &&
                    oldItem.time == newItem.time &&
                    oldItem.image.equals(newItem.image);
        }
    };
}
