package com.inlight.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class TextureHelper
{
    private static final String TAG = "TextureHelper";
	public static int[] loadTexture(final Context context,
                                  final int textureResourceId,
                                  final int bumpResourceId)
	{

		final int[] textureHandle = new int[2];
		
		GLES20.glGenTextures(2, textureHandle, 0);
		
		if (textureHandle[0] != 0)
		{
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;	// No pre-scaling


			final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), textureResourceId, options);

            Log.d(TAG, "texture " + " w=" + bitmap.getWidth()+ "  h=" + bitmap.getHeight());

			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
			
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

			bitmap.recycle();
		}
		
		if (textureHandle[0] == 0)
		{
			throw new RuntimeException("Error loading texture.");
		}

        if (textureHandle[1] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;	// No pre-scaling


            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), bumpResourceId, options);

            Log.d(TAG, "Bump " + "w=" + bitmap.getWidth()+ "  h=" + bitmap.getHeight());

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[1]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);


            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            bitmap.recycle();
        }

        if (textureHandle[1] == 0)
        {
            throw new RuntimeException("Error loading bump map.");
        }

        return textureHandle;
	}
}
