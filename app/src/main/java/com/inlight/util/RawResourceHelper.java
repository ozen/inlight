package com.inlight.util;

import android.content.Context;

import com.inlight.R;
import com.inlight.calc.Vector3D;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;

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

    public static double[][][] readBDRFCoeffs(final Context context) {
        final int resourceId = R.raw.brdf_sh_coef;
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

    public static void writeBRDFToFile(Context context) {
      /*  try {

            File file = new File(url.getPath());
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(brdfCoefs);
            oos.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        */
    }
    public static double[][][] readBRDFFromFile(Context context) {
        final int resourceId = R.raw.brdf_sh_coef;
        double brdfCoefs[][][] = new double[33][33][9];
        try {
            final InputStream is = context.getResources().openRawResource(
                    resourceId);


            ObjectInputStream ois = new ObjectInputStream(is);
            brdfCoefs = (double[][][]) ois.readObject();
            ois.close();
            is.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
        }
        return brdfCoefs;
    }

   public static Vector3D[][] readEnvNormals(Context context) {
        // System.out.println("Reading environment map normals from file.");
       Vector3D[][] envNormals = new Vector3D[0][];
        int M, N;
        final InputStream is =context.getResources().openRawResource(R.raw.normals);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String[] firstLine = reader.readLine().split(", ");
            M = Integer.parseInt(firstLine[0]);
            N = Integer.parseInt(firstLine[1]);
            envNormals = new Vector3D[M][N];
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                String[] triplets = line.split(" , ");
                for (int i = 0; i < M; i++) {
                    String[] coords = triplets[i].split(" ");
                    double x = Double.parseDouble(coords[0]);
                    double y = Double.parseDouble(coords[1]);
                    double z = Double.parseDouble(coords[2]);
                    envNormals[lineNo][i] = new Vector3D(x, y, z);
                }
                ++lineNo;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
// do nothing
            }
        }
        //System.out.println("Reading environment map normals completed.");
        return envNormals;
    }



}
