package com.jiaze.wificall.audio;

import android.media.AudioFormat;
import android.media.MediaRecorder;

public class AudioConfig {
	public static final boolean IS_SAVE_AUDIO = false;

	public static final int SAMPLE_RATE = 8000;
	public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	public static final int AUDIO_RESOURCE = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
	public static final int RECORDER_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	public static final int PLAYER_CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_MONO;
}
