package com.theoreticalsciences.endgame;

/**
 * Round 32: an endgame item that runs on Eschaton Charge (see {@link EschatonCharge}). It only works while it holds
 * some; an Eschaton Cell fills it up again.
 */
public interface EschatonPowered {
    /** How much charge it holds when full. */
    int maxCharge();
}
