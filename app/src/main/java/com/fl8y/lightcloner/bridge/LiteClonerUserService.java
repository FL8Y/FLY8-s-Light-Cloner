package com.fl8y.lightcloner.bridge;

import android.os.RemoteException;
import java.io.IOException;

public final class LiteClonerUserService extends ILiteClonerService.Stub {
    @Override
    public void startHelper(String packageName) throws RemoteException {
        String cmd =
            "APK=$(pm path " + shellQuote(packageName) +
            " | head -n 1 | sed 's/package://' ); " +
            "[ -n \"$APK\" ] && CLASSPATH=\"$APK\" nohup " +
            "/system/bin/app_process /system/bin " +
            "io.virtualapp.standalone.HelperMain";
        try {
            Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
        } catch (IOException e) {
            throw new RemoteException("Unable to start helper: " + e);
        }
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}
