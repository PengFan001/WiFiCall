package com.jiaze.wificall.audio;

import android.os.Parcel;
import android.os.Parcelable;

public class AudioData implements Parcelable, Cloneable {
	private int size;
	private byte[] data;

	public AudioData(byte[] audioData, int size) {
		this.size = size;
		this.data = new byte[size];
		System.arraycopy(audioData, 0, this.data, 0, this.size);
	}

	public AudioData(Parcel source) {
        size= source.readInt();
        data = new byte[size];
        source.readByteArray(data);
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        
    }
    
    public static final Creator<AudioData> CREATOR = new Creator<AudioData>() {
        
        @Override
        public AudioData[] newArray(int size) {
            return new AudioData[size];
        }
        
        @Override
        public AudioData createFromParcel(Parcel source) {
            return new AudioData(source);
        }
    };
    
    @Override
    public AudioData clone() throws CloneNotSupportedException {
        AudioData audioData = (AudioData) super.clone();
        audioData.setData(audioData.getData().clone());
        return audioData;
    }
}
