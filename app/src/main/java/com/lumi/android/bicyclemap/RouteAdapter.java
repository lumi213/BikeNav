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

import com.lumi.android.bicyclemap.util.ImageLoader;

public class RouteAdapter extends ListAdapter<Route, RouteAdapter.ViewHolder> {
    public interface OnRouteClickListener { void onRouteClick(Route route, int position); }
    private OnRouteClickListener listener;
    public void setOnRouteClickListener(OnRouteClickListener l) { this.listener = l; }
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

        boolean isBike = "bike".equalsIgnoreCase(route.type == null ? "" : route.type.trim());
        // 카테고리별 기본 이미지 결정
        int fallbackResId = isBike          // ← Route 모델에 boolean isBike 필드(또는 비슷한 구분값)가 있다고 가정
                ? R.drawable.bike_route
                : R.drawable.walk_route;

        // URL → ImageView (비어 있거나 실패 시 fallback 사용)
        String imgUrl = route.image;              // 서버에서 받은 절대 URL (null/"" 일 수 있음)

        if (imgUrl == null || imgUrl.trim().isEmpty()) {
            // URL이 없으면 즉시 기본 이미지 표시
            holder.image.setImageResource(fallbackResId);
        } else {
            ImageLoader.load(holder.image.getContext(), imgUrl, holder.image, fallbackResId);
        }

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
            if (listener != null) listener.onRouteClick(route, position);
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
