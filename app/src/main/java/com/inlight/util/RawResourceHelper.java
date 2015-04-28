package com.inlight.util;

import android.content.Context;

import com.inlight.calc.Vector3D;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RawResourceHelper {
    public static String readTextFileFromRawResource(final Context context,
                                                     final int resourceId) {
        final InputStream inputStream = context.getResources().openRawResource(
                resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(
                inputStream);
        final BufferedReader bufferedReader = new BufferedReader(
                inputStreamReader);

        String nextLine;
        final StringBuilder body = new StringBuilder();

        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            return null;
        }

        return body.toString();
    }

    public static double[][][] readBDRFCoeffs(final Context context, final int resourceId) {
        final InputStream inputStream = context.getResources().openRawResource(
                resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(
                inputStream);
        final BufferedReader reader = new BufferedReader(
                inputStreamReader);
        double[][][] coeffs = new double[0][][];
        try {
            String[] firstLine = reader.readLine().split(" ");
            int T = Integer.parseInt(firstLine[0]);
            int P = Integer.parseInt(firstLine[1]);
            coeffs = new double[T][P][9];
            String line="";
            int lineNo = 0;
            for(int p=0;p<P;p++)
                for(int t=0; t<T; t++){
                    String[] fs = line.split(" ");
                for (int i= 0; i < 9; i++) {
                    coeffs[t][p][i] = Double.parseDouble(fs[i]);
                }
             }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
        }
        return coeffs;
    }
}
