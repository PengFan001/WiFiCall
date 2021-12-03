package com.jiaze.wificall.audio.receiver;

import android.util.Base64;

import com.jiaze.wificall.MyService;
import com.jiaze.wificall.audio.AudioData;
import com.jiaze.wificall.util.LogUtil;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * =========================================
 * The Project:WifiCall
 * the Package:com.jiaze.wificall.audio.receiver
 * on 2021/11/25
 * =========================================
 */
public class AudioReceiver implements Runnable {
    private static final String TAG = "AudioReceiver";

    private boolean isReceiving = false;
    private final List<AudioData> mReceiveData;
    private AudioPlayer mAudioPlayer;
    private Thread mReceiverThread;

    public AudioReceiver(MyService service) {
        mAudioPlayer = new AudioPlayer(service);
        /**Collections.synchronizedList**/
        //When we add, we don't have to lock it;Traversal avoids data being changed by other threads,
        //so lock protection is added;
        mReceiveData = Collections.synchronizedList(new LinkedList<AudioData>());
    }

    public void addReceivedAudioData(byte[] data, int size) {
        if (isReceiving) {
            AudioData audioData = new AudioData(data, size);
            mReceiveData.add(audioData);
        }
    }

    public void startReceiving() {
        LogUtil.d(TAG, "startReceiving: isReceiving = " + isReceiving);
        if (isReceiving) {
            return;
        }

        mAudioPlayer.startPlaying();
        if (mReceiverThread != null) {
            mReceiverThread.interrupt();
            mReceiverThread = null;
        }
        mReceiverThread = new Thread(this);
        mReceiverThread.start();
    }

    public void stopReceiving() {
        isReceiving = false;
        mAudioPlayer.stopPlaying();
        synchronized (mReceiveData) {
            if (mReceiveData.size() > 0) {
                mReceiveData.clear();
            }
        }
        if (mReceiverThread != null) {
            mReceiverThread.interrupt();
            mReceiverThread = null;
        }
        LogUtil.d(TAG, "stopReceiving: -----");
    }

    public boolean isReceiving() {
        return isReceiving;
    }

    @Override
    public void run() {
        LogUtil.d(TAG, "run: Audio Receiver start!!!");
        isReceiving = true;
        while (isReceiving) {
            AudioData audioData;
            synchronized (mReceiveData) {
                if (mReceiveData.isEmpty()) {
                    continue;
                }
                audioData = mReceiveData.remove(0);
            }

            if (audioData != null) {
                byte[] decode = Base64.decode(audioData.getData(), Base64.DEFAULT);
                mAudioPlayer.addAudioPlayData(decode, decode.length);
                //mAudioPlayer.addAudioPlayData(audioData.getData(), audioData.getSize());
            }
        }
        LogUtil.d(TAG, "run: Audio Receiver end!!!");
    }
}
