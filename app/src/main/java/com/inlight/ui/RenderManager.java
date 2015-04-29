package com.inlight.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.inlight.R;
import com.inlight.calc.Irradiance;
import com.inlight.calc.SH;
import com.inlight.util.RawResourceHelper;
import com.inlight.util.ShaderHelper;
import com.inlight.util.TextureHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class RenderManager implements GLSurfaceView.Renderer {
    public static final String TAG = "RenderManager";

    private Context mContext;
    private GLSurfaceView mView;
    private Camera mCamera;
    private FloatBuffer vertexBuffer;
    private int mProgramHandle;
    private int[] mTextureDataHandle;
    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private double[][] mIrradianceMatrix = new double[3][16];
    private double[][][] mBRDFCoeffs = new double[33][33][9];
    private int mTextureResId;
    private int mBumpResId;
    private long lastTime = -1;
    private CameraPreview mCameraPreview;

    public RenderManager(Context c, GLSurfaceView v, int texResId, int bumpResId){
        mContext = c;
        mView = v;
        mTextureResId = texResId;
        mBumpResId = bumpResId;

        //   Camera.Size s = mCamera.getParameters().getPictureSize();
        //  Log.d(TAG, "w =  " + s.width + "   h = " +  s.height);

    }

    public void onCreate(){
        SH.readEnvNormals(mContext);
        mBRDFCoeffs = SH.computeBRDFCoefs();
    }

    public void onResume(){
        if(mCameraPreview == null)
            mCameraPreview = new CameraPreview();
        mCameraPreview.startPreview();
    }

    public void onPause(){
        mCameraPreview.stopPreview();
        mCameraPreview.releaseCamera();
    }


    class CameraPreview implements Camera.PreviewCallback{
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
            final Bitmap pic = BitmapFactory.decodeByteArray(data, 0, data.length);
            final Bitmap bitmap = Bitmap.createScaledBitmap(pic,140,140,true);
            mIrradianceMatrix = SH.computeIrradianceMatrix(SH.computeLightCoefs(bitmap));

            mView.requestRender();
            bitmap.recycle();
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
                Log.e(TAG, "Camera is not available"); // Camera is not available (in use or does not exist)
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


    private void setupRectangle(){

        float[] vertices = new float[]{
                -1.0f, 1.0f,
               -1.0f, -1.0f,
                1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f
                };

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

    }

    private int getCompiledProgramHandle(){
        // Read shaders from file
        final String vertexShaderSource = RawResourceHelper.readTextFileFromRawResource(mContext, R.raw.inlight_vertex_shader);
        final String fragmentShaderSource = RawResourceHelper.readTextFileFromRawResource(mContext, R.raw.inlight_fragment_shader);
        // Compile, link shaders
        final int vertexShaderHandle = ShaderHelper.compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource);

        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource);

        return ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[] {"a_Position" });
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        mProgramHandle = getCompiledProgramHandle();
        GLES30.glUseProgram(mProgramHandle);

        setupRectangle();

        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA);


        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 0f, 0f, 0f, -5.0f, 0.0f, 1.0f, 0.0f);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.multiplyMM(mMVMatrix,0,mViewMatrix, 0, mModelMatrix,0);

        mTextureDataHandle = TextureHelper.loadTexture(mContext, mTextureResId, mBumpResId);



    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        GLES30.glViewport(0, 0, width, height);
        float ratio = (float) width/height;
        Matrix.orthoM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f, 7.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);

    }

    private void printFPS(){
        long now = SystemClock.uptimeMillis();
        if(lastTime != -1)
            Log.d(TAG, "fps = " + (1000.0 / (now - lastTime)));
        lastTime = now;
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        printFPS();

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        if(mIrradianceMatrix == null) return;

        int mTextureUniformHandle = GLES30.glGetUniformLocation(mProgramHandle, "u_Texture");
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureDataHandle[0]);
        GLES30.glUniform1i(mTextureUniformHandle, 0);

        int mBumpUniformHandle = GLES30.glGetUniformLocation(mProgramHandle, "u_Bump");
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureDataHandle[1]);
        GLES30.glUniform1i(mBumpUniformHandle, 1);


        int mPositionHandle = GLES30.glGetAttribLocation(mProgramHandle, "a_Position");
        GLES30.glEnableVertexAttribArray(mPositionHandle);
        GLES30.glVertexAttribPointer(mPositionHandle, 2,
                GLES30.GL_FLOAT, false, 0, vertexBuffer);


        int mMVPHandle = GLES30.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        GLES30.glUniformMatrix4fv(mMVPHandle, 1, false, mMVPMatrix, 0);

        //mIrradianceMatrix
        float[] mIrradianceArray = new float[48];

        for(int j=0;j<3;j++)
           for(int i=0;i<16;i++)
               mIrradianceArray[16*j+i] = (float) mIrradianceMatrix[j][i];

        float[] mBRDFArray = new float[33*33*9];
        for(int i=0;i<33;i++)
            for(int j=0;j<33;j++)
                for(int k=0;k<9;k++)
                    mBRDFArray[9*33*i+9*j+k] = (float) mBRDFCoeffs[i][j][k];

        int mIrradianceMatrixHandle = GLES30.glGetUniformLocation(mProgramHandle, "u_IrradianceMatrix");
        GLES30.glUniformMatrix4fv(mIrradianceMatrixHandle, 3, false, mIrradianceArray, 0);

        //mBRDFCoeffs
        int mBRDFCoeffsHandle = GLES30.glGetAttribLocation(mProgramHandle, "u_BRDFCoeffs");
        GLES30.glUniform3fv(mBRDFCoeffsHandle, 3267, mBRDFArray, 0 );

    /*    int mCoeffMatrixRedHandle = GLES30.glGetUniformLocation(mProgramHandle, "u_CoeffMatrixRed");
        GLES30.glUniformMatrix4fv(mCoeffMatrixRedHandle, 1, false, mCoefficientMatrix[0], 0);
        int mCoeffMatrixGreenHandle = GLES30.glGetUniformLocation(mProgramHandle, "u_CoeffMatrixGreen");
        GLES30.glUniformMatrix4fv(mCoeffMatrixGreenHandle, 1, false, mCoefficientMatrix[1], 0);
        int mCoeffMatrixBlueHandle = GLES30.glGetUniformLocation(mProgramHandle, "u_CoeffMatrixBlue");
        GLES30.glUniformMatrix4fv(mCoeffMatrixBlueHandle, 1, false, mCoefficientMatrix[2], 0);

        int mLightDirectionHandle = GLES30.glGetUniformLocation(mProgramHandle, "u_LightDirection");
        GLES30.glUniform3fv(mLightDirectionHandle, 1, lightDirection,0);

    */
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6);

        GLES30.glDisableVertexAttribArray(mPositionHandle);

    }


}
