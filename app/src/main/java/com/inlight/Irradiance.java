package com.inlight;

import android.graphics.Bitmap;
import android.graphics.Color;

public class Irradiance  {
    public static final String TAG = "Irradiance";



    private static double sinc(double x) { /* Supporting sinc function */
        if (Math.abs(x) < 1.0e-4) return 1.0 ;
        else return(Math.sin(x)/x) ;
    }
    private static double getColor(Bitmap bitmap, int i, int j, int ch){
        int col = bitmap.getPixel(i,j);
        int chCol=0;
        switch(ch){
            case 0: chCol = Color.red(col); break;
            case 1: chCol = Color.green(col); break;
            case 2: chCol = Color.blue(col); break;
        }
        return chCol/255.0;

    }

    public static double[][] prefilter(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        double[][] coeffs = new double[9][3];

        for (int i = 0 ; i < width ; i++) {
            for (int j = 0 ; j < height ; j++) {
/* We now find the cartesian components for the point (i,j) */
                double u,v,r,theta,phi,x,y,z,domega ;
                v = (width/2.0f - i)/(width/2.0f); /* v ranges from -1 to 1 */
                u = (j - height/2.0f)/(height/2.0f); /* u ranges from -1 to 1 */
                r = Math.sqrt(u*u + v*v) ; /* The "radius" */
                if (r <= 1.0) { /* Consider only circle with r<1 */
                    theta = Math.PI*r ; /* theta parameter of (i,j) */
                    phi = Math.atan2(v,u) ; /* phi parameter */
                    x = Math.sin(theta)*Math.cos(phi) ; /* Cartesian components */
                    y = Math.sin(theta)*Math.sin(phi) ;
                    z = Math.cos(theta) ;
                    domega = (2*Math.PI/width)*(2*Math.PI/width)*sinc(theta) ;
                    for (int channel = 0 ; channel < 3 ; channel++) {
                        double c ; /* A different constant for each coefficient */
/* L_{00}. Note that Y_{00} = 0.282095 */
                        c = 0.282095 ;
                        coeffs[0][channel] += getColor(bitmap,i,j,channel)*c*domega ;
/* L_{1m}. -1 <= m <= 1. The linear terms */
                        c = 0.488603 ;
                        coeffs[1][channel] += getColor(bitmap, i,j,channel)*(c*y)*domega ; /* Y_{1-1} = 0.488603 y */
                        coeffs[2][channel] += getColor(bitmap,i,j,channel)*(c*z)*domega ; /* Y_{10} = 0.488603 z */
                        coeffs[3][channel] += getColor(bitmap,i,j,channel)*(c*x)*domega ; /* Y_{11} = 0.488603 x */
/* The Quadratic terms, L_{2m} -2 <= m <= 2 */
/* First, L_{2-2}, L_{2-1}, L_{21} corresponding to xy,yz,xz */
                        c = 1.092548 ;
                        coeffs[4][channel] += getColor(bitmap,i,j,channel)*(c*x*y)*domega ; /* Y_{2-2} = 1.092548 xy */
                        coeffs[5][channel] += getColor(bitmap,i,j,channel)*(c*y*z)*domega ; /* Y_{2-1} = 1.092548 yz */
                        coeffs[7][channel] += getColor(bitmap, i,j,channel)*(c*x*z)*domega ; /* Y_{21} = 1.092548 xz */
/* L_{20}. Note that Y_{20} = 0.315392 (3z^2 - 1) */
                        c = 0.315392 ;
                        coeffs[6][channel] += getColor(bitmap, i,j,channel)*(c*(3*z*z-1))*domega ;
/* L_{22}. Note that Y_{22} = 0.546274 (x^2 - y^2) */
                        c = 0.546274 ;
                        coeffs[8][channel] += getColor(bitmap, i,j,channel)*(c*(x*x-y*y))*domega ;
                    }
                }
            }
        }
        return coeffs;
    }

    public static float[][] toMatrix(double coeffs[][]) {
/* Form the quadratic form matrix (see equations 11 and 12 in paper) */
        int col ;
        float[][] matrix = new float[3][16];
        double c1,c2,c3,c4,c5 ;
        c1 = 0.429043 ; c2 = 0.511664 ;
        c3 = 0.743125 ; c4 = 0.886227 ; c5 = 0.247708 ;
        for (col = 0 ; col < 3 ; col++) { /* Equation 12 */
            matrix[col][0] = (float)(c1*coeffs[8][col]) ; /* c1 L_{22} */
            matrix[col][1] = (float) (c1*coeffs[4][col]) ; /* c1 L_{2-2} */
            matrix[col][2] = (float) (c1*coeffs[7][col]) ; /* c1 L_{21} */
            matrix[col][3] = (float) (c2*coeffs[3][col]) ; /* c2 L_{11} */
            matrix[col][4] = (float) (c1*coeffs[4][col]) ; /* c1 L_{2-2} */
            matrix[col][5] = (float) (-c1*coeffs[8][col]); /*-c1 L_{22} */
            matrix[col][6] = (float) (c1*coeffs[5][col]) ; /* c1 L_{2-1} */
            matrix[col][7] = (float) (c2*coeffs[1][col]) ; /* c2 L_{1-1} */
            matrix[col][8] = (float) (c1*coeffs[7][col]) ; /* c1 L_{21} */
            matrix[col][9] = (float) (c1*coeffs[5][col]) ; /* c1 L_{2-1} */
            matrix[col][10] = (float) (c3*coeffs[6][col]) ; /* c3 L_{20} */
            matrix[col][11] = (float) (c2*coeffs[2][col]) ; /* c2 L_{10} */
            matrix[col][12] = (float) (c2*coeffs[3][col]) ; /* c2 L_{11} */
            matrix[col][13] = (float) (c2*coeffs[1][col]) ; /* c2 L_{1-1} */
            matrix[col][14] = (float) (c2*coeffs[2][col]) ; /* c2 L_{10} */
            matrix[col][15] = (float) (c4*coeffs[0][col] - c5*coeffs[6][col]) ;
/* c4 L_{00} - c5 L_{20} */
        }
        return matrix;
    }

    private static void normalize(double[] a){

        double magSqr = 0.0;
        for(double x:a) magSqr += x*x;
        double mag = Math.sqrt(magSqr);
        for(int i=0;i<a.length;i++) a[i]/=mag;

    }
    public static float[] calculateLightDirection(double[][] le){
        float[] res = new float[3];
        float[] rations = new float[]{0.3f, 0.59f, 0.11f};

        for(int col=0;col<3;col++) {
            double d[] = new double[]{-le[3][col], -le[1][col], le[2][col]};
            normalize(d);
            for (int i = 0; i < 3; i++) res[i] += rations[col] * (float) d[i];
        }
        return res;

    }
}
