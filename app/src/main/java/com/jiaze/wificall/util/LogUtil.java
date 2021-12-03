package com.jiaze.wificall.util;

import android.util.Log;

public class LogUtil {
    private static boolean IS_DEBUG = true;
    private static final String DEFAULT_LOG_TAG = "WifiCall-";

    public static void i(String TAG, String msg) {
        if (IS_DEBUG) {
            Log.i(DEFAULT_LOG_TAG + TAG, msg);
        }
    }

    public static void i(String msg) {
        if (IS_DEBUG) {
            i(DEFAULT_LOG_TAG, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (IS_DEBUG) {
            Log.e(DEFAULT_LOG_TAG + tag, msg);
        }
    }

    public static void e(String msg) {
        if (IS_DEBUG) {
            e(DEFAULT_LOG_TAG, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (IS_DEBUG) {
            Log.d(DEFAULT_LOG_TAG + tag, msg);
        }
    }

    public static void d(String msg) {
        if (IS_DEBUG) {
            d(DEFAULT_LOG_TAG, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (IS_DEBUG) {
            Log.w(DEFAULT_LOG_TAG + tag, msg);
        }
    }

    public static void w(String msg) {
        if (IS_DEBUG) {
            w(DEFAULT_LOG_TAG, msg);
        }
    }

    public static void v(String tag, String msg) {
        if (IS_DEBUG) {
            Log.v(DEFAULT_LOG_TAG + tag, msg);
        }
    }

    public static void v(String msg) {
        if (IS_DEBUG) {
            v(DEFAULT_LOG_TAG, msg);
        }
    }
}
