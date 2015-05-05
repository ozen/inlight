package com.inlight.calc;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.inlight.util.RawResourceHelper;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class SH {
    private static Vector3D[][] envNormals;


    public static void readEnvNormals(Context context){
        envNormals = RawResourceHelper.readEnvNormals(context);
    }

    public static double[][] computeIrradianceMatrix(double[][] coeffs) {

        int col ;
        double[][] matrix = new double[3][16];
        double c1,c2,c3,c4,c5 ;
        c1 = 0.429043 ; c2 = 0.511664 ;
        c3 = 0.743125 ; c4 = 0.886227 ; c5 = 0.247708 ;
        for (col = 0 ; col < 3 ; col++) { /* Equation 12 */
            matrix[col][0] = (c1*coeffs[8][col]) ; /* c1 L_{22} */
            matrix[col][1] =  (c1*coeffs[4][col]) ; /* c1 L_{2-2} */
            matrix[col][2] =  (c1*coeffs[7][col]) ; /* c1 L_{21} */
            matrix[col][3] =  (c2*coeffs[3][col]) ; /* c2 L_{11} */
            matrix[col][4] =  (c1*coeffs[4][col]) ; /* c1 L_{2-2} */
            matrix[col][5] =  (-c1*coeffs[8][col]); /*-c1 L_{22} */
            matrix[col][6] =  (c1*coeffs[5][col]) ; /* c1 L_{2-1} */
            matrix[col][7] =  (c2*coeffs[1][col]) ; /* c2 L_{1-1} */
            matrix[col][8] =  (c1*coeffs[7][col]) ; /* c1 L_{21} */
            matrix[col][9] =  (c1*coeffs[5][col]) ; /* c1 L_{2-1} */
            matrix[col][10] =  (c3*coeffs[6][col]) ; /* c3 L_{20} */
            matrix[col][11] =  (c2*coeffs[2][col]) ; /* c2 L_{10} */
            matrix[col][12] =  (c2*coeffs[3][col]) ; /* c2 L_{11} */
            matrix[col][13] =  (c2*coeffs[1][col]) ; /* c2 L_{1-1} */
            matrix[col][14] =  (c2*coeffs[2][col]) ; /* c2 L_{10} */
            matrix[col][15] =  (c4*coeffs[0][col] - c5*coeffs[6][col]) ;
/* c4 L_{00} - c5 L_{20} */
        }
        return matrix;
    }




    public static double[][] computeLightCoefs(Bitmap bitmap) {

        double lightCoefs[][] = new double[9][3];
        int width = envNormals.length;
        int height = envNormals[0].length;
        for (int band = 0; band < 3; band++) {
            for (int x = 0; x < width; x+=3) {
                for (int y = 0; y < height; y+=3) {
                    Vector3D L = envNormals[x][y];
                    int light = bitmap.getPixel(x, y);
                    int value = 0;
                    switch (band) {
                        case 0:
                            value = Color.red(light);
                            break;
                        case 1:
                            value = Color.green(light);
                            break;
                        case 2:
                            value = Color.blue(light);
                            break;
                    }
                    int coefNo = 0;
                    for (int l = 0; l <= 2; l++) {
                        for (int m = -l; m <= l; m++) {
                            lightCoefs[coefNo++][band] += value * yml(l, m, L.x, L.y, L.z);
                        }
                    }
                }
            }
            for (int k = 0; k < 9; k++) {
                lightCoefs[k][band] *= (4 * Math.PI) / (width * height * 9);
            }
        }
        return lightCoefs;
    }



    public static double[][][] computeBRDFCoefs(Vector3D V, double roughness, double fresnel) {
     //   System.out.println("Calculating BRDF SH projection.");
        ArrayList<Vector3D> sampleVectors = generateSampleVectors(1000);
        double brdfCoefs[][][] = new double[5][5][9];

        long startTime = System.currentTimeMillis();
//        Vector3D V = new Vector3D(1.0, 1.0, 0.1);
        for (int t = 0; t < 5; t++) {
            for (int p = 0; p < 5; p++) {
                Vector3D N = new Vector3D(index2cart(new int[]{t, p}));
                for (int k = 0; k < 9; k++) {
                    brdfCoefs[t][p][k] = 0;
                }
                for (Vector3D L : sampleVectors) {
                    double brdf;
                    if (L.z >= 0) {
                        brdf = BRDF.brdf(V, L, N, roughness, fresnel);
                    } else {
                        brdf = BRDF.brdf(V, L.zmirror(), N, roughness, fresnel);
                    }
                    if (brdf < 0.000001) continue;
                    int coefNo = 0;
                    for (int l = 0; l <= 2; l++) {
                        for (int m = -l; m <= l; m++) {
                            brdfCoefs[t][p][coefNo++] += brdf * yml(l, m, L.x, L.y, L.z);
                        }
                    }
                }
                for (int k = 0; k < 9; k++) {
                    brdfCoefs[t][p][k] *= (4 * Math.PI) / sampleVectors.size();
                }
            }
        }
       // System.out.format("BRDF SH projection completed in %d seconds.\n", (System.currentTimeMillis() - startTime) / 1000);
        return brdfCoefs;
    }

    private static ArrayList<Vector3D> generateSampleVectors(int N) {
        ArrayList<Vector3D> sampleVectors = new ArrayList<Vector3D>();
        int sqN = (int) Math.sqrt(N);
        double oneover = 1.0 / sqN;
        for (int a = 0; a < sqN; a++) {
            for (int b = 0; b < sqN; b++) {
                double p = (a + Math.random()) * oneover;
                double q = (b + Math.random()) * oneover;
                double theta = 2.0 * Math.acos(Math.sqrt(1.0 - p));
                double phi = 2.0 * Math.PI * q;
                sampleVectors.add(new Vector3D(sph2cart(new double[]{theta, phi})));
            }
        }
        return sampleVectors;
    }

    private static double yml(int l, int m, double x, double y, double z) {
        if (l == 0 && m == 0) {
            return 0.282095;
        }
        if (l == 1 && m == -1) {
            return 0.488603 * y;
        }
        if (l == 1 && m == 0) {
            return 0.488603 * z;
        }
        if (l == 1 && m == 1) {
            return 0.488603 * x;
        }
        if (l == 2 && m == -2) {
            return 1.092548 * x * y;
        }
        if (l == 2 && m == -1) {
            return 1.092548 * y * z;
        }
        if (l == 2 && m == 0) {
            return 0.315392 * (3 * z * z - 1);
        }
        if (l == 2 && m == 1) {
            return 1.092548 * x * z;
        }
        if (l == 2 && m == 2) {
            return 0.546274 * (x * x - y * y);
        }
        return 0;
    }



    private static double[] index2sph(int[] index) {
        double theta = (index[0] - 2) * (Math.PI / 4);
        double phi = (index[1] - 2) * (Math.PI / 4);
        return new double[]{theta, phi};
    }
    private static int[] sph2index(double[] sph) {
        if (sph[0] > Math.PI / 2 || sph[0] < -Math.PI / 2 || sph[1] > Math.PI / 2 || sph[1] < -Math.PI / 2) {
            return new int[]{};
        } else {
            int t = Math.round((float) (sph[0] * (4.0 / Math.PI)) + 2);
            int p = Math.round((float) (sph[1] * (4.0 / Math.PI)) + 2);
            return new int[]{t, p};
        }
    }
    private static double[] sph2cart(double[] sph) {
        double x = Math.sin(sph[0]) * Math.cos(sph[1]);
        double y = Math.sin(sph[0]) * Math.sin(sph[1]);
        double z = Math.cos(sph[0]);
        return new double[]{x, y, z};
    }
    private static double[] cart2sph(double[] cart) {
        double r = Math.sqrt(cart[0] * cart[0] + cart[1] * cart[1] + cart[2] * cart[2]);
        double theta = Math.acos(cart[2] / r);
        if (cart[0] < 0) theta *= -1;
        double phi = Math.atan(cart[1] / cart[0]);
        return new double[]{theta, phi};
    }
    private static double[] index2cart(int[] index) {
        return sph2cart(index2sph(index));
    }
    private static int[] cart2index(double[] cart) {
        return sph2index(cart2sph(cart));
    }
}