package com.jiaze.wificall;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.jiaze.wificall.audio.SatAudioManager;
import com.jiaze.wificall.call.WifiCallManager;
import com.jiaze.wificall.call.WifiCallStateListener;

public class MyService extends Service {
    private static final String TAG = "MyService";

    private SatAudioManager mSatAudioManager;
    private WifiCallManager mWifiCallManager;

    public MyService() {
        mSatAudioManager = new SatAudioManager(this);
        mWifiCallManager = new WifiCallManager(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyServiceBinder();
    }

    public class MyServiceBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
    }

    public void sendData(byte[] data) {
        mWifiCallManager.sendData(data);
    }

    public void registerWifiCallListener(WifiCallStateListener listener) {
        mWifiCallManager.registerWifiCallStateListener(listener);
    }

    public void unregisterWifiCallListener(WifiCallStateListener listener) {
        mWifiCallManager.unregisterWifiCallStateListener(listener);
    }

    public int getAudioSessionId() {
        return mSatAudioManager.getAudioSessionId();
    }

    public SatAudioManager getSatAudioManager() {
        return mSatAudioManager;
    }

    public void configureWifiCall(int senderPort, int receiverPort, String ip) {
        mWifiCallManager.configureWifiCall(senderPort, receiverPort, ip);
    }

    public void call() {
        mWifiCallManager.call();
    }

    public void hungUp() {
        mWifiCallManager.hungUp();
    }

    public int getWifiCallState() {
        return mWifiCallManager.getWifiCallState();
    }

    public int getSenderPort() {
        return mWifiCallManager.getSendPort();
    }

    public int getReceiverPort() {
        return mWifiCallManager.getReceiverPort();
    }

    public String getReceiverIp() {
        return mWifiCallManager.getReceiverIp();
    }
}