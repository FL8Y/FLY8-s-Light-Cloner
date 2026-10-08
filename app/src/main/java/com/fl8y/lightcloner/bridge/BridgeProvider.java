package com.fl8y.lightcloner.bridge;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class BridgeProvider extends ContentProvider {
    private static final String SPLASH = "io.virtualapp.splash.SplashActivity";
    private static final String ACTIVATION = "io.virtualapp.license.ActivationActivity";
    private static final String HOME = "io.virtualapp.home.NewHomeActivity";
    private static final String CONTROL = "io.virtualapp.standalone.ControlSetupActivity";

    @Override
    public boolean onCreate() {
        if (getContext() == null) return false;
        Application app = (Application) getContext().getApplicationContext();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle state) {
                handle(activity);
            }
            @Override public void onActivityResumed(Activity activity) {
                handle(activity);
            }
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
        return true;
    }

    private void handle(Activity activity) {
        String name = activity.getClass().getName();
        scrubBetaUi(activity);
        String lowerName = name.toLowerCase();
        if (ACTIVATION.equals(name) || lowerName.contains(".license.") || lowerName.contains("activation")) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> redirectHome(activity), 120);
        } else if (HOME.equals(name) || CONTROL.equals(name)) {
            ShizukuBridge.install(activity);
        }
        String data = activity.getIntent() == null ? null : String.valueOf(activity.getIntent().getData());
        if (data != null && data.toLowerCase().contains("discord")) {
            try { activity.finish(); } catch (Throwable ignored) {}
        }
    }

    private void scrubBetaUi(Activity activity) {
        try {
            View root = activity.findViewById(android.R.id.content);
            scrubView(root);
        } catch (Throwable ignored) {}
    }

    private void scrubView(View view) {
        if (view == null) return;
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            CharSequence desc = view.getContentDescription();
            String a = text == null ? "" : text.toString();
            String b = desc == null ? "" : desc.toString();
            String joined = (a + " " + b).toLowerCase();
            if (joined.contains("discord") ||
                joined.contains("webhook") ||
                joined.contains("enter license key") ||
                joined.contains("activate your key") ||
                joined.contains("activate your license") ||
                joined.contains("change key") ||
                joined.contains("license key") ||
                joined.contains("app license") ||
                joined.contains("license active")) {
                view.setVisibility(View.GONE);
                return;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                scrubView(group.getChildAt(i));
            }
        }
    }

    private void redirectHome(Activity activity) {
        try {
            Class<?> home = Class.forName(HOME);
            Intent intent = new Intent(activity, home);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
        } catch (Throwable ignored) {
        } finally {
            try { activity.finish(); } catch (Throwable ignored) {}
        }
    }

    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String sort) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String s, String[] a) { return 0; }
}
