package com.fl8y.lightcloner.bridge;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import java.io.IOException;

public final class LiteClonerUserService extends Binder {
    private static final String DESCRIPTOR =
            "com.fl8y.lightcloner.bridge.ILiteClonerService";
    private static final int TRANSACTION_DESTROY = 16777114;
    private static final int TRANSACTION_START_HELPER = IBinder.FIRST_CALL_TRANSACTION;

    public LiteClonerUserService() {
        attachInterface(null, DESCRIPTOR);
    }

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

    private void destroy() {
        System.exit(0);
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code == TRANSACTION_DESTROY) {
            destroy();
            if (reply != null) reply.writeNoException();
            return true;
        }
        if (code == TRANSACTION_START_HELPER) {
            data.enforceInterface(DESCRIPTOR);
            startHelper(data.readString());
            reply.writeNoException();
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }
}
