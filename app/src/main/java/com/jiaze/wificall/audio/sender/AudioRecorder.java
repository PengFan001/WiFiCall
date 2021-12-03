package com.jiaze.wificall.audio.sender;

import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;

import com.jiaze.wificall.MyService;
import com.jiaze.wificall.audio.AudioConfig;
import com.jiaze.wificall.common.Constants;
import com.jiaze.wificall.util.LogUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * =========================================
 * The Project:WifiCall
 * the Package:com.jiaze.wificall.audio.sender
 * on 2021/11/25
 * =========================================
 */
public class AudioRecorder implements Runnable{
    private static final String TAG = "AudioRecorder";

    private static final int BUFFER_FRAME_SIZE = 320;
    private AudioRecord mAudioRecord;
    private AudioSender mAudioSender;
    private AcousticEchoCanceler mCanceler;
    private Thread mRecordThread;

    private int mBufferSize;
    private int mBufferRead;
    private byte[] mReadAudioData;
    private byte[] mAudioLock = new byte[0];
    private boolean isRecording = false;

    public AudioRecorder(MyService service) {
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
        mAudioSender = new AudioSender(service);
    }

    public void startRecording() {
        LogUtil.d(TAG, "startRecording: isRecording = " + isRecording);
        if (isRecording) {
            return;
        }

        mBufferSize = BUFFER_FRAME_SIZE;
        mReadAudioData = new byte[mBufferSize];
        int audioBufferSize = AudioRecord.getMinBufferSize(AudioConfig.SAMPLE_RATE,
                AudioConfig.RECORDER_CHANNEL_CONFIG, AudioConfig.AUDIO_FORMAT);
        int rest = audioBufferSize % mBufferSize;
        if (rest != 0) {
            audioBufferSize = audioBufferSize + mBufferSize - rest;
        }
        if (audioBufferSize < 0) {
            throw new IllegalArgumentException("init: audioBufferSize = " + audioBufferSize);
        }

        synchronized (mAudioLock) {
            mAudioRecord = new AudioRecord(AudioConfig.AUDIO_RESOURCE, AudioConfig.SAMPLE_RATE,
                    AudioConfig.RECORDER_CHANNEL_CONFIG, AudioConfig.AUDIO_FORMAT, audioBufferSize);
            initAEC();
        }

        mBufferRead = 0;
        if (mRecordThread != null && mRecordThread.isAlive()) {
            mRecordThread.interrupt();
            mRecordThread = null;
        }
        mAudioSender.startSending();
        mRecordThread = new Thread(this);
        mRecordThread.start();
    }

    public void stopRecording() {
        isRecording = false;

        if (mAudioRecord != null) {
            synchronized (mAudioLock) {
                if (mAudioRecord != null) {
                    if (mAudioRecord.getState() != AudioRecord.STATE_UNINITIALIZED) {
                        if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
                            mAudioRecord.stop();
                        }
                    }
                    mAudioRecord.release();
                    mAudioRecord = null;
                    mReadAudioData = null;
                }
            }
        }

        if (mRecordThread != null) {
            mRecordThread.interrupt();
            mRecordThread = null;
        }
        mAudioSender.stopSending();
        LogUtil.d(TAG, "stopRecording: -----");
    }

    public boolean isRecording() {
        return isRecording;
    }

    public int getAudioSessionId() {
        synchronized (mAudioLock) {
            if (mAudioRecord != null) {
                return mAudioRecord.getAudioSessionId();
            }
        }

        return -1;
    }

    /**
     * inti audio echo cancellation
     */
    private void initAEC() {
        if (AcousticEchoCanceler.isAvailable()) {
            int sessionId = mAudioRecord.getAudioSessionId();
            mCanceler = AcousticEchoCanceler.create(sessionId);
            if (mCanceler == null) {
                throw new NullPointerException("initAEC: mCanceler is null");
            } else {
                mCanceler.setEnabled(true);
            }
        } else {
            LogUtil.d(TAG, "initAEC: device not support AcousticEchoCanceler");
        }
    }

    @Override
    public void run() {
        LogUtil.d(TAG, "run: start recording =====");
        synchronized (mAudioLock) {
            if (mAudioRecord == null) {
                throw new NullPointerException("run: mAudioRecord is null");
            }
            mAudioRecord.startRecording();
        }
        isRecording = true;

        byte[] res;
        FileOutputStream fos;
        if (AudioConfig.IS_SAVE_AUDIO) {
            String path = Constants.AUDIO_SAVE_PATH + "/" + Constants.AUDIO_RECORD_FILENAME;
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

        while (isRecording) {
            if (mAudioRecord != null) {
                synchronized (mAudioLock) {
                    if (mAudioRecord != null) {
                        mBufferRead = mAudioRecord.read(mReadAudioData, 0, mBufferSize);
                        if (mBufferRead > 0) {
                            if (AudioConfig.IS_SAVE_AUDIO) {
                                if (fos != null) {
                                    byte[] data = new byte[mBufferRead];
                                    System.arraycopy(mReadAudioData, 0, data, 0,
                                            mBufferRead);
                                    try {
                                        fos.write(data);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            if (mBufferRead == mBufferSize) {
                                res = mReadAudioData.clone();
                                if (mAudioSender != null && res != null) {
                                    mAudioSender.addSendAudioData(res, res.length);
                                }
                            }
                        }
                    } else {
                        LogUtil.d(TAG, "run: mAudioRecord is null");
                    }
                }
            }
        }

        LogUtil.d(TAG, "run: Record Thread end!!!");
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
