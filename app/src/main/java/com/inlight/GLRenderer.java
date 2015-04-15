package com.inlight;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;

public class GLRenderer implements Renderer {

    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];

    public static float vertices[];
    public static float colors[];
    public static short indices[];
    public static float uvs[];
    public FloatBuffer vertexBuffer;
    public ShortBuffer drawListBuffer;
    public FloatBuffer uvBuffer;
    public FloatBuffer colorBuffer;

    float	mScreenWidth = 1080;
    float	mScreenHeight = 1920;

    Context mContext;
    long mLastTime;
    int mProgram;

    public GLRenderer(Context c)
    {
        mContext = c;
        mLastTime = System.currentTimeMillis() + 100;
    }

    public void onResume()
    {
        mLastTime = System.currentTimeMillis();
    }

    @Override
    public void onDrawFrame(GL10 unused) {

        long now = System.currentTimeMillis();
        if (mLastTime > now) return;

        long elapsed = now - mLastTime;
        Render(mtrxProjectionAndView);
        mLastTime = now;
    }

    private void Render(float[] m) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        int mPositionHandle = GLES20.glGetAttribLocation(ShaderManager.sp_Image, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        int mColorHandle = GLES20.glGetAttribLocation(ShaderManager.sp_Image, "a_Color");
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        int mTexCoordLoc = GLES20.glGetAttribLocation(ShaderManager.sp_Image, "a_texCoord" );
        GLES20.glEnableVertexAttribArray (mTexCoordLoc);
        GLES20.glVertexAttribPointer ( mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        int mtrxhandle = GLES20.glGetUniformLocation(ShaderManager.sp_Image, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

        int mSamplerLoc = GLES20.glGetUniformLocation (ShaderManager.sp_Image, "s_texture" );
        GLES20.glUniform1i ( mSamplerLoc, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mColorHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        mScreenWidth = width;
        mScreenHeight = height;

        GLES20.glViewport(0, 0, (int)mScreenWidth, (int)mScreenHeight);

        for(int i=0;i<16;i++)
        {
            mtrxProjection[i] = 0.0f;
            mtrxView[i] = 0.0f;
            mtrxProjectionAndView[i] = 0.0f;
        }

        Matrix.orthoM(mtrxProjection, 0, 0f, mScreenWidth, 0.0f, mScreenHeight, 0, 50);
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        SetupTriangle();
        SetupImage();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int vertexShader = ShaderManager.loadShader(GLES20.GL_VERTEX_SHADER, ShaderManager.vs_SolidColor);
        int fragmentShader = ShaderManager.loadShader(GLES20.GL_FRAGMENT_SHADER, ShaderManager.fs_SolidColor);

        // might remove the solid one
        ShaderManager.readShaders();
        ShaderManager.sp_SolidColor = GLES20.glCreateProgram();
        GLES20.glAttachShader(ShaderManager.sp_SolidColor, vertexShader);
        GLES20.glAttachShader(ShaderManager.sp_SolidColor, fragmentShader);
        GLES20.glLinkProgram(ShaderManager.sp_SolidColor);

        vertexShader = ShaderManager.loadShader(GLES20.GL_VERTEX_SHADER, ShaderManager.vs_Image);
        fragmentShader = ShaderManager.loadShader(GLES20.GL_FRAGMENT_SHADER, ShaderManager.fs_Image);

        ShaderManager.sp_Image = GLES20.glCreateProgram();
        GLES20.glAttachShader(ShaderManager.sp_Image, vertexShader);
        GLES20.glAttachShader(ShaderManager.sp_Image, fragmentShader);
        GLES20.glLinkProgram(ShaderManager.sp_Image);

        GLES20.glUseProgram(ShaderManager.sp_Image);
    }

    public void SetupImage()
    {
        uvs = new float[] {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        int[] texturenames = new int[1];
        GLES20.glGenTextures(1, texturenames, 0);

        int id = mContext.getResources().getIdentifier("drawable/t5510", null, mContext.getPackageName());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), id, options);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);

        bmp.recycle();
    }

    public void SetupTriangle()
    {
        vertices = new float[]
                {       0.0f, mScreenHeight, 0.0f,
                        0.0f, 0f, 0.0f,
                        mScreenWidth, 0f, 0.0f,
                        mScreenWidth, mScreenHeight, 0.0f,
                };

        colors = new float[]
                {   1f, 1f, 1f, 1f,
                    1f, 1f, 1f, 1f,
                    1f, 1f, 1f, 1f,
                    1f, 1f, 1f, 1f,
                };

        indices = new short[] {0, 1, 2, 0, 2, 3};

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

        ByteBuffer cb = ByteBuffer.allocateDirect(colors.length * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);
    }
}

