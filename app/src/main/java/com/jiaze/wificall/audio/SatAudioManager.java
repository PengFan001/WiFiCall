package com.jiaze.wificall.audio;

import android.content.Context;
import android.media.AudioManager;

import com.jiaze.wificall.MyApplication;
import com.jiaze.wificall.MyService;
import com.jiaze.wificall.audio.receiver.AudioReceiver;
import com.jiaze.wificall.audio.sender.AudioRecorder;
import com.jiaze.wificall.util.LogUtil;

/**
 * =========================================
 * The Project:WifiCall
 * the Package:com.jiaze.wificall.audio
 * on 2021/11/25
 * =========================================
 */
public class SatAudioManager {
    private static final String TAG = "SatAudioManager";

    private AudioManager mAudioManager;
    private AudioRecorder mAudioRecorder;
    private AudioReceiver mAudioReceiver;
    private boolean isAudioFocus;

    private final AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener
            = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            LogUtil.d(TAG, "onAudioFocusChange: focusChange = " + focusChange);
        }
    };

    public SatAudioManager(MyService service) {
        mAudioReceiver = new AudioReceiver(service);
        mAudioRecorder = new AudioRecorder(service);
        mAudioManager = (AudioManager) MyApplication.getInstance()
                .getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    public void startAudioRecording() {
        mAudioRecorder.startRecording();
    }

    public void stopAudioRecording() {
        if (mAudioRecorder.isRecording()) {
            mAudioRecorder.stopRecording();
        }
    }

    public void startAudioReceiving() {
        mAudioReceiver.startReceiving();
    }

    public void stopAudioReceiving() {
        if (mAudioReceiver.isReceiving()) {
            mAudioReceiver.stopReceiving();
        }
    }

    public void requestAudioFocus() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        int result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
                AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            isAudioFocus = true;
        } else {
            isAudioFocus = false;
        }
    }

    public void releaseAudioFocus() {
        if (isAudioFocus) {
            mAudioManager.setMode(AudioManager.MODE_NORMAL);
            int result = mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                isAudioFocus = false;
            } else {
                isAudioFocus = true;
            }
        }
    }

    public boolean isAudioFocus() {
        return isAudioFocus;
    }

    public int getAudioSessionId() {
        return mAudioRecorder.getAudioSessionId();
    }

    public void handleReceiverAudioData(byte[] audioData) {
        if (audioData == null) {
            throw new NullPointerException("handleReceiverAudioData: audioData is null");
        }
        mAudioReceiver.addReceivedAudioData(audioData, audioData.length);
    }
}
