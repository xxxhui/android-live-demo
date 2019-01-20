package com.xxxhui.videocapture;

import android.hardware.Camera;

import com.xxxhui.common.BaseProcessor;

import java.io.IOException;

public class CameraCapture extends BaseProcessor<CameraParam, Void> {

//    private Camera mCamera;
    private CameraParam mParam;
    private KitkatCamera mKitkatCamera;

    @Override
    protected boolean onInit(CameraParam param) {
        if(param == null) {
            return false;
        }
        mKitkatCamera = new KitkatCamera();

//        mCamera = Camera.open(param.getCameraId());
        this.mParam = param;
        return true;
    }

    public KitkatCamera getKitkatCamera() {
        return mKitkatCamera;
    }

    @Override
    protected boolean onStart() {

        if(mKitkatCamera != null) {
            mKitkatCamera.open(mParam.getCameraId());
            mKitkatCamera.setPreviewTexture(mParam.getSurfaceTexture());
            return mKitkatCamera.preview();
        }


//        if(mCamera != null) {
//            try {
//                mCamera.setPreviewTexture(mParam.getSurfaceTexture());
////                mCamera.getParameters().setPictureSize(mParam.getSize().first, mParam.getSize().second);
//                mCamera.startPreview();
//                return true;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        return false;
    }

    @Override
    protected boolean onPauseReStart() {
        return false;
    }

    @Override
    protected boolean onStopReStart() {
        return false;
    }

    @Override
    protected boolean onProcess(Void aVoid) {
        return false;
    }

    @Override
    protected boolean onPause() {
        return false;
    }

    @Override
    protected boolean onStop() {
//        if(mCamera != null) {
//            mCamera.stopPreview();
//            return true;
//        }

        return false;
    }

    @Override
    protected void onRelease() {
//        if(mCamera != null) {
//            mCamera.release();
//        }

        if(mKitkatCamera != null) {
            mKitkatCamera.close();
        }
    }
}
