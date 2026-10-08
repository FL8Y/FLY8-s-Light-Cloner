package com.fl8y.lightcloner.bridge;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public final class ShizukuBridge {
    private static final int REQUEST_CODE = 7311;
    private static ILiteClonerService service;

    private ShizukuBridge() {}

    public static void install(final Activity activity) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                FrameLayout root = activity.findViewById(android.R.id.content);
                if (root == null || root.findViewWithTag("fly8_shizuku_button") != null) return;

                Button button = new Button(activity);
                button.setTag("fly8_shizuku_button");
                button.setText("Shizuku");
                button.setAllCaps(false);
                button.setTextColor(Color.WHITE);
                button.setTextSize(12f);

                GradientDrawable background = new GradientDrawable();
                background.setColor(0xCC202124);
                background.setCornerRadius(28f);
                button.setBackground(background);
                button.setOnClickListener(v -> requestAndStart(activity));

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.END
                );
                lp.setMargins(0, 0, 18, 22);
                root.addView(button, lp);
            } catch (Throwable ignored) {}
        }, 500);
    }

    private static void requestAndStart(Activity activity) {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(activity, "Start Shizuku first", Toast.LENGTH_LONG).show();
            return;
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                if (requestCode == REQUEST_CODE &&
                    grantResult == PackageManager.PERMISSION_GRANTED) {
                    bindAndStart(activity);
                }
            });
            Shizuku.requestPermission(REQUEST_CODE);
            return;
        }
        bindAndStart(activity);
    }

    private static void bindAndStart(Activity activity) {
        if (service != null) {
            try {
                service.startHelper(activity.getPackageName());
                Toast.makeText(activity, "Shizuku controls started", Toast.LENGTH_SHORT).show();
                return;
            } catch (Throwable ignored) {}
        }

        Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
            new ComponentName(activity, LiteClonerUserService.class)
        ).daemon(false)
         .debuggable(false)
         .processNameSuffix("fly8cloner")
         .version(1)
         .tag("fly8-light-cloner");

        Shizuku.bindUserService(args, new Shizuku.ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                service = ILiteClonerService.Stub.asInterface(binder);
                try {
                    service.startHelper(activity.getPackageName());
                    Toast.makeText(activity, "Shizuku controls started", Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    Toast.makeText(activity, "Shizuku helper failed", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        });
    }
}
