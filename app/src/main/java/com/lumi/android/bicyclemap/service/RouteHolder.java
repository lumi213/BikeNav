package com.lumi.android.bicyclemap.service;

import androidx.annotation.Nullable;

import com.lumi.android.bicyclemap.api.dto.CourseDto;

public final class RouteHolder {
    private static volatile CourseDto current;

    private RouteHolder() {}

    public static void set(@Nullable CourseDto route) { current = route; }
    @Nullable public static CourseDto get() { return current; }
    public static void clear() { current = null; }
}
