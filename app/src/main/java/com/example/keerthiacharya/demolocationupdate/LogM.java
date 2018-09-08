package com.example.keerthiacharya.demolocationupdate;

import android.text.TextUtils;
import android.util.Log;


/**
 * CommonUtils class
 * <p/>
 * <p>
 * This is Log utils class which show log with tag 'log_tag'
 * </p>
 *
 * @author Sumeet Bhut
 * @version 1.0
 * @since 2015-11-30
 */
public class LogM {
    public static void e(String message) {
        if (BuildConfig.DEBUG) Log.e("log_tag", message);
    }

    public static void w(String message) {
        if (BuildConfig.DEBUG) Log.w("log_tag", message);
    }

    public static void e(String key, String message) {
        if (BuildConfig.DEBUG) Log.i(TextUtils.isEmpty(key) ? "log_tag" : key, message);

    }

    public static void v(String key, String message) {
        if (BuildConfig.DEBUG) Log.i(TextUtils.isEmpty(key) ? "log_tag" : key, message);

    }

    public static void d(String key, String message) {
        if (BuildConfig.DEBUG) Log.i(TextUtils.isEmpty(key) ? "log_tag" : key, message);
    }

    public static void i(String key, String message) {
        if (BuildConfig.DEBUG) Log.i(TextUtils.isEmpty(key) ? "log_tag" : key, message);
    }

    public static void i(String message) {
//		Log.i("log_tag", message);
    }

    public static void v(String message) {
//		Log.v("log_tag", message);
    }
}
