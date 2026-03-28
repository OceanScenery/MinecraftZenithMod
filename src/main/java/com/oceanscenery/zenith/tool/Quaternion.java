package com.oceanscenery.zenith.tool;

import org.joml.Quaternionf;

import java.io.Serial;
import java.io.Serializable;
import java.util.InputMismatchException;
import java.util.Objects;
import java.util.Scanner;

public class Quaternion implements Serializable {
    @Serial
    private static final long serialVersionUID = 19448292481L;
    //四元数
    private double s;
    private double x,y,z;

    public Quaternion(){
        s=0;
        x=0;
        y=0;
        z=0;
    }

    public Quaternion(double s,double x,double y,double z){
        this.s=s;
        this.x=x;
        this.y=y;
        this.z=z;
    }

    public Quaternion(String s){
        String[] value=s.split("[\\[\\],]");
        if(value.length!=5){
            throw new IllegalArgumentException("illegal form");
        }
        try{
            this.setValue(
                    new Scanner(value[1]).nextDouble(),
                    new Scanner(value[2]).nextDouble(),
                    new Scanner(value[3]).nextDouble(),
                    new Scanner(value[4]).nextDouble()
            );
        } catch (InputMismatchException e) {
            throw new IllegalArgumentException("illegal form");
        }
    }

    public Quaternion(double[] num){
        if(num.length!=4){
            throw new IllegalArgumentException("illegal group length");
        }
        s=num[0];
        x=num[1];
        y=num[2];
        z=num[3];
    }

    public Quaternion(double s,Vector3 vec){
        this(s, vec.getX(), vec.getY(),vec.getZ());
    }

    public double getS() {
        return s;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setValue(double s,double x,double y,double z){
        this.s=s;
        this.x=x;
        this.y=y;
        this.z=z;
    }

    public Quaternion setValueWithMcString(String s){
        String[] tmp =s.split("[,\\[\\]]");
        this.setValue(new Scanner(tmp[4]).nextDouble(),new Scanner(tmp[1]).nextDouble(),new Scanner(tmp[2]).nextDouble(),new Scanner(tmp[3]).nextDouble());
        return this;
    }

    public void setS(double s) {
        this.s = s;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("[%f,%f,%f,%f]",s,x,y,z);
    }

    public String toMcString(){
        return String.format("[%f,%f,%f,%f]",x,y,z,s);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Quaternion that)) return false;
        return Double.compare(s, that.s) == 0 && Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0 && Double.compare(z, that.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(s, x, y, z);
    }

    public Quaternion add(Quaternion num){
        return new Quaternion(this.s+num.s,this.x+num.x,this.y+num.y,this.z+num.z);
    }

    public Quaternion multiply(Quaternion num){
        double s=this.s*num.s-this.x*num.x-this.y*num.y-this.z*num.z;
        double x=this.s*num.x+this.x*num.s+this.y*num.z-this.z*num.y;
        double y=this.s*num.y+this.y*num.s+this.z*num.x-this.x*num.z;
        double z=this.s*num.z+this.z*num.s+this.x*num.y-this.y*num.x;
        return new Quaternion(s,x,y,z);
    }

    public Quaternion multiply(double factor){
        return new Quaternion(s*factor,x*factor,y*factor,z*factor);
    }

    public Quaternion conjugate(){
        return new Quaternion(s,-x,-y,-z);
    }

    public Quaternion inverse(){
        return this.conjugate().multiply(Math.pow(this.module(),-2));
    }

    public double module(){
        return Math.sqrt(s*s+x*x+y*y+z*z);
    }

    public Quaternion normalize(){
        return new Quaternion(s/module(),x/module(),y/module(),z/module());
    }

    public double toDouble(){
        if(x==0&&y==0&&z==0){
            return s;
        }else{
            throw new IllegalArgumentException("not a double");
        }
    }

    public Quaternionf toQuaternionf(){
        return new Quaternionf(this.x,this.y,this.z,this.s);
    }

    public static Quaternion trans(Vector3 v1,Vector3 v2){
        return trans(v1,v2,1);
    }

    public static Quaternion trans(Vector3 v1,Vector3 v2,double delta){
        Vector3 V=v1.normalize().cross(v2.normalize());
        double angle=delta*Math.acos(v1.dot(v2)/(v1.length()*v2.length()));
        if(V.length()==0){
            if(v1.dot(v2)>=0){
                return new Quaternion(1,0,0,0);
            }else{
                Vector3 axis = new Vector3(1, 0, 0);
                if (Math.abs(v1.normalize().dot(axis))==1) {
                    axis = new Vector3(0, 1, 0);
                }
                return new Quaternion(0, axis.getX(), axis.getY(), axis.getZ());
            }
        }
        V=V.normalize();
        return new Quaternion(
                Math.cos(angle/2),
                V.getX()*Math.sin(angle/2),
                V.getY()*Math.sin(angle/2),
                V.getZ()*Math.sin(angle/2)
        ).normalize();
    }

    public static Quaternion rotate(Vector3 normal,double angle){
        if(normal.length()<1e-8){
            return new Quaternion(1,0,0,0);
        }
        normal=normal.normalize();
        return new Quaternion(
                Math.cos(angle/2),
                normal.getX()*Math.sin(angle/2),
                normal.getY()*Math.sin(angle/2),
                normal.getZ()*Math.sin(angle/2)
        );
    }
}
