package com.theoreticalsciences.compat.kubejs;

import com.theoreticalsciences.api.event.MachineRecipeEvent;
import com.theoreticalsciences.api.event.MultiblockFormedEvent;
import com.theoreticalsciences.api.event.ResearchUnlockedEvent;
import com.theoreticalsciences.machine.MachineBlockEntity;
import com.theoreticalsciences.recipe.QHRecipe;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

import java.util.UUID;

public final class QHKubeEvents {
    /** Base for machine events: {@code event.machineType}, {@code event.recipeId}, {@code event.pos}, {@code event.level}, {@code event.owner}. */
    public abstract static class MachineEvent implements KubeEvent {
        protected final MachineRecipeEvent e;

        MachineEvent(MachineRecipeEvent e) {
            this.e = e;
        }

        public MachineBlockEntity getMachine() {
            return e.getMachine();
        }

        public String getMachineType() {
            return e.getMachine().type.id();
        }

        public ResourceLocation getRecipeId() {
            return e.getRecipeId();
        }

        public QHRecipe getRecipe() {
            return e.getRecipe();
        }

        public BlockPos getPos() {
            return e.getMachine().getBlockPos();
        }

        @Nullable
        public Level getLevel() {
            return e.getMachine().getLevel();
        }

        @Nullable
        public ServerPlayer getOwner() {
            UUID id = e.getMachine().getOwner();
            Level l = getLevel();
            return id == null || l == null || l.getServer() == null ? null : l.getServer().getPlayerList().getPlayer(id);
        }
    }

    /** Cancellable: {@code event.cancel()} stops the machine from starting the recipe. */
    public static class RecipeStarted extends MachineEvent {
        RecipeStarted(MachineRecipeEvent.Start e) {
            super(e);
        }
    }

    public static class RecipeFinished extends MachineEvent {
        RecipeFinished(MachineRecipeEvent.Finish e) {
            super(e);
        }
    }

    public static class ResearchUnlocked implements KubeEvent {
        private final ResearchUnlockedEvent e;

        ResearchUnlocked(ResearchUnlockedEvent e) {
            this.e = e;
        }

        public ServerPlayer getPlayer() {
            return e.getPlayer();
        }

        public ResourceLocation getResearch() {
            return e.getResearch();
        }
    }

    public static class MultiblockChanged implements KubeEvent {
        private final MultiblockFormedEvent e;

        MultiblockChanged(MultiblockFormedEvent e) {
            this.e = e;
        }

        public boolean isFormed() {
            return e.isFormed();
        }

        public String getMachineType() {
            return e.getController().type.id();
        }

        public BlockPos getPos() {
            return e.getController().getBlockPos();
        }

        @Nullable
        public Level getLevel() {
            return e.getController().getLevel();
        }
    }

    private QHKubeEvents() {}
}
