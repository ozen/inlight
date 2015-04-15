package com.inlight;

import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class ShaderManager {

    public static int sp_SolidColor;
    public static int sp_Image;

    public static String vs_SolidColor;
    public static String fs_SolidColor;
    public static String vs_Image;
    public static String fs_Image;

    public static String readText(String fileName) throws IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(fileName));

        try
        {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null)
            {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }

            return sb.toString();
        }


        finally
        {
            br.close();
        }
    }

    public static void readShaders()
    {
        try {
            // Read shaders from res/raw
        }

        catch (Exception e) {
            Log.d("SHADER READ", e.getMessage());
        }
    }

    public static int loadShader(int type, String shaderCode){

        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
}

