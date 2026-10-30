package com.theoreticalsciences.machine;

/**
 * Shared GUI geometry for menus (server+client), screens and JEI.
 * <p>
 * Ordinary machines: tall fluid tanks either side of a 2-3 row item grid. Machines with many slots (the multiblocks,
 * GTNH-style: 16 item and 16 fluid inputs) use a compact form instead: a 4x4 grid of small tank cells, a 4x4 item grid,
 * then the outputs in 4-row columns, all four rows high.
 */
public final class MachineLayout {
    public static final int TOP = 17, AREA_H = 54;
    /** True for the compact many-slot form. */
    public final boolean big;
    /** Height of the machine area (energy bar), tank cell size. */
    public final int areaH, tankW, tankH;
    public final int width, height, invX, invY;
    public final int energyX = 8, energyY = TOP;
    public final int[] fluidInX, fluidInY, itemInX, itemInY, itemOutX, itemOutY, fluidOutX, fluidOutY;
    public final int arrowX, arrowY;

    public MachineLayout(MachineType t) {
        big = t.itemInputs > 9 || t.fluidInputs > 4 || t.itemOutputs > 9 || t.fluidOutputs > 4;
        areaH = big ? 72 : AREA_H;
        tankW = 16;
        tankH = big ? 16 : AREA_H;
        int x = 26;
        fluidInX = new int[t.fluidInputs];
        fluidInY = new int[t.fluidInputs];
        x = big ? cells(t.fluidInputs, x, fluidInX, fluidInY) : bars(t.fluidInputs, x, fluidInX, fluidInY);
        x += 4;
        itemInX = new int[t.itemInputs];
        itemInY = new int[t.itemInputs];
        x = big ? cells(t.itemInputs, x, itemInX, itemInY, 1) : grid(t.itemInputs, x, itemInX, itemInY);
        arrowX = x + 4;
        arrowY = TOP + (areaH - 16) / 2;
        x = arrowX + 30;
        itemOutX = new int[t.itemOutputs];
        itemOutY = new int[t.itemOutputs];
        x = big ? cells(t.itemOutputs, x, itemOutX, itemOutY, 1) : grid(t.itemOutputs, x, itemOutX, itemOutY);
        x += 4;
        fluidOutX = new int[t.fluidOutputs];
        fluidOutY = new int[t.fluidOutputs];
        x = big ? cells(t.fluidOutputs, x, fluidOutX, fluidOutY) : bars(t.fluidOutputs, x, fluidOutX, fluidOutY);
        width = Math.max(176, x + 8);
        invX = (width - 162) / 2 + 1;
        invY = TOP + areaH + 23;
        height = invY + 82;
    }

    /** x, y of tank i (inputs first, then outputs). */
    public int tankX(MachineType t, int i) {
        return i < t.fluidInputs ? fluidInX[i] : fluidOutX[i - t.fluidInputs];
    }

    public int tankY(MachineType t, int i) {
        return i < t.fluidInputs ? fluidInY[i] : fluidOutY[i - t.fluidInputs];
    }

    /** Tall tanks side by side. */
    private static int bars(int n, int x, int[] xs, int[] ys) {
        for (int i = 0; i < n; i++, x += 18) {
            xs[i] = x;
            ys[i] = TOP;
        }
        return x;
    }

    /** Small 16x16 tank cells, four to a column. */
    private static int cells(int n, int x, int[] xs, int[] ys) {
        return cells(n, x, xs, ys, 0);
    }

    /** Cells (or slots, offset 1 for the slot's inner square) in columns of four, filled row by row. */
    private static int cells(int n, int x, int[] xs, int[] ys, int off) {
        if (n == 0) return x;
        int cols = (n + 3) / 4;
        for (int i = 0; i < n; i++) {
            xs[i] = x + off + (i % cols) * 18;
            ys[i] = TOP + off + (i / cols) * 18;
        }
        return x + cols * 18;
    }

    /** Lays out n slots column-major in up to 3 rows; returns the x after the grid. */
    private static int grid(int n, int x, int[] xs, int[] ys) {
        if (n == 0) return x;
        if (n == 1) {
            xs[0] = x + 1;
            ys[0] = TOP + 19;
            return x + 18;
        }
        int rows = n > 6 ? 3 : 2;
        int cols = (n + rows - 1) / rows;
        for (int i = 0; i < n; i++) {
            xs[i] = x + 1 + (i / rows) * 18;
            ys[i] = rows == 3 ? TOP + 1 + (i % 3) * 18 : TOP + 10 + (i % 2) * 18;
        }
        return x + cols * 18;
    }
}
