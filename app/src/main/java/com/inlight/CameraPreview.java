package com.inlight;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import java.io.IOException;


public class CameraPreview implements Camera.PreviewCallback{
    public static final String TAG = "CameraPreview";

    private Camera mCamera;


    public CameraPreview() {

        mCamera = getCameraInstance();
        try {
            // Yalandan SurfaceTexture veriyoruz bi tane
            mCamera.setPreviewTexture(new SurfaceTexture(1));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.setPreviewCallback(this);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // **************
        // BURADA DATAYI VERIYOR KAMERA
        //
        //**************
    }

    public void startPreview(){
        mCamera.startPreview();
    }

    public void stopPreview(){
        mCamera.stopPreview();
    }
    public void releaseCamera(){
        mCamera.release();
    }

    private Camera getCameraInstance(){
        Camera c=null;
        try {
            c = Camera.open(findFrontFacingCameraId()); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.e(TAG, "Camera is not available");    // Camera is not available (in use or does not exist)
        }
        return c;
    }

    private int findFrontFacingCameraId() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }


}
