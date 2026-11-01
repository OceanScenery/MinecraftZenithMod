package com.oceanscenery.agent.util;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;

public record MethodData(String owner, String name, String srgName, String desc) {
    public static MethodData of(String owner, String name, String srg, String desc) {
        return new MethodData(owner, name, srg, desc);
    }

    public static MethodData of(String owner, String name, String srg, ParamType desc) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (String arg : desc.params) {
            builder.append(arg);
        }
        builder.append(")");
        builder.append(desc.returnType);
        return new MethodData(owner, name, srg, builder.toString());
    }

    public boolean compareWith(MethodNode node) {
        return (this.name.equals(node.name) || this.srgName.equals(node.name))
            && this.desc.equals(node.desc);
    }

    public boolean compareWith(MethodInsnNode node) {
        return (this.name.equals(node.name) || this.srgName.equals(node.name))
            && this.desc.equals(node.desc);
    }

    public boolean compareWith(Handle handle) {
        return (this.name.equals(handle.getName()) || this.srgName.equals(handle.getName()))
            && this.desc.equals(handle.getDesc());
    }

    public boolean nameEquals(String name) {
        return this.name.equals(name) || this.srgName.equals(name);
    }

    public record ParamType(String[] params, String returnType) {
        public static final String VOID = "V";
        public static final String BOOL = "Z";
        public static final String INT = "I";
        public static final String DOUBLE = "D";
        public static final String FLOAT = "F";
        public static final String LONG = "J";

        public static String ofClass(String innerClassName) {
            return "L" + innerClassName + ";";
        }

        public static ParamType of(String... args) {
            int len = args.length;
            if (len == 0) {
                throw new IllegalStateException("Missing return value");
            }
            if (len == 1) {
                return new ParamType(new String[]{}, args[0]);
            }
            String[] p1 = Arrays.copyOfRange(args, 0, args.length - 1);
            return new ParamType(p1, args[args.length - 1]);
        }
    }
}
