package com.inlight.calc;


public class Vector3D {
    public double x;
    public double y;
    public double z;
    public Vector3D(double[] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
    }
    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Vector3D(Vector3D u, Vector3D v) {
        this.x = u.x + v.x;
        this.y = u.y + v.y;
        this.z = u.z + v.z;
    }
    public void normalize() {
        double norm = Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2) + Math.pow(this.z, 2));
        this.x = this.x / norm;
        this.y = this.y / norm;
        this.z = this.z / norm;
    }
    public Vector3D normalized() {
        double norm = Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2) + Math.pow(this.z, 2));
        return new Vector3D(x / norm, y / norm, z / norm);
    }
    public Vector3D zmirror() {
        return new Vector3D(x, y, -z);
    }
    public double dot(Vector3D v) {
        return this.x * v.x + this.y * v.y + this.z * v.z;
    }
    public String toString() {
        return String.format("%f %f %f", x, y, z);
    }
}