package com.fl8y.lightcloner.bridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public final class BetaBootstrap {
    private BetaBootstrap() {}

    public static void onSplash(Activity activity) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                Class<?> home = Class.forName("io.virtualapp.home.NewHomeActivity");
                activity.startActivity(new Intent(activity, home));
                activity.finish();
            } catch (Throwable ignored) {}
        }, 300);
    }

    public static void onActivation(Activity activity) {
        try {
            Class<?> home = Class.forName("io.virtualapp.home.NewHomeActivity");
            activity.startActivity(new Intent(activity, home));
        } catch (Throwable ignored) {}
        activity.finish();
    }
}
