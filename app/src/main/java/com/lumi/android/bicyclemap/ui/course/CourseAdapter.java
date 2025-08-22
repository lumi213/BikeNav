package com.lumi.android.bicyclemap.ui.course;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import com.lumi.android.bicyclemap.api.dto.CourseDto;

public class CourseAdapter extends ListAdapter<CourseDto, CourseAdapter.CourseViewHolder> {

    public enum Mode { COURSE, COMPLETED } // COURSE=코스탭, COMPLETED=설정탭(완주)
    private final MainViewModel viewModel;
    private OnCourseClickListener listener;

    /* ───────────────────────── Click Listener ───────────────────────── */
    public interface OnCourseClickListener {
        void onCourseClick(CourseDto route);
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
        CourseDto r = getItem(pos);

        // 텍스트
        h.title.setText(r.getTitle());
        h.desc.setText("경로 " + r.getDist_km() + "km · " + r.getTime() + "분 · "
                + (r.getTourist_spots() != null && !r.getTourist_spots().isEmpty()
                ? r.getTourist_spots().get(0) : ""));
        h.summary.setText(r.getDescription() != null ? r.getDescription() : "");
        h.diff.setText(r.getDiff() == 1 ? " 상" : r.getDiff() == 2 ? " 중" : r.getDiff() == 3 ? " 하" : "");
        GradientDrawable bg = (GradientDrawable)h.diff.getBackground();
        bg.setColor(r.getDiff() == 1 ? Color.parseColor("#D32F2F") : r.getDiff() == 2 ? Color.parseColor("#3F51B5") : r.getDiff() == 3 ? Color.parseColor("#388E3C") : Color.parseColor("#FFFFFF"));

        // 해시태그
        h.tags.removeAllViews();
        if (r.getTags() != null) {
            for (String tag : r.getTags()) {
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

        // resolveCourseImage로 대체 리소스 탐색
        int localResId = resolveCourseImage(h.image.getContext(), r, NO_URL_IMG);

        if (r.getImage() != null && !r.getImage().trim().isEmpty()) {
            String src = r.getImage().trim();
            Object glideSrc = src.startsWith("data:image") ? Uri.parse(src) : src;

            Glide.with(h.image.getContext())
                    .load(glideSrc)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .placeholder(PLACEHOLDER)
                    .error(localResId)   // 💡 실패 시 로컬 이미지 (있으면) 사용
                    .centerCrop()
                    .into(h.image);
        } else {
            // URL 없음 → 로컬 이미지 있으면 사용, 없으면 noimg
            h.image.setImageResource(localResId);
        }

        // 클릭
        h.itemView.setOnClickListener(v -> {
            viewModel.setSelectedRoute(r);
            if (listener != null) listener.onCourseClick(r);
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

    /* ───────────────────────── ViewHolder ───────────────────────── */
    static class CourseViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, desc, summary, diff;
        LinearLayout tags;
        CourseViewHolder(@NonNull View v) {
            super(v);
            image   = v.findViewById(R.id.course_image);
            title   = v.findViewById(R.id.course_title);
            desc    = v.findViewById(R.id.course_desc);
            summary = v.findViewById(R.id.course_summary);
            tags    = v.findViewById(R.id.course_tags);
            diff    = v.findViewById(R.id.course_diff);
        }
    }

    /* ───────────────────────── DiffUtil ───────────────────────── */
    private static final DiffUtil.ItemCallback<CourseDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CourseDto>() {
                public boolean areItemsTheSame(@NonNull CourseDto o,@NonNull CourseDto n){
                    return o.getCourse_id() == n.getCourse_id();
                }
                public boolean areContentsTheSame(@NonNull CourseDto o,@NonNull CourseDto n){
                    boolean sameTitle = o.getTitle() != null && o.getTitle().equals(n.getTitle());
                    boolean sameCat   = o.getType() != null && n.getType() != null
                            && o.getType().equals(n.getType());
                    return sameTitle && sameCat;
                }
            };
}
