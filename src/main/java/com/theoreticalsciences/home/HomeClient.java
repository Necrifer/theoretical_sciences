package com.theoreticalsciences.home;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

/** Client side of Recreating Home: what the server last said about the checklist and about the open table. */
public final class HomeClient {
    private HomeClient() {}

    public static boolean unlocked;
    public static long[] given = new long[0];
    /** Why the open table cannot craft ("" if it can). */
    public static String blocked = "no_recipe";

    public static void state(HomePayloads.State s) {
        unlocked = s.unlocked();
        given = s.given();
    }

    public static void table(BlockPos pos, CompoundTag tag, String why) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof EmpyreanTableBlockEntity be) be.read(tag);
        blocked = why;
    }

    public static boolean complete() {
        if (given.length < HomeMaterials.IDS.size()) return false;
        for (long g : given) if (g < HomeMaterials.TARGET) return false;
        return true;
    }

    /** 4096, 262k, 1.05M, 33.5M, 2.1B: at most five characters. */
    public static String compact(long n) {
        if (n < 10_000) return Long.toString(n);
        if (n < 1_000_000) return n / 1000 + "k";
        if (n < 1_000_000_000L) return trim(n / 1e6) + "M";
        if (n < 1_000_000_000_000L) return trim(n / 1e9) + "B";
        return trim(n / 1e12) + "T";
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, v >= 100 ? "%.0f" : v >= 10 ? "%.1f" : "%.2f", Math.floor(v * 100) / 100);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }

    public static String full(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }
}
