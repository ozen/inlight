package com.inlight.calc;


public class BRDF {
    public static double brdf(Vector3D V, Vector3D L, Vector3D N, double roughness, double fresnel) {
        Vector3D n = N.normalized();
        Vector3D l = L.normalized();
        Vector3D v = V.normalized();
        double NdotL = clamp(n.dot(l), 0.0, 1.0);
        double NdotV = clamp(n.dot(v), 0.0, 1.0);
        if(NdotL < 1.0e-6 || NdotV < 1.0e-6) {
            return 0.0;
        }
        Vector3D h = new Vector3D(v, l);
        h.normalize();
        double NdotH = clamp(n.dot(h), 0.0, 1.0);
        double LdotH = clamp(l.dot(h), 0.0, 1.0);
// System.out.println(NdotL + " " + NdotV + " " + NdotH + " " + LdotH);
        double rSq = roughness * roughness;
        double G = geometricSubTerm(NdotL, rSq) * geometricSubTerm(NdotV, rSq);
        double F = fresnelTerm(LdotH, fresnel);
        double D = beckmannTerm(NdotH, rSq);
        double result = (D * F * G) / (4.0 * NdotV * NdotL);
// System.out.format("%f %f %f %f %f %f\n", D, F, G, NdotV, NdotL, result);
        if (result >= 0) {
            return result;
        }
        else {
            return 0.0;
        }
    }
    private static double geometricSubTerm(double dot, double rSq) {
        double ga = 2.0 * dot;
        double gb = dot + Math.sqrt(rSq + (1.0 - rSq) * dot * dot);
        return ga / gb;
    }
    private static double fresnelTerm(double NdotH, double f) {
        double inner = Math.pow(1.0 - NdotH, 5.0);
        return f + inner * (1.0 - f);
    }
    private static double beckmannTerm(double NdotH, double rSq) {
        double rb = Math.PI * Math.pow((NdotH * NdotH * (rSq - 1.0) + 1.0), 2.0);
        return rSq / rb;
    }
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}