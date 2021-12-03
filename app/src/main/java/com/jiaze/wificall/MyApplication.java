package com.jiaze.wificall;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.jiaze.wificall.util.LogUtil;

import java.util.ArrayList;

/**
 * =========================================
 * The Project:WifiCall
 * the Package:com.jiaze.wificall
 * on 2021/11/25
 * =========================================
 */
public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    private static MyApplication mInstance;
    private MyService mService;
    private ArrayList<ServiceStateListener> mServiceStateListeners;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MyService.MyServiceBinder)service).getService();
            for (ServiceStateListener listener : mServiceStateListeners) {
                listener.onServiceConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            for (ServiceStateListener listener : mServiceStateListeners) {
                listener.onServiceDisconnected();
            }
        }
    };

    public static MyApplication getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "onCreate: -----");
        mInstance = this;
        mServiceStateListeners = new ArrayList<ServiceStateListener>();
        Intent intent = new Intent(this, MyService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public MyService getService() {
        return mService;
    }

    public void registerServiceStateListener(ServiceStateListener listener) {
        if (listener != null && !mServiceStateListeners.contains(listener)) {
            mServiceStateListeners.add(listener);
        }
    }

    public void unregisterServiceStateListener(ServiceStateListener listener) {
        mServiceStateListeners.remove(listener);
    }
}
