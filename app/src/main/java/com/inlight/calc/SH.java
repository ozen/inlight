package com.inlight.calc;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class SH {
    private static Vector3D[][] envNormals;
    private static ArrayList<Vector3D> sampleVectors = new ArrayList<>();
    private static double brdfCoefs[][][] = new double[33][33][9];

    private static double roughness = 3.6;
    private static double fresnel = 0.2;


    public static double[][][] computeBRDFCoefs(Context ctx) {

        generateSampleVectors(1000);
        projectBRDF();

        return brdfCoefs;
    }

    public static double[][] computeLightCoefs(Bitmap bitmap) {
        double lightCoefs[][] = new double[9][3];
        int width = envNormals.length;
        int height = envNormals[0].length;

        for (int band = 0; band < 3; band++) {

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
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
                lightCoefs[k][band] *= (4 * Math.PI) / (width * height);
            }
        }

        return lightCoefs;
    }


    private static void projectBRDF() {
        //   System.out.println("Calculating BRDF SH projection.");
        long startTime = System.currentTimeMillis();
        Vector3D V = new Vector3D(0.0, 0.0, 1.0);
        for (int t = 0; t < 33; t++) {
            for (int p = 0; p < 33; p++) {
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
    }

    private static void generateSampleVectors(int N) {
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
    }

    private static void writeBRDFToFile() {
        try {
            URL url = getClass().getResource("brdf_sh_coef.obj");
            File file = new File(url.getPath());
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(brdfCoefs);
            oos.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void readBRDFFromFile() {
        try {

            URL url = getClass().getResource("brdf_sh_coef.obj");
            File file = new File(url.getPath());
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            brdfCoefs = (double[][][]) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
        }
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

    private static double checkBRDF(Vector3D V, Vector3D L, Vector3D N) {
        double brdf = BRDF.brdf(V, L, N, roughness, fresnel);
        int[] index = cart2index(new double[]{N.x, N.y, N.z});
        double expansion = 0;
        int coefNo = 0;
        for (int l = 0; l <= 2; l++) {
            for (int m = -l; m <= l; m++) {
                expansion += yml(l, m, L.x, L.y, L.z) * brdfCoefs[index[0]][index[1]][coefNo++];
            }
        }
        System.out.format("Direct: %f - Expansion: %f\n", brdf, expansion);
        return expansion;
    }

    private static void readEnvNormals() {
        System.out.println("Reading environment map normals from file.");
        int M, N;
        URL url = getClass().getResource("normals.txt");
        File file = new File(url.getPath());
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
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
        System.out.println("Reading environment map normals completed.");
    }

    private static double getColor(Bitmap bitmap, int i, int j, int ch) {
        int col = bitmap.getPixel(i, j);
        int chCol = 0;
        switch (ch) {
            case 0:
                chCol = Color.red(col);
                break;
            case 1:
                chCol = Color.green(col);
                break;
            case 2:
                chCol = Color.blue(col);
                break;
        }
        return chCol / 255.0;

    }

    private static double getLight(Bitmap bitmap, Vector3D vec, int col) {

        return 0.0;
    }

    private static double[] index2sph(int[] index) {
        double theta = (index[0] - 16) * (Math.PI / 32);
        double phi = (index[1] - 16) * (Math.PI / 32);
        return new double[]{theta, phi};
    }

    private static int[] sph2index(double[] sph) {
        if (sph[0] > Math.PI / 2 || sph[0] < -Math.PI / 2 || sph[1] > Math.PI / 2 || sph[1] < -Math.PI / 2) {
            return new int[]{};
        } else {
            int t = Math.round((float) (sph[0] * (32.0 / Math.PI)) + 16);
            int p = Math.round((float) (sph[1] * (32.0 / Math.PI)) + 16);
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