package com.inlight.ui;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.inlight.R;
import com.inlight.calc.SH;
import com.inlight.util.RawResourceHelper;
import com.inlight.util.ShaderHelper;
import com.inlight.util.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SurfaceRenderer implements GLSurfaceView.Renderer {
    public static final String TAG = "SurfaceRenderer";

    private long lastTime = -1;
    private int fpsCounter = 0;

    private Context mContext;
    private float[] mIrradianceArray;
    private float[] mBRDFArray = new float[225];
    private int[] mTextureDataHandles;

    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private FloatBuffer vertexBuffer;
    private int mProgramHandle;

    private Integer mTextureResId;
    private Integer mBumpResId;

    private Semaphore mIrradianceArrayLock = new Semaphore(1);

    public SurfaceRenderer(Context context, Integer textureResId, Integer bumpResId) {
        mContext = context;
        mTextureResId = textureResId;
        mBumpResId = bumpResId;
        mIrradianceArrayLock = new Semaphore(1);
        setupBRDF();
    }

    public void setIrradianceArray(float[] irradianceArray) {
        try {
            mIrradianceArrayLock.tryAcquire(2500, TimeUnit.MILLISECONDS);
            mIrradianceArray = irradianceArray;
            mIrradianceArrayLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        mProgramHandle = getCompiledProgramHandle();
        GLES20.glUseProgram(mProgramHandle);

        setupRectangle();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mTextureDataHandles = TextureHelper.loadTexture(mContext, mTextureResId, mBumpResId);

        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 0f, 0f, 0f, -5.0f, 0.0f, 1.0f, 0.0f);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.orthoM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f, 7.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);

    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (mIrradianceArray == null) return;

        Log.d(TAG, "onDrawFrame BEGIN");

        printFPS();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandles[0]);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        int mBumpUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Bump");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandles[1]);
        GLES20.glUniform1i(mBumpUniformHandle, 1);

        int mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        int mMVPHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPHandle, 1, false, mMVPMatrix, 0);

        int mIrradianceMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_IrradianceMatrix");
        try {
            mIrradianceArrayLock.tryAcquire(2500, TimeUnit.MILLISECONDS);
            GLES20.glUniformMatrix4fv(mIrradianceMatrixHandle, 3, false, mIrradianceArray, 0);
            mIrradianceArrayLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int mBRDFCoeffsHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_BRDFCoeffs");
        GLES20.glUniform1fv(mBRDFCoeffsHandle, 225, mBRDFArray, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glDisableVertexAttribArray(mPositionHandle);

        Log.d(TAG, "onDrawFrame END");
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

    private void setupBRDF() {
        double[][][] mBRDFCoeffs = SH.computeBRDFCoefs();

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 9; k++) {
                    mBRDFArray[45 * i + 9 * j + k] = (float) mBRDFCoeffs[i][j][k];
                }
            }
        }
    }

    private int getCompiledProgramHandle(){
        // Read shaders from file
        final String vertexShaderSource = RawResourceHelper.readTextFileFromRawResource(mContext, R.raw.inlight_vertex_shader);
        final String fragmentShaderSource = RawResourceHelper.readTextFileFromRawResource(mContext, R.raw.inlight_fragment_shader);

        // Compile, link shaders
        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource);

        return ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[]{"a_Position"});
    }

    private void setupRectangle() {
        float[] vertices = new float[]{
                -1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f
        };

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertices).position(0);
    }
}
