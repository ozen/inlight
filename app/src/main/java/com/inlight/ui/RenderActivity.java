package com.inlight.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

import com.inlight.R;

public class RenderActivity extends Activity{
    public static final String EXTRA_IMAGE="POS";
    private RenderManager mRenderManager;
    private int pos;

    public final static Integer[] mImageResIds = new Integer[] {
            R.drawable.fabric_5510,
            R.drawable.fabric_6164,
            R.drawable.fabric_6447};

    public final static Integer[] mBumpResIds = new Integer[] {
            R.drawable.fabric_5510_bump,
            R.drawable.fabric_6164_bump,
            R.drawable.fabric_6447_bump};


    @Override
	public void onCreate(Bundle savedInstanceState) 
	{

        super.onCreate(savedInstanceState);
		Log.d("RenderActivity", "RenderActivity created");
	    GLSurfaceView mGLSurfaceView = new GLSurfaceView(this);
        Intent intent = getIntent();
        pos = (int) intent.getExtras().get(EXTRA_IMAGE);

		// Check if the system supports OpenGL ES 2.0.
		final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
		final boolean supportsEs3 = configurationInfo.reqGlEsVersion >= 0x30000;

		if (supportsEs3)
		{
			// Request an OpenGL ES 3.0 compatible context.
			mGLSurfaceView.setEGLContextClientVersion(3);

			// Set the renderer to our demo renderer, defined below.
            mRenderManager = new RenderManager(this, mGLSurfaceView, mImageResIds[pos],
                    mBumpResIds[pos]);
			mGLSurfaceView.setRenderer(mRenderManager);
		} 
		else 
		{
            Log.e("rgew", "es3 not supported!!!");
			return;
		}

		setContentView(mGLSurfaceView);

        mRenderManager.onCreate();

    }


	@Override
	protected void onResume() 
	{
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
    public void onBackPressed(){
        Intent i=new Intent(this, GridActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }
}