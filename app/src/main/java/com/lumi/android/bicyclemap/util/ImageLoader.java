package com.lumi.android.bicyclemap.util;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

/**
 * 인터넷 URL, data URI(base64) 를 손쉽게 {@link ImageView} 에 로드하는 유틸리티.
 *
 * <pre>
 * ImageLoader.load(context, url, imageView);                 // 기본
 * ImageLoader.load(context, url, imageView, R.drawable.def); // 플레이스홀더 지정
 * ImageLoader.loadFlexible(context, src, imageView, def);    // http·https·data URI 모두 대응
 * </pre>
 */
public final class ImageLoader {

    /** 인스턴스화 방지 */
    private ImageLoader() {
        throw new AssertionError("No instances.");
    }

    /* ------------------------------------------------------------------
     * 1) 기본 형태: URL 문자열 → ImageView
     * ------------------------------------------------------------------ */
    public static void load(@NonNull Context context,
                            @NonNull String url,
                            @NonNull ImageView target) {
        if (TextUtils.isEmpty(url)) return;

        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .centerCrop()
                .into(target);
    }

    /* ------------------------------------------------------------------
     * 2) 플레이스홀더 / 에러 이미지 지정 가능한 오버로드
     * ------------------------------------------------------------------ */
    public static void load(@NonNull Context context,
                            @NonNull String url,
                            @NonNull ImageView target,
                            @DrawableRes int placeholderResId) {
        if (TextUtils.isEmpty(url)) {
            target.setImageResource(placeholderResId);
            return;
        }

        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .centerCrop()
                .into(target);
    }

    /* ------------------------------------------------------------------
     * 3) Flexible 로더: http/https + data:image/...;base64  모두 지원
     * ------------------------------------------------------------------ */
    public static void loadFlexible(@NonNull Context context,
                                    @NonNull String src,
                                    @NonNull ImageView target,
                                    @DrawableRes int placeholderResId) {
        if (TextUtils.isEmpty(src)) {
            target.setImageResource(placeholderResId);
            return;
        }

        String trimmed = src.trim();
        boolean isDataUri = trimmed.startsWith("data:image");

        Glide.with(context)
                .load(isDataUri ? Uri.parse(trimmed) : trimmed) // Glide 는 data URI 도 자동 처리
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .centerCrop()
                .into(target);
    }
}
