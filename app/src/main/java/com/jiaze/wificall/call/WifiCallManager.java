package com.jiaze.wificall.call;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.text.TextUtils;

import com.jiaze.wificall.MyService;
import com.jiaze.wificall.audio.SatAudioManager;
import com.jiaze.wificall.util.LogUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * =========================================
 * The Project:WifiCall
 * the Package:com.jiaze.wificall.datadispatcher
 * on 2021/11/26
 * =========================================
 */
public class WifiCallManager {
    private static final String TAG = "WifiCallManager";

    public static final int CALL_STATE_IDLE = 0;
    public static final int CALL_STATE_ACTIVE = 1;

    private static final String START = "START";
    private static final String STOP = "STOP";
    private static final String OK = "OK";
    private static final String OFF = "OFF";
    private static final String CLOSE = "CLOSE";
    private static final int SEND_QUEUE_SIZE = 200;

    private int mSendPort;
    private int mReceiverPort;
    private String mReceiverIp;
    private int mWifiCallState = CALL_STATE_IDLE;

    private MyService mService;
    private SatAudioManager mSatAudioManager;
    private DatagramSocket mSendSocket;
    private DatagramSocket mReceiverSocket;
    private InetAddress mInetAddress;
    private final LinkedBlockingQueue<byte[]> mDataSendQueue;
    private Sender mSender;
    private Receiver mReceiver;
    private final ArrayList<WifiCallStateListener> mWifiCallStateListeners;

    public WifiCallManager(MyService service) {
        this.mService = service;
        this.mDataSendQueue = new LinkedBlockingQueue<byte[]>(SEND_QUEUE_SIZE);
        mSatAudioManager = mService.getSatAudioManager();
        mWifiCallStateListeners = new ArrayList<WifiCallStateListener>();
        notifyWifiCallState(CALL_STATE_IDLE);
    }

    public void configureWifiCall(int sendPort, int receiverPort, String receiverIp) {
        LogUtil.d(TAG, "configureWifiCall: sendPort = " + sendPort
                + "   receiverPort = " + receiverPort + "   receiverIp = " + receiverIp);
        if (TextUtils.isEmpty(receiverIp)) {
            throw new NullPointerException("configureWifiCall: receiverIp is null");
        }

        if (sendPort == receiverPort) {
            throw new IllegalArgumentException("configureWifiCall: " +
                    "sendPort can't equal receiverPort");
        }

        this.mSendPort = sendPort;
        this.mReceiverPort = receiverPort;
        this.mReceiverIp = receiverIp;

        if (mSender != null) {
            mSender.cancel();
            mSender = null;
        }

        if (mReceiver != null) {
            mReceiver.cancel();
            closeReceiverThread();
            mReceiver = null;
        }

        mSender = new Sender();
        mSender.start();

        mReceiver = new Receiver();
        mReceiver.start();
    }

    public void call() {
        LogUtil.d(TAG, "call: mWifiCallState = " + mWifiCallState);
        if (mWifiCallState == CALL_STATE_IDLE) {
            sendData(START.getBytes());
        }
    }

    public void hungUp() {
        LogUtil.d(TAG, "hungUp: mWifiCallState = " + mWifiCallState);
        if (mWifiCallState == CALL_STATE_ACTIVE) {
            sendData(STOP.getBytes());
        }
    }

    public void sendData(byte[] data) {
        if (data == null) {
            throw new NullPointerException("sendAudioData: audioData is null");
        }
        while (mDataSendQueue.size() >= SEND_QUEUE_SIZE) {
            mDataSendQueue.poll();
            LogUtil.e(TAG, "sendAudioData: lost audio data");
        }

        try {
            mDataSendQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void registerWifiCallStateListener(WifiCallStateListener listener) {
        if (listener != null && !mWifiCallStateListeners.contains(listener)) {
            mWifiCallStateListeners.add(listener);
        }
    }

    public void unregisterWifiCallStateListener(WifiCallStateListener listener) {
        mWifiCallStateListeners.remove(listener);
    }

    private void notifyWifiCallState(int callState) {
        for (WifiCallStateListener listener : mWifiCallStateListeners) {
            listener.callStateListener(callState);
        }
    }

    private class Sender extends Thread {
        private boolean canRun = false;
        Sender() {
            super("Sender");
        }

        @Override
        public void run() {
            LogUtil.d(TAG, "Sender run: start =====");
            canRun = true;
            try {
                if (mSendSocket != null) {
                    mSendSocket.close();
                    mSendSocket = null;
                }
                mSendSocket = new DatagramSocket(mSendPort);
                mInetAddress = InetAddress.getByName(mReceiverIp);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (mSendSocket != null && mInetAddress != null) {
                while (canRun) {
                    byte[] data;
                    if ((data = mDataSendQueue.poll()) == null) {
                        SystemClock.sleep(2);
                        continue;
                    }
                    DatagramPacket datagramPacket = new DatagramPacket(data, data.length,
                            mInetAddress, mSendPort);
                    try {
                        mSendSocket.send(datagramPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            LogUtil.d(TAG, "Sender run: end =====");
        }

        public void cancel() {
            canRun = false;
        }
    }

    private class Receiver extends Thread {
        private boolean canRun = false;
        Receiver() {
            super("Receiver");
        }

        @Override
        public void run() {
            LogUtil.d(TAG, "Receiver run: start =====");
            canRun = true;
            if (mReceiverSocket != null) {
                mReceiverSocket.close();
                mReceiverSocket = null;
            }
            try {
                mReceiverSocket = new DatagramSocket(mReceiverPort);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            if (mReceiverSocket != null) {
                byte[] data = new byte[434];
                DatagramPacket datagramPacket = new DatagramPacket(data, 0, data.length);
                while (canRun) {
                    try {
                        mReceiverSocket.receive(datagramPacket);
                        String line = new String(datagramPacket.getData());
                        if (!TextUtils.isEmpty(line)) {
                            if (line.startsWith(START)) {
                                LogUtil.d(TAG, "Receiver run: START");
                                mWifiCallState = CALL_STATE_ACTIVE;
                                notifyWifiCallState(CALL_STATE_ACTIVE);
                                sendData(OK.getBytes());
                                openVoice();
                            } else if (line.startsWith(OK)) {
                                LogUtil.d(TAG, "Receiver run: OK");
                                mWifiCallState = CALL_STATE_ACTIVE;
                                notifyWifiCallState(CALL_STATE_ACTIVE);
                                openVoice();
                            } else if (line.startsWith(STOP)) {
                                LogUtil.d(TAG, "Receiver run: STOP");
                                mWifiCallState = CALL_STATE_IDLE;
                                notifyWifiCallState(CALL_STATE_IDLE);
                                sendData(OFF.getBytes());
                                closeVoice();
                            } else if (line.startsWith(OFF)) {
                                LogUtil.d(TAG, "Receiver run: OFF");
                                mWifiCallState = CALL_STATE_IDLE;
                                notifyWifiCallState(CALL_STATE_IDLE);
                                closeVoice();
                            } else if (line.startsWith(CLOSE)) {
                                LogUtil.d(TAG, "Receiver run: CLOSE");
                                mWifiCallState = CALL_STATE_IDLE;
                                notifyWifiCallState(CALL_STATE_IDLE);
                            } else {
                                mSatAudioManager.handleReceiverAudioData(datagramPacket.getData());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            LogUtil.d(TAG, "Receiver run: end =====");
        }

        public void cancel() {
            canRun = false;
        }
    }

    private void openVoice() {
        mSatAudioManager.startAudioRecording();
        mSatAudioManager.requestAudioFocus();
        mSatAudioManager.startAudioReceiving();
    }

    private void closeVoice() {
        mSatAudioManager.stopAudioRecording();
        mSatAudioManager.stopAudioReceiving();
        mSatAudioManager.releaseAudioFocus();
    }

    private void closeReceiverThread() {
        try {
            WifiManager wifiManager = (WifiManager) mService.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                String ip = intToIp(wifiManager.getDhcpInfo().ipAddress);
                InetAddress inetAddress = InetAddress.getByName(ip);
                byte[] data = CLOSE.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, mReceiverPort);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mReceiverSocket.send(datagramPacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                Thread.sleep(150);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String intToIp(int i) {
        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

    public int getSendPort() {
        return mSendPort;
    }

    public int getReceiverPort() {
        return mReceiverPort;
    }

    public String getReceiverIp() {
        return mReceiverIp;
    }

    public int getWifiCallState() {
        return mWifiCallState;
    }
}
