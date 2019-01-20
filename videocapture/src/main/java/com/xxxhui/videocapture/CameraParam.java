package com.xxxhui.videocapture;

import android.graphics.SurfaceTexture;
import android.util.Pair;

public class CameraParam {
    private int mCameraId;
    private SurfaceTexture mSurfaceTexture;
    private Pair<Integer, Integer> mSize;

    public int getCameraId() {
        return mCameraId;
    }

    public void setCameraId(int cameraId) {
        mCameraId = cameraId;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
    }

    public void setSize(Pair<Integer, Integer> size) {
        mSize = size;
    }

    public Pair<Integer, Integer> getSize() {
        return mSize;
    }
}
