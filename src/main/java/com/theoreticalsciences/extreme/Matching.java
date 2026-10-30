package com.theoreticalsciences.extreme;

import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/** Which grid stack fills which requirement (each stack used once): Kuhn's augmenting paths. */
public final class Matching {
    /** For each requirement, the index of the stack it takes; null if they cannot all be filled. */
    public static int[] assign(List<? extends Predicate<ItemStack>> need, List<ItemStack> stacks) {
        int n = need.size(), m = stacks.size();
        if (n > m) return null;
        boolean[][] ok = new boolean[n][m];
        for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) ok[i][j] = need.get(i).test(stacks.get(j));
        int[] owner = new int[m];
        Arrays.fill(owner, -1);
        for (int i = 0; i < n; i++) if (!augment(i, ok, owner, new boolean[m])) return null;
        int[] res = new int[n];
        for (int j = 0; j < m; j++) if (owner[j] >= 0) res[owner[j]] = j;
        return res;
    }

    private static boolean augment(int i, boolean[][] ok, int[] owner, boolean[] seen) {
        for (int j = 0; j < owner.length; j++) {
            if (!ok[i][j] || seen[j]) continue;
            seen[j] = true;
            if (owner[j] < 0 || augment(owner[j], ok, owner, seen)) {
                owner[j] = i;
                return true;
            }
        }
        return false;
    }

    private Matching() {}
}
