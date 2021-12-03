package com.jiaze.wificall.audio.receiver;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.AcousticEchoCanceler;

import com.jiaze.wificall.MyService;
import com.jiaze.wificall.audio.AudioConfig;
import com.jiaze.wificall.audio.AudioData;
import com.jiaze.wificall.common.Constants;
import com.jiaze.wificall.util.LogUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
public class AudioPlayer implements Runnable {
    private static final String TAG = "AudioPlayer";

    private MyService mService;
    private boolean isPlaying = false;
    private AudioTrack mAudioTrack;
    private final List<AudioData> mPlayAudioData;
    private Thread mPlayThread;
    private byte[] mAudioTrackLock = new byte[0];

    public AudioPlayer(MyService service) {
        this.mService = service;
        mPlayAudioData = Collections.synchronizedList(new LinkedList<AudioData>());
        if (AudioConfig.IS_SAVE_AUDIO) {
            File file = new File(Constants.AUDIO_SAVE_PATH);
            if (!file.exists()) {
                try {
                    file.mkdirs();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void addAudioPlayData(byte[] data, int size) {
        if (isPlaying) {
            AudioData audioData = new AudioData(data, size);
            mPlayAudioData.add(audioData);
        }
    }

    public void startPlaying() {
        LogUtil.d(TAG, "startPlaying: isPlaying = " + isPlaying);
        if (isPlaying) {
            return;
        }

        int bufferSize = AudioTrack.getMinBufferSize(AudioConfig.SAMPLE_RATE,
                AudioConfig.PLAYER_CHANNEL_CONFIG, AudioConfig.AUDIO_FORMAT);
        if (bufferSize < 0) {
            throw new IllegalArgumentException("startPlaying: bufferSize = " + bufferSize);
        }

        //echo cancellation
        synchronized (mAudioTrackLock) {
            if (AcousticEchoCanceler.isAvailable()) {
                int audioSessionId = mService.getAudioSessionId();
                if (audioSessionId == -1) {
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                            AudioConfig.SAMPLE_RATE, AudioConfig.PLAYER_CHANNEL_CONFIG,
                            AudioConfig.AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);
                } else {
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                            AudioConfig.SAMPLE_RATE, AudioConfig.PLAYER_CHANNEL_CONFIG,
                            AudioConfig.AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM,
                            audioSessionId);
                }
            } else {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                        AudioConfig.SAMPLE_RATE, AudioConfig.PLAYER_CHANNEL_CONFIG,
                        AudioConfig.AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);
            }
        }

        if (mPlayThread != null) {
            mPlayThread.interrupt();
            mPlayThread = null;
        }

        mPlayThread = new Thread(this);
        mPlayThread.start();
    }

    public void stopPlaying() {
        isPlaying = false;
        synchronized (mAudioTrackLock) {
            if (mAudioTrack != null && mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                    mAudioTrack.flush();
                    mAudioTrack.stop();
                }

                mAudioTrack.release();
                mAudioTrack = null;
            }
        }

        synchronized (mPlayAudioData) {
            mPlayAudioData.clear();
        }

        LogUtil.d(TAG, "stopPlaying: -----");
    }

    @Override
    public void run() {
        LogUtil.d(TAG, "run: Audio play start!!!");
        synchronized (mAudioTrackLock) {
            if (mAudioTrack == null) {
                throw new NullPointerException("run: mAudioTrack is null");
            }
            mAudioTrack.play();
        }

        //silent audio data
        byte[] silentData = new byte[320];
        isPlaying = true;
        FileOutputStream fos;
        if (AudioConfig.IS_SAVE_AUDIO) {
            String path = Constants.AUDIO_SAVE_PATH + "/" + Constants.AUDIO_PLAYER_FILENAME;
            File file = new File(path);
            if (file.exists()) {
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fos = null;
            }
        } else {
            fos = null;
        }

        while (isPlaying) {
            AudioData audioData;
            if (mPlayAudioData.size() > 0) {
                synchronized (mPlayAudioData) {
                    if (mPlayAudioData.isEmpty()) {
                        continue;
                    }
                    audioData = mPlayAudioData.remove(0);
                }

                if (audioData != null) {
                    //save the receive audio data
                    if (AudioConfig.IS_SAVE_AUDIO) {
                        if (fos != null) {
                            int size = audioData.getSize();
                            byte[] data = new byte[size];
                            System.arraycopy(audioData, 0, data, 0, size);
                            try {
                                fos.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    synchronized (mAudioTrackLock) {
                        if (mAudioTrack != null) {
                            mAudioTrack.write(audioData.getData(), 0,
                                    audioData.getSize());
                        } else {
                            LogUtil.e(TAG, "run: mAudioTrack is null");
                        }
                    }
                }
            } else {
                synchronized (mAudioTrackLock) {
                    if (mAudioTrack != null) {
                        mAudioTrack.write(silentData, 0, silentData.length);
                    }
                }
            }
        }

        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        LogUtil.d(TAG, "Audio play thread end!!!");
    }
}
