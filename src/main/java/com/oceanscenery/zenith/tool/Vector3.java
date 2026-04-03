package com.oceanscenery.zenith.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Objects;
import java.util.Scanner;

public class Vector3 {
    private double x,y,z;
    public static final Vector3[] WORLD=new Vector3[]{new Vector3(1,0,0),new Vector3(0,1,0),new Vector3(0,0,1)};

    public static final StreamCodec<ByteBuf,Vector3> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.DOUBLE,Vector3::getX,
            ByteBufCodecs.DOUBLE,Vector3::getY,
            ByteBufCodecs.DOUBLE,Vector3::getZ,
            Vector3::new
    );

    public  Vector3(){}

    public Vector3(double n1,double n2,double n3){
        x=n1;
        y=n2;
        z=n3;
    }

    public Vector3(Vec3 vec){
        x=vec.x();
        y=vec.y;
        z=vec.z;
    }

    public Vector3(Vector3 vec){
        this.x=vec.x;
        this.y=vec.y;
        this.z=vec.z;
    }

    public Vector3(double[] num){
        if(num.length!=3){
            throw new IllegalArgumentException("invalid group length");
        }
        x=num[0];
        y=num[1];
        z=num[2];
    }

    public Vector3(String s){
        String[] value=s.split("[\\[\\],]");
        if(value.length!=4){
            throw new IllegalArgumentException("illegal form");
        }
        try{
            this.setX(new Scanner(value[1]).nextDouble()).setY(new Scanner(value[2]).nextDouble()).setZ(new Scanner(value[3]).nextDouble());
        } catch (InputMismatchException e) {
            throw new IllegalArgumentException("illegal form");
        }
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

    public Vector3 setX(double x) {
        this.x = x;
        return this;
    }

    public Vector3 setY(double y) {
        this.y = y;
        return this;
    }

    public Vector3 setZ(double z) {
        this.z = z;
        return this;
    }

    @Override
    public String toString() {
        return String.format("[%f,%f,%f]",x,y,z);
    }

    public double[] toArray(){
        return new double[]{x,y,z};
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vector3 vector3)) return false;
        return Double.compare(x, vector3.x) == 0 && Double.compare(y, vector3.y) == 0 && Double.compare(z, vector3.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    public double[] toGroup(){
        return new double[]{x, y, z};
    }

    public Vector3 multiply(double factor){
        return new Vector3(x*factor,y*factor,z*factor);
    }

    public Vector3 add(Vector3 v){
        return new Vector3(x+v.x,y+v.y,z+v.z);
    }

    public Vector3 subtract(Vector3 v){
        return new Vector3(this.x-v.x,this.y-v.y,this.z-v.z);
    }

    public Vector3 scale(double x,double y,double z){
        return new Vector3(this.x*x,this.y*y,this.z*z);
    }

    public double dot(Vector3 v){
        return x*v.x+y*v.y+z*v.z;
    }

    public Vector3 cross(Vector3 v){
        return new Vector3(
            y * v.z - z * v.y,
            z * v.x - x * v.z,
            x * v.y - y * v.x
        );
    }

    public double length(){
        return Math.sqrt(x*x+y*y+z*z);
    }

    public Vector3 normalize(){
        return new Vector3(x/length(),y/length(),z/length());
    }

    public Vec3 toVec3(){
        return new Vec3(x,y,z);
    }

    public Vector3f toVector3f(){
        return new Vector3f((float)this.x,(float)this.y,(float)this.z);
    }

    public static Vector3 transToVector3(Vec3 vec){
        return new Vector3(vec.x,vec.y,vec.z);
    }

    public Vector3 rotLerp(double delta,Vector3 start,Vector3 end){
        Quaternion transfer=Quaternion.trans(start,end,delta);
        Quaternion result=transfer.multiply(new Quaternion(0,start)).multiply(transfer.inverse());
        return new Vector3(result.getX(), result.getY(),result.getZ());
    }

    public Vector3 rot(Quaternion q){
        Quaternion result=q.multiply(new Quaternion(0,this)).multiply(q.inverse());
        return new Vector3(result.getX(),result.getY(),result.getZ());
    }

    public Vector3 VecInNewRefer(Vector3 pastX,Vector3 pastY,Vector3 pastZ,Vector3 thisX,Vector3 thisY,Vector3 thisZ){
        double x,y,z;
        x=(this.x*(pastX.dot(thisX))+this.y*(pastY.dot(thisX))+this.z*(pastZ.dot(thisX)));
        y=(this.x*(pastX.dot(thisY))+this.y*(pastY.dot(thisY))+this.z*(pastZ.dot(thisY)));
        z=(this.x*(pastX.dot(thisZ))+this.y*(pastY.dot(thisZ))+this.z*(pastZ.dot(thisZ)));
        return new Vector3(x,y,z);
    }

    public Vector3 VecInNewRefer(Vector3[] past,Vector3[] curr){
        if(past.length!=3 || curr.length!=3){
            throw new IllegalArgumentException("illegal reference length");
        }
        return this.VecInNewRefer(
                past[0],past[1],past[2],
                curr[0],curr[1],curr[2]
        );
    }

    public static Vector3 fromPitchAndYaw(double pitch,double yaw){
        yaw=Math.toRadians(yaw);
        pitch=Math.toRadians(pitch);
        return new Vector3(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));
    }

    public static Vector3[] getReferFromAngle(double pitch,double yaw){
        Vector3[] result=new Vector3[3];
        result[2]=fromPitchAndYaw(pitch,yaw).normalize();
        pitch=Math.toRadians(pitch);
        yaw=Math.toRadians(yaw);
        Vector3 tmp=new Vector3(0,1,0).cross(result[2]);
        result[0]=Math.abs(tmp.length())<1e-8?new Vector3(Math.cos(-yaw),0,-Math.sin(-yaw)):tmp.normalize();
        result[1]=result[2].cross(result[0]).normalize();
        return result;
    }

    public static Vector3[] getReferFromVec(Vector3 vecZ){
        Vector3[] result=new Vector3[3];
        result[2]=vecZ.normalize();
        Vector3 tmp=new Vector3(0,1,0).cross(result[2]);
        result[0]=Math.abs(tmp.length())<1e-8?new Vector3(1,0,0):tmp.normalize().multiply(-1);
        result[1]=result[2].cross(result[0]).normalize();
        return result;
    }

    public Vector3 applyOffset(double offset){
        if(this.length()!=0){
            return this.add(this.normalize().multiply(offset));
        }else{
            throw new IllegalArgumentException("试图给零向量添加长度偏移");
        }
    }

    public double radiusTo(Vector3 vec){
        return Math.acos(this.dot(vec)/this.length()/vec.length());
    }

    public static void main(String[] args) {
        System.out.println(PosUtil.calCenPos(10,Math.PI/4,0).normalize());
        System.out.println(new Vector3(0,0,-1).rot(Quaternion.rotate(new Vector3(0,1,0),5)).normalize());
    }
}
