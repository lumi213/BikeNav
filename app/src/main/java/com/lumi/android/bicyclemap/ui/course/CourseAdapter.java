package com.lumi.android.bicyclemap.ui.course;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.Route;

import java.util.List;

public class CourseAdapter extends ListAdapter<Route, CourseAdapter.CourseViewHolder> {

    private final MainViewModel viewModel;
    private OnCourseClickListener listener;

    public interface OnCourseClickListener {
        void onCourseClick(Route route);
    }

    public void setOnCourseClickListener(OnCourseClickListener listener) {
        this.listener = listener;
    }

    public CourseAdapter(MainViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Route route = getItem(position);

        holder.title.setText(route.name);
        holder.desc.setText("경로 " + route.distance + "km · " + route.time + "분 · " + (route.tourist_point != null && !route.tourist_point.isEmpty() ? route.tourist_point.get(0) : ""));
        holder.summary.setText(route.explanation != null ? route.explanation : "");

        // 해시태그 동적 추가
        holder.tags.removeAllViews();
        if (route.category != null) {
            for (String tag : route.category) {
                TextView tagView = new TextView(holder.tags.getContext());
                tagView.setText("#" + tag);
                tagView.setTextSize(12);
                tagView.setTextColor(0xFF555555);
                tagView.setPadding(0, 0, 16, 0);
                holder.tags.addView(tagView);
            }
        }

        int imageResId = holder.image.getContext().getResources()
                .getIdentifier(route.image.replace(".jpg", "").replace(".png", ""),
                        "drawable", holder.image.getContext().getPackageName());
        holder.image.setImageResource(imageResId);

        holder.itemView.setOnClickListener(v -> {
            viewModel.setSelectedRoute(route);
            if (listener != null) listener.onCourseClick(route);
        });
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, desc, summary;
        LinearLayout tags;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.course_image);
            title = itemView.findViewById(R.id.course_title);
            desc = itemView.findViewById(R.id.course_desc);
            summary = itemView.findViewById(R.id.course_summary);
            tags = itemView.findViewById(R.id.course_tags);
        }
    }

    public static final DiffUtil.ItemCallback<Route> DIFF_CALLBACK = new DiffUtil.ItemCallback<Route>() {
        @Override
        public boolean areItemsTheSame(@NonNull Route oldItem, @NonNull Route newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Route oldItem, @NonNull Route newItem) {
            return oldItem.name.equals(newItem.name)
                    && oldItem.image.equals(newItem.image)
                    && oldItem.distance == newItem.distance
                    && oldItem.time == newItem.time
                    && oldItem.category.equals(newItem.category);
        }
    };
}
