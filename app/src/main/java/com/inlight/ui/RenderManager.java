package com.inlight.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.inlight.R;
import com.inlight.calc.SH;
import com.inlight.calc.Vector3D;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RenderManager {
    public static final String TAG = "RenderManager";

    public static final Integer[] mImageResIds = new Integer[]{
            R.drawable.fabric_5510,
            R.drawable.fabric_6164,
            R.drawable.fabric_6447
    };

    public static final Integer[] mBumpResIds = new Integer[]{
            R.drawable.fabric_5510_bump,
            R.drawable.fabric_6164_bump,
            R.drawable.fabric_6447_bump,
            R.drawable.fabric_6164_bump2,
            R.drawable.fabric_6164_bump3
    };

    private Context mContext;
    private GLSurfaceView mSurfaceView1;
    private GLSurfaceView mSurfaceView2;
    private SurfaceRenderer mSurfaceRenderer1;
    private SurfaceRenderer mSurfaceRenderer2;
    private Camera mCamera;
    private IrradianceComputeTask computeTask;
    private Camera.Size mPreviewSize;
    private SurfaceView mDummyView;
    private Camera.Parameters mCameraParameters;

    private long lastTime = -1;
    private int fpsCounter = 0;

    float[] mIrradianceArray = null;
    private int lockExposureAfter = 5;


    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//            printFPS();

//            Log.d(TAG, "onPreviewFrame BEGIN");
            checkLockExposure();
            computeIrradiance(data);
            triggerRender();
//            Log.d(TAG, "onPreviewFrame END");

//            if (computeTask == null || computeTask.getStatus() == AsyncTask.Status.FINISHED) {
//                // Log.d(TAG, "inside onPreviewFrame");
//                computeTask = new IrradianceComputeTask();
//                computeTask.execute(data);
//
//            if (lockExposureAfter == 0) {
//                mCameraParameters.setAutoExposureLock(true);
//                mCamera.setParameters(mCameraParameters);
//                lockExposureAfter--;
//            } else if (lockExposureAfter > 0) {
//                lockExposureAfter--;
//            }
//            }
        }
    };

    private void checkLockExposure() {
        if (lockExposureAfter == 0) {
            mCameraParameters.setAutoExposureLock(true);
            mCamera.setParameters(mCameraParameters);
            lockExposureAfter--;
        } else if (lockExposureAfter > 0) {
            lockExposureAfter--;
        }
    }


    private final SurfaceHolder.Callback mHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (mCamera == null) {
                    openCamera();
                }
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException exception) {
                Log.e(TAG, exception.getMessage());
                releaseCamera();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCamera();
        }
    };


    public RenderManager(Context c, GLSurfaceView view1, GLSurfaceView view2, SurfaceView dummyView) {
        mContext = c;
        mSurfaceView1 = view1;
        mSurfaceView2 = view2;
        mDummyView = dummyView;

        mSurfaceRenderer1 = new SurfaceRenderer(c, mImageResIds[1], mBumpResIds[4],
                new Vector3D(0.0, 0.0, 1.0), 3.0, 1.0);
        mSurfaceRenderer2 = new SurfaceRenderer(c, mImageResIds[1], mBumpResIds[4],
                new Vector3D(0.0, 0.0, 1.0), 1.5, 0.5);

        view1.setRenderer(mSurfaceRenderer1);
        view2.setRenderer(mSurfaceRenderer2);
    }

    public void openCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    mCamera = Camera.open(camIdx);
                    mCameraParameters = mCamera.getParameters();
                    mCameraParameters.setPreviewFpsRange(30000, 30000);
                    mCameraParameters.setPreviewSize(176, 144);
                    mPreviewSize = mCameraParameters.getPreviewSize();
                    mCamera.setParameters(mCameraParameters);
                    mCamera.setPreviewCallback(mPreviewCallback);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }
    }

    public void releaseCamera() {
        if(mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void onCreate() {
        SH.readEnvNormals(mContext);

        SurfaceHolder sHolder = mDummyView.getHolder();

        //add the callback interface methods defined below as the Surface View callbacks
        sHolder.addCallback(mHolderCallback);

        //tells Android that this surface will have its data constantly replaced
        sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void onPause() {
        releaseCamera();
    }

    public void onResume() {
        openCamera();
    }

    private void printFPS() {
        long now = SystemClock.uptimeMillis();

        if (now - lastTime > 1000) {
            Log.d(TAG, "fps = " + fpsCounter);
            fpsCounter = 0;
            lastTime = now;
        } else {
            fpsCounter++;
        }
    }

    private void computeIrradiance(byte[] data) {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, mPreviewSize.width, mPreviewSize.height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, mPreviewSize.width, mPreviewSize.height), 80, baos);
        byte[] jdata = baos.toByteArray();

        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);

        final Bitmap bitmap = Bitmap.createScaledBitmap(bmp, 140, 140, true);

        double[][] mIrradianceMatrix = SH.computeIrradianceMatrix(SH.computeLightCoefs(bitmap));
        mIrradianceArray = new float[48];
        for (int j = 0; j < 3; j++)
            for (int i = 0; i < 16; i++)
                mIrradianceArray[16 * j + i] = (float) mIrradianceMatrix[j][i];

        bmp.recycle();
        bitmap.recycle();
    }

    private void triggerRender() {
        mSurfaceRenderer1.setIrradianceArray(mIrradianceArray);
        mSurfaceRenderer2.setIrradianceArray(mIrradianceArray);
        mSurfaceView1.requestRender();
        mSurfaceView2.requestRender();
    }


    class IrradianceComputeTask extends AsyncTask<byte[], Void, Bitmap> {
        private long startTime;

        public IrradianceComputeTask() {
            super();
            startTime = SystemClock.uptimeMillis();
        }

        @Override
        protected Bitmap doInBackground(byte[]... params) {
            byte[] data = params[0];

//            startTime = SystemClock.uptimeMillis();

            // Convert to JPG
            YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, mPreviewSize.width, mPreviewSize.height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, mPreviewSize.width, mPreviewSize.height), 80, baos);
            byte[] jdata = baos.toByteArray();

            // Convert to Bitmap
            Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);

            final Bitmap bitmap = Bitmap.createScaledBitmap(bmp, 140, 140, true);

//            Log.d(TAG, "Image conversion completed in: " + (SystemClock.uptimeMillis() - startTime) + " ms.");
//            startTime = SystemClock.uptimeMillis();

            double[][] mIrradianceMatrix = SH.computeIrradianceMatrix(SH.computeLightCoefs(bitmap));
            mIrradianceArray = new float[48];
            for (int j = 0; j < 3; j++)
                for (int i = 0; i < 16; i++)
                    mIrradianceArray[16 * j + i] = (float) mIrradianceMatrix[j][i];

//            Log.d(TAG, "Lighting SH projection completed in: " + (SystemClock.uptimeMillis() - startTime) + " ms.");

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mSurfaceRenderer1.setIrradianceArray(mIrradianceArray);
            mSurfaceRenderer2.setIrradianceArray(mIrradianceArray);
            mSurfaceView1.requestRender();
            mSurfaceView2.requestRender();

            if (bitmap != null) {
                bitmap.recycle();
            }

            Log.d(TAG, "IrradianceComputeTask completed in: " + (SystemClock.uptimeMillis() - startTime) + " ms.");
        }
    }
}
