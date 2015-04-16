package com.inlight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class RenderManager implements GLSurfaceView.Renderer,
                                      Camera.PictureCallback {
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
    private float[][] mCoefficientMatrix;
    private float[] lightDirection;

    private long lastTime = -1;

    public RenderManager(Context c, GLSurfaceView v){
        mContext = c;
        mView = v;
        mCamera = Camera.open(findFrontFacingCameraId());
        //   Camera.Size s = mCamera.getParameters().getPictureSize();
        //  Log.d(TAG, "w =  " + s.width + "   h = " +  s.height);

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

        double[][] coeffs = Irradiance.prefilter(bitmap);
        lightDirection = Irradiance.calculateLightDirection(coeffs);
        mCoefficientMatrix = Irradiance.toMatrix(coeffs);

        mView.requestRender();
        mCamera.takePicture(null, null, this);
        bitmap.recycle();
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
        final String vertexShaderSource = RawResourceReader.readTextFileFromRawResource(mContext, R.raw.inlight_vertex_shader);
        final String fragmentShaderSource = RawResourceReader.readTextFileFromRawResource(mContext, R.raw.inlight_fragment_shader);
        // Compile, link shaders
        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource);

        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource);

        return ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[] {"a_Position" });
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        mProgramHandle = getCompiledProgramHandle();
        GLES20.glUseProgram(mProgramHandle);

        setupRectangle();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);


        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 0f, 0f, 0f, -5.0f, 0.0f, 1.0f, 0.0f);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.multiplyMM(mMVMatrix,0,mViewMatrix, 0, mModelMatrix,0);

        mTextureDataHandle = TextureHelper.loadTexture(mContext,
                            R.drawable.fabric_5510, R.drawable.fabric_5510_bump);

        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        GLES20.glViewport(0, 0, width, height);
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

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if(mCoefficientMatrix == null) return;

        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[0]);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        int mBumpUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Bump");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[1]);
        GLES20.glUniform1i(mBumpUniformHandle, 1);


        int mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 2,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);


        int mMVPHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPHandle, 1, false, mMVPMatrix, 0);

        int mCoeffMatrixRedHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_CoeffMatrixRed");
        GLES20.glUniformMatrix4fv(mCoeffMatrixRedHandle, 1, false, mCoefficientMatrix[0], 0);
        int mCoeffMatrixGreenHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_CoeffMatrixGreen");
        GLES20.glUniformMatrix4fv(mCoeffMatrixGreenHandle, 1, false, mCoefficientMatrix[1], 0);
        int mCoeffMatrixBlueHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_CoeffMatrixBlue");
        GLES20.glUniformMatrix4fv(mCoeffMatrixBlueHandle, 1, false, mCoefficientMatrix[2], 0);

        int mLightDirectionHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightDirection");
        GLES20.glUniform3fv(mLightDirectionHandle, 1, lightDirection,0);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glDisableVertexAttribArray(mPositionHandle);

    }


}
