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

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRenderer.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRenderer.onResume();
    }

}
