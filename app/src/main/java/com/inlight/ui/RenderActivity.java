package com.inlight.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.LinearLayout;

import com.inlight.R;

public class RenderActivity extends Activity {
    public static final String EXTRA_IMAGE = "POS";
    private RenderManager mRenderManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d("RenderActivity", "RenderActivity created");
        setContentView(R.layout.activity_view);
        LinearLayout layout = (LinearLayout) findViewById(R.id.screenLayout);

        GLSurfaceView mGLSurfaceView = new GLSurfaceView(this);
        layout.addView(mGLSurfaceView);
        LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) mGLSurfaceView.getLayoutParams();
        params1.height = 1600;
        params1.width = 1000;
        mGLSurfaceView.setLayoutParams(params1);

        GLSurfaceView mGLSurfaceView2 = new GLSurfaceView(this);
        layout.addView(mGLSurfaceView2);
        LinearLayout.LayoutParams params3 = (LinearLayout.LayoutParams) mGLSurfaceView2.getLayoutParams();
        params3.height = 1600;
        params3.width = 1000;
        mGLSurfaceView2.setLayoutParams(params3);

        SurfaceView dummyView = new SurfaceView(this);
        layout.addView(dummyView);
        LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) dummyView.getLayoutParams();
        params2.height = 100;
        params2.width = 100;
        dummyView.setLayoutParams(params2);

        Intent intent = getIntent();
        int pos = (int) intent.getExtras().get(EXTRA_IMAGE);

        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            mGLSurfaceView.setEGLContextClientVersion(2);
            mGLSurfaceView2.setEGLContextClientVersion(2);
            mRenderManager = new RenderManager(this, mGLSurfaceView, mGLSurfaceView2, dummyView);
        } else {
            Log.e("rgew", "GLES version mismatch!!!");
            return;
        }

//        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//        mGLSurfaceView2.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mRenderManager.onCreate();
    }


    @Override
    protected void onResume() {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        mRenderManager.onResume();
    }

    @Override
    protected void onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        mRenderManager.onPause();
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(this, GridActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }
}