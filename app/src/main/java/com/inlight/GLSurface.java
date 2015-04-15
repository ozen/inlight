package com.inlight;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class GLSurface extends GLSurfaceView {

    private final GLRenderer mRenderer;

    public GLSurface(Context context) {
        super(context);

        setEGLContextClientVersion(2);
        mRenderer = new GLRenderer(context);
        setRenderer(mRenderer);

        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onResume() {
        super.onResume();
        mRenderer.onResume();
    }

}
