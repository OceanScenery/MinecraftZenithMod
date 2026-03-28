package com.oceanscenery.zenith.tool;

import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

import java.util.UUID;

public class PosUtil {
    public static Vector3 calPos(double distance,int progress,double angle){
        Vector3 origin=null;
        double a=distance/2;
        double b=Math.min(distance/4,10);
        double theta=Mth.PI-Mth.TWO_PI*(double)progress/ZenithProjectile.STAGE_COUNT;
        double z=a*Math.cos(theta)+a;
        double x=b*Math.sin(theta)*Math.cos(angle);
        double y=b*Math.sin(theta)*Math.sin(angle);
        return new Vector3(x,y,z);
    }

    public static Vector3 calPos(double distance,double theta,double angle){
        double a=distance/2;
        double b=Math.min(distance/4,10);
        theta=Mth.PI-theta;
        double z=a*Math.cos(theta)+a;
        double x=b*Math.sin(theta)*Math.cos(angle);
        double y=b*Math.sin(theta)*Math.sin(angle);
        return new Vector3(x,y,z);
    }

    public static Vector3 calCenPos(double distance,double theta,double angle){
        double a=distance/2;
        double b=Math.min(distance/4,10);
        theta=Mth.PI-theta;
        double z=a*Math.cos(theta);
        double x=b*Math.sin(theta)*Math.cos(angle);
        double y=b*Math.sin(theta)*Math.sin(angle);
        return new Vector3(x,y,z);
    }

    public static double getPolar(double process_angle,double distance){
        double a=distance/2;
        double b=Math.min(distance/4,10);
        return Math.atan2(b*Math.sin(process_angle),a*Math.cos(process_angle));
    }

    public static Vector3 lerp(Vector3 past,Vector3 curr,double partial){
        return past.multiply(1-partial).add(curr.multiply(partial));
    }

    public static void main(String[] args) {
        for(int i=0;i<=20;i++){
            System.out.println(calPos(20,i,0));
        }
    }

    public static class Rotation{
        private double yaw;
        private double pitch;
        public Rotation(){
            this.yaw=0;
            this.pitch=0;
        }
        public Rotation(double pitch,double yaw){
            this.pitch=pitch;
            this.yaw=yaw;
        }

        public double getPitch() {
            return pitch;
        }

        public double getYaw() {
            return yaw;
        }

        public void setPitch(double pitch) {
            this.pitch = pitch;
        }

        public void setYaw(double yaw) {
            this.yaw = yaw;
        }
    }

    public static final StreamCodec<ByteBuf,Rotation> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.DOUBLE,Rotation::getPitch,
            ByteBufCodecs.DOUBLE,Rotation::getYaw,
            Rotation::new
    );
}
