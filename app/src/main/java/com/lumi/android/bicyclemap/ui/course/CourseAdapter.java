package com.lumi.android.bicyclemap.ui.course;

import android.net.Uri;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.Route;

public class CourseAdapter extends ListAdapter<Route, CourseAdapter.CourseViewHolder> {

    private final MainViewModel viewModel;
    private OnCourseClickListener listener;

    /* ───────────────────────── Click Listener ───────────────────────── */
    public interface OnCourseClickListener {
        void onCourseClick(Route route);
    }
    public void setOnCourseClickListener(OnCourseClickListener l) { this.listener = l; }

    public CourseAdapter(MainViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    /* ───────────────────────── ViewHolder 생성 ───────────────────────── */
    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(v);
    }

    /* ───────────────────────── 데이터 바인딩 ───────────────────────── */
    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder h, int pos) {
        Route r = getItem(pos);

        // 텍스트
        h.title.setText(r.title);
        h.desc.setText("경로 " + r.dist_km + "km · " + r.time + "분 · "
                + (r.tourist_point != null && !r.tourist_point.isEmpty()
                ? r.tourist_point.get(0) : ""));
        h.summary.setText(r.explanation != null ? r.explanation : "");

        // 해시태그
        h.tags.removeAllViews();
        if (r.category != null) {
            for (String tag : r.category) {
                TextView tv = new TextView(h.tags.getContext());
                tv.setText("#" + tag);
                tv.setTextSize(12);
                tv.setTextColor(0xFF555555);
                tv.setPadding(0,0,16,0);
                h.tags.addView(tv);
            }
        }

        /* ───── 이미지 로딩 변경 ───── */
        final int PLACEHOLDER = R.drawable.loading;       // 로딩 중
        final int ERROR_IMG   = R.drawable.sample_image;  // 로딩 실패
        final int NO_URL_IMG  = R.drawable.noimg;         // URL 없음

        if (r.image != null && !r.image.trim().isEmpty()) {
            String src = r.image.trim();
            Object glideSrc = src.startsWith("data:image") ? Uri.parse(src) : src;

            Glide.with(h.image.getContext())
                    .load(glideSrc)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .placeholder(PLACEHOLDER)
                    .error(ERROR_IMG)
                    .centerCrop()
                    .into(h.image);
        } else {
            h.image.setImageResource(NO_URL_IMG);
        }

        // 클릭
        h.itemView.setOnClickListener(v -> {
            viewModel.setSelectedRoute(r);
            if (listener != null) listener.onCourseClick(r);
        });
    }

    /* ───────────────────────── ViewHolder ───────────────────────── */
    static class CourseViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, desc, summary;
        LinearLayout tags;
        CourseViewHolder(@NonNull View v) {
            super(v);
            image   = v.findViewById(R.id.course_image);
            title   = v.findViewById(R.id.course_title);
            desc    = v.findViewById(R.id.course_desc);
            summary = v.findViewById(R.id.course_summary);
            tags    = v.findViewById(R.id.course_tags);
        }
    }

    /* ───────────────────────── DiffUtil ───────────────────────── */
    private static final DiffUtil.ItemCallback<Route> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Route>() {
                public boolean areItemsTheSame(@NonNull Route o,@NonNull Route n){
                    return o.id == n.id;
                }
                public boolean areContentsTheSame(@NonNull Route o,@NonNull Route n){
                    boolean sameTitle = o.title != null && o.title.equals(n.title);
                    boolean sameCat   = o.category != null && n.category != null
                            && o.category.equals(n.category);
                    return sameTitle && sameCat;
                }
            };
}
