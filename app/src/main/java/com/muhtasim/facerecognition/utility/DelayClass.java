package com.muhtasim.facerecognition.utility;

import android.os.Handler;

public class DelayClass {

    // Delay mechanism

    public static void delay(int secs, final DelayCallback delayCallback) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                delayCallback.afterDelay();
            }
        }, secs * 1000); // afterDelay will be executed after (secs*1000) milliseconds.
    }

    public interface DelayCallback {
        void afterDelay();
    }
}
