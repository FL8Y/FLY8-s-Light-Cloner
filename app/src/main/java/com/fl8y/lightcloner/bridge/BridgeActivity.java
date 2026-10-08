package com.fl8y.lightcloner.bridge;

import android.app.Activity;
import android.os.Bundle;

public final class BridgeActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        finish();
    }
}
