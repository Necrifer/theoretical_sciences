package com.theoreticalsciences.machine;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * FE buffer that counts in 64-bit internally (so machines can hold far more than the 2.1 billion FE an ordinary int
 * allows), while still speaking the normal int-based FE interface to cables and other mods. Internal helpers bypass
 * the external receive/extract limits.
 */
public class QHEnergyStorage implements IEnergyStorage {
    protected long energy, capacity;
    protected long maxReceive, maxExtract;
    private final Runnable onChange;

    public QHEnergyStorage(long capacity, long maxReceive, long maxExtract, Runnable onChange) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.onChange = onChange;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        if (maxReceive <= 0 || amount <= 0) return 0;
        long r = Math.min(capacity - energy, Math.min(maxReceive, amount));
        if (r <= 0) return 0;
        if (!simulate) {
            energy += r;
            onChange.run();
        }
        return (int) r;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        if (maxExtract <= 0 || amount <= 0) return 0;
        long r = Math.min(energy, Math.min(maxExtract, amount));
        if (r <= 0) return 0;
        if (!simulate) {
            energy -= r;
            onChange.run();
        }
        return (int) r;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, energy);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    public long stored() {
        return energy;
    }

    public long capacity() {
        return capacity;
    }

    public void setCapacity(long c) {
        capacity = Math.max(1, c);
        if (energy > capacity) energy = capacity;
    }

    public void setLimits(long receive, long extract) {
        maxReceive = receive;
        maxExtract = extract;
    }

    public void consume(long amount) {
        energy = Math.max(0, energy - amount);
        onChange.run();
    }

    public long generate(long amount) {
        if (amount <= 0) return 0;               // round 35: a broken physics model's negative rate never drains the buffer
        long added = Math.min(capacity - energy, amount);
        energy += added;
        if (added > 0) onChange.run();
        return added;
    }

    /** Pulls up to {@code amount} straight out (used by entangled links and storage cells). */
    public long drain(long amount) {
        long d = Math.min(energy, amount);
        if (d > 0) {
            energy -= d;
            onChange.run();
        }
        return d;
    }

    public long fill(long amount) {
        return generate(amount);
    }

    public void setRaw(long value) {
        energy = Math.max(0, Math.min(capacity, value));
    }
}
