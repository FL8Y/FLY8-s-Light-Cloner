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
        if (ACTIVATION.equals(name)) {
            new Handler(Looper.getMainLooper()).post(() -> redirectHome(activity));
        } else if (HOME.equals(name) || CONTROL.equals(name)) {
            ShizukuBridge.install(activity);
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
