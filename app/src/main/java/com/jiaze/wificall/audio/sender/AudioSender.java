package com.jiaze.wificall.audio.sender;

import android.util.Base64;

import com.jiaze.wificall.MyService;
import com.jiaze.wificall.audio.AudioData;
import com.jiaze.wificall.util.LogUtil;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * =========================================
 * The Project:WifiCall
 * the Package:com.jiaze.wificall.audio.sender
 * on 2021/11/25
 * =========================================
 */
public class AudioSender implements Runnable {
    private static final String TAG = "AudioSender";

    private boolean isSending;
    private final LinkedBlockingDeque<AudioData> mSendData = new LinkedBlockingDeque<>(200);
    private Thread mSendThread;
    private MyService mService;

    public AudioSender(MyService service) {
        this.mService = service;
    }

    public void addSendAudioData(byte[] data, int size) {
        if (isSending) {
            AudioData audioData = new AudioData(data, size);
            if (mSendData.remainingCapacity() == 0) {
                mSendData.poll();
                LogUtil.d(TAG, "addSendAudioData: lost audio data");
            }
            mSendData.add(audioData);
        }
    }

    private void sendAudioData(byte[] data) {
        mService.sendData(data);
    }

    public void startSending() {
        LogUtil.d(TAG, "startSending: isSending = " + isSending);
        if (isSending) {
            return;
        }

        if (mSendThread != null) {
            mSendThread.interrupt();
            mSendThread = null;
        }

        mSendThread = new Thread(this);
        mSendThread.start();
    }

    public void stopSending() {
        isSending = false;
        synchronized (mSendData) {
            if (mSendData.size() > 0) {
                mSendData.clear();
            }
        }

        if (mSendThread != null) {
            mSendThread.interrupt();
            mSendThread = null;
        }
        LogUtil.d(TAG, "stopSending: -----");
    }

    @Override
    public void run() {
        LogUtil.d(TAG, "run: start sending =====");
        isSending = true;
        while (isSending) {
            long startTime = System.currentTimeMillis();
            if (mSendData.size() > 0) {
                AudioData audioData = null;
                synchronized (mSendData) {
                    if (mSendData.size() > 0) {
                        audioData = mSendData.poll();
                    }
                }

                if (audioData != null) {
                    byte[] encode = Base64.encode(audioData.getData(), Base64.DEFAULT);
                    sendAudioData(encode);
                    //sendAudioData(audioData.getData());
                }

            }

            long endTime = System.currentTimeMillis();
            long delay = endTime - startTime;
            if (delay < 20) {
                try {
                    Thread.sleep(20 - delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        LogUtil.d(TAG, "AudioSender thread end!!!");
    }
}
