package com.lumi.android.bicyclemap.ui.home;

import android.content.Context;
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

import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.util.ImageLoader;

public class RouteAdapter extends ListAdapter<CourseDto, RouteAdapter.ViewHolder> {
    public interface OnRouteClickListener { void onRouteClick(CourseDto route, int position); }
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
        CourseDto route = getItem(position);

        holder.title.setText(route.getTitle());
        holder.info.setText("약 " + route.getDist_km() + "km · " + route.getTime() + "분");

        boolean isBike = "bike".equalsIgnoreCase(route.getType() == null ? "" : route.getType().trim());

        // URL → ImageView (비어 있거나 실패 시 fallback 사용)
        String imgUrl = route.getImage();              // 서버에서 받은 절대 URL (null/"" 일 수 있음)

        int categoryFallback = isBike ? R.drawable.bike_route : R.drawable.walk_route;
        int localResId = resolveCourseImage(holder.itemView.getContext(), route, categoryFallback);

        if (imgUrl == null || imgUrl.trim().isEmpty()) {
            // URL이 없으면 즉시 기본 이미지 표시
            holder.image.setImageResource(localResId);
        } else {
            ImageLoader.load(holder.image.getContext(), imgUrl, holder.image, localResId);
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

    /** 코스ID로 drawable 리소스가 있으면 반환, 없으면 categoryFallback 반환 */
    private int resolveCourseImage(Context ctx, CourseDto route, int categoryFallback) {
        // ⚠️ drawable 파일 이름은 'route_<id>.png' 형태로 넣어주세요 (예: route_201.png)
        // 코스 ID 접근자 이름은 프로젝트에 맞춰 아래 중 하나를 쓰세요.
        // Integer id = route.getId(); // 또는
        Integer id = route.getCourse_id(); // ← 이게 없다면 위 라인으로 교체

        if (id != null) {
            String name = "l" + id; // res/drawable/route_201.png
            int resId = ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
            if (resId != 0) return resId;
        }
        return categoryFallback;
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

    public static final DiffUtil.ItemCallback<CourseDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<CourseDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull CourseDto oldItem, @NonNull CourseDto newItem) {
            return oldItem.getTitle().equals(newItem.getTitle());
        }

        @Override
        public boolean areContentsTheSame(@NonNull CourseDto oldItem, @NonNull CourseDto newItem) {
            return oldItem.getDist_km() == newItem.getDist_km() &&
                    oldItem.getTime() == newItem.getTime() &&
                    oldItem.getImage().equals(newItem.getImage());
        }
    };
}
