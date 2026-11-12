package com.theoreticalsciences.compat.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import velrondevs.botania.api.BotaniaCapabilities;
import velrondevs.botania.api.mana.ManaReceiver;

/** Botania's side of {@link Magic}: only loaded when Botania is installed. */
final class BotaniaMana {
    private BotaniaMana() {}

    /** The Mana Port as Botania sees it: a receiver that takes bursts from spreaders (and mana from Sparks' pools). */
    private record Port(com.theoreticalsciences.machine.ManaPortBlockEntity be) implements ManaReceiver {
        @Override
        public net.minecraft.world.level.Level getManaReceiverLevel() {
            return be.getLevel();
        }

        @Override
        public BlockPos getManaReceiverPos() {
            return be.getBlockPos();
        }

        @Override
        public int getCurrentMana() {
            return be.mana();
        }

        @Override
        public boolean isFull() {
            return be.mana() >= com.theoreticalsciences.machine.ManaPortBlockEntity.CAPACITY;
        }

        @Override
        public void receiveMana(int mana) {
            be.add(mana);
        }

        @Override
        public boolean canReceiveManaFromBursts() {
            return true;
        }
    }

    static void registerPort(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(BotaniaCapabilities.MANA_RECEIVER, com.theoreticalsciences.registry.QHBlockEntities.MANA_PORT.get(), (be, side) -> new Port(be));
    }

    /** The mana held by everything that stands against the machine (the mod's own Mana Ports are counted by Magic, not here). */
    static long stored(ServerLevel level, BlockPos pos) {
        long sum = 0;
        for (Direction d : Direction.values()) {
            ManaReceiver r = level.getCapability(BotaniaCapabilities.MANA_RECEIVER, pos.relative(d), d.getOpposite());
            if (r != null && !(r instanceof Port)) sum += Math.max(0, r.getCurrentMana());
        }
        return sum;
    }

    static boolean drain(ServerLevel level, BlockPos pos, int amount) {
        if (stored(level, pos) < amount) return false;
        int left = amount;
        for (Direction d : Direction.values()) {
            if (left <= 0) break;
            ManaReceiver r = level.getCapability(BotaniaCapabilities.MANA_RECEIVER, pos.relative(d), d.getOpposite());
            if (r == null || r instanceof Port) continue;
            int take = Math.min(left, Math.max(0, r.getCurrentMana()));
            if (take > 0) {
                r.receiveMana(-take);          // a pool takes a negative amount as a withdrawal, as Botania's own tablets do
                left -= take;
            }
        }
        return left <= 0;
    }
}
