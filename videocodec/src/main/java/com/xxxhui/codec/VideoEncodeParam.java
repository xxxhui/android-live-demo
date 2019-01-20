package com.xxxhui.codec;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.Surface;

public class VideoEncodeParam implements Parcelable {

    private int width;
    private int height;
    private int bitRate;
    private int frameRate;
    private int iFrameInterval;
    private EncoderDrainListener mOutListener;
    private Surface mSurface;

    public Surface getSurface() {
        return mSurface;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public EncoderDrainListener getOutListener() {
        return mOutListener;
    }

    public void setOutListener(EncoderDrainListener outListener) {
        mOutListener = outListener;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public int getiFrameInterval() {
        return iFrameInterval;
    }

    public void setiFrameInterval(int iFrameInterval) {
        this.iFrameInterval = iFrameInterval;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeInt(this.bitRate);
        dest.writeInt(this.frameRate);
        dest.writeInt(this.iFrameInterval);
    }

    public VideoEncodeParam() {
    }

    protected VideoEncodeParam(Parcel in) {
        this.width = in.readInt();
        this.height = in.readInt();
        this.bitRate = in.readInt();
        this.frameRate = in.readInt();
        this.iFrameInterval = in.readInt();
    }

    public static final Parcelable.Creator<VideoEncodeParam> CREATOR = new Parcelable.Creator<VideoEncodeParam>() {
        @Override
        public VideoEncodeParam createFromParcel(Parcel source) {
            return new VideoEncodeParam(source);
        }

        @Override
        public VideoEncodeParam[] newArray(int size) {
            return new VideoEncodeParam[size];
        }
    };
}
