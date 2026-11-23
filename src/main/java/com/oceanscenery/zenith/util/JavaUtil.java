package com.oceanscenery.zenith.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.function.Supplier;

public class JavaUtil {
    public static final Holder<Unsafe> UNSAFE = getUs();
    public static final Holder<MethodHandles.Lookup> IMPL = getImpl();

    public static class Holder<T> {
        private T value = null;
        private final Supplier<T> initialize;

        public Holder(Supplier<T> initialize) {
            this.initialize = initialize;
        }

        public static <T> Holder<T> empty() {
            return new Holder<>(() -> null);
        }

        public Supplier<T> get() {
            return value == null ? () -> (value = initialize.get()) : () -> value;
        }
    }

    private static Holder<Unsafe> getUs() {
        try {
            Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeType.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            return new Holder<>(() -> (Unsafe) unsafe);
        } catch (Throwable t) {
            return Holder.empty();
        }
    }

    private static Holder<MethodHandles.Lookup> getImpl() {
        try {
            Class<?> lookupClass = Class.forName("java.lang.invoke.MethodHandles$Lookup");
            Field f = lookupClass.getDeclaredField("IMPL_LOOKUP");
            Unsafe unsafe = UNSAFE.get().get();
            Object base = unsafe.staticFieldBase(f);
            long off = unsafe.staticFieldOffset(f);
            return new Holder<>(() -> (MethodHandles.Lookup) unsafe.getObject(base, off));
        } catch (Throwable t) {
            return Holder.empty();
        }
    }
}
