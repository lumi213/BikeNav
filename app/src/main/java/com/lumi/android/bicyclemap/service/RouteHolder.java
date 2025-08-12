package com.lumi.android.bicyclemap.service;

import androidx.annotation.Nullable;
import com.lumi.android.bicyclemap.Route;

public final class RouteHolder {
    private static volatile Route current;

    private RouteHolder() {}

    public static void set(@Nullable Route route) { current = route; }
    @Nullable public static Route get() { return current; }
    public static void clear() { current = null; }
}
